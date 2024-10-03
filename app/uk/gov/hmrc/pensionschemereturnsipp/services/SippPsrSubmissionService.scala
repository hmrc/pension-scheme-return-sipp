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

import cats.data.{EitherT, NonEmptyList}
import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import cats.syntax.either._
import com.google.inject.{Inject, Singleton}
import io.scalaland.chimney.dsl._
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.connectors.{MinimalDetailsConnector, MinimalDetailsError, PsrConnector}
import uk.gov.hmrc.pensionschemereturnsipp.models.api._
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{PsrVersionsResponse, SubmittedBy, YesNo}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration.Declaration
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.MemberDetails.compare
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.{
  SippPsrJourneySubmissionEtmpResponse,
  SippPsrSubmissionEtmpResponse
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{MemberDetails, _}
import uk.gov.hmrc.pensionschemereturnsipp.models.{Journey, JourneyType, MinimalDetails, PensionSchemeId}
import uk.gov.hmrc.pensionschemereturnsipp.transformations._
import uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp.{
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
        transactions,
        transformer
      ).map { etmpDataAfterMerge =>
        SippPsrSubmissionEtmpRequest(
          reportDetails = reportDetails.transformInto[EtmpSippReportDetails],
          accountingPeriodDetails = None,
          memberAndTransactions = NonEmptyList.fromList(etmpDataAfterMerge),
          psrDeclaration = None
        )
      }
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
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map {
        case Some(existingEtmpData) =>
          Some(transformer.transformToResponse(existingEtmpData.memberAndTransactions.getOrElse(List.empty)))
        case None =>
          None
      }

  private def mergeWithExistingEtmpData[A, V](
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    reportDetails: ReportDetails,
    transactions: Option[NonEmptyList[A]],
    transformer: Transformer[A, V]
  )(implicit
    hc: HeaderCarrier,
    requestHeader: RequestHeader
  ): Future[List[EtmpMemberAndTransactions]] =
    psrConnector
      .getSippPsr(reportDetails.pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
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
    journeyType: JourneyType,
    submission: PsrSubmissionRequest,
    submittedBy: SubmittedBy,
    submitterId: String,
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[Either[String, Unit]] =
    for {
      response <- psrSubmission(journeyType, submission, submittedBy, submitterId, pensionSchemeId)
      emailResponse <- emailSubmissionService.submitEmail(submission.schemeName, response, pensionSchemeId)
    } yield emailResponse

  private def psrSubmission(
    journeyType: JourneyType,
    submission: PsrSubmissionRequest,
    submittedBy: SubmittedBy,
    submitterId: String,
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[SippPsrSubmissionEtmpResponse] = {
    import submission._

    psrConnector
      .getSippPsr(pstr, fbNumber, periodStartDate, psrVersion)
      .flatMap {
        case Some(response) =>
          val updateRequest = SippPsrSubmissionEtmpRequest(
            response.reportDetails.copy(status = EtmpPsrStatus.Submitted, version = None),
            response.accountingPeriodDetails,
            response.memberAndTransactions.flatMap(NonEmptyList.fromList),
            EtmpSippPsrDeclaration(
              submittedBy = submittedBy,
              submitterID = submitterId,
              psaID = pensionSchemeId.value.some,
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

  def getSippPsr(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier, requestHeader: RequestHeader): Future[Option[PSRSubmissionResponse]] =
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map(_.map(psrSubmissionTransformer.transform))

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
    psrConnector
      .getSippPsr(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
      .map(_.flatMap(memberDetailsTransformer.transform))

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
          Future.failed(new Exception(s"Submission with pstr $pstr not found"))
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
            reportDetails = response.reportDetails.copy(status = EtmpPsrStatus.Compiled, version = None),
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
          Future.failed(new Exception(s"Submission with pstr $pstr not found"))
      }

  private def deleteAssetForJourney(journey: Journey, member: EtmpMemberAndTransactions): EtmpMemberAndTransactions = {
    if (member.status == Deleted) return member

    // Attempt to delete the specific asset based on the journey
    val updatedMember = journey match {
      case Journey.InterestInLandOrProperty =>
        member.landConnectedParty.map(_ => member.copy(landConnectedParty = None))

      case Journey.ArmsLengthLandOrProperty =>
        member.landArmsLength.map(_ => member.copy(landArmsLength = None))

      case Journey.TangibleMoveableProperty =>
        member.tangibleProperty.map(_ => member.copy(tangibleProperty = None))

      case Journey.OutstandingLoans =>
        member.loanOutstanding.map(_ => member.copy(loanOutstanding = None))

      case Journey.UnquotedShares =>
        member.unquotedShares.map(_ => member.copy(unquotedShares = None))

      case Journey.AssetFromConnectedParty =>
        member.otherAssetsConnectedParty.map(_ => member.copy(otherAssetsConnectedParty = None))
    }

    // Check if an asset was deleted and determine the status
    updatedMember
      .map { m =>
        if (noRemainingAssets(m)) {
          m.copy(status = SectionStatus.Deleted, version = None)
        } else {
          m.copy(status = SectionStatus.Changed, version = None)
        }
      }
      .getOrElse(member) // Return original member if no asset was deleted
  }

  private def noRemainingAssets(updatedMember: EtmpMemberAndTransactions): Boolean =
    updatedMember.landConnectedParty.isEmpty &&
      updatedMember.landArmsLength.isEmpty &&
      updatedMember.tangibleProperty.isEmpty &&
      updatedMember.loanOutstanding.isEmpty &&
      updatedMember.unquotedShares.isEmpty &&
      updatedMember.otherAssetsConnectedParty.isEmpty

}
