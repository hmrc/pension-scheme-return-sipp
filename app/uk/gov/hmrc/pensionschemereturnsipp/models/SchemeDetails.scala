/*
 * Copyright 2023 HM Revenue & Customs
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

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.*

case class SchemeDetails(
  schemeName: String,
  pstr: String,
  schemeStatus: SchemeStatus,
  schemeType: String,
  authorisingPSAID: Option[String],
  establishers: List[Establisher]
)

case class Establisher(
  name: String,
  kind: EstablisherKind
)

sealed trait EstablisherKind extends EnumEntry with Lowercase

object EstablisherKind extends Enum[EstablisherKind] with PlayJsonEnum[EstablisherKind] {
  case object Company extends EstablisherKind
  case object Partnership extends EstablisherKind
  case object Individual extends EstablisherKind

  override def values: IndexedSeq[EstablisherKind] = findValues
}

object Establisher {
  implicit val format: Format[Establisher] = Json.format[Establisher]
}

object SchemeDetails {
  implicit val format: Format[SchemeDetails] = Json.format[SchemeDetails]
}

sealed abstract class SchemeStatus(override val entryName: String) extends EnumEntry

object SchemeStatus extends Enum[SchemeStatus] with PlayJsonEnum[SchemeStatus] {

  case object Pending extends SchemeStatus("Pending")
  case object PendingInfoRequired extends SchemeStatus("Pending Info Required")
  case object PendingInfoReceived extends SchemeStatus("Pending Info Received")
  case object Rejected extends SchemeStatus("Rejected")
  case object Open extends SchemeStatus("Open")
  case object Deregistered extends SchemeStatus("Deregistered")
  case object WoundUp extends SchemeStatus("Wound-up")
  case object RejectedUnderAppeal extends SchemeStatus("Rejected Under Appeal")

  override def values: IndexedSeq[SchemeStatus] = findValues
}
