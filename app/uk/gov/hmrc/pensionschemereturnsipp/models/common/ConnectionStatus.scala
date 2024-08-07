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
import uk.gov.hmrc.pensionschemereturnsipp.models.common.ConnectionStatus.Connected

sealed abstract class ConnectionStatus(override val entryName: String) extends EnumEntry {
  def toBoolean: Boolean = this == Connected
}

object ConnectionStatus extends Enum[ConnectionStatus] with PlayJsonEnum[ConnectionStatus] {
  def apply(connected: Boolean): ConnectionStatus = if (connected) Connected else Unconnected

  case object Connected extends ConnectionStatus("01")
  case object Unconnected extends ConnectionStatus("02")

  override def values: IndexedSeq[ConnectionStatus] = findValues
}
