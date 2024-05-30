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

import uk.gov.hmrc.pensionschemereturnsipp.models.{api, etmp}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.AssetsFromConnectedPartyRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, SippOtherAssetsConnectedParty}

import javax.inject.{Inject, Singleton}

@Singleton
class AssetsFromConnectedPartyTransformer @Inject()() {

  def merge(
    assetsFromConnectedParty: List[api.AssetsFromConnectedPartyRequest.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] = {

    val assetsFromConnectedPartyByMember =
      assetsFromConnectedParty.groupBy(k => k.nameDOB.firstName -> k.nameDOB.lastName -> k.nameDOB.dob -> k.nino.nino)

    val etmpDataByMember = etmpData.groupBy(
      k => k.memberDetails.firstName -> k.memberDetails.lastName -> k.memberDetails.dateOfBirth -> k.memberDetails.nino
    )

    val updatedEtmpDataByMember = etmpDataByMember.flatMap {
      case (memberKey, etmpTxsByMember) =>
        val assetsFromConnectedPartyToUpdate = assetsFromConnectedPartyByMember
          .get(memberKey)
          .map { landArmsTxs =>
            landArmsTxs.map(transformSingle)
          }

        etmpTxsByMember.map(
          etmpTx =>
            etmpTx.copy(
              otherAssetsConnectedParty = assetsFromConnectedPartyToUpdate.map(
                otherAssetTrx => SippOtherAssetsConnectedParty(otherAssetTrx.length, Some(otherAssetTrx))
              )
            )
        )
    }.toList

    val newMembers = assetsFromConnectedPartyByMember.keySet.diff(etmpDataByMember.keySet)

    val newEtmpDataByMember =
      newMembers.toList.flatMap(memberKey => assetsFromConnectedPartyByMember.get(memberKey)).flatMap(transform)

    updatedEtmpDataByMember ++ newEtmpDataByMember
  }

  def transform(list: List[api.AssetsFromConnectedPartyRequest.TransactionDetails]): List[EtmpMemberAndTransactions] =
    list
      .groupMap(p => p.nameDOB -> p.nino)(transformSingle)
      .map {
        case ((nameDoB, nino), transactions) =>
          val otherAssets = SippOtherAssetsConnectedParty(transactions.length, Some(transactions).filter(_.nonEmpty))
          EtmpMemberAndTransactions(
            status = SectionStatus.New,
            version = None,
            memberDetails = toMemberDetails(nameDoB, nino),
            landConnectedParty = None,
            otherAssetsConnectedParty = Some(otherAssets),
            landArmsLength = None,
            tangibleProperty = None,
            loanOutstanding = None,
            unquotedShares = None
          )
      }
      .toList

  private def transformSingle(
    property: api.AssetsFromConnectedPartyRequest.TransactionDetails
  ): etmp.SippOtherAssetsConnectedParty.TransactionDetail =
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
}
