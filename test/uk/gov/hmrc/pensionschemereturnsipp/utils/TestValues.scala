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

package uk.gov.hmrc.pensionschemereturnsipp.utils

import com.networknt.schema.ValidationMessage
import uk.gov.hmrc.pensionschemereturnsipp.models.{SippPsrSubmission, SippReportDetailsSubmission}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  Compiled,
  EtmpSippAccountingPeriodDetails,
  EtmpSippReportDetails,
  EtmsSippAccountingPeriod
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.{psaEnrolmentKey, psaIdKey}

import java.time.LocalDate

trait TestValues {

  val externalId: String = "externalId"

  val pstr = "testPstr"
  val sampleToday: LocalDate = LocalDate.of(2023, 10, 19)

  val enrolments: Enrolments = Enrolments(
    Set(
      Enrolment(
        psaEnrolmentKey,
        Seq(
          EnrolmentIdentifier(psaIdKey, "A0000000")
        ),
        "Activated",
        None
      )
    )
  )

  // SIPP - PSR
  val sampleSippReportDetailsSubmission: SippReportDetailsSubmission = SippReportDetailsSubmission(
    "17836742CF",
    periodStart = LocalDate.of(2020, 12, 12),
    periodEnd = LocalDate.of(2021, 12, 12),
    memberTransactions = "Yes"
  )

  val sampleSippPsrSubmission: SippPsrSubmission = SippPsrSubmission(
    sampleSippReportDetailsSubmission
  )

  // SIPP - ETMP
  private val sampleEtmpAccountingPeriodDetails: EtmpSippAccountingPeriodDetails = EtmpSippAccountingPeriodDetails(
    version = Some("002"),
    accountingPeriods = List(
      EtmsSippAccountingPeriod(
        accPeriodStart = LocalDate.parse("2022-04-06"),
        accPeriodEnd = LocalDate.parse("2022-12-31")
      ),
      EtmsSippAccountingPeriod(
        accPeriodStart = LocalDate.parse("2023-01-01"),
        accPeriodEnd = LocalDate.parse("2023-04-05")
      )
    )
  )

  val sampleSippPsrSubmissionEtmpRequest: SippPsrSubmissionEtmpRequest = SippPsrSubmissionEtmpRequest(
    reportDetails = EtmpSippReportDetails(None, Compiled, sampleToday, sampleToday, "Yes", None, None),
    accountingPeriodDetails = None,
    memberAndTransactions = None,
    psrDeclaration = None
  )

  val sampleSippPsrSubmissionEtmpResponse: SippPsrSubmissionEtmpResponse = SippPsrSubmissionEtmpResponse(
    reportDetails = EtmpSippReportDetails(
      pstr = Some("12345678AA"),
      status = Compiled,
      periodStart = LocalDate.parse("2022-04-06"),
      periodEnd = LocalDate.parse("2023-04-05"),
      memberTransactions = "Yes",
      schemeName = Some("PSR Scheme"),
      psrVersion = Some("001")
    ),
    accountingPeriodDetails = sampleEtmpAccountingPeriodDetails,
    memberAndTransactions = None
  )

  val validationMessage: ValidationMessage = {
    val builder = new ValidationMessage.Builder()
    builder
      .code("CustomErrorMessageType")
      .message("customMessage")
    builder.build
  }
}
