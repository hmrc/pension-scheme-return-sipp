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
import org.mockito.ArgumentMatchers.{any, eq => mockitoEq}
import org.mockito.MockitoSugar.{never, reset, times, verify, when}
import play.api.http.Status.{BAD_REQUEST, EXPECTATION_FAILED}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{BadRequestException, ExpectationFailedException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.pensionschemereturnsipp.connectors.PsrConnector
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeReturnValidationFailureException
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{LandOrConnectedPropertyRequest, PSRSubmissionResponse}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp.{PSRSubmissionTransformer, SippPsrSubmissionToEtmp}
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  LandArmsLengthTransformer,
  LandConnectedPartyTransformer,
  OutstandingLoansTransformer,
  TangibleMoveablePropertyTransformer,
  UnquotedSharesTransformer
}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpTestValues, TestValues}
import uk.gov.hmrc.pensionschemereturnsipp.validators.{JSONSchemaValidator, SchemaValidationResult}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SippPsrSubmissionServiceSpec extends BaseSpec with TestValues with SippEtmpTestValues {

  override def beforeEach(): Unit = {
    reset(mockPsrConnector)
    reset(mockJSONSchemaValidator)
    reset(mockSippPsrSubmissionToEtmp)
    reset(mockSippPsrFromEtmp)
  }

  private val mockPsrConnector = mock[PsrConnector]
  private val mockJSONSchemaValidator = mock[JSONSchemaValidator]
  private val mockSippPsrSubmissionToEtmp = mock[SippPsrSubmissionToEtmp]
  private val mockSippPsrFromEtmp = mock[PSRSubmissionTransformer]
  private val mockLandConnectedPartyTransformer = mock[LandConnectedPartyTransformer]
  private val mockArmsLengthTransformer = mock[LandArmsLengthTransformer]
  private val mockOutstandingLoansTransformer = mock[OutstandingLoansTransformer]
  private val mockAssetsFromConnectedPartyTransformer = mock[AssetsFromConnectedPartyTransformer]
  private val mockUnquotedSharesTransformer = mock[UnquotedSharesTransformer]
  private val mockTangibleMovablePropertyTransformer = mock[TangibleMoveablePropertyTransformer]

  private val service: SippPsrSubmissionService = new SippPsrSubmissionService(
    mockPsrConnector,
    mockJSONSchemaValidator,
    mockSippPsrSubmissionToEtmp,
    mockSippPsrFromEtmp,
    mockLandConnectedPartyTransformer,
    mockArmsLengthTransformer,
    mockOutstandingLoansTransformer,
    mockAssetsFromConnectedPartyTransformer,
    mockUnquotedSharesTransformer,
    mockTangibleMovablePropertyTransformer
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "submitLandOrConnectedProperty" should {
    "fetch and construct new ETMP request without transactions when no ETMP or transaction data exists" in {
      val response = HttpResponse(200, "OK")
      val etmpRequest =
        fullSippPsrSubmissionEtmpRequest.copy(memberAndTransactions = None, accountingPeriodDetails = None)

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockLandConnectedPartyTransformer.merge(any(), any())).thenReturn(List())

      when(mockPsrConnector.submitSippPsr(any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = LandOrConnectedPropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(landConnectedTransaction))
      )

      whenReady(service.submitLandOrConnectedProperty(request)) { result: HttpResponse =>
        result mustBe response

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(any(), mockitoEq(etmpRequest))(any(), any())
      }

    }

    "fetch and construct new ETMP request with transactions when no ETMP data exists, but transactions are passed in" in {
      val response = HttpResponse(200, "OK")
      val etmpRequest = fullSippPsrSubmissionEtmpRequest.copy(
        memberAndTransactions = Some(NonEmptyList.one(etmpDataWithLandConnectedTx)),
        accountingPeriodDetails = None
      )

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockLandConnectedPartyTransformer.merge(any(), any())).thenReturn(List(etmpDataWithLandConnectedTx))

      when(mockPsrConnector.submitSippPsr(any(), any())(any(), any()))
        .thenReturn(Future.successful(response))

      val request = LandOrConnectedPropertyRequest(
        testReportDetails,
        Some(NonEmptyList.one(landConnectedTransaction))
      )

      whenReady(service.submitLandOrConnectedProperty(request)) { result: HttpResponse =>
        result mustBe response

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(any(), mockitoEq(etmpRequest))(any(), any())
      }

    }
  }

  "getSippPsr" should {
    "return 200 without data when connector returns successfully" in {

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      whenReady(service.getSippPsr("testPstr", Some("fbNumber"), None, None)) { result: Option[PSRSubmissionResponse] =>
        result mustBe None

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockSippPsrFromEtmp, never).transform(any())
      }
    }

    "return 200 with data when connector returns successfully" in {

      when(mockPsrConnector.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleSippPsrSubmissionEtmpResponse)))
      when(mockSippPsrFromEtmp.transform(any())).thenReturn(samplePsrSubmission)

      whenReady(service.getSippPsr("testPstr", Some("fbNumber"), None, None)) { result: Option[PSRSubmissionResponse] =>
        result mustBe Some(samplePsrSubmission)

        verify(mockPsrConnector, times(1)).getSippPsr(any(), any(), any(), any())(any(), any())
        verify(mockSippPsrFromEtmp, times(1)).transform(any())
      }
    }
  }

  "submitSippPsr" should {
    "successfully submit only minimal required SIPP submission details" in {
      val expectedResponse = HttpResponse(200, Json.obj(), Map.empty)

      when(mockSippPsrSubmissionToEtmp.transform(any())).thenReturn(sampleSippPsrSubmissionEtmpRequest)
      when(mockJSONSchemaValidator.validatePayload(any(), any()))
        .thenReturn(SchemaValidationResult(Set.empty))
      when(mockPsrConnector.submitSippPsr(any(), any())(any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      whenReady(service.submitSippPsr(sampleSippPsrSubmission)) { result: HttpResponse =>
        result mustEqual expectedResponse

        verify(mockSippPsrSubmissionToEtmp, times(1)).transform(any())
        verify(mockJSONSchemaValidator, times(1)).validatePayload(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(any(), any())(any(), any())
      }
    }

    "successfully submit for max SIPP submission details" in {
      val expectedResponse = HttpResponse(200, Json.obj(), Map.empty)

      when(mockSippPsrSubmissionToEtmp.transform(any())).thenReturn(fullSippPsrSubmissionEtmpRequest)
      when(mockJSONSchemaValidator.validatePayload(any(), any()))
        .thenReturn(SchemaValidationResult(Set.empty))
      when(mockPsrConnector.submitSippPsr(any(), any())(any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      whenReady(service.submitSippPsr(sampleSippPsrSubmission)) { result: HttpResponse =>
        result mustEqual expectedResponse

        verify(mockSippPsrSubmissionToEtmp, times(1)).transform(any())
        verify(mockJSONSchemaValidator, times(1)).validatePayload(any(), any())
        verify(mockPsrConnector, times(1)).submitSippPsr(any(), any())(any(), any())
      }
    }

    "throw exception when validation fails for submitSippPsr" in {
      when(mockSippPsrSubmissionToEtmp.transform(any())).thenReturn(sampleSippPsrSubmissionEtmpRequest)
      when(mockJSONSchemaValidator.validatePayload(any(), any()))
        .thenReturn(SchemaValidationResult(Set(validationMessage)))

      val thrown = intercept[PensionSchemeReturnValidationFailureException] {
        await(service.submitSippPsr(sampleSippPsrSubmission))
      }
      thrown.responseCode mustBe BAD_REQUEST
      thrown.message must include("Invalid payload when submitSippPsr :-\ncustomMessage")

      verify(mockSippPsrSubmissionToEtmp, times(1)).transform(any())
      verify(mockJSONSchemaValidator, times(1)).validatePayload(any(), any())
      verify(mockPsrConnector, never).submitSippPsr(any(), any())(any(), any())
    }

    "throw exception when connector call not successful for submitSippPsr" in {
      when(mockSippPsrSubmissionToEtmp.transform(any())).thenReturn(sampleSippPsrSubmissionEtmpRequest)
      when(mockJSONSchemaValidator.validatePayload(any(), any()))
        .thenReturn(SchemaValidationResult(Set.empty))
      when(mockPsrConnector.submitSippPsr(any(), any())(any(), any()))
        .thenReturn(Future.failed(new BadRequestException("invalid-request")))

      val thrown = intercept[ExpectationFailedException] {
        await(service.submitSippPsr(sampleSippPsrSubmission))
      }
      thrown.responseCode mustBe EXPECTATION_FAILED
      thrown.message must include("Nothing to submit")

      verify(mockSippPsrSubmissionToEtmp, times(1)).transform(any())
      verify(mockJSONSchemaValidator, times(1)).validatePayload(any(), any())
      verify(mockPsrConnector, times(1)).submitSippPsr(any(), any())(any(), any())
    }
  }

  "getPsrVersions" should {
    "successfully return the expected versions" in {
      val pstr = "testpstr"
      val date = LocalDate.now()
      when(mockPsrConnector.getPsrVersions(pstr, date)).thenReturn(Future.successful(Seq.empty))
      service.getPsrVersions(pstr, date).futureValue mustEqual Seq.empty
    }
  }
}
