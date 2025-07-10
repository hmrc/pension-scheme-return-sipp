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
import play.api.mvc.Results.BadRequest
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment, Enrolments}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.{
  psaEnrolmentKey,
  psaIdKey,
  pspEnrolmentKey,
  pspIdKey,
  requestRoleHeader,
  PSA,
  PSP
}
import uk.gov.hmrc.pensionschemereturnsipp.connectors.SchemeDetailsConnector
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.{PsaId, PspId}
import uk.gov.hmrc.pensionschemereturnsipp.models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

final case class PsrAuthContext[A](
  externalId: String,
  psaPspId: PensionSchemeId,
  request: Request[A]
)

trait PsrAuth extends AuthorisedFunctions with Logging {

  protected val schemeDetailsConnector: SchemeDetailsConnector

  private val AuthPredicate = Enrolment(psaEnrolmentKey).or(Enrolment(pspEnrolmentKey))

  private val PsrRetrievals = Retrievals.externalId.and(Retrievals.allEnrolments)

  private type PsrAction[A] = PsrAuthContext[A] => Future[Result]

  def authorisedAsPsrUser(srnS: String)(
    body: PsrAction[Any]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[Any]): Future[Result] =
    authorisedUser(srnS)(body)

  private def authorisedUser[A](srnS: String)(
    block: PsrAction[A]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[A]): Future[Result] =
    Srn(srnS) match {
      case Some(srn) =>
        authorised(AuthPredicate)
          .retrieve(PsrRetrievals) {
            case Some(externalId) ~ enrolments =>
              request.headers.get(requestRoleHeader).map(_.toUpperCase()) match {
                case Some(PSA) =>
                  checkPsa(srn, externalId, enrolments)(block)
                case Some(PSP) =>
                  checkPsp(srn, externalId, enrolments)(block)
                case None =>
                  checkPsaOrPsp(srn, externalId, enrolments)(block)
                case _ => Future.failed(new BadRequestException(s"Bad Request invalid $requestRoleHeader header value"))
              }

            case _ =>
              Future.failed(new UnauthorizedException("Not Authorised - Unable to retrieve credentials - externalId"))
          }
      case _ => Future.successful(BadRequest("Invalid scheme reference number"))
    }

  private def checkPsa[A](srn: Srn, externalId: String, enrolments: Enrolments)(
    block: PsrAction[A]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[A]): Future[Result] = {
    val vPsaId = getPsaId(enrolments)
    (vPsaId, getPsaIdAsString(enrolments)) match {
      case (Some(v), Some((psaId, credentialRole, idType))) =>
        schemeDetailsConnector.checkAssociation(psaId, idType, srn).flatMap {
          case true => block(PsrAuthContext(externalId, v, request))
          case false =>
            Future
              .failed(
                new UnauthorizedException("Not Authorised - scheme is not associated with the PSA")
              )
        }
      case psa =>
        Future.failed(new BadRequestException(s"Bad Request without psaId/credentialRole $psa"))
    }
  }

  private def checkPsp[A](srn: Srn, externalId: String, enrolments: Enrolments)(
    block: PsrAction[A]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[A]): Future[Result] = {
    val vPspId = getPspId(enrolments)
    (vPspId, getPspIdAsString(enrolments)) match {
      case (Some(v), Some((pspId, credentialRole, idType))) =>
        schemeDetailsConnector.checkAssociation(pspId, idType, srn).flatMap {
          case true => block(PsrAuthContext(externalId, v, request))
          case false =>
            Future
              .failed(
                new UnauthorizedException("Not Authorised - scheme is not associated with the PSP")
              )
        }

      case psp =>
        Future.failed(new BadRequestException(s"Bad Request without pspId/credentialRole $psp"))
    }
  }

  private def checkPsaOrPsp[A](srn: Srn, externalId: String, enrolments: Enrolments)(
    block: PsrAction[A]
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[A]): Future[Result] = {
    val vPsaPspId = getPsaPspId(enrolments)
    (vPsaPspId, getPsaPspIdAsString(enrolments)) match {
      case (Some(v), Some((psaPspId, credentialRole, idType))) =>
        schemeDetailsConnector.checkAssociation(psaPspId, idType, srn).flatMap {
          case true => block(PsrAuthContext(externalId, v, request))
          case false =>
            Future
              .failed(
                new UnauthorizedException("Not Authorised - scheme is not associated with the user")
              )
        }

      case psa =>
        Future.failed(new BadRequestException(s"Bad Request without psaPspId/credentialRole $psa"))
    }
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

  private def getPsaIdAsString(enrolments: Enrolments): Option[(String, String, String)] =
    getPsaId(enrolments) match {
      case Some(id) => Some((id.value, PSA, psaIdKey))
      case _ => None
    }

  private def getPspIdAsString(enrolments: Enrolments): Option[(String, String, String)] =
    getPspId(enrolments) match {
      case Some(id) => Some((id.value, PSP, pspIdKey))
      case _ => None
    }

  private def getPsaPspIdAsString(enrolments: Enrolments): Option[(String, String, String)] =
    getPsaId(enrolments) match {
      case Some(id) => Some((id.value, PSA, psaIdKey))
      case _ =>
        getPspId(enrolments) match {
          case Some(id) => Some((id.value, PSP, pspIdKey))
          case _ => None
        }
    }

}
