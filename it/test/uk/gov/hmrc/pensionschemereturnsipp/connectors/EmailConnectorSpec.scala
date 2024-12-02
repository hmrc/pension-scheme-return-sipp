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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.EitherValues
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.PsaId

import scala.concurrent.ExecutionContext.Implicits.global

class EmailConnectorSpec extends BaseConnectorSpec with EitherValues {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val url: String = "/hmrc/email"
  private val testPsaId = PsaId("A1234567")
  private val testPstr = "87219363YN"
  private val requestId = "test-Request-Id"
  private val testEmailAddress = "test@test.com"
  private val testTemplate = "testTemplate"
  private val testSchemeName = "testSchemeName"
  private val testUserName = "testUserName"
  private val testTaxYear = "2023-2024"
  private val testReportVersion = "001"

  override lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure(
      "microservice.services.if-hod.port" -> wireMockPort
    )

  private lazy val connector: EmailConnector = applicationBuilder.injector().instanceOf[EmailConnector]

  "Email Connector" must {
    "return an EmailSent" when {
      "email sent successfully with status 202 (Accepted)" in {
        wireMockServer.stubFor(
          post(urlEqualTo(url)).willReturn(
            aResponse()
              .withStatus(Status.ACCEPTED)
              .withHeader("Content-Type", "application/json")
          )
        )
        connector
          .sendEmail(
            testPsaId,
            requestId,
            testPstr,
            testEmailAddress,
            testTemplate,
            testSchemeName,
            testUserName,
            Map.empty,
            testTaxYear,
            testReportVersion
          )
          .map { result =>
            result.value mustBe ()
          }
      }
    }

    "return an EmailNotSent" when {
      "email service is down" in {
        wireMockServer.stubFor(
          post(urlEqualTo(url)).willReturn(
            serviceUnavailable()
              .withHeader("Content-Type", "application/json")
          )
        )

        connector
          .sendEmail(
            testPsaId,
            requestId,
            testPstr,
            testEmailAddress,
            testTemplate,
            testSchemeName,
            testUserName,
            Map.empty,
            testTaxYear,
            testReportVersion
          )
          .map { result =>
            result.left.value mustBe "Failed to send email 503"
          }
      }

      "email service returns back with 204 (No Content)" in {
        wireMockServer.stubFor(
          post(urlEqualTo(url)).willReturn(
            noContent()
              .withHeader("Content-Type", "application/json")
          )
        )
        connector
          .sendEmail(
            testPsaId,
            requestId,
            testPstr,
            testEmailAddress,
            testTemplate,
            testSchemeName,
            testUserName,
            Map.empty,
            testTaxYear,
            testReportVersion
          )
          .map { result =>
            result.left.value mustBe "Failed to send email 204"
          }
      }
    }
  }
}
