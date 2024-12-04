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

package uk.gov.hmrc.pensionschemereturnsipp.auth

import play.api.Logging
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment, Enrolments}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.{psaEnrolmentKey, psaIdKey, pspEnrolmentKey, pspIdKey}
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.{PsaId, PspId}

import scala.concurrent.{ExecutionContext, Future}

final case class PsrAuthContext[A](
  externalId: String,
  psaPspId: PensionSchemeId,
  request: Request[A]
)

trait PsrAuth extends AuthorisedFunctions with Logging {

  private val AuthPredicate = Enrolment(psaEnrolmentKey).or(Enrolment(pspEnrolmentKey))

  private val PsrRetrievals = Retrievals.externalId.and(Retrievals.allEnrolments)

  private type PsrAction[A] = PsrAuthContext[A] => Future[Result]

  def authorisedAsPsrUser(
    body: PsrAction[Any]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Result] =
    authorisedUser(body)

  private def authorisedUser[A](
    block: PsrAction[A]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[A]): Future[Result] =
    authorised(AuthPredicate)
      .retrieve(PsrRetrievals) {
        case Some(externalId) ~ enrolments =>
          getPsaPspId(enrolments) match {
            case Some(psaPspId) => block(PsrAuthContext(externalId, psaPspId, request))
            case None => Future.failed(new BadRequestException(s"Bad Request without psaPspId"))
          }
        case _ =>
          Future.failed(new UnauthorizedException("Not Authorised - Unable to retrieve credentials - externalId"))
      }

  private def getPsaId(enrolments: Enrolments): Option[PsaId] =
    enrolments
      .getEnrolment(psaEnrolmentKey)
      .flatMap(_.getIdentifier(psaIdKey))
      .map(enrolment => PsaId(enrolment.value))

  private def getPspId(enrolments: Enrolments): Option[PspId] =
    enrolments
      .getEnrolment(pspEnrolmentKey)
      .flatMap(_.getIdentifier(pspIdKey))
      .map(enrolment => PspId(enrolment.value))

  private def getPsaPspId(enrolments: Enrolments): Option[PensionSchemeId] =
    getPsaId(enrolments).orElse(getPspId(enrolments))
}
