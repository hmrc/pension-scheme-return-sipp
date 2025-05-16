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

import com.google.inject.{Inject, Singleton}
import io.scalaland.chimney.dsl._
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{MemberDetails => ApiMemberDetails, MemberDetailsResponse}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.response.SippPsrSubmissionEtmpResponse

@Singleton()
class PSRMemberDetailsTransformer @Inject() () {

  def transform(etmpResponse: SippPsrSubmissionEtmpResponse): Option[MemberDetailsResponse] =
    etmpResponse.memberAndTransactions
      .orElse(Some(List.empty))
      .map(mTxs => MemberDetailsResponse(mTxs.map(_.memberDetails.personalDetails.transformInto[ApiMemberDetails])))

}
