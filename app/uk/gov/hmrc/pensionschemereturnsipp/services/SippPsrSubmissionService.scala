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

import cats.syntax.option.*
import cats.syntax.functor.*
import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.implicits.catsSyntaxOptionId
import cats.syntax.either.*
import com.google.inject.{Inject, Singleton}
import io.scalaland.chimney.dsl.transformInto
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.connectors.{MinimalDetailsConnector, MinimalDetailsError, PsrConnector}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.*
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{
  AccountingPeriodDetails,
  PsrVersionsResponse,
  SubmittedBy,
  YesNo
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpPsrStatus.Compiled
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration.Declaration
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.MemberDetails.compare
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.{
  SippPsrJourneySubmissionEtmpResponse,
  SippPsrSubmissionEtmpResponse
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{MemberDetails, *}
import uk.gov.hmrc.pensionschemereturnsipp.models.{Journey, JourneyType, MinimalDetails, PensionSchemeId}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.*
import uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp.{
  PSRAssetDeclarationsTransformer,
  PSRAssetsExistenceTransformer,
  PSRMemberDetailsTransformer,
  PSRSubmissionTransformer
}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SippPsrSubmissionService @Inject() (
  psrConnector: PsrConnector,
  psrSubmissionTransformer: PSRSubmissionTransformer,
  memberDetailsTransformer: PSRMemberDetailsTransformer,
  psrAssetsExistenceTransformer: PSRAssetsExistenceTransformer,
  psrAssetDeclarationsTransformer: PSRAssetDeclarationsTransformer,
  landConnectedPartyTransformer: LandConnectedPartyTransformer,
  armsLengthTransformer: LandArmsLengthTransformer,
  outstandingLoansTransformer: OutstandingLoansTransformer,
  assetsFromConnectedPartyTransformer: AssetsFromConnectedPartyTransformer,
  unquotedSharesTransformer: UnquotedSharesTransformer,
  tangibleMovablePropertyTransformer: TangibleMoveablePropertyTransformer,
  emailSubmissionService: EmailSubmissionService,
  minimalDetailsConnector: MinimalDetailsConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  def submitLandOrConnectedProperty(
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: LandOrConnectedPropertyRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrJourneySubmissionEtmpResponse] =
    submitJourney(
      journeyType,
      optFbNumber,
      optPeriodStartDate,
      optPsrVersion,
      request.reportDetails,
      Journey.InterestInLandOrProperty,
      request.transactions,
      landConnectedPartyTransformer,
      pensionSchemeId
    )

  def getLandOrConnectedProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[LandOrConnectedPropertyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, landConnectedPartyTransformer)

  def submitOutstandingLoans(
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: OutstandingLoansRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrJourneySubmissionEtmpResponse] =
    submitJourney(
      journeyType,
      optFbNumber,
      optPeriodStartDate,
      optPsrVersion,
      request.reportDetails,
      Journey.OutstandingLoans,
      request.transactions,
      outstandingLoansTransformer,
      pensionSchemeId
    )

  def getOutstandingLoans(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[OutstandingLoansResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, outstandingLoansTransformer)

  def submitLandArmsLength(
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: LandOrConnectedPropertyRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrJourneySubmissionEtmpResponse] =
    submitJourney(
      journeyType,
      optFbNumber,
      optPeriodStartDate,
      optPsrVersion,
      request.reportDetails,
      Journey.ArmsLengthLandOrProperty,
      request.transactions,
      armsLengthTransformer,
      pensionSchemeId
    )

  def getLandArmsLength(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[LandOrConnectedPropertyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, armsLengthTransformer)

  def submitAssetsFromConnectedParty(
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: AssetsFromConnectedPartyRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrJourneySubmissionEtmpResponse] =
    submitJourney(
      journeyType,
      optFbNumber,
      optPeriodStartDate,
      optPsrVersion,
      request.reportDetails,
      Journey.AssetFromConnectedParty,
      request.transactions,
      assetsFromConnectedPartyTransformer,
      pensionSchemeId
    )

  def getAssetsFromConnectedParty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[AssetsFromConnectedPartyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, assetsFromConnectedPartyTransformer)

  def submitTangibleMoveableProperty(
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: TangibleMoveablePropertyRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrJourneySubmissionEtmpResponse] =
    submitJourney(
      journeyType,
      optFbNumber,
      optPeriodStartDate,
      optPsrVersion,
      request.reportDetails,
      Journey.TangibleMoveableProperty,
      request.transactions,
      tangibleMovablePropertyTransformer,
      pensionSchemeId
    )

  def getTangibleMoveableProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[TangibleMoveablePropertyResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, tangibleMovablePropertyTransformer)

  def submitUnquotedShares(
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: UnquotedShareRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrJourneySubmissionEtmpResponse] =
    submitJourney(
      journeyType,
      optFbNumber,
      optPeriodStartDate,
      optPsrVersion,
      request.reportDetails,
      Journey.UnquotedShares,
      request.transactions,
      unquotedSharesTransformer,
      pensionSchemeId
    )

  def getUnquotedShares(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[UnquotedShareResponse]] =
    getJourney(pstr, optFbNumber, optPeriodStartDate, optPsrVersion, unquotedSharesTransformer)

  private def submitJourney[A, V](
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    reportDetails: ReportDetails,
    journey: Journey,
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A, V],
    pensionSchemeId: PensionSchemeId
  )(implicit
    hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[SippPsrJourneySubmissionEtmpResponse] =
    submitWithRequest(
      journeyType,
      reportDetails.pstr,
      pensionSchemeId,
      mergeWithExistingEtmpData(
        optFbNumber,
        optPeriodStartDate,
        optPsrVersion,
        reportDetails,
        journey,
        transactions,
        transformer
      )
    )

  private def submitWithRequest(
    journeyType: JourneyType,
    pstr: String,
    pensionSchemeId: PensionSchemeId,
    thunk: => Future[SippPsrSubmissionEtmpRequest],
    maybeTaxYear: Option[DateRange] = None,
    maybeSchemeName: Option[String] = None
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val response = for {
      submissionRequest <- EitherT(thunk.map(_.asRight[MinimalDetailsError]))
      details <- EitherT(getMinimalDetails(pensionSchemeId))
      result <- EitherT(
        psrConnector
          .submitSippPsr(journeyType, pstr, pensionSchemeId, details, submissionRequest, maybeTaxYear, maybeSchemeName)
          .map(_.asRight[MinimalDetailsError])
      )
    } yield result

    response
      .valueOrF(error => Future.failed(new RuntimeException(s"Failed tp submit psr: $error")))
      .map(_.json.as[SippPsrJourneySubmissionEtmpResponse])
  }

  private def getMinimalDetails(
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Either[MinimalDetailsError, MinimalDetails]] =
    pensionSchemeId match {
      case id: PensionSchemeId.PspId => minimalDetailsConnector.fetch(id)
      case id: PensionSchemeId.PsaId => minimalDetailsConnector.fetch(id)
    }

  private def getJourney[A, V](
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    transformer: Transformer[A, V]
  )(implicit
    hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[V]] =
    getSippPsrFiltered(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map(response => transformer.transformToResponse(response.memberAndTransactions.getOrElse(List.empty)))
      .value

  private def mergeWithExistingEtmpData[A, V](
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    reportDetails: ReportDetails,
    journey: Journey,
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A, V]
  )(implicit
    hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[SippPsrSubmissionEtmpRequest] =
    psrConnector
      .getSippPsr(reportDetails.pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map(merge(reportDetails, journey, _, transactions, transformer))

  private def merge[A, V](
    reportDetails: ReportDetails,
    journey: Journey,
    response: Option[SippPsrSubmissionEtmpResponse],
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A, V]
  ) = {
    val (details, merged) = response match
      case Some(existingEtmpData) =>
        val existingMemberAndTransactions = existingEtmpData.memberAndTransactions.getOrElse(List())
        val merged =
          transactions.fold(existingMemberAndTransactions)(transformer.merge(_, existingMemberAndTransactions))
        existingEtmpData.reportDetails -> merged

      case None =>
        reportDetails.transformInto[EtmpSippReportDetails] -> transactions.toList.flatMap(transformer.merge(_, Nil))

    SippPsrSubmissionEtmpRequest(
      reportDetails = details
        .transactionsAssetClassDeclaration(journey, transactions)
        .copy(status = Compiled, version = None),
      accountingPeriodDetails = None,
      memberAndTransactions = NonEmptyList.fromList(merged),
      psrDeclaration = None
    )
  }

  def submitSippPsr(
    journeyType: JourneyType,
    submission: PsrSubmissionRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[Either[String, Unit]] =
    for {
      response <- psrSubmission(journeyType, submission, pensionSchemeId)
      emailResponse <- emailSubmissionService.submitEmail(submission.schemeName, response, pensionSchemeId)
    } yield emailResponse

  private def psrSubmission(
    journeyType: JourneyType,
    submission: PsrSubmissionRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrSubmissionEtmpResponse] = {
    import submission._

    psrConnector
      .getSippPsr(pstr, fbNumber, periodStartDate, psrVersion)
      .flatMap {
        case Some(response) =>
          val submittedBy = pensionSchemeId.submittedBy
          val updateRequest = SippPsrSubmissionEtmpRequest(
            response.reportDetails
              .copy(status = EtmpPsrStatus.Submitted, version = None)
              .completeOutstandingAssetDeclarations(response.memberAndTransactions),
            response.accountingPeriodDetails,
            response.memberAndTransactions.flatMap(NonEmptyList.fromList),
            EtmpSippPsrDeclaration(
              submittedBy = submittedBy,
              submitterID = pensionSchemeId.value,
              psaID = Some(submission.psaId),
              psaDeclaration =
                Option.when(SubmittedBy.PSA == submittedBy)(Declaration(declaration1 = true, declaration2 = true)),
              pspDeclaration =
                Option.when(SubmittedBy.PSP == submittedBy)(Declaration(declaration1 = true, declaration2 = true))
            ).some
          )
          submitWithRequest(
            journeyType,
            pstr,
            pensionSchemeId,
            Future.successful(updateRequest),
            maybeTaxYear = submission.taxYear.some,
            maybeSchemeName = submission.schemeName
          ).map(_ => response)
        case None =>
          Future.failed(new Exception(s"Submission with pstr $pstr not found"))
      }
  }

  def createEmptySippPsr(
    reportDetails: ReportDetails,
    pensionSchemeId: PensionSchemeId
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val request = SippPsrSubmissionEtmpRequest(
      reportDetails = reportDetails.transformInto[EtmpSippReportDetails].copy(memberTransactions = YesNo.No),
      accountingPeriodDetails = None,
      memberAndTransactions = None,
      psrDeclaration = None
    )
    submitWithRequest(
      JourneyType.Standard,
      pstr = reportDetails.pstr,
      pensionSchemeId = pensionSchemeId,
      thunk = Future.successful(request),
      maybeTaxYear = Some(reportDetails.taxYearDateRange),
      maybeSchemeName = reportDetails.schemeName
    )
  }

  def updateMemberTransactions(
    pstr: String,
    journeyType: JourneyType,
    fbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    memberTransactions: MemberTransactions,
    pensionSchemeId: PensionSchemeId
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[SippPsrJourneySubmissionEtmpResponse] =
    psrConnector
      .getSippPsr(pstr, fbNumber, optPeriodStartDate, optPsrVersion)
      .flatMap {
        case Some(response) =>
          val reportDetails = response.reportDetails
          val updated = SippPsrSubmissionEtmpRequest(
            reportDetails = reportDetails.copy(
              memberTransactions = memberTransactions.value,
              version = None
            ),
            accountingPeriodDetails = response.accountingPeriodDetails,
            memberAndTransactions = response.memberAndTransactions.flatMap(NonEmptyList.fromList),
            psrDeclaration = response.psrDeclaration
          )
          submitWithRequest(journeyType, pstr, pensionSchemeId, Future.successful(updated))

        case None =>
          Future.failed(
            new RuntimeException(
              s"Failed to update report details member transactions: Submission with pstr $pstr not found"
            )
          )
      }

  def updateAccountingPeriodDetails(
    pstr: String,
    journeyType: JourneyType,
    fbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    accountingPeriodDetailsRequest: AccountingPeriodDetailsRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[SippPsrJourneySubmissionEtmpResponse] =
    psrConnector
      .getSippPsr(pstr, fbNumber, optPeriodStartDate, optPsrVersion)
      .flatMap {
        case Some(response) =>
          val updated = SippPsrSubmissionEtmpRequest(
            reportDetails = response.reportDetails.copy(version = None),
            accountingPeriodDetails = accountingPeriodDetailsRequest.toEtmp,
            memberAndTransactions = response.memberAndTransactions.flatMap(NonEmptyList.fromList),
            psrDeclaration = response.psrDeclaration
          )
          submitWithRequest(journeyType, pstr, pensionSchemeId, Future.successful(updated))

        case None =>
          Future.failed(
            new RuntimeException(s"Failed to update accounting periods: Submission with pstr $pstr not found")
          )
      }

  def getSippPsr(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[Option[PSRSubmissionResponse]] =
    OptionT(psrConnector.getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion))
      .map(response => psrSubmissionTransformer.transform(response, true))
      .value

  private def getSippPsrFiltered(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader) =
    OptionT(psrConnector.getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion))
      .map(filterDeletedMembers)

  private def filterDeletedMembers(response: SippPsrSubmissionEtmpResponse) =
    response.copy(memberAndTransactions = response.memberAndTransactions.map(_.filter(_.status != Deleted)))

  def getPsrVersions(
    pstr: String,
    startDate: LocalDate
  )(implicit hc: HeaderCarrier, rh: RequestHeader): Future[Seq[PsrVersionsResponse]] =
    psrConnector.getPsrVersions(pstr, startDate)

  def getMemberDetails(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[Option[MemberDetailsResponse]] =
    getSippPsrFiltered(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .subflatMap(memberDetailsTransformer.transform)
      .value

  def getPsrAssetsExistence(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Either[Unit, Option[PsrAssetCountsResponse]]] =
    (for {
      psr <- EitherT(
        psrConnector
          .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
          .map(_.toRight(()))
      )
      counts <- EitherT.pure[Future, Unit](psrAssetsExistenceTransformer.transform(psr))
    } yield counts).value

  def getPsrAssetDeclarations(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit
    headerCarrier: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[Option[PsrAssetDeclarationsResponse]] =
    OptionT(psrConnector.getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion))
      .map(psrAssetDeclarationsTransformer.transform)
      .value

  def updateMemberDetails(
    journeyType: JourneyType,
    pstr: String,
    fbNumber: String,
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: UpdateMemberDetailsRequest,
    pensionSchemeId: PensionSchemeId
  )(implicit hc: HeaderCarrier, requestHeader: RequestHeader): Future[Option[SippPsrJourneySubmissionEtmpResponse]] =
    psrConnector
      .getSippPsr(pstr, fbNumber.some, optPeriodStartDate, optPsrVersion)
      .flatMap {
        case Some(response) =>
          val recordFound = response.memberAndTransactions.toList.flatten
            .exists(t => compare(t.memberDetails.personalDetails, request.current))
          if (recordFound) {
            val updateRequest = SippPsrSubmissionEtmpRequest(
              reportDetails = response.reportDetails.copy(status = EtmpPsrStatus.Compiled, version = None),
              accountingPeriodDetails = response.accountingPeriodDetails,
              memberAndTransactions = response.memberAndTransactions.flatMap { memberAndTransactions =>
                val updatedMemberAndTransactions = memberAndTransactions.map { memberAndTransactions =>
                  if (compare(memberAndTransactions.memberDetails.personalDetails, request.current)) {
                    memberAndTransactions.copy(
                      memberDetails = memberAndTransactions.memberDetails.copy(personalDetails = request.updated),
                      status = SectionStatus.Changed,
                      version = None
                    )
                  } else memberAndTransactions
                }
                NonEmptyList.fromList(updatedMemberAndTransactions)
              },
              psrDeclaration = response.psrDeclaration
            )
            submitWithRequest(journeyType, pstr, pensionSchemeId, Future.successful(updateRequest)).map(_.some)
          } else
            Future.successful(SippPsrJourneySubmissionEtmpResponse(fbNumber).some)
        case None =>
          Future.successful(None)
      }

  def deleteMember(
    journeyType: JourneyType,
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    personalDetails: PersonalDetails,
    pensionSchemeId: PensionSchemeId
  )(implicit
    hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[SippPsrJourneySubmissionEtmpResponse] =
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .flatMap {
        case Some(response) =>
          val updateRequest = SippPsrSubmissionEtmpRequest(
            // Declaration changed to Compiled state back
            reportDetails = response.reportDetails.copy(status = EtmpPsrStatus.Compiled, version = None),
            accountingPeriodDetails = response.accountingPeriodDetails,
            memberAndTransactions = response.memberAndTransactions.flatMap { members =>
              val updatedMembers = members.map { member =>
                if (MemberDetails.compare(member.memberDetails.personalDetails, personalDetails)) {
                  member.copy(status = Deleted, version = None) // Soft delete
                } else {
                  member
                }
              }
              NonEmptyList.fromList(updatedMembers)
            },
            psrDeclaration = response.psrDeclaration.map(declaration => // Declaration changed to Compiled state back
              declaration.copy(
                psaDeclaration =
                  declaration.psaDeclaration.map(current => current.copy(declaration1 = false, declaration2 = false)),
                pspDeclaration =
                  declaration.pspDeclaration.map(current => current.copy(declaration1 = false, declaration2 = false))
              )
            )
          )
          submitWithRequest(journeyType, pstr, pensionSchemeId, Future.successful(updateRequest))
        case None =>
          Future.failed(new RuntimeException(s"Submission with pstr $pstr not found"))
      }

  def deleteAssets(
    journey: Journey,
    journeyType: JourneyType,
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    pensionSchemeId: PensionSchemeId
  )(implicit
    hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[SippPsrJourneySubmissionEtmpResponse] =
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .flatMap {
        case Some(response) =>
          val updatedMembers = response.memberAndTransactions.flatMap { mTxs =>
            NonEmptyList.fromList(mTxs.map(mTx => deleteAssetForJourney(journey, mTx)))
          }

          val updateRequest = SippPsrSubmissionEtmpRequest(
            reportDetails = response.reportDetails
              .copy(status = EtmpPsrStatus.Compiled, version = None)
              .withAssetClassDeclaration(journey, declaration = none[YesNo]),
            accountingPeriodDetails = response.accountingPeriodDetails,
            memberAndTransactions = updatedMembers,
            psrDeclaration = response.psrDeclaration.map(declaration =>
              declaration.copy(
                psaDeclaration =
                  declaration.psaDeclaration.map(current => current.copy(declaration1 = false, declaration2 = false)),
                pspDeclaration =
                  declaration.pspDeclaration.map(current => current.copy(declaration1 = false, declaration2 = false))
              )
            )
          )

          submitWithRequest(journeyType, pstr, pensionSchemeId, Future.successful(updateRequest))

        case None =>
          Future.failed(new RuntimeException(s"Submission with pstr $pstr not found"))
      }

  private def deleteAssetForJourney(journey: Journey, member: EtmpMemberAndTransactions): EtmpMemberAndTransactions = {
    if (member.status == Deleted) return member

    // Attempt to delete the specific asset based on the journey
    val updatedMember = journey match {
      case Journey.InterestInLandOrProperty =>
        member.landConnectedParty.as(member.copy(landConnectedParty = Some(SippLandConnectedParty(0, None, None))))

      case Journey.ArmsLengthLandOrProperty =>
        member.landArmsLength.as(member.copy(landArmsLength = Some(SippLandArmsLength(0, None, None))))

      case Journey.TangibleMoveableProperty =>
        member.tangibleProperty.as(member.copy(tangibleProperty = Some(SippTangibleProperty(0, None, None))))

      case Journey.OutstandingLoans =>
        member.loanOutstanding.as(member.copy(loanOutstanding = Some(SippLoanOutstanding(0, None, None))))

      case Journey.UnquotedShares =>
        member.unquotedShares.as(member.copy(unquotedShares = Some(SippUnquotedShares(0, None, None))))

      case Journey.AssetFromConnectedParty =>
        member.otherAssetsConnectedParty.as(
          member.copy(otherAssetsConnectedParty = Some(SippOtherAssetsConnectedParty(0, None, None)))
        )
    }

    // Check if an asset was deleted and determine the status
    updatedMember
      .map { m =>
        if (m.areJourneysEmpty) {
          m.copy(status = SectionStatus.Deleted, version = None)
        } else {
          m.copy(status = SectionStatus.Changed, version = None)
        }
      }
      .getOrElse(member) // Return original member if no asset was deleted
  }

}
