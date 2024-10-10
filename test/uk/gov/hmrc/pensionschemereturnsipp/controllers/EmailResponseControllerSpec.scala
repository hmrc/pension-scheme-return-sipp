/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.pensionschemereturnsipp.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.pensionschemereturnsipp.services.AuditService
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec

import scala.concurrent.Future

class EmailResponseControllerSpec extends BaseSpec { // scalastyle:off magic.number

  import EmailResponseControllerSpec._

  private val mockAuditService = mock[AuditService]
  private val mockAuthConnector = mock[AuthConnector]

  private val application: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "auditing.enabled" -> false,
      "metrics.enabled" -> false,
      "metrics.jvm" -> false,
      "queryParameter.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ=="
    )
    .overrides(
      Seq(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[AuditService].toInstance(mockAuditService)
      )
    )
    .build()

  private val injector = application.injector
  private val controller = injector.instanceOf[EmailResponseController]
  private val crypto = injector.instanceOf[ApplicationCrypto].QueryParameterCrypto
  private val encryptedPsaId = crypto.encrypt(PlainText(psaOrPspId)).value
  private val encryptedPstr = crypto.encrypt(PlainText(pstr)).value
  private val encryptedSchemeName = crypto.encrypt(PlainText(schemeName)).value
  private val encryptedUserName = crypto.encrypt(PlainText(userName)).value

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockAuthConnector)

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
      .thenReturn(Future.successful(enrolments))
  }

  "EmailResponseController" must {

    // TODO add more tests?

    "respond with FORBIDDEN when email address is invalid" in {
      val invalidEmail = "invalid"

      val result = controller.sendAuditEvents(
        schemeAdministratorTypeAsPsp,
        requestId,
        email = crypto.encrypt(PlainText(invalidEmail)).value,
        encryptedPsaId,
        encryptedPstr,
        reportVersion,
        encryptedSchemeName,
        taxYear,
        encryptedUserName
      )(
        fakeRequest.withBody(Json.obj("name" -> "invalid"))
      )

      verify(mockAuditService, never).sendEvent(any())(any(), any())
      status(result) mustBe FORBIDDEN
    }
  }
}

object EmailResponseControllerSpec {
  private val psaOrPspId = "A7654321"
  private val schemeAdministratorTypeAsPsp = "PSP"
  private val requestId = "test-request-id"
  private val reportVersion = "1"
  private val taxYear = "Test tax year"

  private val fakeRequest = FakeRequest("", "")
}
