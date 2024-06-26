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

import cats.data.NonEmptyList
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.PSRSubmissionResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  EtmpReportDetailsOps,
  LandArmsLengthTransformer,
  LandConnectedPartyTransformer,
  OutstandingLoansTransformer,
  TangibleMoveablePropertyTransformer,
  UnquotedSharesTransformer
}

@Singleton()
class PSRSubmissionTransformer @Inject()(
  assetsFromConnectedPartyTransformer: AssetsFromConnectedPartyTransformer,
  landArmsLengthTransformer: LandArmsLengthTransformer,
  landConnectedPartyTransformer: LandConnectedPartyTransformer,
  outstandingLoansTransformer: OutstandingLoansTransformer,
  tangibleMoveablePropertyTransformer: TangibleMoveablePropertyTransformer,
  unquotedSharesTransformer: UnquotedSharesTransformer
) {

  def transform(etmpResponse: SippPsrSubmissionEtmpResponse): PSRSubmissionResponse = {
    val membTxs = etmpResponse.memberAndTransactions

    PSRSubmissionResponse(
      details = etmpResponse.reportDetails.toApi,
      landConnectedParty = membTxs
        .flatMap(mTxs => NonEmptyList.fromList(landConnectedPartyTransformer.transformToResponse(mTxs).transactions)),
      otherAssetsConnectedParty = membTxs.flatMap(
        _ => None //TODO: Implement me
      ),
      landArmsLength = membTxs.flatMap(
        mTxs => NonEmptyList.fromList(landArmsLengthTransformer.transformToResponse(mTxs).transactions)
      ),
      tangibleProperty = membTxs.flatMap(
        _ => None //TODO: Implement me
      ),
      loanOutstanding = membTxs.flatMap(
        _ => None //TODO: Implement me
      ),
      unquotedShares = membTxs.flatMap(
        _ => None //TODO: Implement me
      )
    )
  }
}
