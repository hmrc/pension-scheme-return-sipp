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
import org.apache.pekko.Done
import play.api.{Configuration, Logging}
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.pensionschemereturnsipp.config.AppConfig
import uk.gov.hmrc.pensionschemereturnsipp.models.{PensionSchemeId, SendEmailRequest}
import play.api.libs.ws.writeableOf_JsValue

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2,
  config: Configuration
) extends Logging {

  private lazy val jsonCrypto: Encrypter & Decrypter =
    SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = "queryParameter.encryption", config.underlying)

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
      jsonCrypto.encrypt(PlainText(pensionSchemeId.value)).value,
      StandardCharsets.UTF_8.toString
    )
    val encryptedPstr =
      URLEncoder.encode(jsonCrypto.encrypt(PlainText(pstr)).value, StandardCharsets.UTF_8.toString)
    val encryptedEmail =
      URLEncoder.encode(jsonCrypto.encrypt(PlainText(email)).value, StandardCharsets.UTF_8.toString)
    val encryptedSchemeName =
      URLEncoder.encode(
        jsonCrypto.encrypt(PlainText(schemeName)).value,
        StandardCharsets.UTF_8.toString
      )
    val encryptedUserName =
      URLEncoder.encode(jsonCrypto.encrypt(PlainText(userName)).value, StandardCharsets.UTF_8.toString)

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

  // scalastyle:off parameter.number
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
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Either[String, Done]] = {
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
      .post(url"$emailServiceUrl")
      .withBody(jsonData)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case ACCEPTED =>
            logger.debug(s"Email sent successfully")
            Right(Done)
          case status =>
            logger.warn(s"Sending Email failed with response status $status")
            Left(s"Failed to send email $status")
        }
      }
      .recoverWith(logExceptions)
  }

  private def logExceptions: PartialFunction[Throwable, Future[Either[String, Done]]] = { case t: Throwable =>
    logger.warn("Unable to connect to Email Service", t)
    Future.successful(Left("Could not connect to email service"))
  }
}
