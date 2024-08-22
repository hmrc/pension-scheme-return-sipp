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

package uk.gov.hmrc.pensionschemereturnsipp.audit

import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.json._
import uk.gov.hmrc.pensionschemereturnsipp.audit.ApiAuditUtil.AuditDetailPsrStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.{MinimalDetails, PensionSchemeId}

case class PsrPostAuditEvent(
  pstr: String,
  payload: JsValue,
  status: Option[Int],
  response: Option[JsValue],
  errorMessage: Option[String],
  pensionSchemeId: PensionSchemeId,
  minimalDetails: MinimalDetails,
  auditDetailPsrStatus: Option[AuditDetailPsrStatus]
) extends AuditEvent {
  override def auditType: String = "PSRPost"

  override def details: JsObject = {

    val optStatus = status.fold[JsObject](Json.obj())(s => Json.obj("HttpStatus" -> s))
    val optResponse = response.fold[JsObject](Json.obj())(s => Json.obj("Response" -> s))
    val optErrorMessage = errorMessage.fold[JsObject](Json.obj())(s => Json.obj("ErrorMessage" -> s))

    def credentialRole: String = if (pensionSchemeId.isPSP) "PSP" else "PSA"
    def affinityGroup: String = if (minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual"

    val psrDetails = Json.obj(
      "PensionSchemeTaxReference" -> pstr,
      "affinityGroup" -> affinityGroup,
      "Payload" -> payload
    ) ++ psaOrPspIdDetails(
      credentialRole,
      pensionSchemeId.value,
      minimalDetails.individualDetails.map(_.fullName).getOrElse("")
    ) ++
      JsObject(auditDetailPsrStatus.map(status => "psrStatus" -> JsString(status.name)).toList)

    val details = Json.obj(
      "details" -> psrDetails,
      "payload" -> payload
    )

    details ++ optStatus ++ optResponse ++ optErrorMessage
  }
}
