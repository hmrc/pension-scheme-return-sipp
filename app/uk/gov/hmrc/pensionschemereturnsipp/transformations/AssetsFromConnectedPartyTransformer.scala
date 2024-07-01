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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{AssetsFromConnectedPartyApi, AssetsFromConnectedPartyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  MemberDetails,
  SippOtherAssetsConnectedParty
}

import javax.inject.{Inject, Singleton}

@Singleton
class AssetsFromConnectedPartyTransformer @Inject()
    extends Transformer[AssetsFromConnectedPartyApi.TransactionDetails, AssetsFromConnectedPartyResponse] {

  def merge(
    assetsFromConnectedParty: NonEmptyList[AssetsFromConnectedPartyApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[AssetsFromConnectedPartyApi.TransactionDetails, SippOtherAssetsConnectedParty.TransactionDetail](
        assetsFromConnectedParty,
        etmpData,
        transformSingle,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            otherAssetsConnectedParty = maybeTransactions.map(
              transactions => SippOtherAssetsConnectedParty(transactions.length, Some(transactions.toList))
            )
          )
      )

  private def transformSingle(
    property: AssetsFromConnectedPartyApi.TransactionDetails
  ): SippOtherAssetsConnectedParty.TransactionDetail =
    SippOtherAssetsConnectedParty.TransactionDetail(
      acquisitionDate = property.acquisitionDate,
      assetDescription = property.assetDescription,
      acquisitionOfShares = property.acquisitionOfShares,
      sharesCompanyDetails = property.shareCompanyDetails.map(details => toEtmp(details)),
      acquiredFromName = property.acquiredFromName,
      totalCost = property.totalCost,
      independentValution = property.independentValuation,
      tangibleSchedule29A = property.tangibleSchedule29A,
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      isPropertyDisposed = property.isPropertyDisposed,
      disposedPropertyProceedsAmt = property.disposalDetails.map(_.disposedPropertyProceedsAmt),
      purchaserNamesIfDisposed = property.disposalDetails.map(_.namesOfPurchasers),
      anyOfPurchaserConnected = property.disposalDetails.map(_.anyPurchaserConnected),
      independentValutionDisposal = property.disposalDetails.map(_.independentValuationDisposal),
      disposalOfShares = property.disposalOfShares,
      noOfSharesHeld = property.noOfSharesHeld,
      propertyFullyDisposed = property.disposalDetails.map(_.propertyFullyDisposed)
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
    trx: SippOtherAssetsConnectedParty.TransactionDetail
  ): AssetsFromConnectedPartyApi.TransactionDetails =
    AssetsFromConnectedPartyApi.TransactionDetails(
      nameDOB = NameDOB(member.firstName, member.lastName, member.dateOfBirth),
      nino = NinoType(member.nino, member.reasonNoNINO),
      acquisitionDate = trx.acquisitionDate,
      assetDescription = trx.assetDescription,
      acquisitionOfShares = trx.acquisitionOfShares,
      shareCompanyDetails = trx.sharesCompanyDetails,
      acquiredFromName = trx.acquiredFromName,
      totalCost = trx.totalCost,
      independentValuation = trx.independentValution,
      tangibleSchedule29A = trx.tangibleSchedule29A,
      totalIncomeOrReceipts = trx.totalIncomeOrReceipts,
      isPropertyDisposed = trx.isPropertyDisposed,
      disposalDetails = Option.when(trx.isPropertyDisposed.boolean)(
        DisposalDetails(
          disposedPropertyProceedsAmt = trx.disposedPropertyProceedsAmt.get,
          namesOfPurchasers = trx.purchaserNamesIfDisposed.get,
          anyPurchaserConnected = trx.anyOfPurchaserConnected.get,
          independentValuationDisposal = trx.independentValutionDisposal.get,
          propertyFullyDisposed = trx.propertyFullyDisposed.get
        )
      ),
      disposalOfShares = ???,
      noOfSharesHeld = ???
    )
}
