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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.AssetsFromConnectedPartyApi
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.SharesCompanyDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  MemberDetails,
  SippOtherAssetsConnectedParty
}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

import java.time.LocalDate

class AssetsFromConnectedPartyTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  private val transformer: AssetsFromConnectedPartyTransformer = new AssetsFromConnectedPartyTransformer()

  val assetsFromConnectedPartyTx = AssetsFromConnectedPartyApi.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    acquisitionDate = LocalDate.of(2020, 1, 1),
    assetDescription = "Asset Description",
    acquisitionOfShares = Yes,
    sharesCompanyDetails = Some(
      SharesCompanyDetails(
        companySharesName = "companySharesName",
        companySharesCRN = Some("12345678"),
        reasonNoCRN = None,
        sharesClass = "sharesClass",
        noOfShares = 1
      )
    ),
    acquiredFromName = "acquiredFromName",
    totalCost = 20.0,
    independentValuation = Yes,
    tangibleSchedule29A = Yes,
    totalIncomeOrReceipts = 20.0,
    isPropertyDisposed = No,
    disposalDetails = None,
    disposalOfShares = Some(No),
    noOfSharesHeld = Some(1),
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
    otherAssetsConnectedParty = Some(
      SippOtherAssetsConnectedParty(
        1,
        None,
        Some(
          List(
            SippOtherAssetsConnectedParty.TransactionDetails(
              acquisitionDate = LocalDate.of(2020, 1, 1),
              assetDescription = "Asset Description",
              acquisitionOfShares = Yes,
              sharesCompanyDetails = None,
              acquiredFromName = "acquiredFromName",
              totalCost = 20.0,
              independentValuation = Yes,
              tangibleSchedule29A = Yes,
              totalIncomeOrReceipts = 20.0, // Updated
              isPropertyDisposed = No,
              disposalDetails = None,
              disposalOfShares = Some(Yes),
              noOfSharesHeld = None
            )
          )
        )
      )
    ),
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

  "merge" should {
    "put new member data if there is no previous data" in {
      val testEtmpData = etmpData.copy(otherAssetsConnectedParty = None)

      val result = transformer.merge(NonEmptyList.of(assetsFromConnectedPartyTx), List(testEtmpData))

      result mustBe List(
        etmpData.copy(
          otherAssetsConnectedParty = Some(
            SippOtherAssetsConnectedParty(
              1,
              None,
              Some(
                List(
                  SippOtherAssetsConnectedParty.TransactionDetails(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquisitionOfShares = Yes,
                    sharesCompanyDetails = Some(
                      SharesCompanyDetails(
                        companySharesName = "companySharesName",
                        companySharesCRN = Some("12345678"),
                        reasonNoCRN = None,
                        sharesClass = "sharesClass",
                        noOfShares = 1
                      )
                    ),
                    acquiredFromName = "acquiredFromName",
                    totalCost = 20.0,
                    independentValuation = Yes,
                    tangibleSchedule29A = Yes,
                    totalIncomeOrReceipts = 20.0,
                    isPropertyDisposed = No,
                    disposalDetails = None,
                    disposalOfShares = Some(No),
                    noOfSharesHeld = Some(1)
                  )
                )
              )
            )
          )
        )
      )

    }

    "replace assets from connected party data for a single member when member match is found" in {
      val updateValues = assetsFromConnectedPartyTx.copy(
        acquiredFromName = "test2",
        noOfSharesHeld = Some(2),
        sharesCompanyDetails = assetsFromConnectedPartyTx.sharesCompanyDetails.map(
          _.copy(
            companySharesName = "testCompanySharesName2"
          )
        )
      )
      val result = transformer.merge(NonEmptyList.of(updateValues), List(etmpData))

      result mustBe List(
        etmpData.copy(
          otherAssetsConnectedParty = Some(
            SippOtherAssetsConnectedParty(
              1,
              None,
              Some(
                List(
                  SippOtherAssetsConnectedParty.TransactionDetails(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquisitionOfShares = Yes,
                    sharesCompanyDetails = Some(
                      SharesCompanyDetails(
                        companySharesName = "testCompanySharesName2",
                        companySharesCRN = Some("12345678"),
                        reasonNoCRN = None,
                        sharesClass = "sharesClass",
                        noOfShares = 1
                      )
                    ),
                    acquiredFromName = "test2",
                    totalCost = 20.0,
                    independentValuation = Yes,
                    tangibleSchedule29A = Yes,
                    totalIncomeOrReceipts = 20.0,
                    isPropertyDisposed = No,
                    disposalDetails = None,
                    disposalOfShares = Some(No),
                    noOfSharesHeld = Some(2)
                  )
                )
              )
            )
          )
        )
      )

    }

    "add assets from connected party data data with new member details for a single member when match is not found" in {
      val testData = assetsFromConnectedPartyTx.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testData), List(etmpData))

      result mustBe List(
        etmpData.copy(otherAssetsConnectedParty = None, status = Deleted), // No more tx for first member :/
        etmpData.copy(
          memberDetails = MemberDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Some("otherNino"),
            reasonNoNINO = None,
            dateOfBirth = LocalDate.of(2020, 1, 1)
          ),
          otherAssetsConnectedParty = Some(
            SippOtherAssetsConnectedParty(
              1,
              None,
              Some(
                List(
                  SippOtherAssetsConnectedParty.TransactionDetails(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquisitionOfShares = Yes,
                    sharesCompanyDetails = Some(
                      SharesCompanyDetails(
                        companySharesName = "companySharesName",
                        companySharesCRN = Some("12345678"),
                        reasonNoCRN = None,
                        sharesClass = "sharesClass",
                        noOfShares = 1
                      )
                    ),
                    acquiredFromName = "acquiredFromName",
                    totalCost = 20.0,
                    independentValuation = Yes,
                    tangibleSchedule29A = Yes,
                    totalIncomeOrReceipts = 20.0,
                    isPropertyDisposed = No,
                    disposalDetails = None,
                    disposalOfShares = Some(No),
                    noOfSharesHeld = Some(1)
                  )
                )
              )
            )
          )
        )
      )

    }

  }

}
