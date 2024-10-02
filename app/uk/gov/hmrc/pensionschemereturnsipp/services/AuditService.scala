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

import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.pensionschemereturnsipp.audit.AuditEvent
import uk.gov.hmrc.pensionschemereturnsipp.config.AppConfig
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {

  def sendEvent[T <: AuditEvent](event: T)(implicit rh: RequestHeader, ec: ExecutionContext): Unit

  def sendEventWithSource[T <: AuditEvent](event: T, auditSource: String)(implicit
    rh: RequestHeader,
    ec: ExecutionContext
  ): Unit

}

class AuditServiceImpl @Inject() (config: AppConfig, connector: AuditConnector) extends AuditService with Logging {

  private implicit def toHc(request: RequestHeader): AuditHeaderCarrier =
    auditHeaderCarrier(HeaderCarrierConverter.fromRequest(request))

  def sendEvent[T <: AuditEvent](event: T)(implicit rh: RequestHeader, ec: ExecutionContext): Unit =
    sendEventWithSource(event, config.appName)

  override def sendEventWithSource[T <: AuditEvent](
    event: T,
    auditSource: String
  )(implicit rh: RequestHeader, ec: ExecutionContext): Unit = {

    logger.info(s"[AuditService][sendEvent] sending ${event.auditType}")

    val result: Future[AuditResult] = connector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = auditSource,
        auditType = event.auditType,
        tags = rh.toAuditTags(
          transactionName = event.auditType,
          path = rh.path
        ),
        detail = event.details
      )
    )

    result.onComplete {
      case Success(_) =>
        logger.debug(s"[AuditService][sendEvent] successfully sent ${event.auditType}")
      case Failure(e) =>
        logger.error(s"[AuditService][sendEvent] failed to send event ${event.auditType}", e)
    }
  }

}
