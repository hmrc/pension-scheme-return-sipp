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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pensionschemereturnsipp.auth.PsrAuth
import uk.gov.hmrc.pensionschemereturnsipp.connectors.SchemeDetailsConnector
import uk.gov.hmrc.pensionschemereturnsipp.models.JourneyType
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedPropertyApi.*
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedPropertyRequest
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class LandArmsLengthController @Inject() (
  cc: ControllerComponents,
  service: SippPsrSubmissionService,
  val authConnector: AuthConnector,
  override protected val schemeDetailsConnector: SchemeDetailsConnector
)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with PsrAuth
    with PsrBaseController {
  def put(
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ): Action[JsValue] = Action(parse.json).async { implicit request =>
    val Seq(srnS) = requiredHeaders("srn")
    authorisedAsPsrUser(srnS) { user =>
      val requestContent = request.body.as[LandOrConnectedPropertyRequest]
      service
        .submitLandArmsLength(
          journeyType,
          optFbNumber,
          optPeriodStartDate,
          optPsrVersion,
          requestContent,
          user.psaPspId,
          requestContent.auditContext
        )
        .map { response =>
          logger.debug(
            message = s"Submit Land Arms Length PSR details - new form bundle number: ${response.formBundleNumber}"
          )
          Created(Json.toJson(response))
        }
    }
  }

  def get(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ): Action[AnyContent] = Action.async { implicit request =>
    val Seq(srnS) = requiredHeaders("srn")
    authorisedAsPsrUser(srnS) { _ =>
      logger.debug(
        s"Retrieving SIPP PSR for LandArmsLength - with pstr: $pstr, fbNumber: $optFbNumber, periodStartDate: $optPeriodStartDate, psrVersion: $optPsrVersion"
      )
      service
        .getLandArmsLength(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
        .map {
          case Some(data) =>
            Ok(Json.toJson(data))
          case _ =>
            NoContent
        }
    }
  }
}
