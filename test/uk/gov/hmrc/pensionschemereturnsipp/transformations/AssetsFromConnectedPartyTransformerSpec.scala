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
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{EtmpSippSharesCompanyDetail, SectionStatus}
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
    acquisitionOfShares = YesNo.Yes,
    shareCompanyDetails = Some(
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
    independentValuation = YesNo.Yes,
    tangibleSchedule29A = YesNo.Yes,
    totalIncomeOrReceipts = 20.0,
    isPropertyDisposed = YesNo.No,
    disposalDetails = None,
    disposalOfShares = YesNo.No,
    noOfSharesHeld = Some(1)
  )

  val etmpData = EtmpMemberAndTransactions(
    status = SectionStatus.New,
    version = None,
    memberDetails = MemberDetails(
      firstName = "firstName",
      middleName = None,
      lastName = "lastName",
      nino = Some("nino"),
      reasonNoNINO = None,
      dateOfBirth = LocalDate.of(2020, 1, 1)
    ),
    landConnectedParty = None,
    otherAssetsConnectedParty = Some(
      SippOtherAssetsConnectedParty(
        1,
        Some(
          List(
            SippOtherAssetsConnectedParty.TransactionDetail(
              acquisitionDate = LocalDate.of(2020, 1, 1),
              assetDescription = "Asset Description",
              acquisitionOfShares = YesNo.Yes,
              sharesCompanyDetails = None,
              acquiredFromName = "acquiredFromName",
              totalCost = 20.0,
              independentValution = YesNo.Yes,
              tangibleSchedule29A = YesNo.Yes,
              totalIncomeOrReceipts = 20.0, // Updated
              isPropertyDisposed = YesNo.No,
              disposedPropertyProceedsAmt = None,
              purchaserNamesIfDisposed = None,
              anyOfPurchaserConnected = None,
              independentValutionDisposal = None,
              disposalOfShares = YesNo.Yes,
              noOfSharesHeld = None,
              propertyFullyDisposed = None
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
              Some(
                List(
                  SippOtherAssetsConnectedParty.TransactionDetail(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquisitionOfShares = YesNo.Yes,
                    sharesCompanyDetails = Some(
                      EtmpSippSharesCompanyDetail(
                        companySharesName = "companySharesName",
                        companySharesCRN = Some("12345678"),
                        reasonNoCRN = None,
                        sharesClass = "sharesClass",
                        noOfShares = 1
                      )
                    ),
                    acquiredFromName = "acquiredFromName",
                    totalCost = 20.0,
                    independentValution = YesNo.Yes,
                    tangibleSchedule29A = YesNo.Yes,
                    totalIncomeOrReceipts = 20.0,
                    isPropertyDisposed = YesNo.No,
                    disposedPropertyProceedsAmt = None,
                    purchaserNamesIfDisposed = None,
                    anyOfPurchaserConnected = None,
                    independentValutionDisposal = None,
                    disposalOfShares = YesNo.No,
                    noOfSharesHeld = Some(1),
                    propertyFullyDisposed = None
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
        shareCompanyDetails = assetsFromConnectedPartyTx.shareCompanyDetails.map(
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
              Some(
                List(
                  SippOtherAssetsConnectedParty.TransactionDetail(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquisitionOfShares = YesNo.Yes,
                    sharesCompanyDetails = Some(
                      EtmpSippSharesCompanyDetail(
                        companySharesName = "testCompanySharesName2",
                        companySharesCRN = Some("12345678"),
                        reasonNoCRN = None,
                        sharesClass = "sharesClass",
                        noOfShares = 1
                      )
                    ),
                    acquiredFromName = "test2",
                    totalCost = 20.0,
                    independentValution = YesNo.Yes,
                    tangibleSchedule29A = YesNo.Yes,
                    totalIncomeOrReceipts = 20.0,
                    isPropertyDisposed = YesNo.No,
                    disposedPropertyProceedsAmt = None,
                    purchaserNamesIfDisposed = None,
                    anyOfPurchaserConnected = None,
                    independentValutionDisposal = None,
                    disposalOfShares = YesNo.No,
                    noOfSharesHeld = Some(2),
                    propertyFullyDisposed = None
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
        etmpData.copy(otherAssetsConnectedParty = None), // No more tx for first member :/
        etmpData.copy(
          memberDetails = MemberDetails(
            firstName = "firstName",
            middleName = None,
            lastName = "lastName",
            nino = Some("otherNino"),
            reasonNoNINO = None,
            dateOfBirth = LocalDate.of(2020, 1, 1)
          ),
          otherAssetsConnectedParty = Some(
            SippOtherAssetsConnectedParty(
              1,
              Some(
                List(
                  SippOtherAssetsConnectedParty.TransactionDetail(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquisitionOfShares = YesNo.Yes,
                    sharesCompanyDetails = Some(
                      EtmpSippSharesCompanyDetail(
                        companySharesName = "companySharesName",
                        companySharesCRN = Some("12345678"),
                        reasonNoCRN = None,
                        sharesClass = "sharesClass",
                        noOfShares = 1
                      )
                    ),
                    acquiredFromName = "acquiredFromName",
                    totalCost = 20.0,
                    independentValution = YesNo.Yes,
                    tangibleSchedule29A = YesNo.Yes,
                    totalIncomeOrReceipts = 20.0,
                    isPropertyDisposed = YesNo.No,
                    disposedPropertyProceedsAmt = None,
                    purchaserNamesIfDisposed = None,
                    anyOfPurchaserConnected = None,
                    independentValutionDisposal = None,
                    disposalOfShares = YesNo.No,
                    noOfSharesHeld = Some(1),
                    propertyFullyDisposed = None
                  )
                )
              )
            )
          )
        )
      )

    }

  }

  lazy val sipp: AssetsFromConnectedPartyApi.TransactionDetails = {
    import sippOtherAssetsConnectedPartyTransactionDetail._

    AssetsFromConnectedPartyApi.TransactionDetails(
      nameDOB = NameDOB(sippMemberDetails.firstName, sippMemberDetails.lastName, sippMemberDetails.dateOfBirth),
      nino = NinoType(sippMemberDetails.nino, sippMemberDetails.reasonNoNINO),
      acquisitionDate = acquisitionDate,
      assetDescription = assetDescription,
      acquisitionOfShares = acquisitionOfShares,
      shareCompanyDetails = sharesCompanyDetails.map(
        details =>
          SharesCompanyDetails(
            details.companySharesName,
            details.companySharesCRN,
            details.reasonNoCRN,
            details.sharesClass,
            details.noOfShares
          )
      ),
      acquiredFromName = acquiredFromName,
      totalCost = totalCost,
      independentValuation = independentValution,
      tangibleSchedule29A = tangibleSchedule29A,
      totalIncomeOrReceipts = totalIncomeOrReceipts,
      isPropertyDisposed = YesNo(isPropertyDisposed.boolean),
      disposalDetails = Option.when(isPropertyDisposed.boolean) {
        DisposalDetails(
          disposedPropertyProceedsAmt.get,
          purchaserNamesIfDisposed.get,
          YesNo(anyOfPurchaserConnected.get.boolean),
          YesNo(independentValutionDisposal.get.boolean),
          YesNo(propertyFullyDisposed.get.boolean)
        )
      },
      disposalOfShares = disposalOfShares,
      noOfSharesHeld = noOfSharesHeld
    )
  }

}
