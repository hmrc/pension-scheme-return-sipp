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

import cats.data.NonEmptyList
import uk.gov.hmrc.pensionschemereturnsipp.models.api.UnquotedShareApi
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.SharesCompanyDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.SippUnquotedShares.TransactionDetail
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippUnquotedShares}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

class UnquotedSharesTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  import java.time.LocalDate

  private val transformer: UnquotedSharesTransformer = new UnquotedSharesTransformer()

  val unquotedDataRow1 = UnquotedShareApi.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    sharesCompanyDetails = SharesCompanyDetails(
      companySharesName = "companySharesName",
      companySharesCRN = Some("companySharesCRN"),
      reasonNoCRN = None,
      sharesClass = "sharesClass",
      noOfShares = 1
    ),
    acquiredFromName = "acquiredFromName",
    totalCost = 10,
    independentValuation = Yes,
    totalDividendsIncome = 20,
    sharesDisposed = No,
    sharesDisposalDetails = None,
    transactionCount = None
  )

  val etmpData = EtmpMemberAndTransactions(
    status = SectionStatus.New,
    version = None,
    memberDetails = MemberDetails(
      firstName = "firstName",
      lastName = "lastName",
      nino = Some("nino"),
      reasonNoNINO = None,
      dateOfBirth = LocalDate.of(2020, 1, 1)
    ),
    landConnectedParty = None,
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = Some(
      SippUnquotedShares(
        noOfTransactions = 1,
        version = None,
        transactionDetails = Some(
          List(
            TransactionDetail(
              sharesCompanyDetails = SharesCompanyDetails(
                companySharesName = "companySharesName",
                companySharesCRN = Some("companySharesCRN"),
                reasonNoCRN = None,
                sharesClass = "sharesClass",
                noOfShares = 1
              ),
              acquiredFromName = "acquiredFromName",
              totalCost = 10,
              independentValuation = Yes,
              totalDividendsIncome = 20,
              sharesDisposed = No,
              sharesDisposalDetails = None
            )
          )
        )
      )
    )
  )

  "merge" should {
    "add unquoted data for a single member when member match is found" in {
      val testEtmpData = etmpData.copy(unquotedShares = None)

      val result = transformer.merge(NonEmptyList.of(unquotedDataRow1), List(testEtmpData))

      result mustBe List(
        etmpData.copy(
          status = SectionStatus.Changed,
          unquotedShares = Some(
            SippUnquotedShares(
              noOfTransactions = 1,
              version = None,
              transactionDetails = Some(
                List(
                  TransactionDetail(
                    sharesCompanyDetails = SharesCompanyDetails(
                      companySharesName = "companySharesName",
                      companySharesCRN = Some("companySharesCRN"),
                      reasonNoCRN = None,
                      sharesClass = "sharesClass",
                      noOfShares = 1
                    ),
                    acquiredFromName = "acquiredFromName",
                    totalCost = 10,
                    independentValuation = Yes,
                    totalDividendsIncome = 20,
                    sharesDisposed = No,
                    sharesDisposalDetails = None
                  )
                )
              )
            )
          )
        )
      )

    }

    "update unquoted data for a single member when member match is found" in {

      val testLandArmsDataRow1 = unquotedDataRow1.copy(acquiredFromName = "test2222")
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(
          status = SectionStatus.Changed,
          unquotedShares = Some(
            SippUnquotedShares(
              noOfTransactions = 1,
              version = None,
              transactionDetails = Some(
                List(
                  TransactionDetail(
                    sharesCompanyDetails = SharesCompanyDetails(
                      companySharesName = "companySharesName",
                      companySharesCRN = Some("companySharesCRN"),
                      reasonNoCRN = None,
                      sharesClass = "sharesClass",
                      noOfShares = 1
                    ),
                    acquiredFromName = "test2222",
                    totalCost = 10,
                    independentValuation = Yes,
                    totalDividendsIncome = 20,
                    sharesDisposed = No,
                    sharesDisposalDetails = None
                  )
                )
              )
            )
          )
        )
      )

    }

    "add unquoted data with new member details for a single member when match is not found" in {
      val testUnquotedRow1 = unquotedDataRow1.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testUnquotedRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(unquotedShares = None, status = Deleted),
        etmpData.copy(
          memberDetails = MemberDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Some("otherNino"),
            reasonNoNINO = None,
            dateOfBirth = LocalDate.of(2020, 1, 1)
          )
        )
      )

    }
  }

  "transformToResponse" should {
    "return correct response" in {
      val result = transformer.transformToResponse(
        List(etmpSippMemberAndTransactions)
      )

      result.transactions.length mustBe 1
      result.transactions.head.transactionCount mustBe Some(1)
      result.transactions.head.nino.nino mustBe sippMemberDetails.nino
      result.transactions.head.nameDOB.firstName mustBe sippMemberDetails.firstName
      result.transactions.head.nameDOB.lastName mustBe sippMemberDetails.lastName

    }

    "return no transaction if related not exist" in {
      val result = transformer.transformToResponse(
        List(etmpSippMemberAndTransactions.copy(unquotedShares = None))
      )

      result.transactions.length mustBe 0
    }
  }

}
