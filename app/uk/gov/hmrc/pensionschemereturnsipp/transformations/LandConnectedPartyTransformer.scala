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

import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedProperty
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, SippLandConnectedParty}

import javax.inject.{Inject, Singleton}

@Singleton
class LandConnectedPartyTransformer @Inject() {

  def merge(
    landConnectedPartyData: List[LandOrConnectedProperty.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] = {

    val landConnectedDataByMember =
      landConnectedPartyData.groupBy(k => k.nameDOB.firstName -> k.nameDOB.lastName -> k.nameDOB.dob -> k.nino.nino)

    val etmpDataByMember = etmpData.groupBy(
      k => k.memberDetails.firstName -> k.memberDetails.lastName -> k.memberDetails.dateOfBirth -> k.memberDetails.nino
    )

    val updatedEtmpDataByMember = etmpDataByMember.flatMap {
      case (memberKey, etmpTxsByMember) =>
        val landConnectedDataToUpdate = landConnectedDataByMember
          .get(memberKey)
          .map { landArmsTxs =>
            landArmsTxs.map(transformSingle)
          }

        etmpTxsByMember.map(
          etmpTx =>
            etmpTx.copy(
              landConnectedParty = landConnectedDataToUpdate.map(
                landArmsTxs => SippLandConnectedParty(landArmsTxs.length, Some(landArmsTxs))
              )
            )
        )
    }.toList

    val newMembers = landConnectedDataByMember.keySet.diff(etmpDataByMember.keySet)

    val newEtmpDataByMember =
      newMembers.toList.flatMap(memberKey => landConnectedDataByMember.get(memberKey)).flatMap(transform)

    updatedEtmpDataByMember ++ newEtmpDataByMember
  }

  def transform(list: List[LandOrConnectedProperty.TransactionDetails]): List[EtmpMemberAndTransactions] =
    list
      .groupMap(p => p.nameDOB -> p.nino)(transformSingle)
      .map {
        case ((nameDoB, nino), transactions) =>
          val landConnected = SippLandConnectedParty(transactions.length, Some(transactions).filter(_.nonEmpty))
          EtmpMemberAndTransactions(
            status = SectionStatus.New,
            version = None,
            memberDetails = toMemberDetails(nameDoB, nino),
            landConnectedParty = Some(landConnected),
            otherAssetsConnectedParty = None,
            landArmsLength = None,
            tangibleProperty = None,
            loanOutstanding = None,
            unquotedShares = None
          )
      }
      .toList

  private def transformSingle(
    property: LandOrConnectedProperty.TransactionDetails
  ): etmp.SippLandConnectedParty.TransactionDetail =
    SippLandConnectedParty.TransactionDetail(
      acquisitionDate = property.acquisitionDate,
      landOrPropertyInUK = property.landOrPropertyinUK,
      addressDetails = toEtmp(property.addressDetails),
      registryDetails = property.registryDetails,
      acquiredFromName = property.acquiredFromName,
      totalCost = property.totalCost,
      independentValution = property.independentValuation,
      jointlyHeld = property.jointlyHeld,
      noOfPersonsIfJointlyHeld = property.noOfPersons,
      residentialSchedule29A = property.residentialSchedule29A,
      isLeased = property.isLeased,
      noOfPersonsForLessees = property.lesseeDetails.flatMap(_.countOfLessees),
      anyOfLesseesConnected = property.lesseeDetails.map(_.anyOfLesseesConnected),
      leaseGrantedDate = property.lesseeDetails.map(_.leaseGrantedDate),
      annualLeaseAmount = property.lesseeDetails.map(_.annualLeaseAmount),
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      isPropertyDisposed = property.isPropertyDisposed,
      disposedPropertyProceedsAmt = property.disposalDetails.map(_.disposedPropertyProceedsAmt),
      purchaserNamesIfDisposed = property.disposalDetails.map(_.namesOfPurchasers),
      anyOfPurchaserConnected = property.disposalDetails.map(_.anyPurchaserConnected),
      independentValutionDisposal = property.disposalDetails.map(_.independentValuationDisposal),
      propertyFullyDisposed = property.disposalDetails.map(_.propertyFullyDisposed)
    )
}
