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

package uk.gov.hmrc.pensionschemereturnsipp.models.etmp

import cats.data.NonEmptyList
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pensionschemereturnsipp.models.Journey
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo

import java.time.LocalDate

case class EtmpSippReportDetails(
  pstr: String,
  status: EtmpPsrStatus,
  periodStart: LocalDate,
  periodEnd: LocalDate,
  memberTransactions: YesNo,
  memberTransLandPropCon: Option[YesNo],
  memberTransAssetCon: Option[YesNo],
  memberTransLandPropArmsLen: Option[YesNo],
  memberTransTangPropArmsLen: Option[YesNo],
  memberTransOutstandingLoan: Option[YesNo],
  memberTransUnquotedShares: Option[YesNo],
  version: Option[String]
)

object EtmpSippReportDetails {
  implicit val format: OFormat[EtmpSippReportDetails] = Json.format[EtmpSippReportDetails]

  implicit class EtmpSippReportDetailsOps(val etmpSippReportDetails: EtmpSippReportDetails) extends AnyVal {
    def withAssetClassDeclaration[A](
      journey: Journey,
      transactions: Option[NonEmptyList[A]]
    ): EtmpSippReportDetails = {
      val declaration = Some(if (transactions.isEmpty) YesNo.No else YesNo.Yes)

      journey match
        case Journey.InterestInLandOrProperty => etmpSippReportDetails.copy(memberTransLandPropCon = declaration)
        case Journey.ArmsLengthLandOrProperty => etmpSippReportDetails.copy(memberTransLandPropArmsLen = declaration)
        case Journey.TangibleMoveableProperty => etmpSippReportDetails.copy(memberTransTangPropArmsLen = declaration)
        case Journey.OutstandingLoans => etmpSippReportDetails.copy(memberTransOutstandingLoan = declaration)
        case Journey.UnquotedShares => etmpSippReportDetails.copy(memberTransUnquotedShares = declaration)
        case Journey.AssetFromConnectedParty => etmpSippReportDetails.copy(memberTransAssetCon = declaration)
    }
  }
}
