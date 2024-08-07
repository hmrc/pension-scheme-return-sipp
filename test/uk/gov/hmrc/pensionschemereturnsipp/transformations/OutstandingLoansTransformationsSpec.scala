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

package uk.gov.hmrc.pensionschemereturnsipp.transformations

import cats.data.NonEmptyList
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo
import uk.gov.hmrc.pensionschemereturnsipp.models.common.ConnectionStatus.Connected
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippLoanOutstanding}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

import java.time.LocalDate

class OutstandingLoansTransformationsSpec extends BaseSpec with SippEtmpDummyTestValues {

  private val transformer: OutstandingLoansTransformer = new OutstandingLoansTransformer()

  val etmpData = EtmpMemberAndTransactions(
    status = SectionStatus.New,
    version = None,
    memberDetails = MemberDetails(
      firstName = "firstName",
      lastName = "lastName",
      nino = Some("nino"),
      reasonNoNINO = None,
      dateOfBirth = LocalDate.of(2020, 1, 1)
    ),
    landConnectedParty = None,
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = Some(
      SippLoanOutstanding(
        1,
        None,
        Some(
          List(
            SippLoanOutstanding.TransactionDetail(
              loanRecipientName = "test",
              dateOfLoan = LocalDate.of(2020, 1, 1),
              amountOfLoan = 1,
              loanConnectedParty = Connected,
              repayDate = LocalDate.of(2020, 1, 1),
              interestRate = 1,
              loanSecurity = YesNo.Yes,
              capitalRepayments = 1,
              arrearsOutstandingPrYears = YesNo.Yes,
              arrearsOutstandingPrYearsAmt = Some(1),
              outstandingYearEndAmount = 1
            )
          )
        )
      )
    ),
    unquotedShares = None
  )

  "merge" should {
    "update data for a single member when member match is found" in {
      val testData = etmpData.copy(loanOutstanding = None)

      val result = transformer.merge(NonEmptyList.of(sippOutstandingLoansApi), List(testData), None)

      result mustBe List(
        etmpData.copy(
          loanOutstanding = Some(
            SippLoanOutstanding(
              1,
              None,
              Some(
                List(
                  SippLoanOutstanding.TransactionDetail(
                    loanRecipientName = "test",
                    dateOfLoan = LocalDate.of(2020, 1, 1),
                    amountOfLoan = 1,
                    loanConnectedParty = Connected,
                    repayDate = LocalDate.of(2020, 1, 1),
                    interestRate = 1,
                    loanSecurity = YesNo.Yes,
                    capitalRepayments = 1,
                    arrearsOutstandingPrYears = YesNo.Yes,
                    arrearsOutstandingPrYearsAmt = Some(1),
                    outstandingYearEndAmount = 1
                  )
                )
              )
            )
          )
        )
      )

    }

    "replace data for a single member when member match is found" in {

      val testDataWithDifferentRow = sippOutstandingLoansApi.copy(loanRecipientName = "test2")
      val result = transformer.merge(NonEmptyList.of(testDataWithDifferentRow), List(etmpData), None)

      result mustBe List(
        etmpData.copy(
          loanOutstanding = Some(
            SippLoanOutstanding(
              1,
              None,
              Some(
                List(
                  SippLoanOutstanding.TransactionDetail(
                    loanRecipientName = "test2",
                    dateOfLoan = LocalDate.of(2020, 1, 1),
                    amountOfLoan = 1,
                    loanConnectedParty = Connected,
                    repayDate = LocalDate.of(2020, 1, 1),
                    interestRate = 1,
                    loanSecurity = YesNo.Yes,
                    capitalRepayments = 1,
                    arrearsOutstandingPrYears = YesNo.Yes,
                    arrearsOutstandingPrYearsAmt = Some(1),
                    outstandingYearEndAmount = 1
                  )
                )
              )
            )
          )
        )
      )

    }

    "add data with new member details for a single member when match is not found" in {
      val testDataWithDifferentRow = sippOutstandingLoansApi.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testDataWithDifferentRow), List(etmpData), None)

      result mustBe List(
        etmpData.copy(loanOutstanding = None),
        etmpData.copy(
          memberDetails = MemberDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Some("otherNino"),
            reasonNoNINO = None,
            dateOfBirth = LocalDate.of(2020, 1, 1)
          )
        )
      )
    }
  }

  "transformToResponse" should {
    "return correct response" in {
      val result = transformer.transformToResponse(
        List(etmpSippMemberAndTransactions)
      )

      result.transactions.length mustBe 1
      result.transactions.head.transactionCount mustBe Some(1)
      result.transactions.head.nino.nino mustBe sippMemberDetails.nino
      result.transactions.head.nameDOB.firstName mustBe sippMemberDetails.firstName
      result.transactions.head.nameDOB.lastName mustBe sippMemberDetails.lastName

    }

    "return no transaction if related not exist" in {
      val result = transformer.transformToResponse(
        List(etmpSippMemberAndTransactions.copy(loanOutstanding = None))
      )

      result.transactions.length mustBe 0
    }
  }
}
