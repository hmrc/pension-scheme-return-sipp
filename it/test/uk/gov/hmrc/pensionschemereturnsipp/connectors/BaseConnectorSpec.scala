/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.pensionschemereturnsipp.connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.softwaremill.diffx.generic.AutoDerivation
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
import org.scalatest.time.{Millis, Span}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec

abstract class BaseConnectorSpec
    extends BaseSpec
    with WireMockSupport
    with HttpClientV2Support
    with DiffShouldMatcher
    with AutoDerivation {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(500, Millis)), interval = scaled(Span(50, Millis)))

  def stubGet(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo(url))
        .willReturn(response)
    )

  def stubPost[A: Writes](url: String, requestBody: A, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo(url))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson(Json.toJson(requestBody).toString))
        .willReturn(response)
    )

  def stubDelete(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      delete(urlEqualTo(url))
        .willReturn(response)
    )
}
