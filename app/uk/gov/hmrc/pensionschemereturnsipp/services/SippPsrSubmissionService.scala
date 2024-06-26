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
import uk.gov.hmrc.pensionschemereturnsipp.models.api._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.PsrVersionsResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpMemberAndTransactions
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.{PensionSchemeReturnValidationFailureException, SippPsrSubmission}
import uk.gov.hmrc.pensionschemereturnsipp.transformations._
import uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp.{SippPsrFromEtmp, SippPsrSubmissionToEtmp}
import uk.gov.hmrc.pensionschemereturnsipp.validators.JSONSchemaValidator
import uk.gov.hmrc.pensionschemereturnsipp.validators.SchemaPaths.API_1997

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SippPsrSubmissionService @Inject()(
  psrConnector: PsrConnector,
  jsonPayloadSchemaValidator: JSONSchemaValidator,
  sippPsrSubmissionToEtmp: SippPsrSubmissionToEtmp,
  sippPsrFromEtmp: SippPsrFromEtmp,
  landConnectedPartyTransformer: LandConnectedPartyTransformer,
  armsLengthTransformer: LandArmsLengthTransformer,
  outstandingLoansTransformer: OutstandingLoansTransformer,
  assetsFromConnectedPartyTransformer: AssetsFromConnectedPartyTransformer,
  unquotedSharesTransformer: UnquotedSharesTransformer,
  tangibleMovablePropertyTransformer: TangibleMoveablePropertyTransformer
)(implicit ec: ExecutionContext)
    extends Logging {

  def submitLandOrConnectedProperty(
    request: LandOrConnectedPropertyRequest
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, landConnectedPartyTransformer)

  def getLandOrConnectedProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(
    implicit headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[LandOrConnectedPropertyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, landConnectedPartyTransformer)

  def submitOutstandingLoans(
    request: OutstandingLoansRequest
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, outstandingLoansTransformer)

  def submitLandArmsLength(
    request: LandOrConnectedPropertyRequest
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, armsLengthTransformer)

  def getLandArmsLength(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(
    implicit headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[LandOrConnectedPropertyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, armsLengthTransformer)

  def submitAssetsFromConnectedParty(
    request: AssetsFromConnectedPartyRequest
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, assetsFromConnectedPartyTransformer)

  def getAssetsFromConnectedParty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(
    implicit headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[AssetsFromConnectedPartyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, assetsFromConnectedPartyTransformer)

  def submitTangibleMoveableProperty(
    request: TangibleMoveablePropertyRequest
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, tangibleMovablePropertyTransformer)

  def getTangibleMoveableProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(
    implicit headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[TangibleMoveablePropertyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, tangibleMovablePropertyTransformer)

  def submitUnquotedShares(
    request: UnquotedShareRequest
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, unquotedSharesTransformer)

  def getUnquotedShares(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(
    implicit headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[UnquotedShareResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, unquotedSharesTransformer)

  private def submitJourney[A, V](
    reportDetails: ReportDetails,
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A, V]
  )(
    implicit hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[HttpResponse] =
    mergeWithExistingEtmpData(reportDetails, transactions, transformer)
      .flatMap { etmpDataAfterMerge =>
        val request = SippPsrSubmissionEtmpRequest(
          reportDetails = reportDetails.toEtmp,
          accountingPeriodDetails = None,
          memberAndTransactions = NonEmptyList.fromList(etmpDataAfterMerge),
          psrDeclaration = None
        )
        psrConnector.submitSippPsr(reportDetails.pstr, request)
      }

  private def getJourney[A, V](
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    transformer: Transformer[A, V]
  )(
    implicit hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[V]] =
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map {
        case Some(existingEtmpData) =>
          Some(transformer.transformToResponse(existingEtmpData.memberAndTransactions.getOrElse(List.empty)))
        case None =>
          None
      }

  private def mergeWithExistingEtmpData[A, V](
    reportDetails: ReportDetails,
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A, V]
  )(
    implicit hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[List[EtmpMemberAndTransactions]] =
    psrConnector
      .getSippPsr(reportDetails.pstr, None, Some("2024-06-03"), Some("001"))
      .map {
        case Some(existingEtmpData) =>
          val merged = for {
            txs <- transactions
            etmpTxs <- existingEtmpData.memberAndTransactions
          } yield transformer.merge(txs, etmpTxs)
          merged.toList.flatten
        case None =>
          transactions.toList.flatMap(txs => transformer.merge(txs, Nil))
      }

  def submitSippPsr(
    sippPsrSubmission: SippPsrSubmission
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[HttpResponse] = {
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
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[Option[SippPsrSubmission]] =
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map(_.map(sippPsrFromEtmp.transform))

  def getPsrVersions(
    pstr: String,
    startDate: LocalDate
  )(implicit hc: HeaderCarrier, rh: RequestHeader): Future[Seq[PsrVersionsResponse]] =
    psrConnector.getPsrVersions(pstr, startDate)
}
