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

import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, never, reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status
import play.api.libs.json.{JsDefined, JsString, JsValue, Json}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HttpException, HttpResponse, RequestEntityTooLargeException, UpstreamErrorResponse}
import uk.gov.hmrc.pensionschemereturnsipp.Generators.minimalDetailsGen
import uk.gov.hmrc.pensionschemereturnsipp.models.SchemeStatus.Open
import uk.gov.hmrc.pensionschemereturnsipp.models.api.FileUploadAuditContext
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.DateRange
import uk.gov.hmrc.pensionschemereturnsipp.models.common.SubmittedBy
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration.Declaration
import uk.gov.hmrc.pensionschemereturnsipp.models.{Establisher, MinimalDetails, PensionSchemeId, SchemeDetails}
import uk.gov.hmrc.pensionschemereturnsipp.services.AuditService
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec
import uk.gov.hmrc.pensionschemereturnsipp.models.JourneyType
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpPsrStatus
import uk.gov.hmrc.pensionschemereturnsipp.audit.ApiAuditUtil.*
import uk.gov.hmrc.pensionschemereturnsipp.audit.ApiAuditUtil.SippPsrSubmissionEtmpRequestOps

import java.time.LocalDate
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
        payload = payload,
        checkReturnDates = None
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

  "fireFileUploadAuditEvent" must {

    doNothing().when(mockAuditService).sendEvent(any())(any(), any())

    val fileUploadAuditContext = FileUploadAuditContext(
      schemeDetails = SchemeDetails(
        schemeName = "test",
        pstr = "test",
        schemeStatus = Open,
        schemeType = "test",
        authorisingPSAID = None,
        establishers = List.empty[Establisher]
      ),
      fileUploadType = "test",
      fileUploadStatus = "test",
      fileName = "test.csv",
      fileReference = "test",
      fileSize = 12345L,
      validationCompleted = LocalDate.of(2020, 1, 1),
      taxYear = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))
    )

    val fireFileUploadAuditEventPf =
      service.fireFileUploadAuditEvent(
        pensionSchemeId,
        minimalDetails,
        Some(fileUploadAuditContext)
      )

    "do not send audit event for a successful response" in {
      fireFileUploadAuditEventPf(Success(HttpResponse.apply(Status.OK, responseData, Map.empty)))
      verify(mockAuditService, never()).sendEvent(any())(any(), any())
    }

    "do not send the audit event when a non RequestEntityTooLargeException error occurs" in {
      val message = "The request had a network error"
      val status = Status.SERVICE_UNAVAILABLE
      fireFileUploadAuditEventPf(Failure(new HttpException(message, status)))
      verify(mockAuditService, never()).sendEventWithSource(any(), any())(any(), any())
    }

    "send the audit event when a RequestEntityTooLargeException is thrown" in {
      val message = "Too Large"
      fireFileUploadAuditEventPf(Failure(new RequestEntityTooLargeException(message)))

      val expectedAuditEvent = FileUploadAuditEvent(
        fileUploadType = fileUploadAuditContext.fileUploadType,
        fileUploadStatus = fileUploadAuditContext.fileUploadStatus,
        fileName = fileUploadAuditContext.fileName,
        fileReference = fileUploadAuditContext.fileReference,
        typeOfError = FileUploadAuditEvent.ERROR_SIZE_LIMIT,
        fileSize = fileUploadAuditContext.fileSize,
        validationCompleted = LocalDate.now(),
        pensionSchemeId = pensionSchemeId,
        minimalDetails = minimalDetails,
        schemeDetails = fileUploadAuditContext.schemeDetails,
        taxYear = fileUploadAuditContext.taxYear
      )

      verify(mockAuditService, times(1))
        .sendEventWithSource(ArgumentMatchers.eq(expectedAuditEvent), any())(any(), any())
    }

    "send audit with correct JSON structure on RequestEntityTooLargeException" in {
      val message = "Too Large"
      val fixedNow = LocalDate.of(2024, 1, 1)

      val context = fileUploadAuditContext.copy(validationCompleted = fixedNow)
      val handler = service.fireFileUploadAuditEvent(
        pensionSchemeId,
        minimalDetails,
        Some(context)
      )

      handler(Failure(new RequestEntityTooLargeException(message)))

      val eventCaptor = ArgumentCaptor.forClass(classOf[AuditEvent])
      verify(mockAuditService).sendEventWithSource(eventCaptor.capture(), any())(any(), any())

      val actualEvent = eventCaptor.getValue.asInstanceOf[FileUploadAuditEvent]
      val actualDetails = actualEvent.details

      actualDetails \ "fileUploadType" mustBe JsDefined(JsString(context.fileUploadType))
      actualDetails \ "fileUploadStatus" mustBe JsDefined(JsString(context.fileUploadStatus))
      actualDetails \ "fileName" mustBe JsDefined(JsString(context.fileName))
      actualDetails \ "fileReference" mustBe JsDefined(JsString(context.fileReference))
      actualDetails \ "fileSize" mustBe JsDefined(JsString(context.fileSize.toString))
      actualDetails \ "taxYear" mustBe JsDefined(
        JsString(s"${context.taxYear.from.getYear}-${context.taxYear.to.getYear}")
      )
      actualDetails \ "typeOfError" mustBe JsDefined(JsString(FileUploadAuditEvent.ERROR_SIZE_LIMIT))
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

    "return ChangedCompiled for JourneyType.Amend with EtmpPsrStatus.Compiled" in {
      val request = sampleSippPsrSubmissionEtmpRequest.copy(
        reportDetails = sampleSippPsrSubmissionEtmpRequest.reportDetails.copy(
          status = EtmpPsrStatus.Compiled
        )
      )

      val result = request.auditAmendDetailPsrStatus(JourneyType.Amend)

      result mustBe Some(ChangedCompiled)
    }

    "return ChangedSubmitted for JourneyType.Amend with EtmpPsrStatus.Submitted" in {
      val request = sampleSippPsrSubmissionEtmpRequest.copy(
        reportDetails = sampleSippPsrSubmissionEtmpRequest.reportDetails.copy(
          status = EtmpPsrStatus.Submitted
        )
      )

      val result = request.auditAmendDetailPsrStatus(JourneyType.Amend)

      result mustBe Some(ChangedSubmitted)
    }

    "return None for JourneyType.Standard" in {
      val request = sampleSippPsrSubmissionEtmpRequest.copy(
        reportDetails = sampleSippPsrSubmissionEtmpRequest.reportDetails.copy(
          status = EtmpPsrStatus.Submitted
        )
      )

      val result = request.auditAmendDetailPsrStatus(JourneyType.Standard)

      result mustBe None
    }

  }
}
