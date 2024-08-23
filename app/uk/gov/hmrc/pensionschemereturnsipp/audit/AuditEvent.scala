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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.PSA

trait AuditEvent {
  def auditType: String

  def details: JsObject

  def psaOrPspIdDetails(
    credentialRole: String,
    psaOrPspId: String,
    schemeAdministratorOrPractitionerName: String
  ): JsObject =
    credentialRole match {
      case PSA =>
        Json.obj(
          "PensionSchemeAdministratorId" -> psaOrPspId,
          "SchemeAdministratorName" -> schemeAdministratorOrPractitionerName,
          "credentialRolePsaPsp" -> credentialRole
        )
      case _ =>
        Json.obj(
          "PensionSchemePractitionerId" -> psaOrPspId,
          "SchemePractitionerName" -> schemeAdministratorOrPractitionerName,
          "credentialRolePsaPsp" -> credentialRole
        )
    }
}
