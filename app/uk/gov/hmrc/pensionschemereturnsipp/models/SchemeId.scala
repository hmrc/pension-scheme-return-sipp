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

sealed abstract class SchemeId(val idType: String) {
  val value: String
}

object SchemeId {

  case class Srn(value: String) extends SchemeId("srn")

  object Srn {
    val srnRegex = "^S[0-9]{10}$"

    def apply(value: String): Option[Srn] =
      if (value.matches(srnRegex)) Some(new Srn(value)) else None
  }

  object asSrn {
    def unapply(arg: String): Option[Srn] = Srn(arg)
  }

  case class Pstr(value: String) extends SchemeId("pstr")

}
