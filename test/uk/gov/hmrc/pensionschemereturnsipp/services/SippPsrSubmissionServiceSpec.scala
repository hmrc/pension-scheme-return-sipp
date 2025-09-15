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
import cats.implicits.catsSyntaxOptionId
import cats.syntax.either.*
import org.mockito.ArgumentMatchers.{any, eq as mockitoEq}
import org.mockito.Mockito.*
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pensionschemereturnsipp.Generators.minimalDetailsGen
import uk.gov.hmrc.pensionschemereturnsipp.connectors.{MinimalDetailsConnector, PsrConnector}
import uk.gov.hmrc.pensionschemereturnsipp.models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  OutstandingLoans,
  TangibleMoveableProperty,
  UnquotedShares
}
import uk.gov.hmrc.pensionschemereturnsipp.models.JourneyType.Standard
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{
  AssetsFromConnectedPartyRequest,
  AssetsFromConnectedPartyResponse,
  LandOrConnectedPropertyRequest,
  LandOrConnectedPropertyResponse,
  OutstandingLoansRequest,
  OutstandingLoansResponse,
  PSRSubmissionResponse,
  PsrSubmissionRequest,
  TangibleMoveablePropertyRequest,
  TangibleMoveablePropertyResponse,
  UnquotedShareRequest,
  UnquotedShareResponse,
  UpdateMemberDetailsRequest
}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.SubmittedBy.PSP
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{AccountingPeriod, AccountingPeriodDetails, YesNo}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration.Declaration
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.{
  SippPsrJourneySubmissionEtmpResponse,
  SippPsrSubmissionEtmpResponse
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.*
import uk.gov.hmrc.pensionschemereturnsipp.models.{Journey, JourneyType}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp.{
  PSRAssetDeclarationsTransformer,
  PSRAssetsExistenceTransformer,
  PSRMemberDetailsTransformer,
  PSRSubmissionTransformer
}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  LandArmsLengthTransformer,
  LandConnectedPartyTransformer,
  OutstandingLoansTransformer,
  TangibleMoveablePropertyTransformer,
  UnquotedSharesTransformer
}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpTestValues, TestValues}
import uk.gov.hmrc.pensionschemereturnsipp.validators.JSONSchemaValidator

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SippPsrSubmissionServiceSpec extends BaseSpec with TestValues with SippEtmpTestValues {

  override def beforeEach(): Unit = {
    reset(mockPsrConnector)
    reset(mockJSONSchemaValidator)
    reset(mockSippPsrFromEtmp)
  }

  private val mockPsrConnector = mock[PsrConnector]
  private val mockJSONSchemaValidator = mock[JSONSchemaValidator]
  private val mockSippPsrFromEtmp = mock[PSRSubmissionTransformer]
  private val mockMemberDetailsTransformer = mock[PSRMemberDetailsTransformer]
  private val mockLandConnectedPartyTransformer = mock[LandConnectedPartyTransformer]
  private val mockArmsLengthTransformer = mock[LandArmsLengthTransformer]
  private val mockOutstandingLoansTransformer = mock[OutstandingLoansTransformer]
  private val mockAssetsFromConnectedPartyTransformer = mock[AssetsFromConnectedPartyTransformer]
  private val mockUnquotedSharesTransformer = mock[UnquotedSharesTransformer]
  private val mockTangibleMovablePropertyTransformer = mock[TangibleMoveablePropertyTransformer]
  private val mockEmailSubmissionService = mock[EmailSubmissionService]
  private val mockMinimalDetailsConnector = mock[MinimalDetailsConnector]
  private val mockPsrExistenceTransformer = mock[PSRAssetsExistenceTransformer]
  private val mockPsrAssetDeclarationsTransformer = mock[PSRAssetDeclarationsTransformer]

  private val service: SippPsrSubmissionService = new SippPsrSubmissionService(
    mockPsrConnector,
    mockSippPsrFromEtmp,
    mockMemberDetailsTransformer,
    mockPsrExistenceTransformer,
    mockPsrAssetDeclarationsTransformer,
    mockLandConnectedPartyTransformer,
    mockArmsLengthTransformer,
    mockOutstandingLoansTransformer,
    mockAssetsFromConnectedPartyTransformer,
    mockUnquotedSharesTransformer,
    mockTangibleMovablePropertyTransformer,
    mockEmailSubmissionService,
    mockMinimalDetailsConnector
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private val minimalDetails = minimalDetailsGen.sample.get
  when(mockMinimalDetailsConnector.fetch(samplePsaId)).thenReturn(Future.successful(minimalDetails.asRight))
  when(mockMinimalDetailsConnector.fetch(samplePspId)).thenReturn(Future.successful(minimalDetails.asRight))

  "getLandOrConnectedProperty" should {
    "return the LandOrConnectedPropertyResponse when data is found" in {
      val sampleResponse = LandOrConnectedPropertyResponse(transactions = List(landConnectedTransaction))

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockLandConnectedPartyTransformer.transformToResponse(any()))
        .thenReturn(sampleResponse)

      val result = service.getLandOrConnectedProperty("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe Some(sampleResponse)
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }

    "return None when no data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getLandOrConnectedProperty("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe None
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }
  }

  "getOutstandingLoans" should {
    "return the OutstandingLoansResponse when data is found" in {
      val sampleResponse = OutstandingLoansResponse(transactions = List(outstandingLoanTransaction))

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockOutstandingLoansTransformer.transformToResponse(any()))
        .thenReturn(sampleResponse)

      val result = service.getOutstandingLoans("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe Some(sampleResponse)
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }

    "return None when no data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getOutstandingLoans("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe None
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }
  }

  "getTangibleMoveableProperty" should {
    "return the TangibleMoveablePropertyResponse when data is found" in {
      val sampleResponse = TangibleMoveablePropertyResponse(transactions = List(tangiblePropertyTransaction))

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockTangibleMovablePropertyTransformer.transformToResponse(any()))
        .thenReturn(sampleResponse)

      val result = service.getTangibleMoveableProperty("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe Some(sampleResponse)
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }

    "return None when no data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getTangibleMoveableProperty("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe None
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }
  }

  "getUnquotedShares" should {
    "return the UnquotedSharesResponse when data is found" in {
      val sampleResponse = UnquotedShareResponse(transactions = List(unquotedShareTransaction))

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockUnquotedSharesTransformer.transformToResponse(any()))
        .thenReturn(sampleResponse)

      val result = service.getUnquotedShares("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe Some(sampleResponse)
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }

    "return None when no data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getUnquotedShares("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe None
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }
  }

  "getAssetsFromConnectedParty" should {
    "return the AssetsFromConnectedPartyResponse when data is found" in {
      val sampleResponse = AssetsFromConnectedPartyResponse(transactions = List(assetsFromConnectedPartyTransaction))

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockAssetsFromConnectedPartyTransformer.transformToResponse(any()))
        .thenReturn(sampleResponse)

      val result = service.getAssetsFromConnectedParty("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe Some(sampleResponse)
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
      verify(mockAssetsFromConnectedPartyTransformer, times(1)).transformToResponse(any())
    }

    "return None when no data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getAssetsFromConnectedParty("testPstr", Some("fbNumber"), None, None).futureValue

      result mustBe None
      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
    }
  }

  "submitLandOrConnectedProperty" should {
    "fetch and construct new ETMP request without transactions when no ETMP or transaction data exists" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest =
        fullSippPsrSubmissionEtmpRequest
          .copy(memberAndTransactions = None, accountingPeriodDetails = None)
          .copy(reportDetails =
            fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransLandPropCon = Some(YesNo.Yes))
          )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockLandConnectedPartyTransformer.merge(any(), any())).thenReturn(List())

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = LandOrConnectedPropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(landConnectedTransaction)),
        auditContext = None
      )

      whenReady(
        service.submitLandOrConnectedProperty(Standard, Some("fbNumber"), None, None, request, samplePensionSchemeId)
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }

    }

    "fetch and construct new ETMP request with transactions when no ETMP data exists, but transactions are passed in" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest = fullSippPsrSubmissionEtmpRequest
        .copy(
          memberAndTransactions = Some(NonEmptyList.one(etmpDataWithLandConnectedTx)),
          accountingPeriodDetails = None
        )
        .copy(reportDetails =
          fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransLandPropCon = Some(YesNo.Yes))
        )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockLandConnectedPartyTransformer.merge(any(), any())).thenReturn(List(etmpDataWithLandConnectedTx))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = LandOrConnectedPropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(landConnectedTransaction)),
        auditContext = None
      )

      whenReady(
        service.submitLandOrConnectedProperty(Standard, Some("fbNumber"), None, None, request, samplePensionSchemeId)
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }

    }
  }

  "submitOutstandingLoans" should {
    "fetch and construct new ETMP request without transactions when no ETMP or transaction data exists" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest =
        fullSippPsrSubmissionEtmpRequest
          .copy(memberAndTransactions = None, accountingPeriodDetails = None)
          .copy(reportDetails =
            fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransOutstandingLoan = Some(YesNo.Yes))
          )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockOutstandingLoansTransformer.merge(any(), any())).thenReturn(List())

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = OutstandingLoansRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippOutstandingLoansApi)),
        auditContext = None
      )

      whenReady(
        service.submitOutstandingLoans(
          Standard,
          Some("fbNumber"),
          None,
          None,
          request,
          samplePensionSchemeId,
          auditContext = None
        )
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }

    }

    "fetch and construct new ETMP request with transactions when no ETMP data exists, but transactions are passed in" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest = fullSippPsrSubmissionEtmpRequest
        .copy(
          memberAndTransactions = Some(NonEmptyList.one(etmpDataWithLandConnectedTx)),
          accountingPeriodDetails = None
        )
        .copy(reportDetails =
          fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransOutstandingLoan = Some(YesNo.Yes))
        )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockOutstandingLoansTransformer.merge(any(), any())).thenReturn(List(etmpDataWithLandConnectedTx))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = OutstandingLoansRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippOutstandingLoansApi)),
        auditContext = None
      )

      whenReady(
        service.submitOutstandingLoans(
          Standard,
          Some("fbNumber"),
          None,
          None,
          request,
          samplePensionSchemeId,
          auditContext = None
        )
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }

    }
  }

  "submitLandArmsLength" should {
    "fetch and construct new ETMP request without transactions when no ETMP or transaction data exists" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest =
        fullSippPsrSubmissionEtmpRequest
          .copy(memberAndTransactions = None, accountingPeriodDetails = None)
          .copy(reportDetails =
            fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransLandPropArmsLen = Some(YesNo.Yes))
          )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockArmsLengthTransformer.merge(any(), any())).thenReturn(List())

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = LandOrConnectedPropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(landConnectedTransaction)),
        auditContext = None
      )

      whenReady(
        service.submitLandArmsLength(Standard, Some("fbNumber"), None, None, request, samplePensionSchemeId, None)
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }

    }

    "fetch and construct new ETMP request with transactions when no ETMP data exists, but transactions are passed in" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest = fullSippPsrSubmissionEtmpRequest
        .copy(
          memberAndTransactions = Some(NonEmptyList.one(etmpDataWithLandConnectedTx)),
          accountingPeriodDetails = None
        )
        .copy(reportDetails =
          fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransLandPropArmsLen = Some(YesNo.Yes))
        )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockArmsLengthTransformer.merge(any(), any())).thenReturn(List(etmpDataWithLandConnectedTx))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = LandOrConnectedPropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(landConnectedTransaction)),
        auditContext = None
      )

      whenReady(
        service.submitLandArmsLength(Standard, Some("fbNumber"), None, None, request, samplePensionSchemeId, None)
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }

    }
  }

  "getPsrAssetDeclarations" should {
    "successfully return asset declaration" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockPsrAssetDeclarationsTransformer.transform(any())).thenReturn(samplePsrAssetDeclarationsResponse)

      val result = service.getPsrAssetDeclarations(pstr, Some("test"), None, None).futureValue

      result mustBe Some(samplePsrAssetDeclarationsResponse)
    }
  }

  "submitAssetsFromConnectedParty" should {
    "fetch and construct new ETMP request without transactions when no ETMP or transaction data exists" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest =
        fullSippPsrSubmissionEtmpRequest
          .copy(memberAndTransactions = None, accountingPeriodDetails = None)
          .copy(reportDetails =
            fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransAssetCon = Some(YesNo.Yes))
          )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockAssetsFromConnectedPartyTransformer.merge(any(), any())).thenReturn(List())

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = AssetsFromConnectedPartyRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippAssetsFromConnectedPartyApi)),
        auditContext = None
      )

      whenReady(
        service.submitAssetsFromConnectedParty(
          Standard,
          Some("fbNumber"),
          None,
          None,
          request,
          samplePensionSchemeId,
          None
        )
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }

    }

    "fetch and construct new ETMP request with transactions when no ETMP data exists, but transactions are passed in" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest = fullSippPsrSubmissionEtmpRequest
        .copy(
          memberAndTransactions = Some(NonEmptyList.one(etmpDataWithLandConnectedTx)),
          accountingPeriodDetails = None
        )
        .copy(reportDetails =
          fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransAssetCon = Some(YesNo.Yes))
        )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockAssetsFromConnectedPartyTransformer.merge(any(), any())).thenReturn(List(etmpDataWithLandConnectedTx))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = AssetsFromConnectedPartyRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippAssetsFromConnectedPartyApi)),
        auditContext = None
      )

      whenReady(
        service.submitAssetsFromConnectedParty(
          Standard,
          Some("fbNumber"),
          None,
          None,
          request,
          samplePensionSchemeId,
          None
        )
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }

    }
  }

  "submitTangibleMoveableProperty" should {
    "fetch and construct new ETMP request without transactions when no ETMP or transaction data exists" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest =
        fullSippPsrSubmissionEtmpRequest
          .copy(memberAndTransactions = None, accountingPeriodDetails = None)
          .copy(reportDetails =
            fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransTangPropArmsLen = Some(YesNo.Yes))
          )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockTangibleMovablePropertyTransformer.merge(any(), any())).thenReturn(List())

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = TangibleMoveablePropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippTangibleApi)),
        auditContext = None
      )

      whenReady(
        service.submitTangibleMoveableProperty(
          Standard,
          Some("fbNumber"),
          None,
          None,
          request,
          samplePensionSchemeId,
          None
        )
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }

    }

    "fetch and construct new ETMP request with transactions when no ETMP data exists, but transactions are passed in" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest = fullSippPsrSubmissionEtmpRequest
        .copy(
          memberAndTransactions = Some(NonEmptyList.one(etmpDataWithLandConnectedTx)),
          accountingPeriodDetails = None
        )
        .copy(reportDetails =
          fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransTangPropArmsLen = Some(YesNo.Yes))
        )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockTangibleMovablePropertyTransformer.merge(any(), any())).thenReturn(List(etmpDataWithLandConnectedTx))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = TangibleMoveablePropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippTangibleApi)),
        auditContext = None
      )

      whenReady(
        service.submitTangibleMoveableProperty(
          Standard,
          Some("fbNumber"),
          None,
          None,
          request,
          samplePensionSchemeId,
          None
        )
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }

    }
  }

  "submitUnquotedShare" should {
    "fetch and construct new ETMP request without transactions when no ETMP or transaction data exists" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest =
        fullSippPsrSubmissionEtmpRequest
          .copy(memberAndTransactions = None, accountingPeriodDetails = None)
          .copy(reportDetails =
            fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransUnquotedShares = Some(YesNo.Yes))
          )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockUnquotedSharesTransformer.merge(any(), any())).thenReturn(List())

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = UnquotedShareRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippUnquotedShareApi)),
        auditContext = None
      )

      whenReady(
        service.submitUnquotedShares(Standard, Some("fbNumber"), None, None, request, samplePensionSchemeId, None)
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }

    }

    "fetch and construct new ETMP request with transactions when no ETMP data exists, but transactions are passed in" in {
      val sippResponse = SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")
      val response = HttpResponse(200, Json.toJson(sippResponse).toString())
      val etmpRequest = fullSippPsrSubmissionEtmpRequest
        .copy(
          memberAndTransactions = Some(NonEmptyList.one(etmpDataWithLandConnectedTx)),
          accountingPeriodDetails = None
        )
        .copy(reportDetails =
          fullSippPsrSubmissionEtmpRequest.reportDetails.copy(memberTransUnquotedShares = Some(YesNo.Yes))
        )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockUnquotedSharesTransformer.merge(any(), any())).thenReturn(List(etmpDataWithLandConnectedTx))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = UnquotedShareRequest(
        testReportDetails,
        Some(NonEmptyList.one(sippUnquotedShareApi)),
        auditContext = None
      )

      whenReady(
        service.submitUnquotedShares(Standard, Some("fbNumber"), None, None, request, samplePensionSchemeId, None)
      ) { (result: SippPsrJourneySubmissionEtmpResponse) =>
        result mustBe sippResponse

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(etmpRequest),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }

    }
  }

  "getSippPsr" should {
    "return 200 without data when connector returns successfully" in {

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      whenReady(service.getSippPsr("testPstr", Some("fbNumber"), None, None)) {
        (result: Option[PSRSubmissionResponse]) =>
          result mustBe None

          verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
          verify(mockSippPsrFromEtmp, never).transform(any(), any())
      }
    }

    "return 200 with data when connector returns successfully" in {

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))
      when(mockSippPsrFromEtmp.transform(any(), any())).thenReturn(samplePsrSubmission)

      whenReady(service.getSippPsr("testPstr", Some("fbNumber"), None, None)) {
        (result: Option[PSRSubmissionResponse]) =>
          result mustBe Some(samplePsrSubmission)

          verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
          verify(mockSippPsrFromEtmp, times(1)).transform(any(), any())
      }
    }
  }

  "submitSippPsr" should {
    val psaPspId = samplePsaId
    val req = PsrSubmissionRequest(
      pstr,
      "fb".some,
      "2024-04-06".some,
      psrVersion = None,
      psaId = samplePsaId.value,
      taxYear = DateRange(LocalDate.now(), LocalDate.now()),
      schemeName = None
    )
    val etmpResponse = SippPsrSubmissionEtmpResponse(
      reportDetails = EtmpSippReportDetails(
        pstr,
        EtmpPsrStatus.Compiled,
        LocalDate.parse("2024-09-09"),
        LocalDate.parse("2024-09-09"),
        YesNo.Yes,
        memberTransLandPropCon = None,
        memberTransAssetCon = None,
        memberTransLandPropArmsLen = None,
        memberTransTangPropArmsLen = None,
        memberTransOutstandingLoan = None,
        memberTransUnquotedShares = None,
        None
      ),
      accountingPeriodDetails = AccountingPeriodDetails(
        "".some,
        NonEmptyList.one(AccountingPeriod(LocalDate.parse("2024-09-09"), LocalDate.parse("2025-09-09")))
      ).some,
      memberAndTransactions = None,
      psrDeclaration = None
    )

    "successfully submit only minimal required SIPP submission details" in {
      val sippResponse = Json.toJson(SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")).toString()
      val expectedResponse = HttpResponse(200, sippResponse)

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(etmpResponse.some))
      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(expectedResponse))
      when(mockEmailSubmissionService.submitEmail(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(())))

      whenReady(service.submitSippPsr(Standard, req, samplePensionSchemeId)) { _ =>
        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any(),
          any()
        )
      }
    }

    "successfully submit SIPP submission with correctly set declaration for PSA" in {
      val sippResponse = Json.toJson(SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")).toString()
      val expectedResponse = HttpResponse(200, sippResponse)

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(etmpResponse.some))
      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(expectedResponse))
      when(mockEmailSubmissionService.submitEmail(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(())))

      whenReady(service.submitSippPsr(Standard, req, samplePensionSchemeId)) { _ =>
        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(submittedETMPRequest),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }
    }

    "successfully submit SIPP submission with correctly set declaration for PSP" in {
      val sippResponse = Json.toJson(SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")).toString()
      val expectedResponse = HttpResponse(200, sippResponse)

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(etmpResponse.some))
      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(expectedResponse))
      when(mockEmailSubmissionService.submitEmail(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(())))

      whenReady(service.submitSippPsr(Standard, req, samplePspId)) { _ =>
        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(
            submittedETMPRequest
              .copy(
                psrDeclaration = submittedETMPRequest.psrDeclaration
                  .map(
                    _.copy(
                      pspDeclaration = Some(Declaration(true, true)),
                      psaDeclaration = None,
                      submittedBy = PSP,
                      submitterID = samplePspId.value
                    )
                  )
              )
          ),
          any(),
          any(),
          any(),
          any()
        )(
          any(),
          any()
        )
      }
    }

    "throw exception when connector call not successful for submitSippPsr" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(etmpResponse.some))
      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("invalid-request")))

      val thrown = intercept[BadRequestException] {
        await(service.submitSippPsr(Standard, req, psaPspId))

      }
      thrown.responseCode mustBe BAD_REQUEST
      thrown.message must include("invalid-request")

      verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
      verify(mockPsrConnector, times(1)).submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(
        any(),
        any()
      )
    }
  }

  "getPsrVersions" should {
    "successfully return the expected versions" in {
      val date = LocalDate.now()
      when(mockPsrConnector.getPsrVersions(pstr, date)).thenReturn(Future.successful(Seq.empty))
      service.getPsrVersions(pstr, date).futureValue mustEqual Seq.empty
    }
  }

  "getMemberDetails" should {
    "successfully return Member Details" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockMemberDetailsTransformer.transform(any())).thenReturn(Some(sampleApiMemberDetailsResponse))

      val result = service.getMemberDetails(pstr, Some("test"), None, None).futureValue

      result mustBe Some(sampleApiMemberDetailsResponse)
    }

    "return None when no Member Details are returned" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getMemberDetails(pstr, Some("test"), None, None).futureValue

      result mustBe None
    }
  }

  "updateMemberDetails" should {

    val currentPersonalDetails = PersonalDetails("John", "Doe", Some("AA123456A"), None, LocalDate.of(1980, 1, 1))
    val updatedPersonalDetails = PersonalDetails("John", "Down", Some("AA123456B"), None, LocalDate.of(1990, 1, 1))
    val request = UpdateMemberDetailsRequest(currentPersonalDetails, updatedPersonalDetails)

    val fbNumber = "test-fbNumber"
    val journeyType = JourneyType.Standard

    "update member details when the record is found" in {
      val sippResponse = Json.toJson(SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")).toString()
      val response = HttpResponse(200, sippResponse)

      val sampleResponse = SippPsrSubmissionEtmpResponse(
        reportDetails = EtmpSippReportDetails(
          pstr,
          EtmpPsrStatus.Compiled,
          LocalDate.now(),
          LocalDate.now(),
          YesNo.Yes,
          memberTransLandPropCon = None,
          memberTransAssetCon = None,
          memberTransLandPropArmsLen = None,
          memberTransTangPropArmsLen = None,
          memberTransOutstandingLoan = None,
          memberTransUnquotedShares = None,
          None
        ),
        accountingPeriodDetails = None,
        memberAndTransactions = Some(
          List(
            etmpDataWithLandConnectedTx.copy(
              memberDetails = MemberDetails(currentPersonalDetails)
            )
          )
        ),
        psrDeclaration = None
      )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleResponse)))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      whenReady(
        service.updateMemberDetails(Standard, pstr, "test-fb-num", None, None, request, samplePensionSchemeId)
      ) { _ =>
        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(
          any(),
          any()
        )
      }
    }

    "return the submission response when the record is not found" in {
      val sampleResponse = SippPsrSubmissionEtmpResponse(
        reportDetails = EtmpSippReportDetails(
          pstr,
          EtmpPsrStatus.Compiled,
          LocalDate.now(),
          LocalDate.now(),
          YesNo.Yes,
          memberTransLandPropCon = None,
          memberTransAssetCon = None,
          memberTransLandPropArmsLen = None,
          memberTransTangPropArmsLen = None,
          memberTransOutstandingLoan = None,
          memberTransUnquotedShares = None,
          None
        ),
        accountingPeriodDetails = None,
        memberAndTransactions = Some(
          List(etmpSippMemberAndTransactions)
        ),
        psrDeclaration = None
      )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleResponse)))

      whenReady(service.updateMemberDetails(journeyType, pstr, fbNumber, None, None, request, samplePensionSchemeId)) {
        _ =>
          verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
          verify(mockPsrConnector, times(0)).submitSippPsr(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
          )(
            any(),
            any()
          )
      }
    }

    "return None when no SippPsr data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      whenReady(service.updateMemberDetails(journeyType, pstr, fbNumber, None, None, request, samplePensionSchemeId)) {
        _ =>
          verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
          verify(mockPsrConnector, times(0)).submitSippPsr(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
          )(
            any(),
            any()
          )
      }
    }
  }

  "delete member" should {
    "successfully delete" in {
      val sippResponse = Json.toJson(SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")).toString()
      val response = HttpResponse(200, sippResponse)
      val sampleResponse = SippPsrSubmissionEtmpResponse(
        reportDetails = EtmpSippReportDetails(
          pstr,
          EtmpPsrStatus.Compiled,
          LocalDate.now(),
          LocalDate.now(),
          YesNo.Yes,
          memberTransLandPropCon = None,
          memberTransAssetCon = None,
          memberTransLandPropArmsLen = None,
          memberTransTangPropArmsLen = None,
          memberTransOutstandingLoan = None,
          memberTransUnquotedShares = None,
          None
        ),
        accountingPeriodDetails = None,
        memberAndTransactions = Some(List(etmpDataWithLandConnectedTx)),
        psrDeclaration = None
      )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleResponse)))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      whenReady(
        service.deleteMember(
          Standard,
          pstr,
          None,
          None,
          None,
          etmpDataWithLandConnectedTx.memberDetails.personalDetails,
          samplePensionSchemeId
        )
      ) { _ =>
        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(
            SippPsrSubmissionEtmpRequest(
              reportDetails = sampleResponse.reportDetails.copy(version = None),
              accountingPeriodDetails = None,
              memberAndTransactions = Some(
                NonEmptyList.one(etmpDataWithLandConnectedTx.copy(status = SectionStatus.Deleted, version = None))
              ),
              psrDeclaration = None
            )
          ),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }
    }

    "fail when no SippPsr data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      assertThrows[RuntimeException](
        service
          .deleteMember(
            Standard,
            pstr,
            None,
            None,
            None,
            etmpDataWithLandConnectedTx.memberDetails.personalDetails,
            samplePensionSchemeId
          )
          .futureValue
      )
    }

  }

  "delete assets" should {
    "successfully delete the correct assets for each journey type and mark member as changed" in {
      val sippResponse = Json.toJson(SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")).toString()
      val response = HttpResponse(200, sippResponse)
      val sampleResponse = SippPsrSubmissionEtmpResponse(
        reportDetails = EtmpSippReportDetails(
          pstr,
          EtmpPsrStatus.Compiled,
          LocalDate.now(),
          LocalDate.now(),
          YesNo.Yes,
          memberTransLandPropCon = Some(YesNo.No),
          memberTransAssetCon = None,
          memberTransLandPropArmsLen = None,
          memberTransTangPropArmsLen = None,
          memberTransOutstandingLoan = None,
          memberTransUnquotedShares = None,
          None
        ),
        accountingPeriodDetails = None,
        memberAndTransactions = Some(
          List(
            etmpDataWithLandConnectedTx.copy(
              landConnectedParty = Some(sippLandConnectedParty),
              otherAssetsConnectedParty = Some(sippOtherAssetsConnectedParty),
              landArmsLength = Some(sippLandArmsLength),
              tangibleProperty = Some(sippTangibleProperty),
              loanOutstanding = Some(sippLoanOutstanding),
              unquotedShares = Some(sippUnquotedShares)
            )
          )
        ),
        psrDeclaration = None
      )

      val journeyTypes = Journey.values // All journey types
      journeyTypes.foreach { journey =>
        reset(mockPsrConnector)

        when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(sampleResponse)))

        when(
          mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any())
        )
          .thenReturn(Future.successful(response))

        whenReady(
          service.deleteAssets(
            journey,
            Standard,
            pstr,
            None,
            None,
            None,
            samplePensionSchemeId
          )
        ) { _ =>
          verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
          verify(mockPsrConnector, times(1)).submitSippPsr(
            any(),
            any(),
            any(),
            any(),
            mockitoEq(
              SippPsrSubmissionEtmpRequest(
                reportDetails =
                  sampleResponse.reportDetails.copy(version = None).withAssetClassDeclaration(journey, None),
                accountingPeriodDetails = None,
                memberAndTransactions = Some(
                  NonEmptyList.one(
                    etmpDataWithLandConnectedTx
                      .copy(
                        status = SectionStatus.Changed,
                        version = None,
                        landConnectedParty =
                          if (journey != InterestInLandOrProperty) Some(sippLandConnectedParty)
                          else Some(SippLandConnectedParty(0, None, None)),
                        otherAssetsConnectedParty =
                          if (journey != AssetFromConnectedParty) Some(sippOtherAssetsConnectedParty)
                          else Some(SippOtherAssetsConnectedParty(0, None, None)),
                        landArmsLength =
                          if (journey != ArmsLengthLandOrProperty) Some(sippLandArmsLength)
                          else Some(SippLandArmsLength(0, None, None)),
                        tangibleProperty =
                          if (journey != TangibleMoveableProperty) Some(sippTangibleProperty)
                          else Some(SippTangibleProperty(0, None, None)),
                        loanOutstanding =
                          if (journey != OutstandingLoans) Some(sippLoanOutstanding)
                          else Some(SippLoanOutstanding(0, None, None)),
                        unquotedShares =
                          if (journey != UnquotedShares) Some(sippUnquotedShares)
                          else Some(SippUnquotedShares(0, None, None))
                      )
                  )
                ),
                psrDeclaration = None
              )
            ),
            any(),
            any(),
            any(),
            any()
          )(any(), any())
        }
      }
    }

    "successfully delete a member if no assets left" in {
      val sippResponse = Json.toJson(SippPsrJourneySubmissionEtmpResponse("form-bundle-number-1")).toString()
      val response = HttpResponse(200, sippResponse)
      val sampleResponse = SippPsrSubmissionEtmpResponse(
        reportDetails = EtmpSippReportDetails(
          pstr,
          EtmpPsrStatus.Compiled,
          LocalDate.now(),
          LocalDate.now(),
          YesNo.Yes,
          memberTransLandPropCon = None,
          memberTransAssetCon = None,
          memberTransLandPropArmsLen = None,
          memberTransTangPropArmsLen = None,
          memberTransOutstandingLoan = None,
          memberTransUnquotedShares = None,
          None
        ),
        accountingPeriodDetails = None,
        memberAndTransactions = Some(List(etmpDataWithLandConnectedTx)),
        psrDeclaration = None
      )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleResponse)))

      when(mockPsrConnector.submitSippPsr(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      whenReady(
        service.deleteAssets(
          Journey.InterestInLandOrProperty,
          Standard,
          pstr,
          None,
          None,
          None,
          samplePensionSchemeId
        )
      ) { _ =>
        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(
          any(),
          any(),
          any(),
          any(),
          mockitoEq(
            SippPsrSubmissionEtmpRequest(
              reportDetails = sampleResponse.reportDetails
                .copy(version = None)
                .withAssetClassDeclaration(Journey.InterestInLandOrProperty, None),
              accountingPeriodDetails = None,
              memberAndTransactions = Some(
                NonEmptyList.one(
                  etmpDataWithLandConnectedTx
                    .copy(
                      status = SectionStatus.Deleted,
                      version = None,
                      landConnectedParty = Some(SippLandConnectedParty(0, None, None))
                    )
                )
              ),
              psrDeclaration = None
            )
          ),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      }
    }

    "fail when no SippPsr data is found" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      assertThrows[RuntimeException](
        service
          .deleteAssets(
            Journey.InterestInLandOrProperty,
            Standard,
            pstr,
            None,
            None,
            None,
            samplePensionSchemeId
          )
          .futureValue
      )
    }
  }

  "getAssetsExistence" should {
    "successfully return assets" in {
      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))

      when(mockPsrExistenceTransformer.transform(any())).thenReturn(Some(samplePsrAssetsExistenceResponse))

      val result = service.getPsrAssetsExistence(pstr, Some("test"), None, None).futureValue

      result mustBe Right(Some(samplePsrAssetsExistenceResponse))
    }
  }
}
