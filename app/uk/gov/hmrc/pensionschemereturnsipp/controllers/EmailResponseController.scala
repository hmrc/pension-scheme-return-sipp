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

package uk.gov.hmrc.pensionschemereturnsipp.controllers

import com.google.inject.Inject
import play.api.{Configuration, Logging}
import play.api.libs.json.JsValue
import play.api.mvc.*
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.pensionschemereturnsipp.audit.PsrEmailAuditEvent
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.emailRegex
import uk.gov.hmrc.pensionschemereturnsipp.models.EmailEvents
import uk.gov.hmrc.pensionschemereturnsipp.models.Event.Opened
import uk.gov.hmrc.pensionschemereturnsipp.services.AuditService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class EmailResponseController @Inject() (
  cc: ControllerComponents,
  config: Configuration,
  parser: PlayBodyParsers,
  auditService: AuditService,
  val authConnector: AuthConnector
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  lazy val jsonCrypto: Encrypter & Decrypter =
    SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = "queryParameter.encryption", config.underlying)

  def sendAuditEvents(
    submittedBy: String,
    requestId: String,
    email: String,
    encryptedPsaOrPspId: String,
    encryptedPstr: String,
    encryptedSchemeName: String,
    encryptedUserName: String,
    taxYear: String,
    reportVersion: String
  ): Action[JsValue] = Action(parser.tolerantJson) { implicit request =>
    decryptDetails(encryptedPsaOrPspId, encryptedPstr, email, encryptedSchemeName, encryptedUserName) match {
      case Right(Tuple5(psaOrPspId, pstr, emailAddress, schemeName, userName)) =>
        request.body
          .validate[EmailEvents]
          .fold(
            _ => BadRequest("Bad request received for email call back event"),
            valid => {
              valid.events
                .filterNot(
                  _.event == Opened
                )
                .foreach { event =>
                  logger.debug(s"Email Audit event is $event")
                  auditService.sendEvent(
                    PsrEmailAuditEvent(
                      psaPspId = psaOrPspId,
                      pstr = pstr,
                      submittedBy = submittedBy,
                      emailAddress = emailAddress,
                      event = event,
                      requestId = requestId,
                      reportVersion = reportVersion,
                      schemeName = schemeName,
                      taxYear = taxYear,
                      userName = userName
                    )
                  )(request, implicitly)
                }
              Ok
            }
          )

      case Left(result) => result
    }
  }

  private def decryptDetails(
    encryptedPsaOrPspId: String,
    encryptedPstr: String,
    email: String,
    encryptedSchemeName: String,
    encryptedUserName: String
  ): Either[Result, (String, String, String, String, String)] = {
    val emailAddress: String = decrypt(email)
    try {
      require(emailAddress.matches(emailRegex))
      Right(
        (
          decrypt(encryptedPsaOrPspId),
          decrypt(encryptedPstr),
          emailAddress,
          decrypt(encryptedSchemeName),
          decrypt(encryptedUserName)
        )
      )
    } catch {
      case _: IllegalArgumentException => Left(Forbidden(s"Malformed email : $emailAddress"))
    }
  }

  private def decrypt(encrypted: String): String =
    jsonCrypto.decrypt(Crypted(encrypted)).value
}
