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

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{YesNo => EtmpYesNo}

sealed abstract class YesNo(val value: String, val boolean: Boolean)
object YesNo {
  case object Yes extends YesNo("Yes", true)
  case object No extends YesNo("No", false)

  def apply(yes: Boolean): YesNo = if (yes) Yes else No

  implicit val writes: Writes[YesNo] = yesNo => JsString(yesNo.value)

  implicit val reads: Reads[YesNo] = Reads {
    case JsString(Yes.value) => JsSuccess(Yes)
    case JsString(No.value) => JsSuccess(No)
    case unknown => JsError(s"Unknown value for YesNo: $unknown")
  }

  implicit class YesNoOps(val yesNo: YesNo) extends AnyVal {
    def toEtmp: EtmpYesNo = yesNo match {
      case Yes => EtmpYesNo.Yes
      case No => EtmpYesNo.No
    }
  }
}
