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

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxOptionId
import com.networknt.schema.ValidationMessage
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.{psaEnrolmentKey, psaIdKey}
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.PsaId
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{MemberDetailsResponse, PSRSubmissionResponse, ReportDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{MemberDetails => ApiMemberDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.CostOrMarketType.CostValue
import uk.gov.hmrc.pensionschemereturnsipp.models.common.SubmittedBy.PSP
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{
  AccountingPeriod,
  AccountingPeriodDetails,
  AddressDetails,
  DisposalDetails,
  LesseeDetails,
  RegistryDetails,
  SharesCompanyDetails,
  UnquotedShareDisposalDetails,
  YesNo
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpPsrStatus.Compiled
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration.Declaration
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.ConnectionStatus.Connected
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.New
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse

import java.time.LocalDate
import scala.annotation.unused

trait TestValues {

  val externalId: String = "externalId"

  val pstr = "testPstr"
  val sampleToday: LocalDate = LocalDate.of(2023, 10, 19)

  val samplePsaId: PsaId = PsaId("PSA")
  val samplePensionSchemeId: PensionSchemeId = PsaId("PSA")

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

  val sampleApiMemberDetailsResponse: MemberDetailsResponse = MemberDetailsResponse(
    members = List(ApiMemberDetails("Dave", "Robin", Some("AA200000A"), None, LocalDate.parse("1900-03-14")))
  )

  val samplePsrSubmission: PSRSubmissionResponse = PSRSubmissionResponse(
    details = ReportDetails(
      pstr = "test",
      status = EtmpPsrStatus.Submitted,
      periodStart = LocalDate.of(2020, 12, 12),
      periodEnd = LocalDate.of(2020, 12, 12),
      None,
      None
    ),
    accountingPeriodDetails = AccountingPeriodDetails(
      Some("1.0"),
      NonEmptyList.of(AccountingPeriod(LocalDate.of(2020, 12, 12), LocalDate.of(2020, 12, 12)))
    ).some,
    landConnectedParty = None,
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

  // SIPP - ETMP
  @unused
  private val sampleEtmpAccountingPeriodDetails: AccountingPeriodDetails = AccountingPeriodDetails(
    version = Some("002"),
    accountingPeriods = NonEmptyList.of(
      AccountingPeriod(
        accPeriodStart = LocalDate.parse("2022-04-06"),
        accPeriodEnd = LocalDate.parse("2022-12-31")
      ),
      AccountingPeriod(
        accPeriodStart = LocalDate.parse("2023-01-01"),
        accPeriodEnd = LocalDate.parse("2023-04-05")
      )
    )
  )

  val sampleSippPsrSubmissionEtmpRequest: SippPsrSubmissionEtmpRequest = SippPsrSubmissionEtmpRequest(
    reportDetails = EtmpSippReportDetails("12345678AA", Compiled, sampleToday, sampleToday, YesNo.Yes, None, None),
    accountingPeriodDetails = None,
    memberAndTransactions = None,
    psrDeclaration = None
  )

  val memberAndTransactions: EtmpMemberAndTransactions = EtmpMemberAndTransactions(
    New,
    Some("000"),
    MemberDetails("Dave", "Robin", Some("AA200000A"), None, LocalDate.parse("1900-03-14")),
    Some(
      SippLandConnectedParty(
        1,
        Some(
          List(
            SippLandConnectedParty.TransactionDetail(
              LocalDate.parse("2023-03-14"),
              Yes,
              AddressDetails(
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
              Some(1),
              No,
              Yes,
              Some(LesseeDetails(1, Yes, LocalDate.parse("2023-03-14"), 9999.99)),
              999999.99,
              Yes,
              Some(DisposalDetails(2000.99, "Micheal K", Yes, No, No))
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
            SippOtherAssetsConnectedParty.TransactionDetails(
              LocalDate.parse("2023-03-14"),
              "Tesco store",
              No,
              None,
              "Morrisons XYZ",
              9.999999999e7,
              No,
              No,
              9999.99,
              Yes,
              Some(DisposalDetails(9999999.99, "Morris K", No, No, No)),
              Some(No),
              Some(0)
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
              AddressDetails(
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
              2000.99,
              No,
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
              CostValue,
              99999.99,
              Yes,
              Some(DisposalDetails(9999.99, "Michel K", No, No, No))
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
              10,
              No,
              99999.99,
              No,
              Some(999999.99),
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
              SharesCompanyDetails("Boeing", Some("CRN456789"), None, "Primary", 100),
              "HL Ltd",
              9999.99,
              No,
              999.99,
              Yes,
              Some(UnquotedShareDisposalDetails(9999.99, "Dave SS", Yes, No, 10, 1))
            )
          )
        )
      )
    )
  )
  val sampleSippPsrSubmissionEtmpResponse: SippPsrSubmissionEtmpResponse =
    SippPsrSubmissionEtmpResponse(
      EtmpSippReportDetails(
        "12345678AA",
        Compiled,
        LocalDate.parse("2022-04-06"),
        LocalDate.parse("2023-04-05"),
        YesNo.Yes,
        Some("PSR Scheme"),
        Some("001")
      ),
      AccountingPeriodDetails(
        Some("002"),
        NonEmptyList.of(
          AccountingPeriod(LocalDate.parse("2022-04-06"), LocalDate.parse("2022-12-31")),
          AccountingPeriod(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-04-05"))
        )
      ).some,
      Some(
        List(
          memberAndTransactions
        )
      ),
      Some(
        EtmpSippPsrDeclaration(
          submittedBy = PSP,
          submitterID = "20000019",
          psaID = Some("A0000023"),
          psaDeclaration = None,
          pspDeclaration = Some(Declaration(declaration1 = true, declaration2 = true))
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
