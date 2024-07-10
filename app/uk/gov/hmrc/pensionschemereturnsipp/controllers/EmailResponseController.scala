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

import play.api.mvc._
import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.{JsResultException, JsValue}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.emailRegex
import uk.gov.hmrc.pensionschemereturnsipp.models.{EmailEvents, Event}

class EmailResponseController @Inject()(
  cc: ControllerComponents,
  crypto: ApplicationCrypto,
  parser: PlayBodyParsers,
  val authConnector: AuthConnector
) extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  def sendAuditEvents(
    submittedBy: String,
    requestId: String,
    email: String,
    encryptedPsaOrPspId: String,
    encryptedPstr: String
  ): Action[JsValue] = Action(parser.tolerantJson) { implicit request =>
    decryptPsaOrPspIdAndEmail(encryptedPsaOrPspId, encryptedPstr, email) match {
      case Right((_, _, _)) =>
        request.body
          .validate[EmailEvents]
          .fold(
            errors => {
              logger.error("failed to decode email events response", JsResultException(errors))
              BadRequest("Bad request received for email call back event")
            },
            valid => {
              valid.events
                .filterNot(_.event == Event.Opened)
                .foreach(event => logger.debug(s"Email audit event is $event"))
              Ok
            }
          )

      case Left(result) => result
    }
  }

  private def decryptPsaOrPspIdAndEmail(
    encryptedPsaOrPspId: String,
    encryptedPstr: String,
    email: String
  ): Either[Result, (String, String, String)] = {
    val psaOrPspId = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPsaOrPspId)).value
    val pstr = crypto.QueryParameterCrypto.decrypt(Crypted(encryptedPstr)).value
    val emailAddress = crypto.QueryParameterCrypto.decrypt(Crypted(email)).value

    try {
      require(emailAddress.matches(emailRegex))
      Right((psaOrPspId, pstr, emailAddress))
    } catch {
      case _: IllegalArgumentException => Left(Forbidden(s"Malformed email : $emailAddress"))
    }
  }
}
