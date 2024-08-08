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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{LandOrConnectedPropertyApi, LandOrConnectedPropertyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippLandArmsLength}
import io.scalaland.chimney.dsl._

import javax.inject.{Inject, Singleton}

@Singleton
class LandArmsLengthTransformer @Inject()()
    extends Transformer[LandOrConnectedPropertyApi.TransactionDetails, LandOrConnectedPropertyResponse] {
  def merge(
    landArmsData: NonEmptyList[LandOrConnectedPropertyApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions],
    version: Option[String]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[LandOrConnectedPropertyApi.TransactionDetails, SippLandArmsLength.TransactionDetail](
        landArmsData,
        etmpData,
        _.transformInto[SippLandArmsLength.TransactionDetail],
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            version = version,
            landArmsLength = maybeTransactions.map(
              transactions => SippLandArmsLength(transactions.length, version, Some(transactions.toList))
            )
          )
      )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): LandOrConnectedPropertyResponse =
    LandOrConnectedPropertyResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.landArmsLength
          .map(
            transaction =>
              transaction.transactionDetails
                .getOrElse(List.empty)
                .map(armsLength => transformTransactionDetails(member, transaction.noOfTransactions, armsLength))
          )
          .getOrElse(List.empty)
      }
    )

  private def transformTransactionDetails(
    member: MemberDetails,
    transactionCount: Int,
    armsLength: etmp.SippLandArmsLength.TransactionDetail
  ): LandOrConnectedPropertyApi.TransactionDetails =
    armsLength
      .into[LandOrConnectedPropertyApi.TransactionDetails]
      .withFieldConst(_.nameDOB, toNameDOB(member))
      .withFieldConst(_.nino, toNinoType(member))
      .withFieldConst(_.transactionCount, transactionCount.some)
      .transform
}
