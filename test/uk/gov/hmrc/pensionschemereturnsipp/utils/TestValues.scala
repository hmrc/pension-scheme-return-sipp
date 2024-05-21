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
  EtmpMemberAndTransactions,
  EtmpSippAccountingPeriodDetails,
  EtmpSippReportDetails,
  EtmsSippAccountingPeriod,
  MemberDetails,
  SippLandArmsLength,
  SippLandConnectedParty,
  SippLoanOutstanding,
  SippOtherAssetsConnectedParty,
  SippTangibleProperty,
  SippUnquotedShares
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.{psaEnrolmentKey, psaIdKey}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.RegistryDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.common.ConnectedOrUnconnectedType.Connected
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.EtmpSippCostOrMarketType.Cost
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{
  EtmpAddress,
  EtmpSippSharesCompanyDetail,
  EtmpSippSharesDisposalDetails
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.New
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}

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

  val sampleSippPsrSubmissionEtmpResponse: SippPsrSubmissionEtmpResponse =
    SippPsrSubmissionEtmpResponse(
      EtmpSippReportDetails(
        Some("12345678AA"),
        Compiled,
        LocalDate.parse("2022-04-06"),
        LocalDate.parse("2023-04-05"),
        "Yes",
        Some("PSR Scheme"),
        Some("001")
      ),
      EtmpSippAccountingPeriodDetails(
        Some("002"),
        List(
          EtmsSippAccountingPeriod(LocalDate.parse("2022-04-06"), LocalDate.parse("2022-12-31")),
          EtmsSippAccountingPeriod(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-04-05"))
        )
      ),
      Some(
        List(
          EtmpMemberAndTransactions(
            New,
            Some("000"),
            MemberDetails("Dave", Some("K"), "Robin", Some("AA200000A"), None, LocalDate.parse("1900-03-14")),
            Some(
              SippLandConnectedParty(
                1,
                Some(
                  List(
                    SippLandConnectedParty.TransactionDetail(
                      LocalDate.parse("2023-03-14"),
                      Yes,
                      EtmpAddress(
                        "London1",
                        "London2",
                        Some("London3"),
                        Some("London4"),
                        Some("London5"),
                        Some("LH3 4DG"),
                        "GB"
                      ),
                      RegistryDetails(No, Some("Lost"), None),
                      "SUN Ltd",
                      1234.99,
                      No,
                      Yes,
                      None,
                      No,
                      Yes,
                      None,
                      None,
                      None,
                      None,
                      999999.99,
                      Yes,
                      None,
                      None,
                      None,
                      None,
                      None
                    )
                  )
                )
              )
            ),
            Some(
              SippOtherAssetsConnectedParty(
                1,
                Some(
                  List(
                    SippOtherAssetsConnectedParty.TransactionDetail(
                      LocalDate.parse("2023-03-14"),
                      "Tesco store",
                      No,
                      None,
                      "Morrisons XYZ",
                      9.999999999e7,
                      No,
                      No,
                      9999.99,
                      No,
                      None,
                      None,
                      None,
                      None,
                      No,
                      Some(0),
                      None
                    )
                  )
                )
              )
            ),
            Some(
              SippLandArmsLength(
                1,
                Some(
                  List(
                    SippLandArmsLength.TransactionDetail(
                      LocalDate.parse("2023-03-14"),
                      Yes,
                      EtmpAddress(
                        "Brighton1",
                        "Brighton2",
                        Some("Brighton3"),
                        Some("Brighton4"),
                        Some("Brighton5"),
                        Some("BN12 4XL"),
                        "GB"
                      ),
                      RegistryDetails(Yes, Some("1234XDF"), None),
                      "Willco",
                      999999.99,
                      No,
                      No,
                      None,
                      No,
                      No,
                      None,
                      None,
                      None,
                      None,
                      2000.99,
                      No,
                      None,
                      None,
                      None,
                      None,
                      None
                    )
                  )
                )
              )
            ),
            Some(
              SippTangibleProperty(
                1,
                Some(
                  List(
                    SippTangibleProperty.TransactionDetail(
                      "Ice Cream Machine",
                      LocalDate.parse("2023-03-14"),
                      999999.99,
                      "Rambo M",
                      No,
                      9999.99,
                      Cost,
                      99999.99,
                      No,
                      None,
                      None,
                      None,
                      None,
                      None
                    )
                  )
                )
              )
            ),
            Some(
              SippLoanOutstanding(
                1,
                Some(
                  List(
                    SippLoanOutstanding.TransactionDetail(
                      "Loyds Ltd",
                      LocalDate.parse("2023-03-14"),
                      999.99,
                      Connected,
                      LocalDate.parse("2023-03-14"),
                      10.0,
                      No,
                      99999.99,
                      99999.99,
                      No,
                      9999.99
                    )
                  )
                )
              )
            ),
            Some(
              SippUnquotedShares(
                1,
                Some(
                  List(
                    SippUnquotedShares.TransactionDetail(
                      EtmpSippSharesCompanyDetail("Boeing", Some("CRN456789"), None, "Primary", 100),
                      "HL Ltd",
                      9999.99,
                      No,
                      Some(10),
                      999.99,
                      Yes,
                      EtmpSippSharesDisposalDetails(9999.99, Connected, "Dave SS", No),
                      None
                    )
                  )
                )
              )
            )
          )
        )
      )
    )

  val validationMessage: ValidationMessage = {
    val builder = new ValidationMessage.Builder()
    builder
      .code("CustomErrorMessageType")
      .message("customMessage")
    builder.build
  }
}
