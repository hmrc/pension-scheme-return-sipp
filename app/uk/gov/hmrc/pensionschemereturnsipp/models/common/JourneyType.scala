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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.mvc.QueryStringBindable

import scala.util.Try

sealed abstract class JourneyType(override val entryName: String) extends EnumEntry

object JourneyType extends Enum[JourneyType] with PlayJsonEnum[JourneyType] {

  case object InterestInLandOrProperty extends JourneyType("interestInLandOrProperty")
  case object ArmsLengthLandOrProperty extends JourneyType("armsLengthLandOrProperty")
  case object TangibleMoveableProperty extends JourneyType("tangibleMoveableProperty")
  case object OutstandingLoans extends JourneyType("outstandingLoans")
  case object UnquotedShares extends JourneyType("unquotedShares")
  case object AssetFromConnectedParty extends JourneyType("assetFromConnectedParty")

  val values = findValues

  def fromString(str: String): Option[JourneyType] =
    Try(JourneyType.withNameInsensitive(str)).toOption

  implicit def queryStringBindable: QueryStringBindable[JourneyType] =
    new QueryStringBindable[JourneyType] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, JourneyType]] =
        params.get(key).flatMap(_.headOption).flatMap(fromString).map(Right(_))

      override def unbind(key: String, value: JourneyType): String =
        value.toString
    }
}
