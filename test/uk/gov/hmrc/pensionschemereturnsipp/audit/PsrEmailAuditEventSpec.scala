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

package uk.gov.hmrc.pensionschemereturnsipp.audit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.pensionschemereturnsipp.models.{EmailEvent, Event}

import java.time.LocalDateTime

class PsrEmailAuditEventSpec extends AnyWordSpec with Matchers {

  "PsrEmailAuditEvent" should {

    "return the correct auditType" in {
      val event = createPsrEmailAuditEvent()

      event.auditType shouldBe "PensionSchemeReturnEmailEvent"
    }

    "return the correct details" in {
      val event = createPsrEmailAuditEvent()

      val expectedDetails: JsObject = Json.obj(
        "emailInitiationRequestId" -> "request123",
        "email" -> "test@example.com",
        "credentialRolePsaPsp" -> "PSP",
        "status" -> "Sent",
        "submittedBy" -> "John Doe",
        "reportVersion" -> "1.0",
        "pensionSchemeTaxReference" -> "test-pstr",
        "schemeName" -> "Test Scheme",
        "taxYear" -> "2024-2025",
        "pensionSchemePractitionerId" -> "psa-123",
        "schemePractitionerName" -> "John Doe"
      )

      event.details shouldBe expectedDetails
    }
  }

  private def createPsrEmailAuditEvent(): PsrEmailAuditEvent =
    PsrEmailAuditEvent(
      psaPspId = "psa-123",
      pstr = "test-pstr",
      submittedBy = "John Doe",
      emailAddress = "test@example.com",
      event = EmailEvent(Event.Sent, LocalDateTime.now),
      requestId = "request123",
      reportVersion = "1.0",
      schemeName = "Test Scheme",
      taxYear = "2024-2025",
      userName = "John Doe"
    )
}
