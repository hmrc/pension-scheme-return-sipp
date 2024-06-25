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
import uk.gov.hmrc.pensionschemereturnsipp.config.AppConfig
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.utils.HttpResponseHelper

import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

class PsrConnector @Inject()(config: AppConfig, http: HttpClient, apiAuditUtil: ApiAuditUtil)(
  implicit ec: ExecutionContext
) extends HttpErrorFunctions
    with HttpResponseHelper
    with Logging {

  def submitSippPsr(pstr: String, request: SippPsrSubmissionEtmpRequest)(
    implicit headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[HttpResponse] = {

    val url: String = config.submitSippPsrUrl.format(pstr)
    logger.info(s"Submit SIPP PSR called URL: $url with payload: $request")

    http
      .POST(url, request, integrationFrameworkHeaders)
      .map {
        case response if response.status == OK => response
        case response => handleErrorResponse("POST", url)(response)
      }
      .andThen(apiAuditUtil.firePsrPostAuditEvent(pstr, Json.toJson(request)))
  }

  def getSippPsr(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(
    implicit headerCarrier: HeaderCarrier,
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
            Some(response.json.as[SippPsrSubmissionEtmpResponse])
          case _ if isNotFound(response) =>
            logger.warn(s"$logMessage and returned ${response.status}")
            None
          case _ => handleErrorResponse("GET", url)(response)
        }
      }
      .andThen(apiAuditUtil.firePsrGetAuditEvent(pstr, optFbNumber, optPeriodStartDate, optPsrVersion))
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
