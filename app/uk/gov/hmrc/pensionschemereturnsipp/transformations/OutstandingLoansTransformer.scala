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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{OutstandingLoansApi, OutstandingLoansResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.EtmpConnectedOrUnconnectedType
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.EtmpConnectedOrUnconnectedType.Connected
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippLoanOutstanding}

import javax.inject.{Inject, Singleton}

@Singleton
class OutstandingLoansTransformer @Inject()
    extends Transformer[OutstandingLoansApi.TransactionDetails, OutstandingLoansResponse] {
  def merge(
    updates: NonEmptyList[OutstandingLoansApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[OutstandingLoansApi.TransactionDetails, SippLoanOutstanding.TransactionDetail](
        updates,
        etmpData,
        transformSingle,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            loanOutstanding =
              maybeTransactions.map(transactions => SippLoanOutstanding(transactions.length, Some(transactions.toList)))
          )
      )

  private def transformSingle(
    property: OutstandingLoansApi.TransactionDetails
  ): SippLoanOutstanding.TransactionDetail =
    SippLoanOutstanding.TransactionDetail(
      loanRecipientName = property.loanRecipientName,
      dateOfLoan = property.dateOfLoan,
      amountOfLoan = property.amountOfLoan,
      loanConnectedParty =
        if (property.loanConnectedParty.boolean) EtmpConnectedOrUnconnectedType.Connected
        else EtmpConnectedOrUnconnectedType.Unconnected, //TODO change api type
      repayDate = property.repayDate,
      interestRate = property.interestRate,
      loanSecurity = property.loanSecurity,
      capitalRepayments = property.capitalRepayments,
      interestPayments = property.interestPayments,
      arrearsOutstandingPrYears = property.arrearsOutstandingPrYears,
      outstandingYearEndAmount = property.outstandingYearEndAmount
    )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): OutstandingLoansResponse =
    OutstandingLoansResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.loanOutstanding
          .map(
            transaction =>
              transaction.transactionDetails
                .getOrElse(List.empty)
                .map(loans => transformTransactionDetails(member, transaction.noOfTransactions, loans))
          )
          .getOrElse(List.empty)
      }
    )

  def transformTransactionDetails(
    member: MemberDetails,
    transactionCount: Int,
    trx: etmp.SippLoanOutstanding.TransactionDetail
  ): OutstandingLoansApi.TransactionDetails =
    OutstandingLoansApi.TransactionDetails(
      nameDOB = NameDOB(member.firstName, member.lastName, member.dateOfBirth),
      nino = NinoType(member.nino, member.reasonNoNINO),
      loanRecipientName = trx.loanRecipientName,
      dateOfLoan = trx.dateOfLoan,
      amountOfLoan = trx.amountOfLoan,
      loanConnectedParty = YesNo(trx.loanConnectedParty == Connected),
      repayDate = trx.repayDate,
      interestRate = trx.interestRate,
      loanSecurity = trx.loanSecurity,
      capitalRepayments = trx.capitalRepayments,
      interestPayments = trx.interestPayments,
      arrearsOutstandingPrYears = trx.arrearsOutstandingPrYears,
      outstandingYearEndAmount = trx.outstandingYearEndAmount,
      transactionCount = Some(transactionCount)
    )
}
