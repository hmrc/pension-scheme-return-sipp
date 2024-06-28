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
import play.api.libs.json.{Format, Json}

import java.time.ZonedDateTime

case class PsrVersionsResponse(
  reportFormBundleNumber: String,
  reportVersion: Int,
  reportStatus: ReportStatus,
  compilationOrSubmissionDate: ZonedDateTime,
  reportSubmitterDetails: Option[ReportSubmitterDetails],
  psaDetails: Option[PsaDetails]
)

case class ReportSubmitterDetails(
  reportSubmittedBy: String,
  organisationOrPartnershipDetails: Option[OrganisationOrPartnershipDetails],
  individualDetails: Option[IndividualDetails]
)

case class OrganisationOrPartnershipDetails(
  organisationOrPartnershipName: String
)

case class IndividualDetails(
  firstName: String,
  middleName: Option[String],
  lastName: String
)

case class PsaDetails(
  psaOrganisationOrPartnershipDetails: Option[PsaOrganisationOrPartnershipDetails],
  psaIndividualDetails: Option[PsaIndividualDetails]
)

case class PsaOrganisationOrPartnershipDetails(
  organisationOrPartnershipName: String
)

case class PsaIndividualDetails(
  firstName: String,
  middleName: Option[String],
  lastName: String
)

sealed trait ReportStatus extends EnumEntry

object ReportStatus extends Enum[ReportStatus] with PlayJsonEnum[ReportStatus] {
  case object Compiled extends ReportStatus
  case object SubmittedAndInProgress extends ReportStatus
  case object SubmittedAndSuccessfullyProcessed extends ReportStatus

  override def values: IndexedSeq[ReportStatus] = findValues
}

object PsrVersionsResponse {
  private implicit val formatOrganisationOrPartnershipDetails: Format[OrganisationOrPartnershipDetails] =
    Json.format[OrganisationOrPartnershipDetails]
  private implicit val formatIndividualDetails: Format[IndividualDetails] = Json.format[IndividualDetails]
  private implicit val formatReportSubmitterDetails: Format[ReportSubmitterDetails] =
    Json.format[ReportSubmitterDetails]
  private implicit val formatPsaOrganisationOrPartnershipDetails: Format[PsaOrganisationOrPartnershipDetails] =
    Json.format[PsaOrganisationOrPartnershipDetails]
  private implicit val formatPsaIndividualDetails: Format[PsaIndividualDetails] = Json.format[PsaIndividualDetails]
  private implicit val formatPsaDetails: Format[PsaDetails] = Json.format[PsaDetails]

  implicit val formats: Format[PsrVersionsResponse] = Json.format[PsrVersionsResponse]
}
