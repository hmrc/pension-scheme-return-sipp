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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{AssetsFromConnectedPartyApi, AssetsFromConnectedPartyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  MemberDetails,
  SippOtherAssetsConnectedParty
}
import io.scalaland.chimney.dsl._

import javax.inject.{Inject, Singleton}

@Singleton
class AssetsFromConnectedPartyTransformer @Inject()
    extends Transformer[AssetsFromConnectedPartyApi.TransactionDetails, AssetsFromConnectedPartyResponse] {

  def merge(
    assetsFromConnectedParty: NonEmptyList[AssetsFromConnectedPartyApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[AssetsFromConnectedPartyApi.TransactionDetails, SippOtherAssetsConnectedParty.TransactionDetails](
        assetsFromConnectedParty,
        etmpData,
        _.transformInto[SippOtherAssetsConnectedParty.TransactionDetails],
        _.otherAssetsConnectedPartyTransactions,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            version = None,
            otherAssetsConnectedParty = maybeTransactions.map(
              transactions => SippOtherAssetsConnectedParty(transactions.length, None, Some(transactions.toList))
            )
          )
      )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): AssetsFromConnectedPartyResponse =
    AssetsFromConnectedPartyResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.otherAssetsConnectedParty
          .map(
            transaction =>
              transaction.transactionDetails
                .getOrElse(List.empty)
                .map(assets => transformTransactionDetails(member, transaction.noOfTransactions, assets))
          )
          .getOrElse(List.empty)
      }
    )

  def transformTransactionDetails(
    member: MemberDetails,
    transactionCount: Int,
    trx: SippOtherAssetsConnectedParty.TransactionDetails
  ): AssetsFromConnectedPartyApi.TransactionDetails =
    trx
      .into[AssetsFromConnectedPartyApi.TransactionDetails]
      .withFieldConst(_.nameDOB, toNameDOB(member))
      .withFieldConst(_.nino, toNinoType(member))
      .withFieldConst(_.transactionCount, transactionCount.some)
      .transform
}
