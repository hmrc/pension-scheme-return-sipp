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

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.connectors.{EmailConnector, MinimalDetailsConnector}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse
import uk.gov.hmrc.pensionschemereturnsipp.models.{MinimalDetails, PensionSchemeId}
import uk.gov.hmrc.pensionschemereturnsipp.services.EmailSubmissionService.{
  SubmissionDateFormatter,
  SubmissionDateTimeFormatter
}

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailSubmissionService @Inject()(
  minimalDetailsConnector: MinimalDetailsConnector,
  emailConnector: EmailConnector,
  clock: Clock
)(implicit executionContext: ExecutionContext) {
  def submitEmail(
    sippPsrSubmissionEtmpResponse: SippPsrSubmissionEtmpResponse,
    pensionSchemeId: PensionSchemeId
  )(
    implicit headerCarrier: HeaderCarrier
  ): Future[Either[String, Unit]] = {
    val reportDetails = sippPsrSubmissionEtmpResponse.reportDetails

    (for {
      minimumDetails <- getMinimumDetails(pensionSchemeId)
      email <- sendEmail(
        pensionSchemeId,
        reportDetails.pstr,
        minimumDetails.individualDetails.map(_.fullName),
        minimumDetails.email,
        reportDetails.schemeName,
        reportDetails.periodStart,
        reportDetails.periodEnd,
        reportDetails.psrVersion.getOrElse("")
      )
    } yield email).value
  }

  private def formatReturnDates(from: LocalDate, to: LocalDate): String =
    s"${from.format(SubmissionDateFormatter)} to ${to.format(SubmissionDateFormatter)}"

  private def getMinimumDetails(
    pensionSchemeId: PensionSchemeId
  )(implicit headerCarrier: HeaderCarrier): EitherT[Future, String, MinimalDetails] =
    EitherT(pensionSchemeId match {
      case id: PensionSchemeId.PspId => minimalDetailsConnector.fetch(id)
      case id: PensionSchemeId.PsaId => minimalDetailsConnector.fetch(id)
    }).leftMap(failure => s"Failed to fetch minimum details: $failure")

  private def sendEmail(
    pensionSchemeId: PensionSchemeId,
    pstr: String,
    psaName: Option[String],
    email: String,
    schemeName: Option[String],
    periodStart: LocalDate,
    periodEnd: LocalDate,
    psrVersion: String
  )(implicit hc: HeaderCarrier): EitherT[Future, String, Unit] = {
    val requestId = hc.requestId.map(_.value).getOrElse("")
    val submittedDate = ZonedDateTime.now(clock).format(SubmissionDateTimeFormatter)

    val templateParams = Map(
      "psaName" -> psaName.getOrElse(""),
      "schemeName" -> schemeName.getOrElse(""),
      "periodOfReturn" -> formatReturnDates(periodStart, periodEnd),
      "dateSubmitted" -> submittedDate
    )

    EitherT(
      emailConnector.sendEmail(
        pensionSchemeId,
        requestId,
        pstr,
        email,
        "pods_pension_scheme_return_sipp_submitted",
        schemeName.getOrElse(""),
        psaName.getOrElse(""),
        templateParams,
        s"${periodStart.getYear}-${periodEnd.getYear}",
        psrVersion
      )
    )
  }
}

object EmailSubmissionService {
  val SubmissionDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  val SubmissionDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm:ss")
}
