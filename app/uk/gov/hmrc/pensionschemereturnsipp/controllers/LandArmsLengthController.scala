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

import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.mvc._
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedPropertyRequest
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import cats.syntax.functor._
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class LandArmsLengthController @Inject()(cc: ControllerComponents, service: SippPsrSubmissionService)(
  implicit ec: ExecutionContext
) extends BackendController(cc) {
  def put: Action[JsValue] = Action(parse.json).async { implicit request =>
    val requestContent = request.body.as[LandOrConnectedPropertyRequest]
    service
      .submitLandArmsLength(requestContent)
      .as(NotImplemented)
  }
}
