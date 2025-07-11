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

package uk.gov.hmrc.pensionschemereturnsipp.auth

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers.should
import play.api.http.Status
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.pensionschemereturnsipp.connectors.SchemeDetailsConnector
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants
import uk.gov.hmrc.pensionschemereturnsipp.config.Constants.*
import uk.gov.hmrc.pensionschemereturnsipp.models.SchemeId.Srn
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PsrAuthSpec extends BaseSpec {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockSchemeDetailsConnector = mock[SchemeDetailsConnector]

  override protected def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSchemeDetailsConnector)
  }

  private val psaEnrolment = Enrolments(
    Set(
      Enrolment(
        psaEnrolmentKey,
        Seq(
          EnrolmentIdentifier("PSAID", psaId)
        ),
        "Activated",
        None
      )
    )
  )

  private val pspEnrolment = Enrolments(
    Set(
      Enrolment(
        pspEnrolmentKey,
        Seq(
          EnrolmentIdentifier("PSPID", psaId)
        ),
        "Activated",
        None
      )
    )
  )

  private val unknownEnrolment = Enrolments(
    Set(
      Enrolment(
        "unknownEnrolmentId",
        Seq(
          EnrolmentIdentifier("unknownId", psaId)
        ),
        "Activated",
        None
      )
    )
  )

  private implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(req)

  private val body: PsrAuthContext[Any] => Future[Result] = _ => Future.successful(Ok)

  val auth: PsrAuth = new PsrAuth {
    override val authConnector: AuthConnector = mockAuthConnector
    override protected val schemeDetailsConnector: SchemeDetailsConnector = mockSchemeDetailsConnector
  }

  "authorisedAsPsrUser" should {

    "fail when srn is not in valid format" in {
      val result = auth.authorisedAsPsrUser("INVALID_SRN")(body)
      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) mustEqual "Invalid scheme reference number"
      verify(mockAuthConnector, never).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, never).checkAssociation(any(), any(), any())(any(), any())
    }

    "fail when it's not possible to authorise as there is empty enrolments and None as externalId" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(None, Enrolments(Set.empty))))

      intercept[UnauthorizedException](Await.result(auth.authorisedAsPsrUser(srn)(body), Duration.Inf))
      verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, never).checkAssociation(any(), any(), any())(any(), any())
    }

    "fail when it's not possible to authorise as there is no psp or psa enrolment" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(Some(externalId), unknownEnrolment)))

      intercept[BadRequestException](Await.result(auth.authorisedAsPsrUser(srn)(body), Duration.Inf))
      verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, never).checkAssociation(any(), any(), any())(any(), any())
    }
  }

  "authorisedAsPsrUser without a `requestRole` header present" should {
    "fail when it's not possible to authorise as the scheme is not associated with the user" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(Some(externalId), psaEnrolment)))
      when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(false))

      intercept[UnauthorizedException](Await.result(auth.authorisedAsPsrUser(srn)(body), Duration.Inf))
      verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
    }

    "authorise a PSA" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(Some(externalId), psaEnrolment)))
      when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result = auth.authorisedAsPsrUser(srn)(body)
      status(result) mustBe Status.OK
      verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
    }

    "authorise a PSP" in {
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(Some(externalId), pspEnrolment)))
      when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(true))

      val result = auth.authorisedAsPsrUser(srn)(body)
      status(result) mustBe Status.OK
      verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
    }
  }

  "authorisedAsPsrUser with a `requestRole` header present" should {
    "throw BadRequestException when request role is not allowed value" in {
      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(requestRoleHeader -> "xxx")
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(Some(externalId), psaEnrolment)))

      intercept[BadRequestException](
        Await.result(auth.authorisedAsPsrUser(srn)(body)(implicitly, implicitly, req), Duration.Inf)
      )
      verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, never).checkAssociation(any(), any(), any())(any(), any())
    }

    "throw UnauthorizedException when it's not possible to authorise PSA as the scheme is not associated with the user" in {
      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(requestRoleHeader -> "PSA")
      when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(Some(externalId), psaEnrolment)))
      when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(false))

      intercept[UnauthorizedException](
        Await.result(auth.authorisedAsPsrUser(srn)(body)(implicitly, implicitly, req), Duration.Inf)
      )
      verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
      verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
    }

    "authorisedAsPsrUser with a `requestRole` header value 'PSA' present" should {
      "throw UnauthorizedException when it's not possible to authorise PSA as the scheme is only associated with the user as a PSP" in {
        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(requestRoleHeader -> "PSA")
        when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(new ~(Some(externalId), psaEnrolment)))
        when(
          mockSchemeDetailsConnector
            .checkAssociation(any(), ArgumentMatchers.eq(Constants.pspIdKey), ArgumentMatchers.eq(Srn(srn).value))(
              any(),
              any()
            )
        ).thenReturn(Future.successful(true))
        when(
          mockSchemeDetailsConnector
            .checkAssociation(any(), ArgumentMatchers.eq(Constants.psaIdKey), ArgumentMatchers.eq(Srn(srn).value))(
              any(),
              any()
            )
        ).thenReturn(Future.successful(false))

        intercept[UnauthorizedException](
          Await.result(auth.authorisedAsPsrUser(srn)(body)(implicitly, implicitly, req), Duration.Inf)
        )
        verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
        verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
      }

      "return ok when PSA is associated" in {
        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(requestRoleHeader -> "psA")
        when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(new ~(Some(externalId), psaEnrolment)))
        when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(true))

        val result = auth.authorisedAsPsrUser(srn)(body)(implicitly, implicitly, req)
        status(result) mustBe Status.OK
        verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
        verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
      }
    }

    "authorisedAsPsrUser with a `requestRole` header value 'PSP' present" should {
      "throw UnauthorizedException when it's not possible to authorise PSP as the scheme is not associated with the user" in {
        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(requestRoleHeader -> "PSP")
        when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(new ~(Some(externalId), pspEnrolment)))
        when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(false))

        intercept[UnauthorizedException](
          Await.result(auth.authorisedAsPsrUser(srn)(body)(implicitly, implicitly, req), Duration.Inf)
        )
        verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
        verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
      }

      "throw UnauthorizedException when it's not possible to authorise PSP as the scheme is only associated with the user as a PSA" in {
        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(requestRoleHeader -> "PSP")
        when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(new ~(Some(externalId), pspEnrolment)))
        when(
          mockSchemeDetailsConnector
            .checkAssociation(any(), ArgumentMatchers.eq(Constants.psaIdKey), ArgumentMatchers.eq(Srn(srn).value))(
              any(),
              any()
            )
        ).thenReturn(Future.successful(true))
        when(
          mockSchemeDetailsConnector
            .checkAssociation(any(), ArgumentMatchers.eq(Constants.pspIdKey), ArgumentMatchers.eq(Srn(srn).value))(
              any(),
              any()
            )
        ).thenReturn(Future.successful(false))

        intercept[UnauthorizedException](
          Await.result(auth.authorisedAsPsrUser(srn)(body)(implicitly, implicitly, req), Duration.Inf)
        )
        verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
        verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
      }

      "return ok when PSP is associated" in {
        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(requestRoleHeader -> "psP")
        when(mockAuthConnector.authorise[Option[String] ~ Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(new ~(Some(externalId), pspEnrolment)))
        when(mockSchemeDetailsConnector.checkAssociation(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(true))

        val result = auth.authorisedAsPsrUser(srn)(body)(implicitly, implicitly, req)
        status(result) mustBe Status.OK
        verify(mockAuthConnector, times(1)).authorise[Option[String] ~ Enrolments](any(), any())(any(), any())
        verify(mockSchemeDetailsConnector, times(1)).checkAssociation(any(), any(), any())(any(), any())
      }
    }
  }
}
