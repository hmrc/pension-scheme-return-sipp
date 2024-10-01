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

import cats.syntax.either._
import com.google.inject.Inject
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.pensionschemereturnsipp.config.AppConfig
import uk.gov.hmrc.pensionschemereturnsipp.models.{PensionSchemeId, SendEmailRequest}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject()(
  appConfig: AppConfig,
  http: HttpClient,
  crypto: ApplicationCrypto
) {

  private val logger = Logger(classOf[EmailConnector])

  private def callBackUrl(
    pensionSchemeId: PensionSchemeId,
    requestId: String,
    pstr: String,
    email: String,
    schemeName: String,
    userName: String,
    taxYear: String,
    reportVersion: String
  ): String = {
    val encryptedPsaOrPspId = URLEncoder.encode(
      crypto.QueryParameterCrypto.encrypt(PlainText(pensionSchemeId.value)).value,
      StandardCharsets.UTF_8.toString
    )
    val encryptedPstr =
      URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pstr)).value, StandardCharsets.UTF_8.toString)
    val encryptedEmail =
      URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(email)).value, StandardCharsets.UTF_8.toString)
    val encryptedSchemeName =
      URLEncoder.encode(
        crypto.QueryParameterCrypto.encrypt(PlainText(schemeName)).value,
        StandardCharsets.UTF_8.toString
      )
    val encryptedUserName =
      URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(userName)).value, StandardCharsets.UTF_8.toString)

    val emailCallback = appConfig.emailCallback(
      pensionSchemeId,
      requestId,
      encryptedEmail,
      encryptedPsaOrPspId,
      encryptedPstr,
      encryptedSchemeName,
      encryptedUserName,
      taxYear,
      reportVersion
    )
    logger.info(s"Callback URL: $emailCallback")
    emailCallback
  }

  //scalastyle:off parameter.number
  def sendEmail(
    pensionSchemeId: PensionSchemeId,
    requestId: String,
    pstr: String,
    emailAddress: String,
    templateId: String,
    schemeName: String,
    userName: String,
    templateParams: Map[String, String],
    taxYear: String,
    reportVersion: String
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Either[String, Unit]] = {
    val emailServiceUrl = s"${appConfig.emailApiUrl}/hmrc/email"

    val sendEmailReq = SendEmailRequest(
      List(emailAddress),
      templateId,
      templateParams,
      appConfig.emailSendForce,
      callBackUrl(pensionSchemeId, requestId, pstr, emailAddress, schemeName, userName, taxYear, reportVersion)
    )
    val jsonData = Json.toJson(sendEmailReq)

    http
      .POST[JsValue, HttpResponse](emailServiceUrl, jsonData)
      .map { response =>
        response.status match {
          case ACCEPTED =>
            logger.debug(s"Email sent successfully")
            ().asRight[String]
          case status =>
            logger.warn(s"Sending Email failed with response status $status")
            s"Failed to send email $status".asLeft[Unit]
        }
      }
      .recoverWith(logExceptions)
  }

  private def logExceptions: PartialFunction[Throwable, Future[Either[String, Unit]]] = {
    case t: Throwable =>
      logger.warn("Unable to connect to Email Service", t)
      Future.successful("Could not connect to email service".asLeft)
  }
}
