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
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.pensionschemereturnsipp.auth.PsrAuth
import uk.gov.hmrc.pensionschemereturnsipp.connectors.PsrConnector
import uk.gov.hmrc.pensionschemereturnsipp.models.common.JourneyType
import uk.gov.hmrc.pensionschemereturnsipp.models.common.JourneyType._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class SippPsrGetController @Inject()(
  cc: ControllerComponents,
  psrConnector: PsrConnector,
  val authConnector: AuthConnector
)(
  implicit ec: ExecutionContext
) extends BackendController(cc)
    with HttpErrorFunctions
    with Results
    with PsrAuth
    with Logging {

  def getMemberCounts(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    journey: JourneyType
  ): Action[AnyContent] = Action.async { implicit request =>
    val eventualResponse = psrConnector.getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
    eventualResponse.map {
      case Some(validResponse) =>
        validResponse.memberAndTransactions match {
          case Some(memberAndTransactions) =>
            Ok((journey match {
              case InterestInLandOrProperty =>
                memberAndTransactions.flatMap(_.landConnectedParty).map(_.noOfTransactions)
              case ArmsLengthLandOrProperty =>
                memberAndTransactions.flatMap(_.landArmsLength).map(_.noOfTransactions)
              case TangibleMoveableProperty =>
                memberAndTransactions.flatMap(_.tangibleProperty).map(_.noOfTransactions)
              case OutstandingLoans =>
                memberAndTransactions.flatMap(_.loanOutstanding).map(_.noOfTransactions)
              case UnquotedShares =>
                memberAndTransactions.flatMap(_.unquotedShares).map(_.noOfTransactions)
              case AssetFromConnectedParty =>
                memberAndTransactions.flatMap(_.otherAssetsConnectedParty).map(_.noOfTransactions)
            }).sum.toString)
          case None => Ok("0")
        }
      case None => Ok("0")
    }
  }

}
