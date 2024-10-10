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

package uk.gov.hmrc.pensionschemereturnsipp.audit

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, never, reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.pensionschemereturnsipp.Generators.minimalDetailsGen
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.common.SubmittedBy
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration.Declaration
import uk.gov.hmrc.pensionschemereturnsipp.models.{MinimalDetails, PensionSchemeId}
import uk.gov.hmrc.pensionschemereturnsipp.services.AuditService
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class ApiAuditUtilSpec extends BaseSpec with BeforeAndAfterEach {

  private implicit lazy val rh: RequestHeader = FakeRequest("", "")

  private val mockAuditService = mock[AuditService]

  val data: JsValue = Json.obj("key" -> "value")
  val pensionSchemeId: PensionSchemeId = PensionSchemeId.PsaId("PSAID")
  val taxYear: Option[DateRange] = None
  val minimalDetails: MinimalDetails = minimalDetailsGen.sample.value

  private val payload = Json.obj("test" -> "test")
  private val responseData = Json.obj("responsetest" -> "test")

  override def beforeEach(): Unit =
    reset(mockAuditService)

  val service = new ApiAuditUtil(mockAuditService)

  "firePSRSubmissionEvent" must {

    doNothing().when(mockAuditService).sendEventWithSource(any(), any())(any(), any())
    val request = sampleSippPsrSubmissionEtmpRequest.copy(psrDeclaration =
      Some(
        EtmpSippPsrDeclaration(
          submittedBy = SubmittedBy.PSA,
          submitterID = "SubmitterID",
          psaID = Some("PsaID"),
          psaDeclaration = Some(Declaration(declaration1 = true, declaration2 = true)),
          pspDeclaration = None
        )
      )
    )
    val psrPostEventPf =
      service.firePSRSubmissionEvent(
        pstr,
        payload,
        pensionSchemeId,
        minimalDetails,
        Some(schemeName),
        None,
        request
      )

    "send the correct audit event for a successful response" in {

      psrPostEventPf(Success(HttpResponse.apply(Status.OK, responseData, Map.empty)))
      val expectedAuditEvent = PSRSubmissionEvent(
        pstr = pstr,
        pensionSchemeId = pensionSchemeId,
        minimalDetails = minimalDetails,
        schemeName = Some(schemeName),
        taxYear = None,
        payload = payload
      )
      verify(mockAuditService, times(1)).sendEventWithSource(
        ArgumentMatchers.eq(expectedAuditEvent),
        ArgumentMatchers.eq("pension-scheme-return-sipp-frontend")
      )(any(), any())
    }

    "not send the audit event with the status code when an HttpException error occurs" in {
      val message = "The request had a network error"
      val status = Status.SERVICE_UNAVAILABLE
      psrPostEventPf(Failure(new HttpException(message, status)))
      verify(mockAuditService, never()).sendEventWithSource(any(), any())(any(), any())
    }

    "not send the audit event when a throwable is thrown" in {
      val message = "The request had a network error"
      psrPostEventPf(Failure(new RuntimeException(message)))
      verify(mockAuditService, never()).sendEventWithSource(any(), any())(any(), any())
    }
  }

  "firePsrPostAuditEvent" must {

    doNothing().when(mockAuditService).sendEvent(any())(any(), any())
    val psrPostEventPf =
      service.firePsrPostAuditEvent(pstr, payload, pensionSchemeId, minimalDetails, None, Some(schemeName), None)

    "send the correct audit event for a successful response" in {

      psrPostEventPf(Success(HttpResponse.apply(Status.OK, responseData, Map.empty)))
      val expectedAuditEvent = PsrPostAuditEvent(
        pstr = pstr,
        payload = payload,
        status = Some(Status.OK),
        response = Some(responseData),
        errorMessage = None,
        schemeName = Some(schemeName),
        taxYear = None,
        minimalDetails = minimalDetails,
        auditDetailPsrStatus = None,
        pensionSchemeId = pensionSchemeId
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }

    "send the audit event with the status code when an upstream error occurs" in {

      val reportAs = 202
      val message = "The request was not found"
      val status = Status.NOT_FOUND
      psrPostEventPf(Failure(UpstreamErrorResponse.apply(message, status, reportAs, Map.empty)))
      val expectedAuditEvent = PsrPostAuditEvent(
        pstr = pstr,
        payload = payload,
        status = Some(status),
        response = None,
        errorMessage = Some(message),
        schemeName = Some(schemeName),
        taxYear = None,
        minimalDetails = minimalDetails,
        auditDetailPsrStatus = None,
        pensionSchemeId = pensionSchemeId
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }

    "send the audit event with the status code when an HttpException error occurs" in {

      val message = "The request had a network error"
      val status = Status.SERVICE_UNAVAILABLE
      psrPostEventPf(Failure(new HttpException(message, status)))
      val expectedAuditEvent = PsrPostAuditEvent(
        pstr = pstr,
        payload = payload,
        status = Some(status),
        response = None,
        errorMessage = Some(message),
        schemeName = Some(schemeName),
        taxYear = None,
        minimalDetails = minimalDetails,
        auditDetailPsrStatus = None,
        pensionSchemeId = pensionSchemeId
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }

    "send the audit event when a throwable is thrown" in {

      val message = "The request had a network error"
      psrPostEventPf(Failure(new RuntimeException(message)))
      val expectedAuditEvent = PsrPostAuditEvent(
        pstr = pstr,
        payload = payload,
        status = None,
        response = None,
        errorMessage = Some(message),
        schemeName = Some(schemeName),
        taxYear = None,
        minimalDetails = minimalDetails,
        auditDetailPsrStatus = None,
        pensionSchemeId = pensionSchemeId
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }
  }

  "firePsrGetAuditEvent" must {

    doNothing().when(mockAuditService).sendEvent(any())(any(), any())

    "send the correct audit event for a successful response" in {
      val psrGetEventPf = service.firePsrGetAuditEvent(pstr, Some("FB_NUMBER"), None, None)
      psrGetEventPf(
        Try.apply(Some(sampleSippPsrSubmissionEtmpResponse))
      )
      val expectedAuditEvent = PsrGetAuditEvent(
        pstr = pstr,
        fbNumber = Some("FB_NUMBER"),
        periodStartDate = None,
        psrVersion = None,
        status = Some(Status.OK),
        response = Some(Json.toJson(sampleSippPsrSubmissionEtmpResponse)),
        errorMessage = None
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }

    "send the audit event with the status code when an upstream error occurs" in {
      val psrGetEventPf = service.firePsrGetAuditEvent(pstr, Some("FB_NUMBER"), None, None)
      val reportAs = 202
      val message = "The request was not found"
      val status = Status.NOT_FOUND
      psrGetEventPf(Failure(UpstreamErrorResponse.apply(message, status, reportAs, Map.empty)))
      val expectedAuditEvent = PsrGetAuditEvent(
        pstr = pstr,
        fbNumber = Some("FB_NUMBER"),
        periodStartDate = None,
        psrVersion = None,
        status = Some(status),
        response = None,
        errorMessage = Some(message)
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }

    "send the audit event with the status code when an HttpException error occurs" in {
      val psrGetEventPf = service.firePsrGetAuditEvent(pstr, Some("FB_NUMBER"), None, None)
      val message = "The request had a network error"
      val status = Status.SERVICE_UNAVAILABLE
      psrGetEventPf(Failure(new HttpException(message, status)))
      val expectedAuditEvent = PsrGetAuditEvent(
        pstr = pstr,
        fbNumber = Some("FB_NUMBER"),
        periodStartDate = None,
        psrVersion = None,
        status = Some(status),
        response = None,
        errorMessage = Some(message)
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }

    "send the audit event when a throwable is thrown" in {
      val psrGetEventPf = service.firePsrGetAuditEvent(pstr, Some("FB_NUMBER"), None, None)
      val message = "The request had a network error"
      psrGetEventPf(Failure(new RuntimeException(message)))
      val expectedAuditEvent = PsrGetAuditEvent(
        pstr = pstr,
        fbNumber = Some("FB_NUMBER"),
        periodStartDate = None,
        psrVersion = None,
        status = None,
        response = None,
        errorMessage = Some(message)
      )
      verify(mockAuditService, times(1)).sendEvent(ArgumentMatchers.eq(expectedAuditEvent))(any(), any())
    }
  }
}
