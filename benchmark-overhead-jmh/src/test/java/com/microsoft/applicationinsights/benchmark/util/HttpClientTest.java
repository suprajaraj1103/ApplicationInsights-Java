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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HttpClientTest {

  @Test
  public void testCombinations() throws IOException {
    byte[] buffer = new byte[8192];
    String response = "HTTP/1.1 200 OK\r\nContent-Length: 12\r\n\r\n123456789012";
    for (int i = 1; i < response.length() - 1; i++) {
      String part1 = response.substring(0, i);
      String part2 = response.substring(i);
      List<byte[]> chunks = new ArrayList<>();
      chunks.add(part1.getBytes());
      chunks.add(part2.getBytes());
      InputStream in = new TestInputStream(chunks);
      HttpClient.drain(in, buffer);
      String string = new String(buffer, 0, response.length());
      assertThat(string).isEqualTo(response);
    }
  }

  private static class TestInputStream extends InputStream {

    private final Iterator<byte[]> chunks;

    private TestInputStream(List<byte[]> chunks) {
      this.chunks = chunks.iterator();
    }

    @Override
    public int read() {
      return 0;
    }

    @Override
    public int read(byte[] buffer, int off, int len) {
      if (!chunks.hasNext()) {
        return -1;
      }
      byte[] next = chunks.next();
      System.arraycopy(next, 0, buffer, off, next.length);
      return next.length;
    }
  }
}
