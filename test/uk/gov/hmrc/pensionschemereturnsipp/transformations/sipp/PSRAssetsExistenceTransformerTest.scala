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

package uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp

import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpTestValues};

class PSRAssetsExistenceTransformerTest extends BaseSpec with SippEtmpTestValues {

  private val transformer: PSRAssetsExistenceTransformer = new PSRAssetsExistenceTransformer()

  "transform" should {

    "transform ETMP response to Assets Existence Response for all exist" in {
      val resultApiResponse = transformer.transform(sampleSippPsrSubmissionEtmpResponse)

      resultApiResponse.isDefined mustBe true
      resultApiResponse.get.interestInLandOrPropertyCount mustBe 1
      resultApiResponse.get.assetsFromConnectedPartyCount mustBe 1
      resultApiResponse.get.landArmsLengthCount mustBe 1
      resultApiResponse.get.tangibleMoveablePropertyCount mustBe 1
      resultApiResponse.get.outstandingLoansCount mustBe 1
      resultApiResponse.get.unquotedSharesCount mustBe 1
    }

    "transform ETMP response to Assets Existence Response for none of them exist" in {
      val resultApiResponse = transformer.transform(
        sampleSippPsrSubmissionEtmpResponse.copy(
          memberAndTransactions = Some(
            List(
              memberAndTransactions.copy(
                landConnectedParty = None,
                otherAssetsConnectedParty = None,
                landArmsLength = None,
                tangibleProperty = None,
                loanOutstanding = None,
                unquotedShares = None
              )
            )
          )
        )
      )

      resultApiResponse.isDefined mustBe true
      resultApiResponse.get.interestInLandOrPropertyCount mustBe 0
      resultApiResponse.get.assetsFromConnectedPartyCount mustBe 0
      resultApiResponse.get.landArmsLengthCount mustBe 0
      resultApiResponse.get.tangibleMoveablePropertyCount mustBe 0
      resultApiResponse.get.outstandingLoansCount mustBe 0
      resultApiResponse.get.unquotedSharesCount mustBe 0
    }
  }
}
