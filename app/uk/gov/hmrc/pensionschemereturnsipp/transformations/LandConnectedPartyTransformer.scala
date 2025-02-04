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
import uk.gov.hmrc.pensionschemereturnsipp.models.api
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{LandOrConnectedPropertyApi, LandOrConnectedPropertyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  MemberDetails,
  SippLandConnectedParty
}
import io.scalaland.chimney.dsl.*

import javax.inject.{Inject, Singleton}

@Singleton
class LandConnectedPartyTransformer @Inject()
    extends Transformer[api.LandOrConnectedPropertyApi.TransactionDetails, LandOrConnectedPropertyResponse] {

  def merge(
    landConnectedPartyData: NonEmptyList[LandOrConnectedPropertyApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[LandOrConnectedPropertyApi.TransactionDetails, SippLandConnectedParty.TransactionDetail](
        landConnectedPartyData,
        etmpData,
        _.transformInto[SippLandConnectedParty.TransactionDetail],
        _.landConnectedPartyTransactions,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            version = None,
            landConnectedParty = maybeTransactions.map(transactions =>
              SippLandConnectedParty(transactions.length, None, Some(transactions.toList))
            )
          )
      )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): LandOrConnectedPropertyResponse =
    LandOrConnectedPropertyResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.landConnectedParty
          .map(transaction =>
            transaction.transactionDetails
              .getOrElse(List.empty)
              .map(property => transformTransactionDetails(member, transaction.noOfTransactions, property))
          )
          .getOrElse(List.empty)
      }
    )

  private def transformTransactionDetails(
    member: MemberDetails,
    transactionCount: Int,
    landConnectedParty: SippLandConnectedParty.TransactionDetail
  ): LandOrConnectedPropertyApi.TransactionDetails =
    landConnectedParty
      .into[LandOrConnectedPropertyApi.TransactionDetails]
      .withFieldConst(_.nameDOB, toNameDOB(member))
      .withFieldConst(_.nino, toNinoType(member))
      .withFieldConst(_.transactionCount, transactionCount.some)
      .transform
}
