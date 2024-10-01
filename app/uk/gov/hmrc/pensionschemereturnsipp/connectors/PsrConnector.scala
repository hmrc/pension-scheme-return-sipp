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

package uk.gov.hmrc.pensionschemereturnsipp.connectors

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http._
import uk.gov.hmrc.pensionschemereturnsipp.audit.ApiAuditUtil
import uk.gov.hmrc.pensionschemereturnsipp.audit.ApiAuditUtil.SippPsrSubmissionEtmpRequestOps
import uk.gov.hmrc.pensionschemereturnsipp.config.AppConfig
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.common.PsrVersionsResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.{
  JourneyType,
  MinimalDetails,
  PensionSchemeId,
  PensionSchemeReturnValidationFailureException
}
import uk.gov.hmrc.pensionschemereturnsipp.utils.HttpResponseHelper
import uk.gov.hmrc.pensionschemereturnsipp.validators.JSONSchemaValidator
import uk.gov.hmrc.pensionschemereturnsipp.validators.SchemaPaths.API_1997

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class PsrConnector @Inject() (
  config: AppConfig,
  jsonPayloadSchemaValidator: JSONSchemaValidator,
  http: HttpClient,
  apiAuditUtil: ApiAuditUtil
)(implicit
  ec: ExecutionContext
) extends HttpErrorFunctions
    with HttpResponseHelper
    with Logging {

  def submitSippPsr(
    journeyType: JourneyType,
    pstr: String,
    pensionSchemeId: PensionSchemeId,
    minimalDetails: MinimalDetails,
    request: SippPsrSubmissionEtmpRequest,
    maybeTaxYear: Option[DateRange],
    maybeSchemeName: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[HttpResponse] = {

    val url: String = config.submitSippPsrUrl.format(pstr)
    logger.info(s"Submit SIPP PSR called URL: $url with payload: $request")

    val jsonRequest = Json.toJson(request)
    val jsonSizeInBytes = jsonRequest.toString().getBytes("UTF-8").length

    if (jsonSizeInBytes > config.maxRequestSize) {
      val errorMessage = s"Request body size exceeds maximum limit of ${config.maxRequestSize} bytes"
      // Fire the audit event for the size limit exceeded case
      apiAuditUtil
        .firePsrPostAuditEvent(
          pstr,
          jsonRequest,
          pensionSchemeId,
          minimalDetails,
          request.auditAmendDetailPsrStatus(journeyType),
          maybeSchemeName,
          maybeTaxYear
        )
        .apply(Failure(new Throwable(errorMessage)))

      Future.failed(new RequestEntityTooLargeException(errorMessage))
    } else {
      val validationResult = jsonPayloadSchemaValidator.validatePayload(API_1997, jsonRequest)
      if (validationResult.hasErrors) {
        logger.error(s"Validation has errors: $validationResult")
        throw PensionSchemeReturnValidationFailureException(
          s"Invalid payload when submitSippPsr :-\n${validationResult.toString}"
        )
      } else {
        http
          .POST(url, request, integrationFrameworkHeaders)
          .map {
            case response if response.status == OK => response
            case response => handleErrorResponse("POST", url)(response)
          }
          .andThen(
            apiAuditUtil
              .firePsrPostAuditEvent(
                pstr,
                jsonRequest,
                pensionSchemeId,
                minimalDetails,
                request.auditAmendDetailPsrStatus(journeyType),
                maybeSchemeName,
                maybeTaxYear
              )
          )
          .andThen(
            apiAuditUtil
              .firePSRSubmissionEvent(
                pstr,
                jsonRequest,
                pensionSchemeId,
                minimalDetails,
                maybeSchemeName,
                maybeTaxYear,
                request
              )
          )
      }
    }
  }

  def getSippPsr(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[SippPsrSubmissionEtmpResponse]] = {

    val params = buildParams(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
    val url: String = config.getSippPsrUrl.format(params)
    val logMessage = s"Get SIPP PSR called URL: $url with pstr: $pstr"

    logger.info(logMessage)

    def isNotFound(response: HttpResponse) =
      response.status == NOT_FOUND ||
        (response.status == UNPROCESSABLE_ENTITY && response.body.contains("PSR_NOT_FOUND"))

    http
      .GET[HttpResponse](url, headers = integrationFrameworkHeaders)
      .map { response =>
        response.status match {
          case OK =>
            val etmpResponse = response.json.as[SippPsrSubmissionEtmpResponse]
            val filteredMembers = etmpResponse.memberAndTransactions.map(_.filter(_.status != Deleted))
            val updatedResponse = etmpResponse.copy(memberAndTransactions = filteredMembers)
            Some(updatedResponse)
          case _ if isNotFound(response) =>
            logger.warn(s"$logMessage and returned (PSR_NOT_FOUND) ${response.status} ")
            None
          case _ => handleErrorResponse("GET", url)(response)
        }
      }
      .andThen(apiAuditUtil.firePsrGetAuditEvent(pstr, optFbNumber, optPeriodStartDate, optPsrVersion))
  }

  def getPsrVersions(
    pstr: String,
    startDate: LocalDate
  )(implicit hc: HeaderCarrier, rh: RequestHeader): Future[Seq[PsrVersionsResponse]] = {
    val startDateStr = startDate.format(DateTimeFormatter.ISO_DATE)
    val url = config.getPsrVersionsUrl.format(pstr)
    http
      .GET[HttpResponse](url, queryParams = Seq("startDate" -> startDateStr), integrationFrameworkHeaders)
      .flatMap {
        case response if response.status == 200 =>
          Future.fromTry(Try(response.json.as[Seq[PsrVersionsResponse]]))
        case response if response.status == 404 =>
          Future.successful(Seq.empty)
        case response =>
          handleErrorResponse("GET", url)(response)
      }
  }

  private def buildParams(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ): String =
    (optFbNumber, optPeriodStartDate, optPsrVersion) match {
      case (Some(fbNumber), _, _) => s"$pstr?psrFormBundleNumber=$fbNumber"
      case (None, Some(periodStartDate), Some(psrVersion)) =>
        s"$pstr?periodStartDate=$periodStartDate&psrVersion=$psrVersion"
      case _ => throw new BadRequestException("Missing url parameters")
    }

  private def getCorrelationId(implicit requestHeader: RequestHeader): String =
    requestHeader.headers.get("CorrelationId").getOrElse(randomUUID.toString)

  private def integrationFrameworkHeaders(implicit requestHeader: RequestHeader): Seq[(String, String)] =
    Seq(
      "Environment" -> config.integrationFrameworkEnvironment,
      "Authorization" -> config.integrationFrameworkAuthorization,
      "Content-Type" -> "application/json",
      "CorrelationId" -> getCorrelationId
    )
}
