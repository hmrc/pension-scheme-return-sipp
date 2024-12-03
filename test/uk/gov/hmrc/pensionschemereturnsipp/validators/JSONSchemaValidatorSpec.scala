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

package uk.gov.hmrc.pensionschemereturnsipp.validators

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.pensionschemereturnsipp.validators.SchemaPaths.API_1997

class JSONSchemaValidatorSpec extends AnyWordSpec with Matchers with JsonFileReader {

  private val mockAuthConnector = mock[AuthConnector]

  val modules: Seq[GuiceableModule] =
    Seq(
      bind[AuthConnector].toInstance(mockAuthConnector)
    )
  val app: Application = new GuiceApplicationBuilder()
    .overrides(modules*)
    .build()

  private lazy val jsonPayloadSchemaValidator: JSONSchemaValidator = app.injector.instanceOf[JSONSchemaValidator]

  "validate json payload against API 1997 schema" must {
    "Do not fail when input is valid" in {
      val json = readJsonFromFile("/api-1997-valid.json")
      val result = jsonPayloadSchemaValidator.validatePayload(API_1997, json)

      val actualErrors = result.errors.map(_.getMessage)

      actualErrors mustBe Set()
    }

    "fail with validation errors when input Json is malformed" in {
      val json = readJsonFromFile("/api-1997-invalid.json")
      val result = jsonPayloadSchemaValidator.validatePayload(API_1997, json)
      val actualErrors = result.errors.map(_.getMessage)

      val expectedErrors = Set(
        "$.memberAndTransactions[0].memberDetails.personalDetails.dateOfBirth: does not match the date pattern must be a valid RFC 3339 full-date",
        "$.memberAndTransactions[0].landConnectedParty.transactionDetails[0]: required property 'addressDetails' not found"
      )

      actualErrors mustBe expectedErrors
    }
  }
}
