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
import uk.gov.hmrc.pensionschemereturnsipp.models.api
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{LandOrConnectedPropertyApi, LandOrConnectedPropertyResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  MemberDetails,
  SippLandConnectedParty
}

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
        transformSingle,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            landConnectedParty = maybeTransactions.map(
              transactions => SippLandConnectedParty(transactions.length, Some(transactions.toList))
            )
          )
      )

  private def transformSingle(
    property: LandOrConnectedPropertyApi.TransactionDetails
  ): SippLandConnectedParty.TransactionDetail =
    SippLandConnectedParty.TransactionDetail(
      acquisitionDate = property.acquisitionDate,
      landOrPropertyInUK = property.landOrPropertyInUK,
      addressDetails = property.addressDetails,
      registryDetails = property.registryDetails,
      acquiredFromName = property.acquiredFromName,
      totalCost = property.totalCost,
      independentValuation = property.independentValuation,
      jointlyHeld = property.jointlyHeld,
      noOfPersons = property.noOfPersons,
      residentialSchedule29A = property.residentialSchedule29A,
      isLeased = property.isLeased,
      lesseeDetails = property.lesseeDetails,
      totalIncomeOrReceipts = property.totalIncomeOrReceipts,
      isPropertyDisposed = property.isPropertyDisposed,
      disposalDetails = property.disposalDetails
    )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): LandOrConnectedPropertyResponse =
    LandOrConnectedPropertyResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.landConnectedParty
          .map(
            transaction =>
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
    LandOrConnectedPropertyApi.TransactionDetails(
      nameDOB = toNameDOB(member),
      nino = toNinoType(member),
      acquisitionDate = landConnectedParty.acquisitionDate,
      landOrPropertyInUK = landConnectedParty.landOrPropertyInUK,
      addressDetails = landConnectedParty.addressDetails,
      registryDetails = landConnectedParty.registryDetails,
      acquiredFromName = landConnectedParty.acquiredFromName,
      totalCost = landConnectedParty.totalCost,
      independentValuation = landConnectedParty.independentValuation,
      jointlyHeld = landConnectedParty.jointlyHeld,
      noOfPersons = landConnectedParty.noOfPersons,
      residentialSchedule29A = landConnectedParty.residentialSchedule29A,
      isLeased = landConnectedParty.isLeased,
      lesseeDetails = landConnectedParty.lesseeDetails,
      totalIncomeOrReceipts = landConnectedParty.totalIncomeOrReceipts,
      isPropertyDisposed = landConnectedParty.isPropertyDisposed,
      disposalDetails = landConnectedParty.disposalDetails,
      transactionCount = Some(transactionCount)
    )
}
