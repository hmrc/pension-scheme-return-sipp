/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers.must
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, TestValues}

class PsrBaseControllerSpec extends BaseSpec with TestValues {

  "PsrBaseController PSR" must {

    "return 400 BadRequest when required header srn is missing" in {
      val request = FakeRequest("GET", "/some-url")
      implicit val typedRequest: Request[AnyContent] = request

      object TestController extends PsrBaseController {
        def testRequiredHeaders(headerName: String)(implicit request: Request[AnyContent]) =
          requiredHeaders(headerName)
      }

      val thrown = intercept[BadRequestException] {
        TestController.testRequiredHeaders("srn") // implicit request is passed
      }

      thrown.getMessage must include("srn missing")
    }
  }
}
