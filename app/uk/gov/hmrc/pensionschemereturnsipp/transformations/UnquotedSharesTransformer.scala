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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{UnquotedShareApi, UnquotedShareResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippUnquotedShares}
import io.scalaland.chimney.dsl._

import javax.inject.{Inject, Singleton}

@Singleton
class UnquotedSharesTransformer @Inject()
    extends Transformer[UnquotedShareApi.TransactionDetails, UnquotedShareResponse] {

  def merge(
    unquotedShares: NonEmptyList[UnquotedShareApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions],
    version: Option[String]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[UnquotedShareApi.TransactionDetails, SippUnquotedShares.TransactionDetail](
        unquotedShares,
        etmpData,
        _.transformInto[SippUnquotedShares.TransactionDetail],
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            version = version,
            unquotedShares = maybeTransactions.map(
              transactions => SippUnquotedShares(transactions.length, version, Some(transactions.toList))
            )
          )
      )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): UnquotedShareResponse =
    UnquotedShareResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.unquotedShares
          .map(
            transaction =>
              transaction.transactionDetails
                .getOrElse(List.empty)
                .map(shares => transformTransactionDetails(member, transaction.noOfTransactions, shares))
          )
          .getOrElse(List.empty)
      }
    )

  def transformTransactionDetails(
    member: MemberDetails,
    transactionCount: Int,
    trx: etmp.SippUnquotedShares.TransactionDetail
  ): UnquotedShareApi.TransactionDetails =
    trx
      .into[UnquotedShareApi.TransactionDetails]
      .withFieldConst(_.nameDOB, toNameDOB(member))
      .withFieldConst(_.nino, toNinoType(member))
      .withFieldConst(_.transactionCount, transactionCount.some)
      .transform
}
