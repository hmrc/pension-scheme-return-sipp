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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.TangibleMoveablePropertyApi
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.CostOrMarketType.MarketValue
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippTangibleProperty}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

import java.time.LocalDate

class TangibleMoveablePropertyTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  private val transformer: TangibleMoveablePropertyTransformer = new TangibleMoveablePropertyTransformer()

  val tanbigleTx = TangibleMoveablePropertyApi.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    acquisitionDate = LocalDate.of(2020, 1, 1),
    assetDescription = "Asset Description",
    acquiredFromName = "acquiredFromName",
    totalCost = 20.0,
    independentValuation = YesNo.Yes,
    totalIncomeOrReceipts = 20.0,
    costOrMarket = MarketValue,
    costMarketValue = 20.0,
    isPropertyDisposed = YesNo.No,
    disposalDetails = None
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
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = Some(
      SippTangibleProperty(
        1,
        Some(
          List(
            SippTangibleProperty.TransactionDetail(
              acquisitionDate = LocalDate.of(2020, 1, 1),
              assetDescription = "Asset Description",
              acquiredFromName = "acquiredFromName",
              totalCost = 20.0,
              independentValution = YesNo.Yes,
              totalIncomeOrReceipts = 20.0, // Updated
              costOrMarket = MarketValue,
              costMarketValue = 20.0,
              isPropertyDisposed = YesNo.No,
              disposedPropertyProceedsAmt = None,
              purchaserNamesIfDisposed = None,
              anyOfPurchaserConnected = None,
              independentValutionDisposal = None,
              propertyFullyDisposed = None
            )
          )
        )
      )
    ),
    loanOutstanding = None,
    unquotedShares = None
  )

  "merge" should {
    "put new member data if there is no previous data" in {
      val testEtmpData = etmpData.copy(otherAssetsConnectedParty = None)

      val result = transformer.merge(NonEmptyList.of(tanbigleTx), List(testEtmpData))

      result mustBe List(
        etmpData.copy(
          tangibleProperty = Some(
            SippTangibleProperty(
              1,
              Some(
                List(
                  SippTangibleProperty.TransactionDetail(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquiredFromName = "acquiredFromName",
                    totalCost = 20.0,
                    independentValution = YesNo.Yes,
                    totalIncomeOrReceipts = 20.0,
                    costOrMarket = MarketValue,
                    costMarketValue = 20.0,
                    isPropertyDisposed = YesNo.No,
                    disposedPropertyProceedsAmt = None,
                    purchaserNamesIfDisposed = None,
                    anyOfPurchaserConnected = None,
                    independentValutionDisposal = None,
                    propertyFullyDisposed = None
                  )
                )
              )
            )
          )
        )
      )

    }

    "replace data for a single member when member match is found" in {
      val updateValues = tanbigleTx.copy(
        acquiredFromName = "test2"
      )
      val result = transformer.merge(NonEmptyList.of(updateValues), List(etmpData))

      result mustBe List(
        etmpData.copy(
          tangibleProperty = Some(
            SippTangibleProperty(
              1,
              Some(
                List(
                  SippTangibleProperty.TransactionDetail(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquiredFromName = "test2",
                    totalCost = 20.0,
                    independentValution = YesNo.Yes,
                    totalIncomeOrReceipts = 20.0,
                    costOrMarket = MarketValue,
                    costMarketValue = 20.0,
                    isPropertyDisposed = YesNo.No,
                    disposedPropertyProceedsAmt = None,
                    purchaserNamesIfDisposed = None,
                    anyOfPurchaserConnected = None,
                    independentValutionDisposal = None,
                    propertyFullyDisposed = None
                  )
                )
              )
            )
          )
        )
      )

    }

    "add data with new member details for a single member when match is not found" in {
      val testData = tanbigleTx.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testData), List(etmpData))

      result mustBe List(
        etmpData.copy(tangibleProperty = None), // No more tx for first member :/
        etmpData.copy(
          memberDetails = MemberDetails(
            firstName = "firstName",
            middleName = None,
            lastName = "lastName",
            nino = Some("otherNino"),
            reasonNoNINO = None,
            dateOfBirth = LocalDate.of(2020, 1, 1)
          ),
          tangibleProperty = Some(
            SippTangibleProperty(
              1,
              Some(
                List(
                  SippTangibleProperty.TransactionDetail(
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    assetDescription = "Asset Description",
                    acquiredFromName = "acquiredFromName",
                    totalCost = 20.0,
                    independentValution = YesNo.Yes,
                    totalIncomeOrReceipts = 20.0,
                    costOrMarket = MarketValue,
                    costMarketValue = 20.0,
                    isPropertyDisposed = YesNo.No,
                    disposedPropertyProceedsAmt = None,
                    purchaserNamesIfDisposed = None,
                    anyOfPurchaserConnected = None,
                    independentValutionDisposal = None,
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
}
