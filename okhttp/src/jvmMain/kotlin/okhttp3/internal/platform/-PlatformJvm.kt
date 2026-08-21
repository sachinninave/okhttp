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
package okhttp3.internal.platform

import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal fun TrustManagerFactory.platformTrustManager(): X509TrustManager {
  init(null as KeyStore?)
  val trustManagers = trustManagers!!
  check(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
    "Unexpected default trust managers: ${trustManagers.contentToString()}"
  }
  return trustManagers[0] as X509TrustManager
}
