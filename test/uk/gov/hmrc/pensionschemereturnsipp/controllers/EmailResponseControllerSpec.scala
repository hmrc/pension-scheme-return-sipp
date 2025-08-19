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

package uk.gov.hmrc.pensionschemereturnsipp.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.crypto.*
import uk.gov.hmrc.pensionschemereturnsipp.models.Event.{Complained, Delivered, Opened, PermanentBounce, Sent}
import uk.gov.hmrc.pensionschemereturnsipp.models.{EmailEvent, EmailEvents}
import uk.gov.hmrc.pensionschemereturnsipp.services.AuditService
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec

import java.time.LocalDateTime
import scala.concurrent.Future

class EmailResponseControllerSpec extends BaseSpec { // scalastyle:off magic.number

  import EmailResponseControllerSpec.*

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
  private val encryptedPsaId = controller.jsonCrypto.encrypt(PlainText(psaOrPspId)).value
  private val encryptedPstr = controller.jsonCrypto.encrypt(PlainText(pstr)).value
  private val encryptedSchemeName = controller.jsonCrypto.encrypt(PlainText(schemeName)).value
  private val encryptedUserName = controller.jsonCrypto.encrypt(PlainText(userName)).value
  private val encryptedEmail = controller.jsonCrypto.encrypt(PlainText(emailAddress)).value

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockAuthConnector)

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
      .thenReturn(Future.successful(enrolments))
  }

  "EmailResponseController" must {

    "respond OK when given EmailEvents for PSA" in {
      val result = controller.sendAuditEvents(
        schemeAdministratorTypeAsPsp,
        requestId,
        email = encryptedEmail,
        encryptedPsaId,
        encryptedPstr,
        encryptedSchemeName,
        encryptedUserName,
        taxYear,
        reportVersion
      )(
        fakeRequest.withBody(Json.toJson(emailEvents))
      )

      status(result) mustBe OK
    }

    "respond BAD REQUEST when not given EmailEvents" in {
      val result = controller.sendAuditEvents(
        schemeAdministratorTypeAsPsp,
        requestId,
        email = encryptedEmail,
        encryptedPsaId,
        encryptedPstr,
        encryptedSchemeName,
        encryptedUserName,
        taxYear,
        reportVersion
      )(
        fakeRequest.withBody(Json.toJson(""))
      )

      status(result) mustBe BAD_REQUEST
    }

    "respond with FORBIDDEN when email address is invalid" in {
      val invalidEmail = "invalid"

      val result = controller.sendAuditEvents(
        schemeAdministratorTypeAsPsp,
        requestId,
        email = controller.jsonCrypto.encrypt(PlainText(invalidEmail)).value,
        encryptedPsaId,
        encryptedPstr,
        encryptedSchemeName,
        encryptedUserName,
        taxYear,
        reportVersion
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
  private val emailAddress = "test@test.com"

  private val fakeRequest = FakeRequest("", "")
  private val emailEvents = EmailEvents(
    Seq(
      EmailEvent(Sent, LocalDateTime.now()),
      EmailEvent(Delivered, LocalDateTime.now()),
      EmailEvent(PermanentBounce, LocalDateTime.now()),
      EmailEvent(Opened, LocalDateTime.now()),
      EmailEvent(Complained, LocalDateTime.now())
    )
  )
}
