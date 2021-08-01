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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.lock.qual.GuardedBy;

public class NetworkStatsbeat extends BaseStatsbeat {

  private static final String REQUEST_SUCCESS_COUNT_METRIC_NAME = "Request Success Count";
  private static final String REQUEST_FAILURE_COUNT_METRIC_NAME = "Requests Failure Count ";
  private static final String REQUEST_DURATION_METRIC_NAME = "Request Duration";
  private static final String RETRY_COUNT_METRIC_NAME = "Retry Count";
  private static final String THROTTLE_COUNT_METRIC_NAME = "Throttle Count";
  private static final String EXCEPTION_COUNT_METRIC_NAME = "Exception Count";

  private static final String INSTRUMENTATION_CUSTOM_DIMENSION = "instrumentation";

  @GuardedBy("lock")
  private IntervalMetrics current;

  private final Object lock = new Object();

  NetworkStatsbeat(CustomDimensions customDimensions) {
    super(customDimensions);
    current = new IntervalMetrics();
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    IntervalMetrics local;
    synchronized (lock) {
      local = current;
      current = new IntervalMetrics();
    }

    // send instrumentation as an UTF-8 string
    String instrumentation = String.valueOf(Instrumentations.encode(local.instrumentationList));

    if (local.requestSuccessCount++ != 0) {
      TelemetryItem requestSuccessCountSt =
          createStatsbeatTelemetry(
              telemetryClient, REQUEST_SUCCESS_COUNT_METRIC_NAME, local.requestSuccessCount++);
      TelemetryUtil.getProperties(requestSuccessCountSt.getData().getBaseData())
          .put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
      telemetryClient.trackStatsbeatAsync(requestSuccessCountSt);
    }

    if (local.requestFailureCount++ != 0) {
      TelemetryItem requestFailureCountSt =
          createStatsbeatTelemetry(
              telemetryClient, REQUEST_FAILURE_COUNT_METRIC_NAME, local.requestFailureCount++);
      TelemetryUtil.getProperties(requestFailureCountSt.getData().getBaseData())
          .put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
      telemetryClient.trackStatsbeatAsync(requestFailureCountSt);
    }

    double durationAvg = local.getRequestDurationAvg();
    if (durationAvg != 0) {
      TelemetryItem requestDurationSt =
          createStatsbeatTelemetry(telemetryClient, REQUEST_DURATION_METRIC_NAME, durationAvg);
      TelemetryUtil.getProperties(requestDurationSt.getData().getBaseData())
          .put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
      telemetryClient.trackStatsbeatAsync(requestDurationSt);
    }

    if (local.retryCount++ != 0) {
      TelemetryItem retryCountSt =
          createStatsbeatTelemetry(telemetryClient, RETRY_COUNT_METRIC_NAME, local.retryCount++);
      TelemetryUtil.getProperties(retryCountSt.getData().getBaseData())
          .put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
      telemetryClient.trackStatsbeatAsync(retryCountSt);
    }

    if (local.throttlingCount++ != 0) {
      TelemetryItem throttleCountSt =
          createStatsbeatTelemetry(
              telemetryClient, THROTTLE_COUNT_METRIC_NAME, local.throttlingCount++);
      TelemetryUtil.getProperties(throttleCountSt.getData().getBaseData())
          .put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
      telemetryClient.trackStatsbeatAsync(throttleCountSt);
    }

    if (local.exceptionCount++ != 0) {
      TelemetryItem exceptionCountSt =
          createStatsbeatTelemetry(
              telemetryClient, EXCEPTION_COUNT_METRIC_NAME, local.exceptionCount++);
      TelemetryUtil.getProperties(exceptionCountSt.getData().getBaseData())
          .put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
      telemetryClient.trackStatsbeatAsync(exceptionCountSt);
    }
  }

  // this is used by Exporter
  public void addInstrumentation(String instrumentation) {
    synchronized (lock) {
      current.instrumentationList.add(instrumentation);
    }
  }

  public void incrementRequestSuccessCount(long duration) {
    synchronized (lock) {
      current.requestSuccessCount++;
      current.totalRequestDuration += duration;
    }
  }

  public void incrementRequestFailureCount() {
    synchronized (lock) {
      current.requestFailureCount++;
    }
  }

  public void incrementRetryCount() {
    synchronized (lock) {
      current.retryCount++;
    }
  }

  public void incrementThrottlingCount() {
    synchronized (lock) {
      current.throttlingCount++;
    }
  }

  void incrementExceptionCount() {
    synchronized (lock) {
      current.exceptionCount++;
    }
  }

  // only used by tests
  long getInstrumentation() {
    synchronized (lock) {
      return Instrumentations.encode(current.instrumentationList);
    }
  }

  // only used by tests
  long getRequestSuccessCount() {
    synchronized (lock) {
      return current.requestSuccessCount;
    }
  }

  // only used by tests
  long getRequestFailureCount() {
    synchronized (lock) {
      return current.requestFailureCount;
    }
  }

  // only used by tests
  double getRequestDurationAvg() {
    synchronized (lock) {
      return current.getRequestDurationAvg();
    }
  }

  // only used by tests
  long getRetryCount() {
    synchronized (lock) {
      return current.retryCount;
    }
  }

  // only used by tests
  long getThrottlingCount() {
    synchronized (lock) {
      return current.throttlingCount;
    }
  }

  // only used by tests
  long getExceptionCount() {
    synchronized (lock) {
      return current.exceptionCount;
    }
  }

  // only used by tests
  Set<String> getInstrumentationList() {
    synchronized (lock) {
      return current.instrumentationList;
    }
  }

  // always used under lock
  private static class IntervalMetrics {
    private final Set<String> instrumentationList = new HashSet<>();
    private long requestSuccessCount;
    private long requestFailureCount;
    // request duration count only counts request success.
    private long totalRequestDuration; // duration in milliseconds
    private long retryCount;
    private long throttlingCount;
    private long exceptionCount;

    private double getRequestDurationAvg() {
      double sum = totalRequestDuration;
      if (requestSuccessCount != 0) {
        return sum / requestSuccessCount;
      }

      return sum;
    }
  }
}
