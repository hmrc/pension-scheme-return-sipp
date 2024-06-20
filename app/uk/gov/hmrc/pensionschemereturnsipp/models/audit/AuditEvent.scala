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

package uk.gov.hmrc.pensionschemereturnsipp.models.audit

import play.api.libs.json.Json
import uk.gov.hmrc.pensionschemereturnsipp.models.audit.AuditType.{GetPSR, GetPSRVersions, PostPSR}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest

sealed abstract class AuditEvent(val auditType: AuditType) {
  val url: String
  val requestPayload: Option[String]
  val additionalDetails: Map[String, String] = Map.empty

  final def details: Map[String, String] =
    Map("auditType" -> auditType.entryName, "url" -> url) ++
      requestPayload.map("requestPayload" -> _).toMap ++
      additionalDetails
}

object AuditEvent {
  trait GetEvent { this: AuditEvent =>
    override val requestPayload: Option[String] = None
  }

  case class GetPsrAuditEvent(override val url: String) extends AuditEvent(GetPSR) with GetEvent

  case class PostPsrAuditEvent(override val url: String, request: SippPsrSubmissionEtmpRequest)
      extends AuditEvent(PostPSR) {
    override val requestPayload: Option[String] = Some(Json.prettyPrint(Json.toJson(request)))
  }

  case class GetPsrVersionsAuditEvent(override val url: String) extends AuditEvent(GetPSRVersions) with GetEvent

  case class AuditEventWithResult(
    event: AuditEvent,
    httpResponseCode: Option[Int],
    responsePayload: Option[String],
    error: Option[Throwable] = None
  ) extends AuditEvent(event.auditType) {
    override val url: String = event.url
    override val requestPayload: Option[String] = event.requestPayload

    override val additionalDetails: Map[String, String] =
      Map("httpResponseCode" -> httpResponseCode.toString) ++
        responsePayload.map("responsePayload" -> _).toMap ++
        error.map("errorMessage" -> _.getMessage).toMap
  }
}
