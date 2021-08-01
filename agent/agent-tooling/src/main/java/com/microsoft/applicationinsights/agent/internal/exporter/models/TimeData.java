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

package com.microsoft.applicationinsights.agent.internal.exporter.models;

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

  private static final int ZERO_CHAR_AS_INT = '0';

  private final long epochMillis;

  public TimeData(long epochMillis) {
    this.epochMillis = epochMillis;
  }

  public static class TimeDataSerializer extends JsonSerializer<TimeData> {

    @Override
    public void serialize(TimeData value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      // TODO(trask): without this, an exception is thrown
      //  "Can not write a field name, expecting a value"
      //  is this the right way to work around this exception?
      // TODO(trask): or should this be using SerializableString?
      gen.writeRawValue("");

      OffsetDateTime offsetDateTime =
          Instant.ofEpochMilli(value.epochMillis).atOffset(ZoneOffset.UTC);
      gen.writeRaw('"');
      writeYear(gen, offsetDateTime.getYear());
      gen.writeRaw('-');
      writeTwoDigits(gen, offsetDateTime.getMonthValue());
      gen.writeRaw('-');
      writeTwoDigits(gen, offsetDateTime.getDayOfMonth());
      gen.writeRaw('T');
      writeTwoDigits(gen, offsetDateTime.getHour());
      gen.writeRaw(':');
      writeTwoDigits(gen, offsetDateTime.getMinute());
      gen.writeRaw(':');
      writeTwoDigits(gen, offsetDateTime.getSecond());
      gen.writeRaw('.');
      writeThreeDigits(gen, (int) (value.epochMillis % 1000));
      gen.writeRaw('Z');
      gen.writeRaw('"');
    }

    private static void writeYear(JsonGenerator gen, int year) throws IOException {
      if (year >= 2000 && year < 3000) {
        // optimization
        gen.writeRaw('2');
        writeThreeDigits(gen, year - 2000);
      } else {
        gen.writeRaw(Integer.toString(year));
      }
    }

    static void writeThreeDigits(JsonGenerator gen, int part) throws IOException {
      gen.writeRaw((char) (ZERO_CHAR_AS_INT + (part / 100)));
      writeTwoDigits(gen, part % 100);
    }

    static void writeTwoDigits(JsonGenerator gen, int part) throws IOException {
      gen.writeRaw((char) (ZERO_CHAR_AS_INT + (part / 10)));
      gen.writeRaw((char) (ZERO_CHAR_AS_INT + (part % 10)));
    }
  }
}
