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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{PSRSubmissionResponse, ReportDetails, Version, Versions}
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
      landConnectedParty =
        NonEmptyList.fromList(landConnectedPartyTransformer.transformToResponse(membTxs).transactions),
      otherAssetsConnectedParty =
        NonEmptyList.fromList(assetsFromConnectedPartyTransformer.transformToResponse(membTxs).transactions),
      landArmsLength = NonEmptyList.fromList(landArmsLengthTransformer.transformToResponse(membTxs).transactions),
      tangibleProperty =
        NonEmptyList.fromList(tangibleMoveablePropertyTransformer.transformToResponse(membTxs).transactions),
      loanOutstanding = NonEmptyList.fromList(outstandingLoansTransformer.transformToResponse(membTxs).transactions),
      unquotedShares = NonEmptyList.fromList(unquotedSharesTransformer.transformToResponse(membTxs).transactions),
      versions = Versions(
        landConnectedParty = version(membTxs.flatMap(_.landConnectedParty)),
        landArmsLength = version(membTxs.flatMap(_.landArmsLength)),
        otherAssetsConnectedParty = version(membTxs.flatMap(_.otherAssetsConnectedParty)),
        tangibleProperty = version(membTxs.flatMap(_.tangibleProperty)),
        loanOutstanding = version(membTxs.flatMap(_.loanOutstanding)),
        unquotedShares = version(membTxs.flatMap(_.unquotedShares)),
        memberDetails = version(membTxs)
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
