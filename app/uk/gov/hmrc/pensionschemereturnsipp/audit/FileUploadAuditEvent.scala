/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.{MinimalDetails, PensionSchemeId, SchemeDetails}

import java.time.LocalDate

case class FileUploadAuditEvent(
  fileUploadType: String,
  fileUploadStatus: String,
  fileName: String,
  fileReference: String,
  typeOfError: String,
  fileSize: Long,
  validationCompleted: LocalDate,
  pensionSchemeId: PensionSchemeId,
  minimalDetails: MinimalDetails,
  schemeDetails: SchemeDetails,
  taxYear: DateRange
) extends AuditEvent {

  override def auditType: String = "PensionSchemeReturnFileUpload"

  private def schemeAdministratorNameKey: String = pensionSchemeId match {
    case PensionSchemeId.PspId(_) => "schemePractitionerName"
    case PensionSchemeId.PsaId(_) => "schemeAdministratorName"
  }

  private def schemeAdministratorIdKey: String = pensionSchemeId match {
    case PensionSchemeId.PspId(_) => "pensionSchemePractitionerId"
    case PensionSchemeId.PsaId(_) => "pensionSchemeAdministratorId"
  }

  private def credentialRole: String = if (pensionSchemeId.isPSP) "PSP" else "PSA"

  private def affinityGroup: String = if (minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual"

  val mainDetails: JsObject = Json.obj(
    "schemeName" -> schemeDetails.schemeName,
    schemeAdministratorNameKey -> schemeDetails.establishers.headOption.map(_.name).getOrElse("empty establisher"),
    schemeAdministratorIdKey -> pensionSchemeId.value,
    "pensionSchemeTaxReference" -> schemeDetails.pstr,
    "affinityGroup" -> affinityGroup,
    "credentialRolePsaPsp" -> credentialRole
  )

  override def details: JsObject = {
    val details = mainDetails ++ Json.obj(
      "fileUploadType" -> fileUploadType,
      "fileUploadStatus" -> fileUploadStatus,
      "fileName" -> fileName,
      "fileReference" -> fileReference,
      "fileSize" -> s"$fileSize",
      "validationCompleted" -> s"$validationCompleted",
      "date" -> LocalDate.now().toString,
      "taxYear" -> s"${taxYear.from.getYear}-${taxYear.to.getYear}",
      "typeOfError" -> typeOfError
    )

    details
  }
}

object FileUploadAuditEvent {
  val ERROR_SIZE_LIMIT = "Your File Size exceed the maximum Limit"
}
