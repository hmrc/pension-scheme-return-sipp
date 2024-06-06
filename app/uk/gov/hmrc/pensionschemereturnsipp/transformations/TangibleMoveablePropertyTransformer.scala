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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.TangibleMoveablePropertyRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, SippTangibleProperty}

import javax.inject.{Inject, Singleton}

@Singleton
class TangibleMoveablePropertyTransformer @Inject()
    extends Transformer[TangibleMoveablePropertyRequest.TransactionDetails] {

  def merge(
    tangibleMovableProperties: NonEmptyList[TangibleMoveablePropertyRequest.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[TangibleMoveablePropertyRequest.TransactionDetails, SippTangibleProperty.TransactionDetail](
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
    property: TangibleMoveablePropertyRequest.TransactionDetails
  ): SippTangibleProperty.TransactionDetail =
    SippTangibleProperty.TransactionDetail(
      assetDescription = property.assetDescription,
      acquisitionDate = property.acquisitionDate,
      totalCost = property.totalCost,
      acquiredFromName = property.acquiredFromName,
      independentValution = property.independentValuation,
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      costOrMarket = property.costOrMarket,
      costMarketValue = property.costMarketValue,
      isPropertyDisposed = property.isPropertyDisposed,
      disposedPropertyProceedsAmt = property.disposalDetails.map(_.disposedPropertyProceedsAmt),
      purchaserNamesIfDisposed = property.disposalDetails.map(_.namesOfPurchasers),
      anyOfPurchaserConnected = property.disposalDetails.map(_.anyPurchaserConnected),
      independentValutionDisposal = property.disposalDetails.map(_.independentValuationDisposal),
      propertyFullyDisposed = property.disposalDetails.map(_.propertyFullyDisposed)
    )
}
