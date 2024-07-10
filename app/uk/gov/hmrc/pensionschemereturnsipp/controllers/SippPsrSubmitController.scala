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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{BadRequestException, HttpErrorFunctions}
import uk.gov.hmrc.pensionschemereturnsipp.auth.PsrAuth
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{PsrSubmissionRequest, PsrSubmittedResponse}
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton()
class SippPsrSubmitController @Inject()(
  cc: ControllerComponents,
  sippPsrSubmissionService: SippPsrSubmissionService,
  val authConnector: AuthConnector
)(
  implicit ec: ExecutionContext
) extends BackendController(cc)
    with HttpErrorFunctions
    with Results
    with PsrAuth
    with Logging {

  private def requiredBody(implicit request: Request[AnyContent]): JsValue =
    request.body.asJson.getOrElse(throw new BadRequestException("Request does not contain Json body"))

  def submitSippPsr: Action[AnyContent] = Action.async { implicit request =>
    authorisedAsPsrUser { user =>
      val submissionRequest = requiredBody.as[PsrSubmissionRequest]
      logger.debug(s"Submitting SIPP PSR - $request")

      sippPsrSubmissionService
        .submitSippPsr(submissionRequest, user.fullName.mkString, user.externalId, user.psaPspId)
        .map(_.isRight)
        .map(emailSent => Created(Json.toJson(PsrSubmittedResponse(emailSent))))
    }
  }

  def getSippPsr(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ): Action[AnyContent] = Action.async { implicit request =>
    authorisedAsPsrUser { _ =>
      logger.debug(
        s"Retrieving SIPP PSR - with pstr: $pstr, fbNumber: $optFbNumber, periodStartDate: $optPeriodStartDate, psrVersion: $optPsrVersion"
      )
      sippPsrSubmissionService.getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion).map {
        case None => NotFound
        case Some(sippPsrSubmission) => Ok(Json.toJson(sippPsrSubmission))
      }
    }
  }

  def getPsrVersions(pstr: String, startDateStr: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAsPsrUser { _ =>
      logger.debug(s"Retrieving PSR versions with pstr $pstr and startDate $startDateStr")
      Try(LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE)) match {
        case Success(startDate) =>
          sippPsrSubmissionService.getPsrVersions(pstr, startDate).map(result => Ok(Json.toJson(result)))
        case Failure(t) =>
          logger.error(s"Retrieving PSR versions for $pstr failed because of invalid startDate $startDateStr", t)
          Future.successful(BadRequest(s"Invalid startDate $startDateStr"))
      }
    }
  }
}
