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
import play.api.libs.json._
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{SharesCompanyDetails, UnquotedShareDisposalDetails, YesNo}

case class UnquotedShareRequest(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[UnquotedShareApi.TransactionDetails]]
)

case class UnquotedShareResponse(
  transactions: List[UnquotedShareApi.TransactionDetails]
)

object UnquotedShareApi {

  case class TransactionDetails(
    nameDOB: NameDOB,
    nino: NinoType,
    shareCompanyDetails: SharesCompanyDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    totalDividendsIncome: Double,
    sharesDisposed: YesNo,
    sharesDisposalDetails: Option[UnquotedShareDisposalDetails],
    transactionCount: Option[Int]
  ) extends MemberKey

  object TransactionDetails {
    implicit val format: OFormat[TransactionDetails] = Json.format[TransactionDetails]
  }

  implicit val formatRes: OFormat[UnquotedShareResponse] = Json.format[UnquotedShareResponse]
  implicit val formatReq: OFormat[UnquotedShareRequest] = Json.format[UnquotedShareRequest]
}
