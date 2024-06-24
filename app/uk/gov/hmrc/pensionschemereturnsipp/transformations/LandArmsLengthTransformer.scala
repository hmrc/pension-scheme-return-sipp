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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{DisposalDetails, LesseeDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{LandOrConnectedPropertyApi, LandOrConnectedPropertyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippLandArmsLength}

import javax.inject.{Inject, Singleton}

@Singleton
class LandArmsLengthTransformer @Inject()()
    extends Transformer[LandOrConnectedPropertyApi.TransactionDetails, LandOrConnectedPropertyResponse] {
  def merge(
    landArmsData: NonEmptyList[LandOrConnectedPropertyApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[LandOrConnectedPropertyApi.TransactionDetails, SippLandArmsLength.TransactionDetail](
        landArmsData,
        etmpData,
        transformSingle,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            landArmsLength =
              maybeTransactions.map(transactions => SippLandArmsLength(transactions.length, Some(transactions.toList)))
          )
      )

  private def transformSingle(
    property: LandOrConnectedPropertyApi.TransactionDetails
  ): SippLandArmsLength.TransactionDetail =
    SippLandArmsLength.TransactionDetail(
      acquisitionDate = property.acquisitionDate,
      landOrPropertyinUK = property.landOrPropertyinUK,
      addressDetails = property.addressDetails.toEtmp,
      registryDetails = property.registryDetails,
      acquiredFromName = property.acquiredFromName,
      totalCost = property.totalCost,
      independentValution = property.independentValuation,
      jointlyHeld = property.jointlyHeld,
      noOfPersonsIfJointlyHeld = property.noOfPersons,
      residentialSchedule29A = property.residentialSchedule29A,
      isLeased = property.isLeased,
      noOfPersonsForLessees = property.lesseeDetails.flatMap(_.countOfLessees),
      anyOfLesseesConnected = property.lesseeDetails.map(l => l.anyOfLesseesConnected),
      lesseesGrantedAt = property.lesseeDetails.map(_.leaseGrantedDate),
      annualLeaseAmount = property.lesseeDetails.map(_.annualLeaseAmount),
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      isPropertyDisposed = property.isPropertyDisposed,
      disposedPropertyProceedsAmt = property.disposalDetails.map(_.disposedPropertyProceedsAmt),
      purchaserNamesIfDisposed = property.disposalDetails.map(_.namesOfPurchasers),
      anyOfPurchaserConnected = property.disposalDetails.map(d => d.anyPurchaserConnected),
      independentValutionDisposal = property.disposalDetails.map(d => d.independentValuationDisposal),
      propertyFullyDisposed = property.disposalDetails.map(d => d.propertyFullyDisposed)
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
    LandOrConnectedPropertyApi.TransactionDetails(
      nameDOB = toNameDOB(member),
      nino = toNinoType(member),
      acquisitionDate = armsLength.acquisitionDate,
      landOrPropertyinUK = armsLength.landOrPropertyinUK,
      addressDetails = armsLength.addressDetails.fromEtmp,
      registryDetails = armsLength.registryDetails,
      acquiredFromName = armsLength.acquiredFromName,
      totalCost = armsLength.totalCost,
      independentValuation = armsLength.independentValution,
      jointlyHeld = armsLength.jointlyHeld,
      noOfPersons = armsLength.noOfPersonsIfJointlyHeld,
      residentialSchedule29A = armsLength.residentialSchedule29A,
      isLeased = armsLength.isLeased,
      lesseeDetails =
        for {
          count <- armsLength.noOfPersonsForLessees
          isAnyConnected <- armsLength.anyOfLesseesConnected
          leaseGrantedDate <- armsLength.lesseesGrantedAt
          annualLeaseAmount <- armsLength.annualLeaseAmount
        } yield LesseeDetails(
          Some(count),
          armsLength.purchaserNamesIfDisposed,
          isAnyConnected,
          leaseGrantedDate,
          annualLeaseAmount
        ),
      totalIncomeOrReceipts = armsLength.totalIncomeOrReceipts,
      isPropertyDisposed = armsLength.isPropertyDisposed,
      disposalDetails =
        for {
          disposedPropertyProceedsAmt <- armsLength.disposedPropertyProceedsAmt
          purchaserNamesIfDisposed <- armsLength.purchaserNamesIfDisposed
          anyOfPurchaserConnected <- armsLength.anyOfPurchaserConnected
          independentValuationDisposal <- armsLength.independentValutionDisposal
          propertyFullyDisposed <- armsLength.propertyFullyDisposed
        } yield DisposalDetails(
          disposedPropertyProceedsAmt,
          purchaserNamesIfDisposed,
          anyOfPurchaserConnected,
          independentValuationDisposal,
          propertyFullyDisposed
        ),
      transactionCount = Some(transactionCount)
    )
}
