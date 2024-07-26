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

import cats.syntax.traverse._
import cats.data.NonEmptyList
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{PSRSubmissionResponse, ReportDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, VersionedAsset}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  LandArmsLengthTransformer,
  LandConnectedPartyTransformer,
  OutstandingLoansTransformer,
  TangibleMoveablePropertyTransformer,
  UnquotedSharesTransformer
}
import io.scalaland.chimney.dsl._

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
    val version = versionOrPsrVersion(_, etmpResponse.reportDetails.psrVersion)
    val membTxs: List[EtmpMemberAndTransactions] = etmpResponse.memberAndTransactions.sequence.flatten

    PSRSubmissionResponse(
      details = etmpResponse.reportDetails.transformInto[ReportDetails],
      accountingPeriodDetails = etmpResponse.accountingPeriodDetails,
      landConnectedParty = membTxs.flatMap(
        mTxs => NonEmptyList.fromList(landConnectedPartyTransformer.transformToResponse(mTxs).transactions)
      ),
      otherAssetsConnectedParty = membTxs.flatMap(
        mTxs => NonEmptyList.fromList(assetsFromConnectedPartyTransformer.transformToResponse(mTxs).transactions)
      ),
      landArmsLength = membTxs.flatMap(
        mTxs => NonEmptyList.fromList(landArmsLengthTransformer.transformToResponse(mTxs).transactions)
      ),
      tangibleProperty = membTxs.flatMap(
        mTxs => NonEmptyList.fromList(tangibleMoveablePropertyTransformer.transformToResponse(mTxs).transactions)
      ),
      loanOutstanding = membTxs.flatMap(
        mTxs => NonEmptyList.fromList(outstandingLoansTransformer.transformToResponse(mTxs).transactions)
      ),
      unquotedShares = membTxs.flatMap(
        mTxs => NonEmptyList.fromList(unquotedSharesTransformer.transformToResponse(mTxs).transactions)
      ),
      Versions(
        landConnectedParty = version(membTxs.flatMap(_.landConnectedParty)),
        landArmsLength = version(membTxs.flatMap(_.landArmsLength)),
        otherAssetsConnectedParty = version(membTxs.flatMap(_.otherAssetsConnectedParty)),
        tangibleProperty = version(membTxs.flatMap(_.tangibleProperty)),
        loanOutstanding = version(membTxs.flatMap(_.loanOutstanding)),
        unquotedShares = version(membTxs.flatMap(_.unquotedShares))
      )
    )
  }

  private def versionOrPsrVersion(
    versionedAssets: Iterable[VersionedAsset],
    psrVersion: Option[String]
  ): Option[Version] = {
    val version = versionedAssets
      .find(_.version != psrVersion)
      .map(_.version)
      .getOrElse(psrVersion)

    version.map(Version(_))
  }
}
