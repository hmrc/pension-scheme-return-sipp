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

package uk.gov.hmrc.pensionschemereturnsipp.utils

import cats.implicits.catsSyntaxEitherId
import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonUtils {
  // Creates a Json format for an either value type
  def eitherFormat[A: Format, B: Format](leftName: String, rightName: String): Format[Either[A, B]] =
    Format(
      fjs = (__ \ leftName).read[A].map(_.asLeft[B]) |
        (__ \ rightName).read[B].map(_.asRight[A]),
      tjs = _.fold(
        left => Json.obj(leftName -> left),
        right => Json.obj(rightName -> right)
      )
    )
}
