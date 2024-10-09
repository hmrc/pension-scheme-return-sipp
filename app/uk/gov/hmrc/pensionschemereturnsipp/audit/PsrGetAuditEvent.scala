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

case class PsrGetAuditEvent(
  pstr: String,
  fbNumber: Option[String],
  periodStartDate: Option[String],
  psrVersion: Option[String],
  status: Option[Int],
  response: Option[JsValue],
  errorMessage: Option[String]
) extends AuditEvent {
  override def auditType: String = "PensionSchemeReturnGet"

  override def details: JsObject = {
    val apiDetails = Json.obj("pensionSchemeTaxReference" -> pstr)

    apiDetails ++
      createOptionalJsonObject("fbNumber", fbNumber) ++
      createOptionalJsonObject("periodStartDate", periodStartDate) ++
      createOptionalJsonObject("psrVersion", psrVersion) ++
      createOptionalJsonObject("httpStatus", status) ++
      createOptionalJsonObject("response", response) ++
      createOptionalJsonObject("errorMessage", errorMessage)
  }
}
