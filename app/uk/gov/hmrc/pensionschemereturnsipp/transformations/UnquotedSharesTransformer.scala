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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{UnquotedShareApi, UnquotedShareResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippUnquotedShares}

import javax.inject.{Inject, Singleton}

@Singleton
class UnquotedSharesTransformer @Inject()
    extends Transformer[UnquotedShareApi.TransactionDetail, UnquotedShareResponse] {

  def merge(
    unquotedShares: NonEmptyList[UnquotedShareApi.TransactionDetail],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[UnquotedShareApi.TransactionDetail, SippUnquotedShares.TransactionDetail](
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
    details: UnquotedShareApi.TransactionDetail
  ): SippUnquotedShares.TransactionDetail =
    SippUnquotedShares.TransactionDetail(
      sharesCompanyDetails = toEtmp(details.shareCompanyDetails),
      acquiredFromName = details.acquiredFromName,
      totalCost = details.transactionDetail.totalCost,
      independentValution = details.transactionDetail.independentValuation,
      noOfSharesSold = details.transactionDetail.noOfIndependentValuationSharesSold,
      totalDividendsIncome = details.transactionDetail.totalDividendsIncome,
      sharesDisposed = details.sharesDisposed,
      sharesDisposalDetails = details.sharesDisposalDetails.map(toEtmp),
      noOfSharesHeld = Some(details.noOfSharesHeld)
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
    landConnectedParty: etmp.SippUnquotedShares.TransactionDetail
  ): UnquotedShareApi.TransactionDetail = ???
}
