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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{DisposalDetails, NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{TangibleMoveablePropertyApi, TangibleMoveablePropertyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippTangibleProperty}

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
        transformSingle,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            tangibleProperty = maybeTransactions.map(
              transactions => SippTangibleProperty(transactions.length, Some(transactions.toList))
            )
          )
      )

  private def transformSingle(
    property: TangibleMoveablePropertyApi.TransactionDetails
  ): SippTangibleProperty.TransactionDetail =
    SippTangibleProperty.TransactionDetail(
      assetDescription = property.assetDescription,
      acquisitionDate = property.acquisitionDate,
      totalCost = property.totalCost,
      acquiredFromName = property.acquiredFromName,
      independentValuation = property.independentValuation,
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      costOrMarket = property.costOrMarket,
      costMarketValue = property.costMarketValue,
      isPropertyDisposed = property.isPropertyDisposed,
      disposedPropertyProceedsAmt = property.disposalDetails.map(_.disposedPropertyProceedsAmt),
      purchaserNamesIfDisposed = property.disposalDetails.map(_.namesOfPurchasers),
      anyOfPurchaserConnected = property.disposalDetails.map(_.anyPurchaserConnected),
      independentValuationDisposal = property.disposalDetails.map(_.independentValuationDisposal),
      propertyFullyDisposed = property.disposalDetails.map(_.propertyFullyDisposed)
    )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): TangibleMoveablePropertyResponse =
    TangibleMoveablePropertyResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.tangibleProperty
          .map(
            transaction =>
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
    TangibleMoveablePropertyApi.TransactionDetails(
      nameDOB = NameDOB(member.firstName, member.lastName, member.dateOfBirth),
      nino = NinoType(member.nino, member.reasonNoNINO),
      assetDescription = trx.assetDescription,
      acquisitionDate = trx.acquisitionDate,
      totalCost = trx.totalCost,
      acquiredFromName = trx.acquiredFromName,
      independentValuation = trx.independentValuation,
      totalIncomeOrReceipts = trx.totalIncomeOrReceipts,
      costOrMarket = trx.costOrMarket,
      costMarketValue = trx.costMarketValue,
      isPropertyDisposed = trx.isPropertyDisposed,
      disposalDetails = Option.when(trx.isPropertyDisposed.boolean)(
        DisposalDetails(
          disposedPropertyProceedsAmt = trx.disposedPropertyProceedsAmt.get,
          namesOfPurchasers = trx.purchaserNamesIfDisposed.get,
          anyPurchaserConnected = trx.anyOfPurchaserConnected.get,
          independentValuationDisposal = trx.independentValuationDisposal.get,
          propertyFullyDisposed = trx.propertyFullyDisposed.get
        )
      ),
      transactionCount = Some(transactionCount)
    )
}
