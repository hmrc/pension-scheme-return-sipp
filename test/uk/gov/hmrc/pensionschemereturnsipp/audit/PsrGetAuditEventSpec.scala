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

class PsrGetAuditEventSpec extends AnyWordSpec with Matchers {

  "PsrGetAuditEvent" should {

    "return the correct auditType" in {
      val event = createPsrGetAuditEvent()

      event.auditType shouldBe "PensionSchemeReturnGet"
    }

    "return the correct details when all fields are present" in {
      val event = createPsrGetAuditEvent()

      val expectedDetails: JsObject = Json.obj(
        "pensionSchemeTaxReference" -> "test-pstr",
        "fbNumber" -> "FB-12345",
        "periodStartDate" -> "2024-04-01",
        "psrVersion" -> "2.0",
        "httpStatus" -> 200,
        "response" -> Json.obj("key" -> "value"),
        "errorMessage" -> "Error occurred"
      )

      event.details shouldBe expectedDetails
    }

    "return the correct details when some optional fields are missing" in {
      val event = createPsrGetAuditEventWithMissingFields()

      val expectedDetails: JsObject = Json.obj(
        "pensionSchemeTaxReference" -> "test-pstr",
        "fbNumber" -> "FB-12345",
        "httpStatus" -> 200
        // Missing optional fields should not appear
      )

      event.details shouldBe expectedDetails
    }
  }

  private def createPsrGetAuditEvent(): PsrGetAuditEvent =
    PsrGetAuditEvent(
      pstr = "test-pstr",
      fbNumber = Some("FB-12345"),
      periodStartDate = Some("2024-04-01"),
      psrVersion = Some("2.0"),
      status = Some(200),
      response = Some(Json.obj("key" -> "value")),
      errorMessage = Some("Error occurred")
    )

  private def createPsrGetAuditEventWithMissingFields(): PsrGetAuditEvent =
    PsrGetAuditEvent(
      pstr = "test-pstr",
      fbNumber = Some("FB-12345"),
      periodStartDate = None,
      psrVersion = None,
      status = Some(200),
      response = None,
      errorMessage = None
    )
}
