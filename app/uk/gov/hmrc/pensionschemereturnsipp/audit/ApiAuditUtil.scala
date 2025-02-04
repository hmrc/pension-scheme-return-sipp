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

import cats.implicits.catsSyntaxOptionId
import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status
import play.api.libs.json.*
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HttpException, HttpResponse, RequestEntityTooLargeException, UpstreamErrorResponse}
import uk.gov.hmrc.pensionschemereturnsipp.audit.ApiAuditUtil.AuditDetailPsrStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.api.FileUploadAuditContext
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpPsrStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.{JourneyType, MinimalDetails, PensionSchemeId}
import uk.gov.hmrc.pensionschemereturnsipp.services.AuditService

import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class ApiAuditUtil @Inject() (auditService: AuditService) extends Logging {

  def fireFileUploadAuditEvent(
    pensionSchemeId: PensionSchemeId,
    minimalDetails: MinimalDetails,
    auditContext: Option[FileUploadAuditContext]
  )(implicit ec: ExecutionContext, request: RequestHeader): PartialFunction[Try[HttpResponse], Unit] = {
    case Failure(error: RequestEntityTooLargeException) =>
      auditContext match
        case Some(auditC) =>
          logger.info(
            s"FileUploadAuditEvent sent. FileUploadAuditEvent ->> ErrorMessage: ${Json.toJson(error.getMessage)}"
          )
          auditService.sendEventWithSource(
            FileUploadAuditEvent(
              fileUploadType = auditC.fileUploadType,
              fileUploadStatus = auditC.fileUploadStatus,
              fileName = auditC.fileName,
              fileReference = auditC.fileReference,
              typeOfError = FileUploadAuditEvent.ERROR_SIZE_LIMIT,
              fileSize = auditC.fileSize,
              validationCompleted = LocalDate.now(),
              pensionSchemeId = pensionSchemeId,
              minimalDetails = minimalDetails,
              schemeDetails = auditC.schemeDetails,
              taxYear = auditC.taxYear
            ),
            auditSource = "pension-scheme-return-sipp-frontend" // for auditing purposes this needs to be frontend
          )

        case None =>
          logger.error(s"FileUploadAuditEvent not sent. Audit Context not found in request")

    case _ =>
      logger.debug(s"FileUploadAuditEvent not sent. Request did not fail with RequestEntityTooLargeException")
  }

  def firePSRSubmissionEvent(
    pstr: String,
    data: JsValue,
    pensionSchemeId: PensionSchemeId,
    minimalDetails: MinimalDetails,
    schemeName: Option[String],
    taxYear: Option[DateRange],
    psrRequest: SippPsrSubmissionEtmpRequest
  )(implicit ec: ExecutionContext, request: RequestHeader): PartialFunction[Try[HttpResponse], Unit] = {

    case Success(_) =>
      Option
        .when(psrRequest.psrDeclaration.isDefined) {
          logger.info(s"PSRSubmissionEvent ->> Status: ${Status.OK}, Payload: ${Json.prettyPrint(data)}")
          auditService.sendEventWithSource(
            PSRSubmissionEvent(
              pstr = pstr,
              payload = data,
              pensionSchemeId = pensionSchemeId,
              minimalDetails = minimalDetails,
              taxYear = taxYear,
              schemeName = schemeName
            ),
            auditSource = "pension-scheme-return-sipp-frontend" // for auditing purposes this needs to be frontend
          )
        }
        .getOrElse(logger.debug(s"PSRSubmissionEvent not sent. PSR Declaration not found in request"))

    case Failure(error: HttpException) =>
      logger.error(s"PSRSubmissionEvent not sent. PSR Submission failed with status: ${error.responseCode}")

    case Failure(error: UpstreamErrorResponse) =>
      logger.error(s"PSRSubmissionEvent not sent. PSR Submission failed with status: ${error.statusCode}")

    case Failure(error: Throwable) =>
      logger.error(s"PSRSubmissionEvent not sent. PSR Submission failed with error: ${Json.toJson(error.getMessage)}")
  }

  def firePsrPostAuditEvent(
    pstr: String,
    data: JsValue,
    pensionSchemeId: PensionSchemeId,
    minimalDetails: MinimalDetails,
    auditDetailPsrStatus: Option[AuditDetailPsrStatus],
    schemeName: Option[String],
    taxYear: Option[DateRange]
  )(implicit ec: ExecutionContext, request: RequestHeader): PartialFunction[Try[HttpResponse], Unit] = {
    case Success(httpResponse) =>
      logger.info(s"PsrPostAuditEvent ->> Status: ${Status.OK}, Payload: ${Json.prettyPrint(data)}")
      auditService.sendEvent(
        PsrPostAuditEvent(
          pstr = pstr,
          payload = data,
          status = Some(Status.OK),
          response = Some(httpResponse.json),
          errorMessage = None,
          schemeName,
          taxYear,
          pensionSchemeId: PensionSchemeId,
          minimalDetails: MinimalDetails,
          auditDetailPsrStatus: Option[AuditDetailPsrStatus]
        )
      )
    case Failure(error: UpstreamErrorResponse) =>
      logger.info(s"PsrPostAuditEvent ->> Status: ${error.statusCode}, ErrorMessage: ${Json.toJson(error.message)}")
      auditService.sendEvent(
        PsrPostAuditEvent(
          pstr = pstr,
          payload = data,
          status = Some(error.statusCode),
          response = None,
          errorMessage = Some(error.message),
          schemeName,
          taxYear,
          pensionSchemeId: PensionSchemeId,
          minimalDetails: MinimalDetails,
          auditDetailPsrStatus: Option[AuditDetailPsrStatus]
        )
      )
    case Failure(error: HttpException) =>
      logger.info(s"PsrPostAuditEvent ->> Status: ${error.responseCode}, ErrorMessage: ${Json.toJson(error.message)}")
      auditService.sendEvent(
        PsrPostAuditEvent(
          pstr = pstr,
          payload = data,
          status = Some(error.responseCode),
          response = None,
          errorMessage = Some(error.message),
          schemeName,
          taxYear,
          pensionSchemeId: PensionSchemeId,
          minimalDetails: MinimalDetails,
          auditDetailPsrStatus: Option[AuditDetailPsrStatus]
        )
      )
    case Failure(error: Throwable) =>
      logger.info(s"PsrPostAuditEvent ->> ErrorMessage: ${Json.toJson(error.getMessage)}")
      auditService.sendEvent(
        PsrPostAuditEvent(
          pstr = pstr,
          payload = data,
          status = None,
          response = None,
          errorMessage = Some(error.getMessage),
          schemeName,
          taxYear,
          pensionSchemeId: PensionSchemeId,
          minimalDetails: MinimalDetails,
          auditDetailPsrStatus: Option[AuditDetailPsrStatus]
        )
      )
  }

  def firePsrGetAuditEvent(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    ec: ExecutionContext,
    request: RequestHeader
  ): PartialFunction[Try[Option[SippPsrSubmissionEtmpResponse]], Unit] = {
    case Success(optResponse) =>
      auditService.sendEvent(
        PsrGetAuditEvent(
          pstr = pstr,
          fbNumber = optFbNumber,
          periodStartDate = optPeriodStartDate,
          psrVersion = optPsrVersion,
          status = Some(Status.OK),
          response = optResponse.map(Json.toJson(_)),
          errorMessage = None
        )
      )
    case Failure(error: UpstreamErrorResponse) =>
      logger.info(s"PsrGetAuditEvent ->> Status: ${error.statusCode}, ErrorMessage: ${error.message}")
      auditService.sendEvent(
        PsrGetAuditEvent(
          pstr = pstr,
          fbNumber = optFbNumber,
          periodStartDate = optPeriodStartDate,
          psrVersion = optPsrVersion,
          status = Some(error.statusCode),
          response = None,
          errorMessage = Some(error.message)
        )
      )
    case Failure(error: HttpException) =>
      logger.info(s"PsrGetAuditEvent ->> Status: ${error.responseCode}, ErrorMessage: ${error.message}")
      auditService.sendEvent(
        PsrGetAuditEvent(
          pstr = pstr,
          fbNumber = optFbNumber,
          periodStartDate = optPeriodStartDate,
          psrVersion = optPsrVersion,
          status = Some(error.responseCode),
          response = None,
          errorMessage = Some(error.message)
        )
      )
    case Failure(error: Throwable) =>
      logger.info(s"PsrGetAuditEvent ->> ErrorMessage: ${error.getMessage}")
      auditService.sendEvent(
        PsrGetAuditEvent(
          pstr = pstr,
          fbNumber = optFbNumber,
          periodStartDate = optPeriodStartDate,
          psrVersion = optPsrVersion,
          status = None,
          response = None,
          errorMessage = Some(error.getMessage)
        )
      )
  }
}

object ApiAuditUtil {
  sealed trait AuditDetailPsrStatus {
    def name: String
  }

  case object ChangedCompiled extends AuditDetailPsrStatus {
    override def name: String = "ChangedCompiled"
  }

  case object ChangedSubmitted extends AuditDetailPsrStatus {
    override def name: String = "ChangedSubmitted"
  }

  implicit class SippPsrSubmissionEtmpRequestOps(val sippPsrSubmissionEtmpRequest: SippPsrSubmissionEtmpRequest)
      extends AnyVal {
    def auditAmendDetailPsrStatus(journeyType: JourneyType): Option[AuditDetailPsrStatus] =
      journeyType match {
        case JourneyType.Standard => None
        case JourneyType.Amend =>
          (sippPsrSubmissionEtmpRequest.reportDetails.status match {
            case EtmpPsrStatus.Compiled => ChangedCompiled
            case EtmpPsrStatus.Submitted => ChangedSubmitted
          }).some
      }

  }
}
