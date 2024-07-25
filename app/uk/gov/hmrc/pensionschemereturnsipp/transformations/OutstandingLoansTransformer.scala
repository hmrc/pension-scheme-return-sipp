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
import cats.implicits.catsSyntaxOptionId
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{OutstandingLoansApi, OutstandingLoansResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippLoanOutstanding}
import io.scalaland.chimney.dsl._

import javax.inject.{Inject, Singleton}

@Singleton
class OutstandingLoansTransformer @Inject()
    extends Transformer[OutstandingLoansApi.TransactionDetails, OutstandingLoansResponse] {
  def merge(
    updates: NonEmptyList[OutstandingLoansApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions],
    version: Option[String]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[OutstandingLoansApi.TransactionDetails, SippLoanOutstanding.TransactionDetail](
        updates,
        etmpData,
        _.transformInto[SippLoanOutstanding.TransactionDetail],
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            version = version,
            loanOutstanding = maybeTransactions.map(
              transactions => SippLoanOutstanding(transactions.length, version, Some(transactions.toList))
            )
          )
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
    trx
      .into[OutstandingLoansApi.TransactionDetails]
      .withFieldConst(_.nameDOB, toNameDOB(member))
      .withFieldConst(_.nino, toNinoType(member))
      .withFieldConst(_.transactionCount, transactionCount.some)
      .transform
}
