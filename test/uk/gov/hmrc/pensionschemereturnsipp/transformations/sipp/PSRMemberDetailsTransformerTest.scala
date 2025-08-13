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

class PSRMemberDetailsTransformerTest extends BaseSpec with SippEtmpTestValues {

  private val transformer: PSRMemberDetailsTransformer = new PSRMemberDetailsTransformer()

  "transform" should {

    "transform response SippPsrSubmissionEtmpResponse to MemberDetailsResponse" in {
      val resultApiResponse = transformer.transform(sampleSippPsrSubmissionEtmpResponse)

      val personalDetails =
        sampleSippPsrSubmissionEtmpResponse.memberAndTransactions.get.head.memberDetails.personalDetails

      resultApiResponse.isDefined mustBe true
      resultApiResponse.get.members.size mustBe 1
      resultApiResponse.get.members.head.firstName mustBe personalDetails.firstName
      resultApiResponse.get.members.head.lastName mustBe personalDetails.lastName
      resultApiResponse.get.members.head.nino mustBe personalDetails.nino
      resultApiResponse.get.members.head.dateOfBirth mustBe personalDetails.dateOfBirth
      resultApiResponse.get.members.head.reasonNoNINO mustBe personalDetails.reasonNoNINO
    }
  }
}
