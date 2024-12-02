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

package uk.gov.hmrc.pensionschemereturnsipp.controllers

import cats.implicits.catsSyntaxEitherId
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{~, Name}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.models.JourneyType.Standard
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.OptionalResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{PsrAssetCountsResponse, UpdateMemberDetailsRequest}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{PsrVersionsResponse, ReportStatus}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.PersonalDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrJourneySubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.{Journey, JourneyType}
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, TestValues}

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class SippPsrSubmitControllerSpec extends BaseSpec with TestValues {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val fakeRequest = FakeRequest("POST", "/")
  private val mockSippPsrSubmissionService = mock[SippPsrSubmissionService]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val requestJson: JsValue = Json.parse(
    s"""
       |{
       |  "pstr" : "17836742CF",
       |  "fbNumber": "fb",
       |  "periodStartDate" : "2022-04-06",
       |  "psrVersion": "1",
       |  "psaId": "A0001234",
       |  "taxYear": {"from": "2022-04-06", "to": "2023-04-06"},
       |  "schemeName": "testScheme"
       |}
       |""".stripMargin
  )

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSippPsrSubmissionService)
  }

  val modules: Seq[GuiceableModule] =
    Seq(
      bind[SippPsrSubmissionService].toInstance(mockSippPsrSubmissionService),
      bind[AuthConnector].toInstance(mockAuthConnector)
    )

  val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false)
    .overrides(modules*)
    .build()

  private val controller = application.injector.instanceOf[SippPsrSubmitController]

  "POST SIPP PSR" must {

    "return 401 - Bearer token expired" in {

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.failed(new BearerTokenExpired)
        )

      val thrown = intercept[AuthorisationException] {
        await(controller.submitSippPsr(Standard)(fakeRequest))
      }

      thrown.reason mustBe "Bearer token expired"

      verify(mockSippPsrSubmissionService, never).submitSippPsr(any(), any(), any())(any(), any())
      verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
    }

    "return 401 - Bearer token not supplied" in {

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.failed(new MissingBearerToken)
        )

      val thrown = intercept[AuthorisationException] {
        await(controller.submitSippPsr(Standard)(fakeRequest))
      }

      thrown.reason mustBe "Bearer token not supplied"

      verify(mockSippPsrSubmissionService, never).submitSippPsr(any(), any(), any())(any(), any())
      verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
    }

    "return 200" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.submitSippPsr(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(().asRight))

      val result = controller.submitSippPsr(Standard)(fakeRequest.withJsonBody(requestJson))
      status(result) mustBe CREATED
    }
  }

  "GET SIPP PSR" must {
    "return 200" in {

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(samplePsrSubmission)))

      val result = controller.getSippPsr("testPstr", Some("fbNumber"), None, None)(fakeRequest)
      status(result) mustBe Status.OK
    }

    "return 404" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getSippPsr(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.getSippPsr("testPstr", None, Some("periodStartDate"), Some("version"))(fakeRequest)
      status(result) mustBe Status.NOT_FOUND
    }
  }

  "getPsrVersions" should {

    "return OK and the PSR versions when service returns valid data" in {
      val pstr = "testPstr"
      val startDate = LocalDate.now()

      val psrVersionsResponse = Seq(
        PsrVersionsResponse(
          reportFormBundleNumber = "bundle1",
          reportVersion = 1,
          reportStatus = ReportStatus.Compiled,
          compilationOrSubmissionDate = ZonedDateTime.now(),
          reportSubmitterDetails = None,
          psaDetails = None
        )
      )

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getPsrVersions(any(), any())(any(), any()))
        .thenReturn(Future.successful(psrVersionsResponse))

      val request = FakeRequest(GET, s"/psr-versions/$pstr/${startDate.toString}")
      val result = controller.getPsrVersions(pstr, startDate.toString).apply(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(psrVersionsResponse)
    }

    "return BadRequest when the start date is invalid" in {
      val pstr = "testPstr"
      val invalidStartDate = "invalid-date"

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      val request = FakeRequest(GET, s"/psr-versions/$pstr/$invalidStartDate")
      val result = controller.getPsrVersions(pstr, invalidStartDate).apply(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid startDate")
    }
  }

  "GET Member Details" must {
    "return 200" in {

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getMemberDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(sampleApiMemberDetailsResponse)))

      val result = controller.getMemberDetails("testPstr", Some("fbNumber"), None, None)(fakeRequest)
      status(result) mustBe Status.OK
    }

    "return 404" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getMemberDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result =
        controller.getMemberDetails("testPstr", None, Some("periodStartDate"), Some("version"))(fakeRequest)
      status(result) mustBe Status.NOT_FOUND
    }
  }

  "Update Member " must {
    "return BadRequest when update fails" in {
      val oldPersonalDetails = PersonalDetails("John", "Doe", Some("AB123456C"), None, LocalDate.of(1980, 1, 1))
      val newPersonalDetails = PersonalDetails("John", "Dow", Some("AB123456D"), None, LocalDate.of(1980, 1, 1))
      val memberDetailsRequest = UpdateMemberDetailsRequest(oldPersonalDetails, newPersonalDetails)

      val putRequest = FakeRequest(PUT, "/")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.toJson(memberDetailsRequest))

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(
        mockSippPsrSubmissionService.updateMemberDetails(any(), any(), any(), any(), any(), any(), any())(any(), any())
      )
        .thenReturn(Future.failed(new Exception("Submission not found")))

      val result = controller.updateMember(pstr, Standard, "fbNumber", None, None)(putRequest)

      status(result) mustBe Status.BAD_REQUEST
    }
  }

  "Delete Member " must {
    "return ok in" in {
      val personalDetails = PersonalDetails("John", "Doe", Some("AB123456C"), None, LocalDate.of(1980, 1, 1))
      val fakeRequest = FakeRequest(DELETE, "/")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.toJson(personalDetails))

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.deleteMember(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(SippPsrJourneySubmissionEtmpResponse("000123456")))

      val result = controller.deleteMember("testPstr", Standard, Some("fbNumber"), None, None)(fakeRequest)
      status(result) mustBe Status.OK
    }

    "return bad request" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.deleteMember(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception(s"Submission with pstr $pstr not found")))

      val result =
        controller.deleteMember("testPstr", Standard, None, Some("periodStartDate"), Some("version"))(fakeRequest)
      status(result) mustBe Status.BAD_REQUEST
    }

    "return 400 when request body is invalid" in {
      val invalidRequest = FakeRequest(DELETE, "/").withHeaders("Content-Type" -> "application/json")

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      val result = controller.deleteMember("testPstr", Standard, Some("fbNumber"), None, None)(invalidRequest)
      status(result) mustBe Status.BAD_REQUEST
    }
  }

  "Delete Assets " must {
    "return ok" in {
      val personalDetails = PersonalDetails("John", "Doe", Some("AB123456C"), None, LocalDate.of(1980, 1, 1))
      val fakeRequest = FakeRequest(DELETE, "/")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.toJson(personalDetails))

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.deleteAssets(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(SippPsrJourneySubmissionEtmpResponse("000123456")))

      val result =
        controller.deleteAssets("testPstr", Journey.InterestInLandOrProperty, Standard, Some("fbNumber"), None, None)(
          fakeRequest
        )
      status(result) mustBe Status.OK
    }

    "return bad request" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.deleteAssets(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception(s"Submission with pstr $pstr not found")))

      val result =
        controller.deleteAssets(
          "testPstr",
          Journey.InterestInLandOrProperty,
          JourneyType.Standard,
          None,
          Some("periodStartDate"),
          Some("version")
        )(fakeRequest)
      status(result) mustBe Status.BAD_REQUEST
    }
  }

  "GET Assets Existence" must {
    "return 200" in {

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getPsrAssetsExistence(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(samplePsrAssetsExistenceResponse).asRight[Unit]))

      val result = controller.getPsrAssetsExistence("testPstr", Some("fbNumber"), None, None)(fakeRequest)
      status(result) mustBe Status.OK

      val response = contentAsJson(result).as[OptionalResponse[PsrAssetCountsResponse]](OptionalResponse.formatter())
      response.response mustBe Some(samplePsrAssetsExistenceResponse)
    }

    "return 200 when the PSR exists but has no members or transactions" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getPsrAssetsExistence(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None.asRight[Unit]))

      val result =
        controller.getPsrAssetsExistence("testPstr", None, Some("periodStartDate"), Some("version"))(fakeRequest)
      status(result) mustBe Status.OK
      val response = contentAsJson(result).as[OptionalResponse[PsrAssetCountsResponse]](OptionalResponse.formatter())
      response.response mustBe None
    }

    "return 404 when the PSR does not exist" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getPsrAssetsExistence(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(().asLeft))

      val result =
        controller.getPsrAssetsExistence("testPstr", None, Some("periodStartDate"), Some("version"))(fakeRequest)
      status(result) mustBe Status.NOT_FOUND
    }
  }

  "GET PSR Asset Declarations" must {
    "return 200" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getPsrAssetDeclarations(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(samplePsrAssetDeclarationsResponse)))

      val result = controller.getPsrAssetDeclarations("testPstr", Some("fbNumber"), None, None)(fakeRequest)
      status(result) mustBe Status.OK
    }

    "return 404" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.getPsrAssetDeclarations(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result =
        controller.getPsrAssetDeclarations("testPstr", None, Some("periodStartDate"), Some("version"))(fakeRequest)
      status(result) mustBe Status.NOT_FOUND
    }
  }
}
