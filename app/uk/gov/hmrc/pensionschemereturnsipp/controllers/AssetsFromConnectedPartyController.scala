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

import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.pensionschemereturnsipp.models.api.AssetsFromConnectedPartyApi._
import uk.gov.hmrc.pensionschemereturnsipp.models.api.AssetsFromConnectedPartyRequest
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class AssetsFromConnectedPartyController @Inject()(
  cc: ControllerComponents,
  sippPsrSubmissionService: SippPsrSubmissionService
)(
  implicit ec: ExecutionContext
) extends BackendController(cc)
    with HttpErrorFunctions
    with Results
    with Logging {

  def put: Action[JsValue] = Action(parse.json).async { implicit request =>
    val assetsFromConnectedPartySubmission = request.body.as[AssetsFromConnectedPartyRequest]
    logger.debug(
      s"Submitting AssetsFromConnectedParty PSR details - Incoming payload: $assetsFromConnectedPartySubmission"
    )
    sippPsrSubmissionService
      .submitAssetsFromConnectedParty(assetsFromConnectedPartySubmission)
      .map { response =>
        logger
          .debug(s"Submit AssetsFromConnectedParty PSR details - response: ${response.status}, body: ${response.body}")
        NoContent
      }
  }

}
