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

import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoansApi
import uk.gov.hmrc.pensionschemereturnsipp.utils.BaseSpec
import Arbitraries.outstandingLoanArbitrary

class OutstandingLoansTransformationsSpec extends BaseSpec with ScalaCheckDrivenPropertyChecks {
  "transforming" should {
    "be isomorphic" in {
      forAll { (transactionDetail: OutstandingLoansApi.TransactionDetail) =>
        val nameDOB = transactionDetail.nameDOB
        val nino = transactionDetail.nino

        transactionDetail.toEtmp
          .toApi(nameDOB, nino) mustBe transactionDetail
      }
    }
  }
}
