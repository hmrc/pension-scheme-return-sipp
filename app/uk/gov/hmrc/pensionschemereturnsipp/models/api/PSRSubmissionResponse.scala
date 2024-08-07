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

package uk.gov.hmrc.pensionschemereturnsipp.models.api

import cats.data.NonEmptyList
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.AccountingPeriodDetails

case class PSRSubmissionResponse(
  details: ReportDetails,
  accountingPeriodDetails: Option[AccountingPeriodDetails],
  landConnectedParty: Option[NonEmptyList[LandOrConnectedPropertyApi.TransactionDetails]],
  otherAssetsConnectedParty: Option[NonEmptyList[AssetsFromConnectedPartyApi.TransactionDetails]],
  landArmsLength: Option[NonEmptyList[LandOrConnectedPropertyApi.TransactionDetails]],
  tangibleProperty: Option[NonEmptyList[TangibleMoveablePropertyApi.TransactionDetails]],
  loanOutstanding: Option[NonEmptyList[OutstandingLoansApi.TransactionDetails]],
  unquotedShares: Option[NonEmptyList[UnquotedShareApi.TransactionDetails]],
  versions: Versions
)

object PSRSubmissionResponse {
  implicit val formatPSRSubmissionResponse: OFormat[PSRSubmissionResponse] = Json.format[PSRSubmissionResponse]
}
