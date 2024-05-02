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

package uk.gov.hmrc.pensionschemereturnsipp.models

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec

class SchemeDetailsSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "SchemeDetails" should {

    "successfully read from json" in {

      forAll(schemeDetailsGen) { details =>
        Json.toJson(details).as[SchemeDetails] mustBe details
      }
    }
  }

  "SchemeStatus" should {

    "successfully read from json" in {
      forAll(schemeStatusGen) { status =>
        Json.toJson(status).as[SchemeStatus] mustBe status
      }
    }

    "return a JsError" when {
      "Scheme status is unknown" in {
        forAll(nonEmptyString) { status =>
          JsString(status).asOpt[SchemeStatus] mustBe None
        }
      }
    }
  }
}
