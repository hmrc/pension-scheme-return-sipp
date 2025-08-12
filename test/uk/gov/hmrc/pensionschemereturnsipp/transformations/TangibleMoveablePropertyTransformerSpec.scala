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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.CostOrMarketType
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.SippTangibleProperty.TransactionDetail
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippTangibleProperty}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpTestValues}

class TangibleMoveablePropertyTransformerSpec extends BaseSpec with SippEtmpTestValues {

  import java.time.LocalDate

  private val transformer: TangibleMoveablePropertyTransformer = new TangibleMoveablePropertyTransformer()

  val tangibleDataRow1 = TangibleMoveablePropertyApi.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    assetDescription = "assetDescription",
    acquisitionDate = LocalDate.of(2020, 1, 1),
    totalCost = 10,
    acquiredFromName = "acquiredFromName",
    independentValuation = Yes,
    totalIncomeOrReceipts = 10,
    costOrMarket = CostOrMarketType.CostValue,
    costMarketValue = 20,
    isPropertyDisposed = No,
    disposalDetails = None,
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
    tangibleProperty = Some(
      SippTangibleProperty(
        noOfTransactions = 1,
        version = None,
        transactionDetails = Some(
          List(
            TransactionDetail(
              assetDescription = "assetDescription",
              acquisitionDate = LocalDate.of(2020, 1, 1),
              totalCost = 10,
              acquiredFromName = "acquiredFromName",
              independentValuation = Yes,
              totalIncomeOrReceipts = 10,
              costOrMarket = CostOrMarketType.CostValue,
              costMarketValue = 20,
              isPropertyDisposed = No,
              disposalDetails = None
            )
          )
        )
      )
    ),
    loanOutstanding = None,
    unquotedShares = None
  )

  "merge" should {
    "add tangible data for a single member when member match is found" in {
      val testEtmpData = etmpData.copy(tangibleProperty = None)

      val result = transformer.merge(NonEmptyList.of(tangibleDataRow1), List(testEtmpData))

      result mustBe List(
        etmpData.copy(
          status = SectionStatus.Changed,
          tangibleProperty = Some(
            SippTangibleProperty(
              noOfTransactions = 1,
              version = None,
              transactionDetails = Some(
                List(
                  TransactionDetail(
                    assetDescription = "assetDescription",
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    totalCost = 10,
                    acquiredFromName = "acquiredFromName",
                    independentValuation = Yes,
                    totalIncomeOrReceipts = 10,
                    costOrMarket = CostOrMarketType.CostValue,
                    costMarketValue = 20,
                    isPropertyDisposed = No,
                    disposalDetails = None
                  )
                )
              )
            )
          )
        )
      )

    }

    "update tangible data for a single member when member match is found" in {

      val testLandArmsDataRow1 = tangibleDataRow1.copy(acquiredFromName = "test22")
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(
          status = SectionStatus.Changed,
          tangibleProperty = Some(
            SippTangibleProperty(
              noOfTransactions = 1,
              version = None,
              transactionDetails = Some(
                List(
                  TransactionDetail(
                    assetDescription = "assetDescription",
                    acquisitionDate = LocalDate.of(2020, 1, 1),
                    totalCost = 10,
                    acquiredFromName = "test22",
                    independentValuation = Yes,
                    totalIncomeOrReceipts = 10,
                    costOrMarket = CostOrMarketType.CostValue,
                    costMarketValue = 20,
                    isPropertyDisposed = No,
                    disposalDetails = None
                  )
                )
              )
            )
          )
        )
      )

    }

    "add tangible data with new member details for a single member when match is not found" in {
      val testTangibleRow1 = tangibleDataRow1.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testTangibleRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(tangibleProperty = None, status = Deleted),
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
        List(etmpSippMemberAndTransactions.copy(tangibleProperty = None))
      )

      result.transactions.length mustBe 0
    }
  }

}
