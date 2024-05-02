/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.pensionschemereturnsipp.utils

import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.New
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  SippLandArmsLength,
  SippLandConnectedParty,
  SippLoanOutstanding,
  SippOtherAssetsConnectedParty,
  SippTangibleProperty,
  SippUnquotedShares
}
import uk.gov.hmrc.pensionschemereturnsipp.models.requests.SippPsrSubmissionEtmpRequest

trait SippEtmpDummyTestValues extends SippEtmpTestValues {

  private val sippLandConnectedPartyLong = SippLandConnectedParty(
    noOfTransactions = 1,
    transactionDetails = Some(List.fill(1)(sippLandConnectedPartyTransactionDetail))
  )

//  private val sippLandConnectedPartyLong2 = SippLandConnectedParty(
//    noOfTransactions = 2,
//    transactionDetails = Some(List.fill(2)(sippLandConnectedPartyTransactionDetail))
//  )

  private val sippOtherAssetsConnectedPartyLong = SippOtherAssetsConnectedParty(
    noOfTransactions = 1,
    transactionDetails = Some(List.fill(1)(sippOtherAssetsConnectedPartyTransactionDetail))
  )

  private val sippLandArmsLengthLong = SippLandArmsLength(
    noOfTransactions = 1,
    transactionDetails = Some(List.fill(1)(sippLandArmsLengthTransactionDetail))
  )

  private val sippTangiblePropertyLong = SippTangibleProperty(
    noOfTransactions = 1,
    transactionDetails = Some(List.fill(1)(sippTangiblePropertyTransactionalDetail))
  )

  private val sippLoanOutstandingLong = SippLoanOutstanding(
    noOfTransactions = 1,
    transactionDetails = Some(List.fill(1)(sippLoanOutstandingTransactionalDetail))
  )

  private val sippUnquotedSharesLong = SippUnquotedShares(
    noOfTransactions = 1,
    transactionDetails = Some(List.fill(1)(sippUnquotedSharesTransactionalDetail))
  )

  private val sippMemberAndTransactionsLongVersion: EtmpMemberAndTransactions = EtmpMemberAndTransactions(
    version = None,
    status = New,
    memberDetails = sippMemberDetails,
    landConnectedParty = Some(sippLandConnectedPartyLong),
    otherAssetsConnectedParty = Some(sippOtherAssetsConnectedPartyLong),
    landArmsLength = Some(sippLandArmsLengthLong),
    tangibleProperty = Some(sippTangiblePropertyLong),
    loanOutstanding = Some(sippLoanOutstandingLong),
    unquotedShares = Some(sippUnquotedSharesLong)
  )

//  private val sippMemberAndTransactionsLongVersion2: SippMemberAndTransactions = SippMemberAndTransactions(
//    version = None,
//    status = New,
//    memberDetails = sippMemberDetails,
//    landConnectedParty = Some(sippLandConnectedPartyLong2),
//    otherAssetsConnectedParty = Some(sippOtherAssetsConnectedPartyLong),
//    landArmsLength = None, //Some(etmpSippLandArmsLengthLong),
//    tangibleProperty = None, //Some(etmpSippTangiblePropertyLong),
//    loanOutstanding = None, //Some(etmpSippLoanOutstandingLong),
//    unquotedShares = None //Some(etmpSippUnquotedSharesLong)
//  )

//  private val etmpSippMemberAndTransactionsJustMemberDetailsVersion: EtmpSippMemberAndTransactions = EtmpSippMemberAndTransactions(
//    version = None,
//    status = New,
//    memberDetails = memberDetails,
//    landConnectedParty = None, //Some(etmpSippLandConnectedPartyLong),
//    otherAssetsConnectedParty = None, //Some(etmpSippOtherAssetsConnectedPartyLong),
//    landArmsLength = None, //Some(etmpSippLandArmsLengthLong),
//    tangibleProperty = None, // Some(etmpSippTangiblePropertyLong),
//    loanOutstanding = None, //Some(etmpSippLoanOutstandingLong),
//    unquotedShares = None //Some(etmpSippUnquotedSharesLong)
//  )

  private val membersAndTransactions: List[EtmpMemberAndTransactions] =
    List.fill(5000)(sippMemberAndTransactionsLongVersion) /* ++ List.fill(500)(sippMemberAndTransactionsLongVersion2) */
//  private val membersAndTransactionsV2: List[EtmpSippMemberAndTransactions] = List.fill(57500)(etmpSippMemberAndTransactionsJustMemberDetailsVersion)

  // SIPP - ETMP
  val fullSippPsrSubmissionRequestLongV2: SippPsrSubmissionEtmpRequest = SippPsrSubmissionEtmpRequest(
    reportDetails = reportDetails,
    accountingPeriodDetails = Some(accountingPeriodDetails),
    memberAndTransactions = Some(membersAndTransactions),
    psrDeclaration = None
  )

}
