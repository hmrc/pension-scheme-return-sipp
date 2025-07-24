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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.{Application, Logger}
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.connectors.SchemeDetailsConnector
import uk.gov.hmrc.pensionschemereturnsipp.models.JourneyType.Standard
import uk.gov.hmrc.pensionschemereturnsipp.models.api.AssetsFromConnectedPartyApi.{formatReq, formatRes}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{
  AssetsFromConnectedPartyRequest,
  AssetsFromConnectedPartyResponse,
  ReportDetails
}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.Yes
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpPsrStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrJourneySubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, TestPayloads, TestValues}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class AssetsFromConnectedPartyControllerSpec extends BaseSpec with TestValues with TestPayloads {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val fakeRequest = FakeRequest("PUT", "/").withHeaders("srn" -> srn)
  private val mockService = mock[SippPsrSubmissionService]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val mockLogger = mock[Logger]
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockService)
    reset(mockLogger)

    when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
      .thenReturn(Future.successful(new ~(Some(externalId), enrolments)))
    when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(true))
  }

  val modules: Seq[GuiceableModule] =
    Seq(
      bind[SippPsrSubmissionService].toInstance(mockService),
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[SchemeDetailsConnector].toInstance(mockSchemeDetailsConnector)
    )

  val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false)
    .overrides(modules*)
    .build()

  private val controller = application.injector.instanceOf[AssetsFromConnectedPartyController]

  "GET AssetsFromConnectedParty" must {

    "return 200 with data" in {

      val response = AssetsFromConnectedPartyResponse(
        transactions = List.empty
      )

      when(mockService.getAssetsFromConnectedParty(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(response)))

      val result = controller.get("testPstr", Some("fbNumber"), Some("2022-04-06"), Some("1.0"))(fakeRequest)
      status(result) mustBe Status.OK
      contentAsJson(result) mustBe Json.toJson(response)
    }

    "return 204 with no data" in {

      when(mockService.getAssetsFromConnectedParty(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get("testPstr", Some("fbNumber"), Some("2022-04-06"), Some("1.0"))(fakeRequest)
      status(result) mustBe Status.NO_CONTENT
    }

    "throw AuthorisationException when bearer token not supplied" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(new MissingBearerToken))
      val thrown = intercept[AuthorisationException] {
        await(
          controller.get("testPstr", Some("fbNumber"), Some("2022-04-06"), Some("1.0"))(fakeRequest)
        )
      }
      thrown.reason mustBe "Bearer token not supplied"
    }
  }

  "PUT AssetsFromConnectedParty" must {

    "return 204 with no data" in {
      val requestBody = Json.toJson(
        AssetsFromConnectedPartyRequest(
          reportDetails = ReportDetails(
            pstr = "test-pstr",
            status = EtmpPsrStatus.Compiled,
            periodStart = LocalDate.now,
            periodEnd = LocalDate.now,
            schemeName = Some("Schema Name"),
            version = Some("001"),
            memberTransactions = Yes
          ),
          transactions = None,
          auditContext = None
        )
      )

      when(mockService.submitAssetsFromConnectedParty(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(SippPsrJourneySubmissionEtmpResponse("form-bundle-no-1")))

      val result = controller.put(Standard, Some("fbNumber"), None, None)(fakePutRequestWithBody(requestBody))

      status(result) mustBe Status.CREATED
    }

    "return 204 with data" in {

      when(mockService.submitAssetsFromConnectedParty(any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(SippPsrJourneySubmissionEtmpResponse("form-bundle-no-1")))

      val result =
        controller.put(Standard, Some("fbNumber"), None, None)(fakePutRequestWithBody(assetFromConnectedPartyPayload))

      status(result) mustBe Status.CREATED
    }

    "throws AuthorisationException when bearer token not supplied" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(new MissingBearerToken))

      val thrown = intercept[AuthorisationException] {
        await(
          controller.put(Standard, Some("fbNumber"), None, None)(fakePutRequestWithBody(assetFromConnectedPartyPayload))
        )
      }
      thrown.reason mustBe "Bearer token not supplied"
    }
  }
}
