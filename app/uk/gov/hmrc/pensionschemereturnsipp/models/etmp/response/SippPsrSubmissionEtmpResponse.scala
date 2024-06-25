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

package uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  EtmpSippAccountingPeriod,
  EtmpSippAccountingPeriodDetails,
  EtmpSippReportDetails
}

case class SippPsrSubmissionEtmpResponse(
  reportDetails: EtmpSippReportDetails,
  accountingPeriodDetails: EtmpSippAccountingPeriodDetails,
  memberAndTransactions: Option[List[EtmpMemberAndTransactions]]
)

object SippPsrSubmissionEtmpResponse {

  implicit val accountingPeriodFormat: Format[EtmpSippAccountingPeriod] = Json.format[EtmpSippAccountingPeriod]
  implicit val accountingPeriodDetailsFormat: Format[EtmpSippAccountingPeriodDetails] =
    Json.format[EtmpSippAccountingPeriodDetails]

  implicit val format: Format[SippPsrSubmissionEtmpResponse] = Json.format[SippPsrSubmissionEtmpResponse]
}
