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
package okhttp3

import java.io.IOException
import java.net.CookieHandler
import java.net.URI
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JavaNetCookieJarTest {
  private val url = "https://example.com/path".toHttpUrl()

  @Test fun loadForRequestSplitsMultipleCookiesInOneHeader() {
    val cookieJar = JavaNetCookieJar(FakeCookieHandler(mapOf("Cookie" to listOf("a=b; c=d"))))

    val cookies = cookieJar.loadForRequest(url)

    assertThat(cookies.map { it.name }).containsExactly("a", "c")
    assertThat(cookies.map { it.value }).containsExactly("b", "d")
    assertThat(cookies.map { it.domain }).containsExactly("example.com", "example.com")
  }

  @Test fun loadForRequestAcceptsCookie2AndIsCaseInsensitive() {
    val cookieJar = JavaNetCookieJar(
      FakeCookieHandler(mapOf("cookie2" to listOf("a=b"), "COOKIE" to listOf("c=d")))
    )

    val cookies = cookieJar.loadForRequest(url)

    assertThat(cookies.map { it.name }).containsExactlyInAnyOrder("a", "c")
  }

  @Test fun loadForRequestIgnoresOtherHeaders() {
    val cookieJar = JavaNetCookieJar(
      FakeCookieHandler(mapOf("Accept" to listOf("text/plain"), "Cookie" to listOf("a=b")))
    )

    val cookies = cookieJar.loadForRequest(url)

    assertThat(cookies.map { it.name }).containsExactly("a")
  }

  @Test fun loadForRequestSkipsAttributesAndUnquotesValues() {
    val cookieJar = JavaNetCookieJar(
      FakeCookieHandler(mapOf("Cookie" to listOf("\$Version=1; a=\"b\"; \$Path=/path")))
    )

    val cookies = cookieJar.loadForRequest(url)

    assertThat(cookies.map { it.name }).containsExactly("a")
    assertThat(cookies.single().value).isEqualTo("b")
  }

  @Test fun loadForRequestReadsNameOnlyCookieAsEmptyValue() {
    val cookieJar = JavaNetCookieJar(FakeCookieHandler(mapOf("Cookie" to listOf("a"))))

    val cookies = cookieJar.loadForRequest(url)

    assertThat(cookies.single().name).isEqualTo("a")
    assertThat(cookies.single().value).isEqualTo("")
  }

  @Test fun loadForRequestIsEmptyForEmptyOrMissingCookieHeaders() {
    assertThat(JavaNetCookieJar(FakeCookieHandler(mapOf())).loadForRequest(url)).isEmpty()
    assertThat(
      JavaNetCookieJar(FakeCookieHandler(mapOf("Cookie" to listOf()))).loadForRequest(url)
    ).isEmpty()
  }

  @Test fun loadForRequestIsEmptyWhenCookieHandlerThrows() {
    val cookieJar = JavaNetCookieJar(
      object : CookieHandler() {
        override fun get(uri: URI, requestHeaders: Map<String, List<String>>) =
          throw IOException("boom")

        override fun put(uri: URI, responseHeaders: Map<String, List<String>>) = Unit
      }
    )

    assertThat(cookieJar.loadForRequest(url)).isEmpty()
  }

  @Test fun saveFromResponsePutsSetCookieHeaders() {
    val cookieHandler = FakeCookieHandler(mapOf())
    val cookieJar = JavaNetCookieJar(cookieHandler)

    cookieJar.saveFromResponse(
      url,
      listOf(
        Cookie.Builder().name("a").value("b").domain("example.com").path("/path").build(),
        Cookie.Builder().name("c").value("d").domain("example.com").secure().build()
      )
    )

    assertThat(cookieHandler.puts).hasSize(1)
    val (uri, headers) = cookieHandler.puts.single()
    assertThat(uri).isEqualTo(URI("https://example.com/path"))
    assertThat(headers.keys).containsExactly("Set-Cookie")
    assertThat(headers.getValue("Set-Cookie")).containsExactly(
      "a=b; domain=.example.com; path=/path",
      "c=d; domain=.example.com; path=/; secure"
    )
  }

  @Test fun saveFromResponseIgnoresIoExceptionFromCookieHandler() {
    val cookieJar = JavaNetCookieJar(
      object : CookieHandler() {
        override fun get(uri: URI, requestHeaders: Map<String, List<String>>) =
          emptyMap<String, List<String>>()

        override fun put(uri: URI, responseHeaders: Map<String, List<String>>) =
          throw IOException("boom")
      }
    )

    cookieJar.saveFromResponse(
      url,
      listOf(Cookie.Builder().name("a").value("b").domain("example.com").build())
    )
  }

  private class FakeCookieHandler(
    private val requestHeaders: Map<String, List<String>>
  ) : CookieHandler() {
    val puts = mutableListOf<Pair<URI, Map<String, List<String>>>>()

    override fun get(uri: URI, requestHeaders: Map<String, List<String>>) = this.requestHeaders

    override fun put(uri: URI, responseHeaders: Map<String, List<String>>) {
      puts += uri to responseHeaders
    }
  }
}
