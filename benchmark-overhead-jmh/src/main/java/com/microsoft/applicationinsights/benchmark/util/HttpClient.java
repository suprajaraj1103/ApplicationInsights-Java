/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.benchmark.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// TODO (trask) switch to HttpURLConnection, but that requires a way to disable HttpURLConnection
//  instrumentation during benchmarking
public class HttpClient {

  private static final int LOWERCASE_OFFSET = 'a' - 'A';

  private static final byte[] CONTENT_LENGTH_BYTES = "CONTENT-LENGTH:".getBytes(UTF_8);

  private final Socket socket;
  private final OutputStream out;
  private final InputStream in;
  private final byte[] requestBytes;

  private final byte[] buffer = new byte[8192];

  public HttpClient(String host, int port, String path) throws IOException {
    socket = new Socket(host, port);
    out = socket.getOutputStream();
    in = socket.getInputStream();
    String request = "GET " + path + " HTTP/1.1\r\nHost: " + host + ":" + port + "\r\n\r\n";
    requestBytes = request.getBytes(UTF_8);
  }

  public void close() throws IOException {
    out.close();
    in.close();
    socket.close();
  }

  public void execute() throws IOException {
    out.write(requestBytes);
    drain(in, buffer);
  }

  // visible for testing
  static void drain(InputStream in, byte[] buffer) throws IOException {
    int startLookingFromIndex = 0;
    int bytesRead = 0;
    int headerLen = 0;
    List<Integer> possibleHeaderPositions = new ArrayList<>(2);
    while (headerLen == 0) {
      bytesRead += in.read(buffer, bytesRead, buffer.length - bytesRead);
      for (int i = startLookingFromIndex; i < bytesRead - 2; i++) {
        if (buffer[i] == '\r') {
          int nextLineStartPosition = i + 2;
          byte b = buffer[nextLineStartPosition];
          if (b == '\r') {
            // found end of headers
            headerLen = nextLineStartPosition + 2;
            break;
          } else if (b == 'C' || b == 'c') {
            possibleHeaderPositions.add(nextLineStartPosition);
          }
        }
      }
      startLookingFromIndex = Math.max(bytesRead - 2, 0);
    }
    int contentLength = getContentLength(buffer, possibleHeaderPositions);
    while (bytesRead < headerLen + contentLength) {
      bytesRead += in.read(buffer, bytesRead, buffer.length - bytesRead);
    }
  }

  private static int getContentLength(byte[] buffer, List<Integer> possibleHeaderPositions) {
    for (int startIndex : possibleHeaderPositions) {
      if (isContentLengthHeader(buffer, startIndex)) {
        String contentLength = getRestOfLine(buffer, startIndex + CONTENT_LENGTH_BYTES.length);
        return Integer.parseInt(contentLength.trim());
      }
    }
    throw new IllegalStateException("Did not find Content-Length header");
  }

  private static boolean isContentLengthHeader(byte[] buffer, int startIndex) {
    for (int i = 0; i < CONTENT_LENGTH_BYTES.length; i++) {
      byte current = buffer[startIndex + i];
      byte expected = CONTENT_LENGTH_BYTES[i];
      if (current != expected && current != expected + LOWERCASE_OFFSET) {
        return false;
      }
    }
    return true;
  }

  private static String getRestOfLine(byte[] buffer, int startIndex) {
    StringBuilder value = new StringBuilder();
    for (int i = startIndex; i < buffer.length; i++) {
      byte b = buffer[i];
      if (b == '\r') {
        break;
      }
      value.append((char) b);
    }
    return value.toString();
  }
}
