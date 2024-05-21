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

package uk.gov.hmrc.pensionschemereturnsipp.models.common

import play.api.libs.json.Reads.StringReads
import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

sealed abstract class ConnectedOrUnconnectedType(val value: String, val definition: String)

object ConnectedOrUnconnectedType {
  case object Connected extends ConnectedOrUnconnectedType("01", "Connected")
  case object Unconnected extends ConnectedOrUnconnectedType("02", "Unconnected")

  implicit val writes: Writes[ConnectedOrUnconnectedType] = invOrOrgType => JsString(invOrOrgType.value)
  implicit val reads: Reads[ConnectedOrUnconnectedType] =
    StringReads.flatMapResult {
      case Connected.value => JsSuccess(Connected)
      case Unconnected.value => JsSuccess(Unconnected)
      case other => JsError(s"Unknown value for ConnectedOrUnconnectedType: $other")
    }

}
