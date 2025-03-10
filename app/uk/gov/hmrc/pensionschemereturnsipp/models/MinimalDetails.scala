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

package uk.gov.hmrc.pensionschemereturnsipp.models

import play.api.libs.json.{Json, Reads, Writes}

case class MinimalDetails(
  email: String,
  isPsaSuspended: Boolean,
  organisationName: Option[String],
  individualDetails: Option[IndividualDetails],
  rlsFlag: Boolean,
  deceasedFlag: Boolean
)

object MinimalDetails {
  implicit val reads: Reads[MinimalDetails] = Json.reads[MinimalDetails]
  implicit val writes: Writes[MinimalDetails] = Json.writes[MinimalDetails]
}

case class IndividualDetails(
  firstName: String,
  middleName: Option[String],
  lastName: String
) {
  val fullName = s"$firstName${middleName.map(_.prepended(' ')).mkString} $lastName"
}

object IndividualDetails {
  implicit val writes: Writes[IndividualDetails] = Json.writes[IndividualDetails]
  implicit val reads: Reads[IndividualDetails] = Json.reads[IndividualDetails]
}
