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

package uk.gov.hmrc.pensionschemereturnsipp.services

import cats.data.NonEmptyList
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{BadRequestException, ExpectationFailedException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pensionschemereturnsipp.connectors.PsrConnector
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{
  AssetsFromConnectedPartyRequest,
  LandOrConnectedPropertyRequest,
  OutstandingLoansRequest
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpMemberAndTransactions
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.{PensionSchemeReturnValidationFailureException, SippPsrSubmission}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp.{SippPsrFromEtmp, SippPsrSubmissionToEtmp}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  LandConnectedPartyTransformer,
  ReportDetailsOps
}
import uk.gov.hmrc.pensionschemereturnsipp.validators.JSONSchemaValidator
import uk.gov.hmrc.pensionschemereturnsipp.validators.SchemaPaths.API_1997

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SippPsrSubmissionService @Inject()(
  psrConnector: PsrConnector,
  jsonPayloadSchemaValidator: JSONSchemaValidator,
  sippPsrSubmissionToEtmp: SippPsrSubmissionToEtmp,
  sippPsrFromEtmp: SippPsrFromEtmp,
  landConnectedPartyTransformer: LandConnectedPartyTransformer,
  assetsFromConnectedPartyTransformer: AssetsFromConnectedPartyTransformer
)(implicit ec: ExecutionContext)
    extends Logging {

  def submitLandOrConnectedProperty(
    landOrConnectedProperty: LandOrConnectedPropertyRequest
  )(implicit headerCarrier: HeaderCarrier, request: RequestHeader): Future[HttpResponse] = {

    def constructMembersAndTransactions(
      connectedPropertyRequest: LandOrConnectedPropertyRequest
    )(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[List[EtmpMemberAndTransactions]] =
      psrConnector
        .getSippPsr(connectedPropertyRequest.reportDetails.pstr, None, None, None)
        .map {
          case Some(existingEtmpData) =>
            (for {
              landOrPropertyTxs <- connectedPropertyRequest.transactions
              etmpTxs <- existingEtmpData.memberAndTransactions
            } yield {
              landConnectedPartyTransformer.merge(landOrPropertyTxs, etmpTxs)
            }).toList.flatten
          case None =>
            connectedPropertyRequest.transactions.toList
              .flatMap(details => landConnectedPartyTransformer.merge(details, List.empty))
        }

    constructMembersAndTransactions(landOrConnectedProperty).flatMap { landOrPropertyMembersAndTxs =>
      val etmpRequest = SippPsrSubmissionEtmpRequest(
        reportDetails = landOrConnectedProperty.reportDetails.toEtmp,
        accountingPeriodDetails = None,
        memberAndTransactions = NonEmptyList.fromList(landOrPropertyMembersAndTxs),
        psrDeclaration = None
      )
      psrConnector
        .submitSippPsr(landOrConnectedProperty.reportDetails.pstr, etmpRequest)
    }
  }

  //TODO implement along with above
  def submitOutstandingLoans(
    outstandingLoansRequest: OutstandingLoansRequest
  )(implicit headerCarrier: HeaderCarrier, request: RequestHeader): Future[HttpResponse] =
    Future.successful(
      HttpResponse.apply(
        status = 201,
        json = JsObject.empty,
        headers = Map.empty
      )
    )

  //TODO implement along with above
  def submitLandArmsLength(request: LandOrConnectedPropertyRequest): Future[Option[List[EtmpMemberAndTransactions]]] =
    Future.successful(None)

  def submitAssetsFromConnectedParty(
    assetsFromConnectedParty: AssetsFromConnectedPartyRequest
  )(implicit headerCarrier: HeaderCarrier, request: RequestHeader): Future[HttpResponse] = {

    def constructMembersAndTransactions(
      assetsFromConnectedParty: AssetsFromConnectedPartyRequest
    )(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[List[EtmpMemberAndTransactions]] =
      psrConnector
        .getSippPsr(
          assetsFromConnectedParty.reportDetails.pstr,
          None, // TODO -> What the hack that FB number !?
          Some(assetsFromConnectedParty.reportDetails.periodStart.toString),
          Some(assetsFromConnectedParty.reportDetails.periodEnd.toString)
        )
        .map {
          case Some(existingEtmpData) =>
            (for {
              assetsFromConnectedPartyTxs <- assetsFromConnectedParty.transactions
              etmpTxs <- existingEtmpData.memberAndTransactions
            } yield {
              assetsFromConnectedPartyTransformer.merge(assetsFromConnectedPartyTxs, etmpTxs)
            }).toList.flatten
          case None =>
            assetsFromConnectedParty.transactions.toList
              .flatten(details => assetsFromConnectedPartyTransformer.merge(details, List.empty))
        }

    constructMembersAndTransactions(assetsFromConnectedParty).flatMap { assetsFromConnectedPartyAndTx =>
      val etmpRequest = SippPsrSubmissionEtmpRequest(
        reportDetails = assetsFromConnectedParty.reportDetails.toEtmp,
        accountingPeriodDetails = None,
        memberAndTransactions = NonEmptyList.fromList(assetsFromConnectedPartyAndTx),
        psrDeclaration = None
      )
      psrConnector
        .submitSippPsr(assetsFromConnectedParty.reportDetails.pstr, etmpRequest)
    }
  }

  def submitSippPsr(
    sippPsrSubmission: SippPsrSubmission
  )(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val request = sippPsrSubmissionToEtmp.transform(sippPsrSubmission)
    val validationResult = jsonPayloadSchemaValidator.validatePayload(API_1997, Json.toJson(request))
    if (validationResult.hasErrors) {
      throw PensionSchemeReturnValidationFailureException(
        s"Invalid payload when submitSippPsr :-\n${validationResult.toString}"
      )
    } else {
      psrConnector.submitSippPsr(sippPsrSubmission.reportDetails.pstr, request).recover {
        case _: BadRequestException => throw new ExpectationFailedException("Nothing to submit")
      }
    }
  }

  def getSippPsr(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Option[SippPsrSubmission]] =
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map(_.map(sippPsrFromEtmp.transform))

}
