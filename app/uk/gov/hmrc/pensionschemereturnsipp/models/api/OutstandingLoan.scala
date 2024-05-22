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

package uk.gov.hmrc.pensionschemereturnsipp.models.api

import cats.data.NonEmptyList
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{ConnectedOrUnconnectedType, YesNo}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.SippLoanOutstanding

import java.time.LocalDate

case class OutstandingLoan(
  noOfTransactions: Int,
  transactionDetails: Option[NonEmptyList[OutstandingLoan.TransactionDetail]]
)

object OutstandingLoan {
  case class TransactionDetail(
    nameDOB: NameDOB,
    nino: NinoType,
    loanRecipientName: String,
    dateOfLoan: LocalDate,
    amountOfLoan: Double,
    loanConnectedParty: YesNo,
    repayDate: LocalDate,
    interestRate: Double,
    loanSecurity: YesNo,
    capitalRepayments: Double,
    interestPayments: Double,
    arrearsOutstandingPrYears: YesNo,
    outstandingYearEndAmount: Double
  ) extends MemberKey

  object TransactionDetail {
    implicit val format: OFormat[TransactionDetail] = Json.format[TransactionDetail]

    implicit class TransformationOps(val transactionDetail: TransactionDetail) extends AnyVal {
      def toEtmp: SippLoanOutstanding.TransactionDetail =
        SippLoanOutstanding.TransactionDetail(
          loanRecipientName = transactionDetail.loanRecipientName,
          dateOfLoan = transactionDetail.dateOfLoan,
          amountOfLoan = transactionDetail.amountOfLoan,
          loanConnectedParty =
            if (transactionDetail.loanConnectedParty.boolean) ConnectedOrUnconnectedType.Connected
            else ConnectedOrUnconnectedType.Unconnected, //TODO change api type
          repayDate = transactionDetail.repayDate,
          interestRate = transactionDetail.interestRate,
          loanSecurity = transactionDetail.loanSecurity,
          capitalRepayments = transactionDetail.capitalRepayments,
          interestPayments = transactionDetail.interestPayments,
          arrearsOutstandingPrYears = transactionDetail.arrearsOutstandingPrYears,
          outstandingYearEndAmount = transactionDetail.outstandingYearEndAmount
        )
    }
  }

  implicit val format: OFormat[OutstandingLoan] = Json.format[OutstandingLoan]
}
