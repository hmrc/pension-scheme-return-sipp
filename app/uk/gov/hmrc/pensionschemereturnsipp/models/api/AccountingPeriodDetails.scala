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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class AccountingPeriodDetails(
  version: Option[String],
  accountingPeriods: List[AccountingPeriod]
)

case class AccountingPeriod(
  accPeriodStart: LocalDate,
  accPeriodEnd: LocalDate
)

object AccountingPeriodDetails {
  implicit val accountingPeriodFormat: OFormat[AccountingPeriod] = Json.format[AccountingPeriod]
  implicit val format: OFormat[AccountingPeriodDetails] = Json.format[AccountingPeriodDetails]
}
