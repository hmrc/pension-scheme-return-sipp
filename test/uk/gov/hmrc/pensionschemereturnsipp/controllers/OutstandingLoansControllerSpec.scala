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
import org.mockito.MockitoSugar.{reset, when}
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{~, Name}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoansApi.formatRes
import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoansResponse
import uk.gov.hmrc.pensionschemereturnsipp.services.SippPsrSubmissionService
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, TestValues}

import scala.concurrent.{ExecutionContext, Future}

class OutstandingLoansControllerSpec extends BaseSpec with TestValues {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val fakeRequest = FakeRequest("PUT", "/")
  private val mockService = mock[SippPsrSubmissionService]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockService)

    when(mockAuthConnector.authorise[Option[String] ~ Enrolments ~ Option[Name]](any(), any())(any(), any()))
      .thenReturn(
        Future.successful(new ~(new ~(Some(externalId), enrolments), Some(Name(Some("FirstName"), Some("lastName")))))
      )
  }

  val modules: Seq[GuiceableModule] =
    Seq(
      bind[SippPsrSubmissionService].toInstance(mockService),
      bind[AuthConnector].toInstance(mockAuthConnector)
    )

  val application: Application = new GuiceApplicationBuilder()
    .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false)
    .overrides(modules: _*)
    .build()

  private val controller = application.injector.instanceOf[OutstandingLoansController]

  "GET LandOrConnectedProperty" must {

    "return 200 with data" in {

      val response = OutstandingLoansResponse(
        transactions = List.empty
      )

      when(mockService.getOutstandingLoans(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(response)))

      val result = controller.get("testPstr", Some("fbNumber"), Some("2022-04-06"), Some("1.0"))(fakeRequest)
      status(result) mustBe Status.OK
      contentAsJson(result) mustBe Json.toJson(response)
    }

    "return 204 with no data" in {

      when(mockService.getOutstandingLoans(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get("testPstr", Some("fbNumber"), Some("2022-04-06"), Some("1.0"))(fakeRequest)
      status(result) mustBe Status.NO_CONTENT
    }
  }
}
