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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.{IndividualDetails, MinimalDetails, PensionSchemeId}

import java.time.LocalDate

class PSRSubmissionEventSpec extends AnyWordSpec with Matchers {

  "PSRSubmissionEvent" should {

    "return the correct audit type" in {
      val event = PSRSubmissionEvent(
        pstr = "test-pstr",
        pensionSchemeId = PensionSchemeId.PsaId("test-id"),
        minimalDetails = MinimalDetails(
          email = "test@example.com",
          isPsaSuspended = false,
          organisationName = Some("Test Organisation"),
          individualDetails = None,
          rlsFlag = false,
          deceasedFlag = false
        ),
        schemeName = Some("Test Scheme"),
        taxYear = Some(DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))),
        payload = Json.obj("key" -> "value"),
        checkReturnDates = Some("true")
      )

      event.auditType mustBe "PensionSchemeReturnSubmitted"
    }

    "return the correct details" in {
      val minimalDetails = MinimalDetails(
        email = "test@example.com",
        isPsaSuspended = false,
        organisationName = Some("Test Organisation"),
        individualDetails = None,
        rlsFlag = false,
        deceasedFlag = false
      )

      val event = PSRSubmissionEvent(
        pstr = "test-pstr",
        pensionSchemeId = PensionSchemeId.PsaId("test-id"),
        minimalDetails = minimalDetails,
        schemeName = Some("Test Scheme"),
        taxYear = Some(DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))),
        payload = Json.obj("key" -> "value"),
        checkReturnDates = Some("true")
      )

      val expectedDetails = Json.obj(
        "pensionSchemeAdministratorId" -> "test-id",
        "schemeAdministratorName" -> "Test Organisation",
        "credentialRolePsaPsp" -> "PSA",
        "pensionSchemeTaxReference" -> "test-pstr",
        "affinityGroup" -> "Organisation",
        "submissionAmendment" -> false,
        "schemeName" -> "Test Scheme",
        "taxYear" -> "2023-2024",
        "date" -> LocalDate.now().toString,
        "payload" -> Json.obj("key" -> "value"),
        "checkReturnDates" -> "true"
      )

      event.details mustBe expectedDetails
    }

    "return the correct details with an individuals details" in {
      val minimalDetails = MinimalDetails(
        email = "test@example.com",
        isPsaSuspended = false,
        organisationName = None,
        individualDetails = Some(IndividualDetails("First", None, "Last")),
        rlsFlag = false,
        deceasedFlag = false
      )

      val event = PSRSubmissionEvent(
        pstr = "test-pstr",
        pensionSchemeId = PensionSchemeId.PsaId("test-id"),
        minimalDetails = minimalDetails,
        schemeName = Some("Test Scheme"),
        taxYear = Some(DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))),
        payload = Json.obj("key" -> "value"),
        isSubmissionAmendment = true,
        checkReturnDates = Some("true")
      )

      val expectedDetails = Json.obj(
        "pensionSchemeAdministratorId" -> "test-id",
        "schemeAdministratorName" -> "First Last",
        "credentialRolePsaPsp" -> "PSA",
        "pensionSchemeTaxReference" -> "test-pstr",
        "affinityGroup" -> "Individual",
        "submissionAmendment" -> true,
        "schemeName" -> "Test Scheme",
        "taxYear" -> "2023-2024",
        "date" -> LocalDate.now().toString,
        "payload" -> Json.obj("key" -> "value"),
        "checkReturnDates" -> "true"
      )

      event.details mustBe expectedDetails
    }

    "handle checkReturnDates correctly" when {
      val baseMinimalDetails = MinimalDetails(
        email = "test@example.com",
        isPsaSuspended = false,
        organisationName = Some("Test Organisation"),
        individualDetails = None,
        rlsFlag = false,
        deceasedFlag = false
      )

      val baseExpectedDetails = Json.obj(
        "pensionSchemeAdministratorId" -> "test-id",
        "schemeAdministratorName" -> "Test Organisation",
        "credentialRolePsaPsp" -> "PSA",
        "pensionSchemeTaxReference" -> "test-pstr",
        "affinityGroup" -> "Organisation",
        "submissionAmendment" -> false,
        "schemeName" -> "Test Scheme",
        "taxYear" -> "2023-2024",
        "date" -> LocalDate.now().toString,
        "payload" -> Json.obj("key" -> "value")
      )

      "checkReturnDates is Some(true)" in {
        val event = PSRSubmissionEvent(
          pstr = "test-pstr",
          pensionSchemeId = PensionSchemeId.PsaId("test-id"),
          minimalDetails = baseMinimalDetails,
          schemeName = Some("Test Scheme"),
          taxYear = Some(DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))),
          payload = Json.obj("key" -> "value"),
          checkReturnDates = Some("true")
        )

        event.details mustBe (baseExpectedDetails ++ Json.obj("checkReturnDates" -> "true"))
      }

      "checkReturnDates is Some(false)" in {
        val event = PSRSubmissionEvent(
          pstr = "test-pstr",
          pensionSchemeId = PensionSchemeId.PsaId("test-id"),
          minimalDetails = baseMinimalDetails,
          schemeName = Some("Test Scheme"),
          taxYear = Some(DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))),
          payload = Json.obj("key" -> "value"),
          checkReturnDates = Some("false")
        )

        event.details mustBe (baseExpectedDetails ++ Json.obj("checkReturnDates" -> "false"))
      }

      "checkReturnDates is None" in {
        val event = PSRSubmissionEvent(
          pstr = "test-pstr",
          pensionSchemeId = PensionSchemeId.PsaId("test-id"),
          minimalDetails = baseMinimalDetails,
          schemeName = Some("Test Scheme"),
          taxYear = Some(DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))),
          payload = Json.obj("key" -> "value"),
          checkReturnDates = None
        )

        event.details mustBe baseExpectedDetails
        event.details.keys must not contain "checkReturnDates"
      }
    }
  }
}
