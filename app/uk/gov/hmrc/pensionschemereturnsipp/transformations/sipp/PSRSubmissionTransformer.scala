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
import cats.syntax.traverse._
import com.google.inject.{Inject, Singleton}
import io.scalaland.chimney.dsl._
import io.scalaland.chimney.{Transformer => ChimneyTransformer}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{PSRSubmissionResponse, ReportDetails, Version, Versions}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  EtmpSippReportDetails,
  VersionedAsset
}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  LandArmsLengthTransformer,
  LandConnectedPartyTransformer,
  OutstandingLoansTransformer,
  TangibleMoveablePropertyTransformer,
  UnquotedSharesTransformer
}

@Singleton()
class PSRSubmissionTransformer @Inject() (
  assetsFromConnectedPartyTransformer: AssetsFromConnectedPartyTransformer,
  landArmsLengthTransformer: LandArmsLengthTransformer,
  landConnectedPartyTransformer: LandConnectedPartyTransformer,
  outstandingLoansTransformer: OutstandingLoansTransformer,
  tangibleMoveablePropertyTransformer: TangibleMoveablePropertyTransformer,
  unquotedSharesTransformer: UnquotedSharesTransformer
) {

  import PSRSubmissionTransformer._

  def transform(
    etmpResponse: SippPsrSubmissionEtmpResponse,
    dropDeletedMembers: Boolean = false
  ): PSRSubmissionResponse = {
    val version = versionOrPsrVersion(_, etmpResponse.reportDetails.version)
    val membTxs: List[EtmpMemberAndTransactions] = etmpResponse.memberAndTransactions.sequence.flatten
    val filteredMemTxs =
      if (dropDeletedMembers)
        membTxs.filter(_.status != Deleted)
      else
        membTxs

    PSRSubmissionResponse(
      details = etmpResponse.reportDetails.transformInto[ReportDetails],
      accountingPeriodDetails = etmpResponse.accountingPeriodDetails,
      landConnectedParty =
        NonEmptyList.fromList(landConnectedPartyTransformer.transformToResponse(filteredMemTxs).transactions),
      otherAssetsConnectedParty =
        NonEmptyList.fromList(assetsFromConnectedPartyTransformer.transformToResponse(filteredMemTxs).transactions),
      landArmsLength =
        NonEmptyList.fromList(landArmsLengthTransformer.transformToResponse(filteredMemTxs).transactions),
      tangibleProperty =
        NonEmptyList.fromList(tangibleMoveablePropertyTransformer.transformToResponse(filteredMemTxs).transactions),
      loanOutstanding =
        NonEmptyList.fromList(outstandingLoansTransformer.transformToResponse(filteredMemTxs).transactions),
      unquotedShares =
        NonEmptyList.fromList(unquotedSharesTransformer.transformToResponse(filteredMemTxs).transactions),
      versions = Versions(
        landConnectedParty = version(filteredMemTxs.flatMap(_.landConnectedParty)),
        landArmsLength = version(filteredMemTxs.flatMap(_.landArmsLength)),
        otherAssetsConnectedParty = version(filteredMemTxs.flatMap(_.otherAssetsConnectedParty)),
        tangibleProperty = version(filteredMemTxs.flatMap(_.tangibleProperty)),
        loanOutstanding = version(filteredMemTxs.flatMap(_.loanOutstanding)),
        unquotedShares = version(filteredMemTxs.flatMap(_.unquotedShares)),
        memberDetails = version(membTxs) // Shouldn't be filtered!
      )
    )
  }

  private def versionOrPsrVersion(
    versionedAssets: Iterable[VersionedAsset],
    psrVersion: Option[String]
  ): Option[Version] =
    versionedAssets
      .map(_.version)
      .maxOption
      .flatten
      .orElse(psrVersion)
      .map(Version(_))
}

object PSRSubmissionTransformer {
  implicit val reportDetailsEtmpToApi: ChimneyTransformer[EtmpSippReportDetails, ReportDetails] =
    _.into[ReportDetails].enableOptionDefaultsToNone.transform
}
