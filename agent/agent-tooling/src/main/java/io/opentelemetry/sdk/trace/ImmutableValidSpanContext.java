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

package io.opentelemetry.sdk.trace;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

@AutoValue
// temporary until OpenTelemetry SDK 1.6.0 is release
// which contains https://github.com/open-telemetry/opentelemetry-java/pull/3564
abstract class ImmutableValidSpanContext implements SpanContext {

  /**
   * This method is provided as an optimization when {@code traceIdHex} and {@code spanIdHex} have
   * already been validated. Only use this method if you are sure these have both been validated,
   * e.g. when using {@code traceIdHex} from a parent {@link SpanContext} and {@code spanIdHex} from
   * an {@link IdGenerator}.
   */
  static SpanContext createBypassingValidation(
      String traceIdHex, String spanIdHex, TraceFlags traceFlags, TraceState traceState) {
    return new AutoValue_ImmutableValidSpanContext(
        traceIdHex, spanIdHex, traceFlags, traceState, /* remote= */ false);
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
