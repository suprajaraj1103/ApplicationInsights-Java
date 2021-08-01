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

package com.microsoft.applicationinsights.benchmark.jackson.time4;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@JsonSerialize(using = TimeData.TimeDataSerializer.class)
public class TimeData {

  private final long epochMillis;

  public TimeData(long epochMillis) {
    this.epochMillis = epochMillis;
  }

  public static class TimeDataSerializer extends JsonSerializer<TimeData> {

    @Override
    public void serialize(TimeData value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      StringBuilder sb = new StringBuilder(24);

      OffsetDateTime offsetDateTime =
          Instant.ofEpochMilli(value.epochMillis).atOffset(ZoneOffset.UTC);
      sb.append(offsetDateTime.getYear());
      sb.append('-');
      writeTwoDigits(sb, offsetDateTime.getMonthValue());
      sb.append('-');
      writeTwoDigits(sb, offsetDateTime.getDayOfMonth());
      sb.append('T');
      writeTwoDigits(sb, offsetDateTime.getHour());
      sb.append(':');
      writeTwoDigits(sb, offsetDateTime.getMinute());
      sb.append(':');
      writeTwoDigits(sb, offsetDateTime.getSecond());
      sb.append('.');
      writeThreeDigits(sb, (int) (value.epochMillis % 1000));
      sb.append('Z');

      gen.writeString(sb.toString());
    }

    static void writeTwoDigits(StringBuilder sb, int part) {
      if (part < 10) {
        sb.append('0');
      }
      sb.append(part);
    }

    static void writeThreeDigits(StringBuilder sb, int part) {
      if (part < 10) {
        sb.append("00");
      } else if (part < 100) {
        sb.append('0');
      }
      sb.append(part);
    }
  }
}
