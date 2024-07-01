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

import play.api.libs.json._

import java.time.LocalDateTime

sealed trait Event

case object Sent extends Event
case object Delivered extends Event
case object PermanentBounce extends Event
case object Opened extends Event
case object Complained extends Event

case class EmailEvent(event: Event, detected: LocalDateTime)

object EmailEvent {
  implicit val formatSent: OFormat[Sent.type] = Json.format[Sent.type]
  implicit val formatDelivered: OFormat[Delivered.type] = Json.format[Delivered.type]
  implicit val formatPermanentBounce: OFormat[PermanentBounce.type] = Json.format[PermanentBounce.type]
  implicit val formatOpened: OFormat[Opened.type] = Json.format[Opened.type]
  implicit val formatComplained: OFormat[Complained.type] = Json.format[Complained.type]

  implicit val eventFormat: OFormat[Event] = Json.format[Event]

  implicit val format: OFormat[EmailEvent] = Json.format[EmailEvent]
}

case class EmailEvents(events: Seq[EmailEvent])

object EmailEvents {
  implicit val format: OFormat[EmailEvents] = Json.format[EmailEvents]
}
