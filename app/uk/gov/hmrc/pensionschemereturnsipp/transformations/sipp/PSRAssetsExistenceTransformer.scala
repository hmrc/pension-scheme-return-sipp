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

package uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.PsrAssetCountsResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse

@Singleton
class PSRAssetsExistenceTransformer @Inject()() {
  def transform(etmpResponse: SippPsrSubmissionEtmpResponse): Option[PsrAssetCountsResponse] =
    etmpResponse.memberAndTransactions.map { transactions =>
      val filteredTxs = transactions.filter(_.status != SectionStatus.Deleted)

      PsrAssetCountsResponse(
        interestInLandOrPropertyCount = filteredTxs.flatMap(_.landConnectedParty).map(_.noOfTransactions).sum,
        landArmsLengthCount = filteredTxs.flatMap(_.landArmsLength).map(_.noOfTransactions).sum,
        assetsFromConnectedPartyCount = filteredTxs.flatMap(_.otherAssetsConnectedParty).map(_.noOfTransactions).sum,
        tangibleMoveablePropertyCount = filteredTxs.flatMap(_.tangibleProperty).map(_.noOfTransactions).sum,
        outstandingLoansCount = filteredTxs.flatMap(_.loanOutstanding).map(_.noOfTransactions).sum,
        unquotedSharesCount = filteredTxs.flatMap(_.unquotedShares).map(_.noOfTransactions).sum
      )
    }
}
