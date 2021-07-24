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

package com.microsoft.applicationinsights.benchmark.analyzer;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;

@SuppressWarnings("SystemOut")
public class SimpleAnalyzer {

  private static final boolean INCLUDE_OPEN_TELEMETRY_AGENT = false;

  private static final Node syntheticRootNode = new Node("");
  private static int totalSamples = 0;

  public static void main(String[] args) throws Exception {
    File jfrFile = new File("benchmark-overhead-jmh/recording.jfr");
    List<RecordedEvent> events =
        RecordingFile.readAllEvents(jfrFile.toPath()).stream()
            .filter(e -> e.getEventType().getName().equals("jdk.ExecutionSample"))
            .collect(Collectors.toList());

    for (RecordedEvent event : events) {
      totalSamples++;
      processStackTrace(event.getStackTrace());
    }

    int totalAgentSamples = 0;
    for (Node rootNode : syntheticRootNode.getOrderedChildNodes()) {
      totalAgentSamples += rootNode.count;
    }

    System.out.println("Total samples: " + totalSamples);
    System.out.print("Total agent samples: " + totalAgentSamples);
    System.out.format(" (%.2f%%)%n", 100 * totalAgentSamples / (double) totalSamples);
    System.out.println();
    for (Node rootNode : syntheticRootNode.getOrderedChildNodes()) {
      printNode(rootNode, 0);
    }
  }

  private static void printNode(Node node, int indent) {
    for (int i = 0; i < indent; i++) {
      System.out.print("  ");
    }
    System.out.format("%3d %s%n", node.count, node.frame);
    for (Node childNode : node.getOrderedChildNodes()) {
      printNode(childNode, indent + 1);
    }
  }

  private static void processStackTrace(RecordedStackTrace stackTrace) {
    boolean analyze = false;
    int analyzeFromIndex = 0;
    List<RecordedFrame> frames = stackTrace.getFrames();
    for (int i = frames.size() - 1; i >= 0; i--) {
      RecordedFrame frame = frames.get(i);
      RecordedMethod method = frame.getMethod();
      if (isAgentMethod(method)) {
        analyze = true;
        analyzeFromIndex = Math.min(i + 1, frames.size() - 1);
        break;
      }
    }
    if (!analyze) {
      return;
    }
    Node node = syntheticRootNode;
    for (int i = analyzeFromIndex; i >= 0; i--) {
      RecordedFrame frame = frames.get(i);
      RecordedMethod method = frame.getMethod();
      String stackTraceElement = getStackTraceElement(method, frame);
      node = node.recordChildSample(stackTraceElement);
    }
  }

  private static boolean isAgentMethod(RecordedMethod method) {
    if (INCLUDE_OPEN_TELEMETRY_AGENT && isOpenTelemetryAgentMethod(method)) {
      return true;
    }
    String className = method.getType().getName();
    return className.startsWith("com.microsoft.applicationinsights.")
        && !className.startsWith("com.microsoft.applicationinsights.benchmark.");
  }

  private static boolean isOpenTelemetryAgentMethod(RecordedMethod method) {
    String className = method.getType().getName();
    String methodName = method.getName();
    return className.startsWith("io.opentelemetry.javaagent.")
        && !className.startsWith("io.opentelemetry.javaagent.benchmark.")
        // this shows up in stack traces because it's part of the filter chain
        && !(className.equals(
                "io.opentelemetry.javaagent.instrumentation.springwebmvc.HandlerMappingResourceNameFilter")
            && methodName.equals("doFilter"));
  }

  private static String getStackTraceElement(RecordedMethod method, RecordedFrame frame) {
    return method.getType().getName()
        + "."
        + method.getName()
        + "() line: "
        + frame.getLineNumber();
  }

  private static class Node {

    private final String frame;
    private final Map<String, Node> childNodes = new HashMap<>();
    private int count;

    private Node(String frame) {
      this.frame = frame;
    }

    private Node recordChildSample(String stackTraceElement) {
      Node childNode = childNodes.get(stackTraceElement);
      if (childNode == null) {
        childNode = new Node(stackTraceElement);
        childNodes.put(stackTraceElement, childNode);
      }
      childNode.count++;
      return childNode;
    }

    private List<Node> getOrderedChildNodes() {
      return childNodes.values().stream()
          .sorted(Comparator.comparingInt(Node::getCount).reversed())
          .collect(Collectors.toList());
    }

    private int getCount() {
      return count;
    }
  }

  private SimpleAnalyzer() {}
}
