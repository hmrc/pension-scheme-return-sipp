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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{TangibleMoveablePropertyApi, TangibleMoveablePropertyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippTangibleProperty}
import io.scalaland.chimney.dsl._

import javax.inject.{Inject, Singleton}

@Singleton
class TangibleMoveablePropertyTransformer @Inject()
    extends Transformer[TangibleMoveablePropertyApi.TransactionDetails, TangibleMoveablePropertyResponse] {

  def merge(
    tangibleMovableProperties: NonEmptyList[TangibleMoveablePropertyApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[TangibleMoveablePropertyApi.TransactionDetails, SippTangibleProperty.TransactionDetail](
        tangibleMovableProperties,
        etmpData,
        _.transformInto[SippTangibleProperty.TransactionDetail],
        _.tangiblePropertyTransactions,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            version = None,
            tangibleProperty = maybeTransactions.map(transactions =>
              SippTangibleProperty(transactions.length, None, Some(transactions.toList))
            )
          )
      )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): TangibleMoveablePropertyResponse =
    TangibleMoveablePropertyResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.tangibleProperty
          .map(transaction =>
            transaction.transactionDetails
              .getOrElse(List.empty)
              .map(tangible => transformTransactionDetails(member, transaction.noOfTransactions, tangible))
          )
          .getOrElse(List.empty)
      }
    )

  def transformTransactionDetails(
    member: MemberDetails,
    transactionCount: Int,
    trx: etmp.SippTangibleProperty.TransactionDetail
  ): TangibleMoveablePropertyApi.TransactionDetails =
    trx
      .into[TangibleMoveablePropertyApi.TransactionDetails]
      .withFieldConst(_.nameDOB, toNameDOB(member))
      .withFieldConst(_.nino, toNinoType(member))
      .withFieldConst(_.transactionCount, transactionCount.some)
      .transform
}
