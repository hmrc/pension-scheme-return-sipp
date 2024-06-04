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
import uk.gov.hmrc.http.{BadRequestException, ExpectationFailedException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pensionschemereturnsipp.connectors.PsrConnector
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{
  AssetsFromConnectedPartyRequest,
  LandOrConnectedPropertyRequest,
  OutstandingLoansRequest,
  ReportDetails,
  TangibleMoveablePropertyRequest
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpMemberAndTransactions
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.{PensionSchemeReturnValidationFailureException, SippPsrSubmission}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp.{SippPsrFromEtmp, SippPsrSubmissionToEtmp}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  LandArmsLengthTransformer,
  LandConnectedPartyTransformer,
  OutstandingLoansTransformer,
  ReportDetailsOps,
  TangibleMoveablePropertyTransformer,
  Transformer
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
  armsLengthTransformer: LandArmsLengthTransformer,
  outstandingLoansTransformer: OutstandingLoansTransformer,
  assetsFromConnectedPartyTransformer: AssetsFromConnectedPartyTransformer,
  tangibleMovablePropertyTransformer: TangibleMoveablePropertyTransformer
)(implicit ec: ExecutionContext)
    extends Logging {

  def submitLandOrConnectedProperty(
    request: LandOrConnectedPropertyRequest
  )(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, landConnectedPartyTransformer)

  def submitOutstandingLoans(
    request: OutstandingLoansRequest
  )(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, outstandingLoansTransformer)

  def submitLandArmsLength(
    request: LandOrConnectedPropertyRequest
  )(implicit hc: HeaderCarrier): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, armsLengthTransformer)

  def submitAssetsFromConnectedParty(
    request: AssetsFromConnectedPartyRequest
  )(implicit hc: HeaderCarrier): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, assetsFromConnectedPartyTransformer)

  def submitTangibleMoveableProperty(
    request: TangibleMoveablePropertyRequest
  )(implicit hc: HeaderCarrier): Future[HttpResponse] =
    submitJourney(request.reportDetails, request.transactions, tangibleMovablePropertyTransformer)

  private def submitJourney[A](
    reportDetails: ReportDetails,
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A]
  )(
    implicit hc: HeaderCarrier
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

  private def mergeWithExistingEtmpData[A](
    reportDetails: ReportDetails,
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A]
  )(
    implicit hc: HeaderCarrier
  ): Future[List[EtmpMemberAndTransactions]] =
    psrConnector
      .getSippPsr(reportDetails.pstr, None, Some("2024-06-03"), Some("1.0"))
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
