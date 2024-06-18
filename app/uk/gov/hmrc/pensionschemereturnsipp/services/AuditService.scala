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

import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.pensionschemereturnsipp.config.AppConfig
import uk.gov.hmrc.pensionschemereturnsipp.models.audit.AuditEvent
import uk.gov.hmrc.pensionschemereturnsipp.models.audit.AuditEvent.AuditEventWithResult
import uk.gov.hmrc.play.audit.AuditExtensions.{auditHeaderCarrier, AuditHeaderCarrier}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

@Singleton
class AuditService @Inject()(config: AppConfig, connector: AuditConnector)(implicit ec: ExecutionContext) {
  private def sendEvent(event: AuditEvent)(implicit rh: RequestHeader): Future[Unit] =
    connector
      .sendEvent(
        DataEvent(
          auditSource = config.appName,
          auditType = event.auditType.entryName,
          tags = rh.toAuditTags(),
          detail = event.details
        )
      )
      .void

  implicit class AuditOps(call: Future[HttpResponse]) {
    def auditLog(event: AuditEvent)(implicit rh: RequestHeader): Future[HttpResponse] =
      call
        .flatMap { response =>
          val eventWithResult = AuditEventWithResult(event, response.status.some, response.body.some.filter(_.nonEmpty))
          sendEvent(eventWithResult) // async call
          Future.successful(response)
        }
        .recoverWith { t: Throwable =>
          sendEvent(AuditEventWithResult(event, None, None, t.some))
          Future.failed(t)
        }
  }

  private implicit def toHc(request: RequestHeader): AuditHeaderCarrier =
    auditHeaderCarrier(HeaderCarrierConverter.fromRequestAndSession(request, request.session))

}
