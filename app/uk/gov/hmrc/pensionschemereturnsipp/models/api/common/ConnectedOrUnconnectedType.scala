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

package uk.gov.hmrc.pensionschemereturnsipp.models.api.common

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

sealed abstract class ConnectedOrUnconnectedType(val value: String, val definition: String)

object ConnectedOrUnconnectedType {
  case object Connected extends ConnectedOrUnconnectedType("01", "CONNECTED")
  case object Unconnected extends ConnectedOrUnconnectedType("02", "UNCONNECTED")

  def apply(definition: String): ConnectedOrUnconnectedType = definition match {
    case Connected.definition => Connected
    case Unconnected.definition => Unconnected
    case other =>
      throw new RuntimeException(
        s"Expected either ${Connected.definition} or ${Unconnected.definition} for ConnectedOrUnconnectedType, but got: $other"
      )
  }

  implicit val writes: Writes[ConnectedOrUnconnectedType] = invOrOrgType => JsString(invOrOrgType.value)
  implicit val reads: Reads[ConnectedOrUnconnectedType] = Reads {
    case JsString(Connected.value) => JsSuccess(Connected)
    case JsString(Unconnected.value) => JsSuccess(Unconnected)
    case unknown => JsError(s"Unknown value for ConnectedOrUnconnectedType: $unknown")
  }
}
