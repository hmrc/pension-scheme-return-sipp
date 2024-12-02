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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.{MinimalDetails, PensionSchemeId}

import java.time.LocalDate

case class PSRSubmissionEvent(
  pstr: String,
  pensionSchemeId: PensionSchemeId,
  minimalDetails: MinimalDetails,
  schemeName: Option[String],
  taxYear: Option[DateRange],
  payload: JsValue
) extends AuditEvent {

  override def auditType: String = "PensionSchemeReturnSubmitted"

  override def details: JsObject = {

    def credentialRole: String = if (pensionSchemeId.isPSP) "PSP" else "PSA"
    def affinityGroup: String = if (minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual"
    val optTaxYear = createOptionalJsonObject("taxYear", taxYear.map(tY => s"${tY.from.getYear}-${tY.to.getYear}"))
    val optSchemeName = createOptionalJsonObject("schemeName", schemeName)

    psaOrPspIdDetails(
      credentialRole,
      pensionSchemeId.value,
      minimalDetails.individualDetails.map(_.fullName).getOrElse("")
    ) ++ Json.obj(
      "pensionSchemeTaxReference" -> pstr,
      "affinityGroup" -> affinityGroup
    ) ++ optSchemeName ++ optTaxYear ++
      Json.obj(
        "date" -> LocalDate.now().toString,
        "payload" -> payload
      )
  }
}
