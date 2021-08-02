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

package com.microsoft.applicationinsights.benchmark.jackson.duration2;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;

@JsonSerialize(using = DurationData.DurationDataSerializer.class)
public class DurationData {

  private static final int ZERO_CHAR = '0';

  private final long nanoseconds;

  public DurationData(long nanoseconds) {
    this.nanoseconds = nanoseconds;
  }

  public long getNanoseconds() {
    return nanoseconds;
  }

  public static class DurationDataSerializer extends JsonSerializer<DurationData> {

    private static final long NANOSECONDS_PER_DAY = DAYS.toNanos(1);
    private static final long NANOSECONDS_PER_HOUR = HOURS.toNanos(1);
    private static final long NANOSECONDS_PER_MINUTE = MINUTES.toNanos(1);
    private static final long NANOSECONDS_PER_SECOND = SECONDS.toNanos(1);

    @Override
    public void serialize(DurationData value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      long remainingNanos = value.nanoseconds;

      if (remainingNanos < NANOSECONDS_PER_MINUTE) {
        int seconds = (int) (remainingNanos / NANOSECONDS_PER_SECOND);
        remainingNanos = remainingNanos % NANOSECONDS_PER_SECOND;

        // optimization for common case
        // TODO (trask) this doesn't seem any faster
        gen.writeRawValue("");

        gen.writeRaw("\"00:00:");
        writeTwoDigits(gen, seconds);
        gen.writeRaw('.');
        writeSixDigits(gen, (int) NANOSECONDS.toMicros(remainingNanos));
        gen.writeRaw('"');
        return;
      }

      int days = (int) (remainingNanos / NANOSECONDS_PER_DAY);
      remainingNanos = remainingNanos % NANOSECONDS_PER_DAY;

      int hours = (int) (remainingNanos / NANOSECONDS_PER_HOUR);
      remainingNanos = remainingNanos % NANOSECONDS_PER_HOUR;

      int minutes = (int) (remainingNanos / NANOSECONDS_PER_MINUTE);
      remainingNanos = remainingNanos % NANOSECONDS_PER_MINUTE;

      int seconds = (int) (remainingNanos / NANOSECONDS_PER_SECOND);
      remainingNanos = remainingNanos % NANOSECONDS_PER_SECOND;

      // TODO(trask): without this, an exception is thrown
      //  "Can not write a field name, expecting a value"
      //  is this the right way to work around this exception?
      // TODO(trask): or should this be using SerializableString?
      gen.writeRawValue("");

      gen.writeRaw('"');
      appendDaysHoursMinutesSeconds(gen, days, hours, minutes, seconds);
      writeSixDigits(gen, (int) NANOSECONDS.toMicros(remainingNanos));
      gen.writeRaw('"');
    }

    private static void appendDaysHoursMinutesSeconds(
        JsonGenerator gen, int days, int hours, int minutes, int seconds) throws IOException {
      if (days > 0) {
        gen.writeRaw(Long.toString(days)); // this is a rare case, no need to optimize
        gen.writeRaw('.');
      }
      writeTwoDigits(gen, hours);
      gen.writeRaw(':');
      writeTwoDigits(gen, minutes);
      gen.writeRaw(':');
      writeTwoDigits(gen, seconds);
      gen.writeRaw('.');
    }

    static void writeSixDigits(JsonGenerator gen, int part) throws IOException {
      gen.writeRaw((char) (ZERO_CHAR + (part / 100000)));
      writeFiveDigits(gen, part % 100000);
    }

    static void writeFiveDigits(JsonGenerator gen, int part) throws IOException {
      gen.writeRaw((char) (ZERO_CHAR + (part / 10000)));
      writeFourDigits(gen, part % 10000);
    }

    static void writeFourDigits(JsonGenerator gen, int part) throws IOException {
      gen.writeRaw((char) (ZERO_CHAR + (part / 1000)));
      writeThreeDigits(gen, part % 1000);
    }

    static void writeThreeDigits(JsonGenerator gen, int part) throws IOException {
      gen.writeRaw((char) (ZERO_CHAR + (part / 100)));
      writeTwoDigits(gen, part % 100);
    }

    static void writeTwoDigits(JsonGenerator gen, int part) throws IOException {
      gen.writeRaw((char) (ZERO_CHAR + (part / 10)));
      gen.writeRaw((char) (ZERO_CHAR + (part % 10)));
    }
  }
}
