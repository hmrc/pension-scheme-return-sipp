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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.NinoType
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, SippLandArmsLength}

import javax.inject.{Inject, Singleton}

@Singleton
class LandArmsLengthTransformer @Inject() {

  def merge(
    landArmsData: List[LandOrConnectedProperty.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] = {

    val landArmsDataByMember =
      landArmsData.groupBy(k => k.nameDOB.firstName -> k.nameDOB.lastName -> k.nameDOB.dob -> k.nino.nino)

    val etmpDataByMember = etmpData.groupBy(
      k => k.memberDetails.firstName -> k.memberDetails.lastName -> k.memberDetails.dateOfBirth -> k.memberDetails.nino
    )

    val updatedEtmpDataByMember = etmpDataByMember.flatMap {
      case (memberKey, etmpTxsByMember) =>
        val landArmsDataToUpdate = landArmsDataByMember
          .get(memberKey)
          .map { landArmsTxs =>
            landArmsTxs.map(transformSingle)
          }

        etmpTxsByMember.map(
          etmpTx =>
            etmpTx.copy(
              landArmsLength =
                landArmsDataToUpdate.map(landArmsTxs => SippLandArmsLength(landArmsTxs.length, Some(landArmsTxs)))
            )
        )
    }.toList

    val newMembers = landArmsDataByMember.keySet.diff(etmpDataByMember.keySet)

    val newEtmpDataByMember =
      newMembers.toList.flatMap(memberKey => landArmsDataByMember.get(memberKey)).flatMap(transform)

    updatedEtmpDataByMember ++ newEtmpDataByMember
  }

  def transform(list: List[LandOrConnectedProperty.TransactionDetails]): List[EtmpMemberAndTransactions] =
    list
      .groupBy(p => p.nameDOB -> p.nino.nino)
      .map {
        case ((nameDoB, nino), transactions) =>
          val reasonNoNino = transactions.find(_.nino.reasonNoNino.nonEmpty).flatMap(_.nino.reasonNoNino)
          val armsLength =
            SippLandArmsLength(transactions.length, Some(transactions.map(transformSingle)).filter(_.nonEmpty))
          EtmpMemberAndTransactions(
            status = SectionStatus.New,
            version = None,
            memberDetails = toMemberDetails(nameDoB, NinoType(nino, reasonNoNino)),
            landConnectedParty = None,
            otherAssetsConnectedParty = None,
            landArmsLength = Some(armsLength),
            tangibleProperty = None,
            loanOutstanding = None,
            unquotedShares = None
          )
      }
      .toList

  private def transformSingle(
    property: LandOrConnectedProperty.TransactionDetails
  ): SippLandArmsLength.TransactionDetail =
    SippLandArmsLength.TransactionDetail(
      acquisitionDate = property.acquisitionDate,
      landOrPropertyinUK = property.landOrPropertyinUK.toEtmp,
      addressDetails = property.addressDetails.toEtmp,
      registryDetails = property.registryDetails.toEtmp,
      acquiredFromName = property.acquiredFromName,
      totalCost = property.totalCost,
      independentValution = property.independentValuation.toEtmp,
      jointlyHeld = property.jointlyHeld.toEtmp,
      noOfPersonsIfJointlyHeld = property.noOfPersons,
      residentialSchedule29A = property.residentialSchedule29A.toEtmp,
      isLeased = property.isLeased.toEtmp,
      noOfPersonsForLessees = property.lesseeDetails.flatMap(_.countOfLessees),
      anyOfLesseesConnected = property.lesseeDetails.map(l => l.anyOfLesseesConnected.toEtmp),
      lesseesGrantedAt = property.lesseeDetails.map(_.leaseGrantedDate),
      annualLeaseAmount = property.lesseeDetails.map(_.annualLeaseAmount),
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      isPropertyDisposed = property.isPropertyDisposed.toEtmp,
      disposedPropertyProceedsAmt = property.disposalDetails.map(_.disposedPropertyProceedsAmt),
      purchaserNamesIfDisposed = property.disposalDetails.map(_.namesOfPurchasers),
      anyOfPurchaserConnected = property.disposalDetails.map(d => d.anyPurchaserConnected.toEtmp),
      independentValutionDisposal = property.disposalDetails.map(d => d.independentValuationDisposal.toEtmp),
      propertyFullyDisposed = property.disposalDetails.map(d => d.propertyFullyDisposed.toEtmp)
    )
}
