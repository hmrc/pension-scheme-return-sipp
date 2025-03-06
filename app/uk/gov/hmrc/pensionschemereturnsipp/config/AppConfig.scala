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

package uk.gov.hmrc.pensionschemereturnsipp.config
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig, runModeConfiguration: Configuration) {

  val appName: String = config.get[String]("appName")

  val pensionsAdministrator: Service = config.get[Service]("microservice.services.pensionAdministrator")

  private val ifURL: String = servicesConfig.baseUrl(serviceName = "if-hod")
  private val pensionsSchemeURL: String = servicesConfig.baseUrl(serviceName = "pensionsScheme")

  lazy val integrationFrameworkEnvironment: String =
    runModeConfiguration.getOptional[String](path = "microservice.services.if-hod.env").getOrElse("local")
  lazy val integrationFrameworkAuthorization: String = "Bearer " + runModeConfiguration
    .getOptional[String](path = "microservice.services.if-hod.authorizationToken")
    .getOrElse("local")

  val submitSippPsrUrl: String = s"$ifURL${config.get[String]("serviceUrls.submit-sipp-psr")}"
  val getSippPsrUrl: String = s"$ifURL${config.get[String]("serviceUrls.get-sipp-psr")}"
  val getPsrVersionsUrl: String = s"$ifURL${config.get[String]("serviceUrls.get-psr-versions")}"
  val isPsaAssociatedUrl: String = s"$pensionsSchemeURL${config.get[String](path = "serviceUrls.is-psa-associated")}"

  val pensionsSchemeReturnUrl: String = servicesConfig.baseUrl("pensionSchemeReturn")

  val emailApiUrl: String = servicesConfig.baseUrl("email")
  val emailCallbackUrl: String = config.get[String](path = "serviceUrls.email-callback")
  val emailSendForce: Boolean = config.getOptional[Boolean]("email.force").getOrElse(false)

  val maxRequestSize: Int = config.get[Int]("etmpConfig.maxRequestSize")

  def emailCallback(
    pensionSchemeId: PensionSchemeId,
    requestId: String,
    encryptedEmail: String,
    encryptedPsaId: String,
    encryptedPstr: String,
    encryptedSchemeName: String,
    encryptedUserName: String,
    taxYear: String,
    reportVersion: String
  ) =
    s"$pensionsSchemeReturnUrl${emailCallbackUrl
        .format(
          pensionSchemeId match {
            case PensionSchemeId.PspId(_) => "PSP"
            case PensionSchemeId.PsaId(_) => "PSA"
          },
          requestId,
          encryptedEmail,
          encryptedPsaId,
          encryptedPstr,
          encryptedSchemeName,
          encryptedUserName,
          taxYear,
          reportVersion
        )}"
}
