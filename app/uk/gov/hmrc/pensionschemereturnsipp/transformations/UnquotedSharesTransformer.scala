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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{
  NameDOB,
  NinoType,
  UnquotedShareDisposalDetail,
  UnquotedShareTransactionDetail
}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{UnquotedShareApi, UnquotedShareResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippUnquotedShares}

import javax.inject.{Inject, Singleton}

@Singleton
class UnquotedSharesTransformer @Inject()
    extends Transformer[UnquotedShareApi.TransactionDetails, UnquotedShareResponse] {

  def merge(
    unquotedShares: NonEmptyList[UnquotedShareApi.TransactionDetails],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[UnquotedShareApi.TransactionDetails, SippUnquotedShares.TransactionDetail](
        unquotedShares,
        etmpData,
        transformSingle,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            unquotedShares = maybeTransactions.map(
              transactions => SippUnquotedShares(transactions.length, Some(transactions.toList))
            )
          )
      )

  private def transformSingle(
    details: UnquotedShareApi.TransactionDetails
  ): SippUnquotedShares.TransactionDetail =
    SippUnquotedShares.TransactionDetail(
      sharesCompanyDetails = details.shareCompanyDetails,
      acquiredFromName = details.acquiredFromName,
      totalCost = details.transactionDetail.totalCost,
      independentValution = details.transactionDetail.independentValuation,
      noOfSharesSold = details.transactionDetail.noOfIndependentValuationSharesSold,
      totalDividendsIncome = details.transactionDetail.totalDividendsIncome,
      sharesDisposed = details.sharesDisposed,
      sharesDisposalDetails = details.sharesDisposalDetails.map(toEtmp),
      noOfSharesHeld = details.noOfSharesHeld
    )

  def transformToResponse(
    memberAndTransactions: List[EtmpMemberAndTransactions]
  ): UnquotedShareResponse =
    UnquotedShareResponse(
      memberAndTransactions.flatMap { memberAndTransaction =>
        val member = memberAndTransaction.memberDetails
        memberAndTransaction.unquotedShares
          .map(
            transaction =>
              transaction.transactionDetails
                .getOrElse(List.empty)
                .map(shares => transformTransactionDetails(member, transaction.noOfTransactions, shares))
          )
          .getOrElse(List.empty)
      }
    )

  def transformTransactionDetails(
    member: MemberDetails,
    transactionCount: Int,
    trx: etmp.SippUnquotedShares.TransactionDetail
  ): UnquotedShareApi.TransactionDetails =
    UnquotedShareApi.TransactionDetails(
      nameDOB = NameDOB(member.firstName, member.lastName, member.dateOfBirth),
      nino = NinoType(member.nino, member.reasonNoNINO),
      shareCompanyDetails = trx.sharesCompanyDetails,
      acquiredFromName = trx.acquiredFromName,
      transactionDetail = UnquotedShareTransactionDetail(
        trx.totalCost,
        trx.independentValution,
        trx.noOfSharesSold,
        trx.totalDividendsIncome
      ),
      sharesDisposed = trx.sharesDisposed,
      sharesDisposalDetails = trx.sharesDisposalDetails
        .map(
          d =>
            UnquotedShareDisposalDetail(
              totalAmount = d.disposedShareAmount,
              nameOfPurchaser = d.purchaserName,
              purchaserConnectedParty = d.disposalConnectedParty,
              independentValuationDisposal = d.independentValutionDisposal
            )
        ),
      noOfSharesHeld = trx.noOfSharesHeld,
      transactionCount = Some(transactionCount)
    )
}
