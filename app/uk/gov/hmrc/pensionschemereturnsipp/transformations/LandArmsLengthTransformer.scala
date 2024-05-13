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
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, SippLandArmsLength}

import javax.inject.{Inject, Singleton}

@Singleton
class LandArmsLengthTransformer @Inject() {
  def transform(list: List[LandOrConnectedProperty.TransactionDetails]): List[EtmpMemberAndTransactions] =
    list
      .groupMap(p => p.nameDOB -> p.nino)(transformSingle)
      .map {
        case ((nameDoB, nino), transactions) =>
          val armsLength = SippLandArmsLength(transactions.length, Some(transactions).filter(_.nonEmpty))
          EtmpMemberAndTransactions(
            status = SectionStatus.New,
            version = None,
            memberDetails = toMemberDetails(nameDoB, nino),
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
      landOrPropertyinUK = toEtmp(property.landOrPropertyinUK),
      addressDetails = toEtmp(property.addressDetails),
      registryDetails = toEtmp(property.registryDetails),
      acquiredFromName = property.acquiredFromName,
      totalCost = property.totalCost,
      independentValution = toEtmp(property.independentValuation),
      jointlyHeld = toEtmp(property.jointlyHeld),
      noOfPersonsIfJointlyHeld = property.noOfPersons,
      residentialSchedule29A = toEtmp(property.residentialSchedule29A),
      isLeased = toEtmp(property.isLeased),
      noOfPersonsForLessees = property.lesseeDetails.flatMap(_.countOfLessees),
      anyOfLesseesConnected = property.lesseeDetails.map(l => toEtmp(l.anyOfLesseesConnected)),
      lesseesGrantedAt = property.lesseeDetails.map(_.leaseGrantedDate),
      annualLeaseAmount = property.lesseeDetails.map(_.annualLeaseAmount),
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      isPropertyDisposed = toEtmp(property.isPropertyDisposed),
      disposedPropertyProceedsAmt = property.disposalDetails.map(_.disposedPropertyProceedsAmt),
      purchaserNamesIfDisposed = property.disposalDetails.map(_.namesOfPurchasers),
      anyOfPurchaserConnected = property.disposalDetails.map(d => toEtmp(d.anyPurchaserConnected)),
      independentValutionDisposal = property.disposalDetails.map(d => toEtmp(d.independentValuationDisposal)),
      propertyFullyDisposed = property.disposalDetails.map(d => toEtmp(d.propertyFullyDisposed))
    )
}
