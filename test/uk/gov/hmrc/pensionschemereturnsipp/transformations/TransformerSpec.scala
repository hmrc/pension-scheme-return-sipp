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

package uk.gov.hmrc.pensionschemereturnsipp.transformations

import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class TransformerSpec extends PlaySpec with MockitoSugar with Transformer {
  "Transformer" should {
    "successfully transform boolean to Yes or No" in {
      toYesNo(true) mustEqual "Yes"
      toYesNo(false) mustEqual "No"
    }

    "successfully transform optional boolean to Yes or No" in {
      optToYesNo(Some("")) mustEqual "Yes"
      optToYesNo(None) mustEqual "No"
    }

    "successfully transform Yes or No to boolean" in {
      fromYesNo(Yes) mustBe true
      fromYesNo(No) mustBe false
    }

    "successfully transform boolean to EtmpConnectedPartyStatus" in {
      transformToEtmpConnectedPartyStatus(true) mustBe "01"
      transformToEtmpConnectedPartyStatus(false) mustBe "02"
    }
  }
}
