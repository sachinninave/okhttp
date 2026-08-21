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

import java.net.Authenticator as JavaAuthenticator
import java.net.PasswordAuthentication
import okhttp3.Protocol.HTTP_1_1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class JavaNetAuthenticatorTest {
  private val authenticator = JavaNetAuthenticator()

  @AfterEach fun tearDown() {
    JavaAuthenticator.setDefault(null)
  }

  @Test fun basicChallengeUsesDefaultJavaNetAuthenticator() {
    JavaAuthenticator.setDefault(FakeAuthenticator("jesse", "password1"))

    val authenticated =
      authenticator.authenticate(null, unauthorizedResponse("Basic realm=\"api\""))

    assertThat(authenticated!!.header("Authorization"))
      .isEqualTo(Credentials.basic("jesse", "password1"))
  }

  @Test fun basicChallengeHonorsChallengeCharset() {
    JavaAuthenticator.setDefault(FakeAuthenticator("jesse", "üñí"))

    val authenticated = authenticator.authenticate(
      null,
      unauthorizedResponse("Basic realm=\"api\", charset=\"UTF-8\"")
    )

    assertThat(authenticated!!.header("Authorization"))
      .isEqualTo(Credentials.basic("jesse", "üñí", Charsets.UTF_8))
  }

  @Test fun realmIsPassedToJavaNetAuthenticator() {
    val fakeAuthenticator = FakeAuthenticator("jesse", "password1")
    JavaAuthenticator.setDefault(fakeAuthenticator)

    authenticator.authenticate(null, unauthorizedResponse("Basic realm=\"api\""))

    assertThat(fakeAuthenticator.requestedRealms).containsExactly("api")
  }

  @Test fun noCredentialsReturnsNoRequest() {
    JavaAuthenticator.setDefault(null)

    val authenticated =
      authenticator.authenticate(null, unauthorizedResponse("Basic realm=\"api\""))

    assertThat(authenticated).isNull()
  }

  @Test fun nonBasicChallengeReturnsNoRequest() {
    JavaAuthenticator.setDefault(FakeAuthenticator("jesse", "password1"))

    val authenticated = authenticator.authenticate(
      null,
      unauthorizedResponse("Bearer realm=\"api\"")
    )

    assertThat(authenticated).isNull()
  }

  @Test fun missingChallengeReturnsNoRequest() {
    JavaAuthenticator.setDefault(FakeAuthenticator("jesse", "password1"))

    val request = Request.Builder()
      .url("https://localhost/robots.txt")
      .build()
    val response = Response.Builder()
      .request(request)
      .code(401)
      .protocol(HTTP_1_1)
      .message("Unauthorized")
      .build()

    assertThat(authenticator.authenticate(null, response)).isNull()
  }

  private fun unauthorizedResponse(challenge: String): Response {
    val request = Request.Builder()
      .url("https://localhost/robots.txt")
      .build()
    return Response.Builder()
      .request(request)
      .code(401)
      .header("WWW-Authenticate", challenge)
      .protocol(HTTP_1_1)
      .message("Unauthorized")
      .build()
  }

  private class FakeAuthenticator(
    private val userName: String,
    private val password: String
  ) : JavaAuthenticator() {
    val requestedRealms = mutableListOf<String?>()

    override fun getPasswordAuthentication(): PasswordAuthentication {
      requestedRealms += requestingPrompt
      return PasswordAuthentication(userName, password.toCharArray())
    }
  }
}
