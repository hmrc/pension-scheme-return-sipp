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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class Journey extends EnumEntry

object Journey extends Enum[Journey] with PlayJsonEnum[Journey] {
  case object InterestInLandOrProperty extends Journey

  case object ArmsLengthLandOrProperty extends Journey

  case object TangibleMoveableProperty extends Journey

  case object OutstandingLoans extends Journey

  case object UnquotedShares extends Journey

  case object AssetFromConnectedParty extends Journey

  override def values: IndexedSeq[Journey] = findValues
}
