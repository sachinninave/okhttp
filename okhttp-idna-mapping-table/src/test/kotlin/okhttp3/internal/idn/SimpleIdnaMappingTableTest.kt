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
package okhttp3.internal.idn

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.IOException
import okio.Buffer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SimpleIdnaMappingTableTest {
  private val table = """
    |# This comment is ignored.
    |
    |0000          ; disallowed                   # null
    |0041..005A    ; mapped                 ; 0061 # trailing comment
    |005F          ; disallowed_STD3_valid        # low line
    |0061..007A    ; valid                        # trailing comment
    |00AD          ; ignored                      # trailing comment
    |00BC          ; mapped                 ; 0031 2044 0034 # vulgar fraction
    |00DF          ; deviation              ; 0073 0073 # sharp s
    """.trimMargin().readMappingTable()

  @Test fun validCodePointsArePreserved() {
    assertThat(table.map("hello")).isEqualTo("hello")
  }

  @Test fun mappedCodePointsUseTheirMappingTarget() {
    // Every code point of the 0041..005A range maps to the same target.
    assertThat(table.map("AZ")).isEqualTo("aa")
    assertThat(table.map("¼")).isEqualTo("1⁄4")
  }

  @Test fun ignoredCodePointsAreDropped() {
    assertThat(table.map("a\u00ada")).isEqualTo("aa")
  }

  @Test fun deviationCodePointsArePreserved() {
    // Non-transitional processing keeps deviation characters as they are.
    assertThat(table.map("ß")).isEqualTo("ß")
  }

  @Test fun disallowedStd3ValidCodePointsArePermitted() {
    assertThat(table.map("_")).isEqualTo("_")
  }

  @Test fun disallowedCodePointsFailButAreEmitted() {
    val sink = Buffer()
    assertThat(table.map(0x0000, sink)).isFalse()
    assertThat(sink.readUtf8()).isEqualTo("\u0000")
  }

  @Test fun mappingSucceedsForCodePointsThatAreMappedOrValid() {
    assertThat(table.map('a'.code, Buffer())).isTrue()
    assertThat(table.map('A'.code, Buffer())).isTrue()
    assertThat(table.map(0x00ad, Buffer())).isTrue()
  }

  @Test fun codePointOutsideOfTheTableIsRejected() {
    assertThrows<IllegalArgumentException> {
      table.map(0x10ffff, Buffer())
    }
  }

  @Test fun rangesAndSingleCodePointsAreParsed() {
    assertThat(table.mappings.map { it.sourceCodePoint0 to it.sourceCodePoint1 }).isEqualTo(
      listOf(
        0x0000 to 0x0000,
        0x0041 to 0x005a,
        0x005f to 0x005f,
        0x0061 to 0x007a,
        0x00ad to 0x00ad,
        0x00bc to 0x00bc,
        0x00df to 0x00df,
      )
    )
  }

  @Test fun mappingTypesAreParsed() {
    assertThat(table.mappings.map { it.type }).isEqualTo(
      listOf(
        TYPE_DISALLOWED,
        TYPE_MAPPED,
        TYPE_DISALLOWED_STD3_VALID,
        TYPE_VALID,
        TYPE_IGNORED,
        TYPE_MAPPED,
        TYPE_DEVIATION,
      )
    )
  }

  @Test fun lineStartingWithADelimiterIsRejected() {
    assertThrows<IOException> {
      "; valid # oops".readMappingTable()
    }
  }

  @Test fun missingSemicolonBeforeTypeIsRejected() {
    assertThrows<IOException> {
      "0041 valid # oops".readMappingTable()
    }
  }

  @Test fun unknownTypeIsRejected() {
    assertThrows<IOException> {
      "0041 ; unknown # oops".readMappingTable()
    }
  }

  @Test fun missingSemicolonBeforeMappingTargetIsRejected() {
    assertThrows<IOException> {
      "0041 ; mapped 0061 # oops".readMappingTable()
    }
  }

  private fun String.readMappingTable(): SimpleIdnaMappingTable =
    Buffer().writeUtf8(this).readPlainTextIdnaMappingTable()

  private fun SimpleIdnaMappingTable.map(string: String): String {
    val sink = Buffer()
    for (codePoint in string.codePoints()) {
      map(codePoint, sink)
    }
    return sink.readUtf8()
  }
}
