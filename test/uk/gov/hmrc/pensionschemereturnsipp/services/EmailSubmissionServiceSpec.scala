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

import cats.syntax.either._
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.Generators.{minimalDetailsGen, psaIdGen}
import uk.gov.hmrc.pensionschemereturnsipp.connectors.MinimalDetailsError.DelimitedAdmin
import uk.gov.hmrc.pensionschemereturnsipp.connectors.{EmailConnector, MinimalDetailsConnector}
import uk.gov.hmrc.pensionschemereturnsipp.models.MinimalDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.PsaId
import uk.gov.hmrc.pensionschemereturnsipp.services.EmailSubmissionService.{
  SubmissionDateFormatter,
  SubmissionDateTimeFormatter
}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpTestValues, TestValues}

import java.time.{Clock, Instant, ZoneOffset, ZonedDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailSubmissionServiceSpec
    extends BaseSpec
    with TestValues
    with SippEtmpTestValues
    with ScalaFutures
    with EitherValues {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "submitEmail" should {
    "compose minimum details with email template parameters and send the request to the email api returning unit on success" in new TestScope {
      when(minimalDetailsConnector.fetch(*[PsaId])(*, *)).thenReturn(Future.successful(minimumDetails.asRight))
      val reportDetails = sampleSippPsrSubmissionEtmpResponse.reportDetails
      val psaName = minimumDetails.individualDetails.map(_.fullName).getOrElse("")
      val schemeName = "SchemeName"

      val templateParams = Map(
        "psaName" -> psaName,
        "schemeName" -> schemeName,
        "periodOfReturn" -> s"${reportDetails.periodStart.format(SubmissionDateFormatter)} to ${reportDetails.periodEnd
          .format(SubmissionDateFormatter)}",
        "dateSubmitted" -> ZonedDateTime.now(clock).format(SubmissionDateTimeFormatter)
      )

      when(
        emailConnector.sendEmail(
          psaId,
          hc.requestId.map(_.value).getOrElse(""),
          reportDetails.pstr,
          minimumDetails.email,
          "pods_pension_scheme_return_sipp_submitted",
          schemeName,
          psaName,
          templateParams,
          s"${reportDetails.periodStart.getYear}-${reportDetails.periodEnd.getYear}",
          reportDetails.version.getOrElse("")
        )
      ).thenReturn(Future.successful(().asRight))

      emailSubmissionService
        .submitEmail(
          Some(schemeName),
          sampleSippPsrSubmissionEtmpResponse,
          psaId
        )
        .futureValue
        .value mustBe ()
    }

    "proxy errors from minimum details connector" in new TestScope {
      when(minimalDetailsConnector.fetch(*[PsaId])(*, *)).thenReturn(Future.successful(DelimitedAdmin.asLeft))

      emailSubmissionService
        .submitEmail(
          None,
          sampleSippPsrSubmissionEtmpResponse,
          psaId
        )
        .futureValue mustBe Left("Failed to fetch minimum details: DelimitedAdmin")
    }
  }

  class TestScope {
    val minimumDetails: MinimalDetails = minimalDetailsGen.sample.value
    val psaId = psaIdGen.sample.value

    val now = Instant.now()
    val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    val minimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
    val emailConnector: EmailConnector = mock[EmailConnector]
    val emailSubmissionService = new EmailSubmissionService(minimalDetailsConnector, emailConnector, clock)
  }
}
