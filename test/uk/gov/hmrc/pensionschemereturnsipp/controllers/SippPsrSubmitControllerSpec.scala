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
import org.mockito.MockitoSugar.{never, reset, times, verify, when}
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{~, Name}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.models.JourneyType.Standard
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.PersonalDetails
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, TestValues}

import java.time.LocalDate
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
       |  "isPsa": true,
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
    .overrides(modules: _*)
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

      verify(mockSippPsrSubmissionService, never).submitSippPsr(any(), any(), any(), any(), any())(any(), any())
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

      verify(mockSippPsrSubmissionService, never).submitSippPsr(any(), any(), any(), any(), any())(any(), any())
      verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
    }

    "return 200" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.submitSippPsr(any(), any(), any(), any(), any())(any(), any()))
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

      val result = controller.getSippPsr("testPstr", None, Some("periodStartDate"), Some("psrVersion"))(fakeRequest)
      status(result) mustBe Status.NOT_FOUND
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
        controller.getMemberDetails("testPstr", None, Some("periodStartDate"), Some("psrVersion"))(fakeRequest)
      status(result) mustBe Status.NOT_FOUND
    }
  }

  "Delete Member " must {
    "return no content" in {
      val personalDetails = PersonalDetails("John", "Doe", Some("AB123456C"), None, LocalDate.of(1980, 1, 1))
      val fakeRequest = FakeRequest(DELETE, "/")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.toJson(personalDetails))

      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.deleteMember(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(()))

      val result = controller.deleteMember("testPstr", Standard, Some("fbNumber"), None, None)(fakeRequest)
      status(result) mustBe Status.NO_CONTENT
    }

    "return bad request" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
        )

      when(mockSippPsrSubmissionService.deleteMember(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception(s"Submission with pstr $pstr not found")))

      val result =
        controller.deleteMember("testPstr", Standard, None, Some("periodStartDate"), Some("psrVersion"))(fakeRequest)
      status(result) mustBe Status.BAD_REQUEST
    }
  }
}
