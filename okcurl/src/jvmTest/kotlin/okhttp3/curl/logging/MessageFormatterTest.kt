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
import assertk.assertions.isEqualTo
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.test.Test

class MessageFormatterTest {
  @Test
  fun formatsMessageWithTrailingNewLine() {
    val record = LogRecord(Level.FINE, ">> 0x00000000     8 SETTINGS")

    assertThat(MessageFormatter.format(record)).isEqualTo(">> 0x00000000     8 SETTINGS\n")
  }

  @Test
  fun ignoresLevelAndLoggerName() {
    val record = LogRecord(Level.SEVERE, "message")
    record.loggerName = "javax.net.ssl"

    assertThat(MessageFormatter.format(record)).isEqualTo("message\n")
  }
}
