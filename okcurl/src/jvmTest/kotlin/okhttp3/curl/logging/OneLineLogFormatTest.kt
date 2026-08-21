/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3.curl.logging

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import java.io.IOException
import java.time.Instant
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.test.Test

class OneLineLogFormatTest {
  private val format = OneLineLogFormat()

  @Test
  fun formatsTimeAndMessageOnOneLine() {
    val record = LogRecord(Level.INFO, "hello")

    assertThat(format.format(record)).matches(Regex("""\d{2}:\d{2}:\d{2}\.\d{3}\thello\n"""))
  }

  @Test
  fun substitutesMessageParameters() {
    val record = LogRecord(Level.INFO, "hello {0}")
    record.parameters = arrayOf("world")

    assertThat(format.format(record)).endsWith("\thello world\n")
  }

  @Test
  fun appendsStackTraceOfThrown() {
    val record = LogRecord(Level.WARNING, "failed")
    record.thrown = IOException("boom")

    val formatted = format.format(record)

    assertThat(formatted).contains("\tfailed\n")
    assertThat(formatted).contains("java.io.IOException: boom")
  }

  @Test
  fun formatsTimeOfLogRecordInstant() {
    val record = LogRecord(Level.INFO, "hello")
    record.instant = Instant.EPOCH

    // The time is rendered in the default time zone, so only its shape is predictable.
    assertThat(format.format(record).substringBefore('\t').length).isEqualTo("00:00:00.000".length)
  }
}
