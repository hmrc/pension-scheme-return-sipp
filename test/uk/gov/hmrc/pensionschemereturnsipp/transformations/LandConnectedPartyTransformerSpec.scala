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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedPropertyApi
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{AddressDetails, RegistryDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.Deleted
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{MemberDetails, SippLandConnectedParty}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

import java.time.LocalDate

class LandConnectedPartyTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  private val transformer: LandConnectedPartyTransformer = new LandConnectedPartyTransformer()

  "merge" should {
    "add LandArms data for a single member when member match is found" in {
      val testEtmpData = etmpDataWithLandConnectedTx.copy(landConnectedParty = None)

      val result = transformer.merge(NonEmptyList.of(landConnectedTransaction), List(testEtmpData))

      result mustBe List(
        etmpDataWithLandConnectedTx.copy(
          status = SectionStatus.Changed,
          landConnectedParty = Some(
            SippLandConnectedParty(
              1,
              None,
              Some(
                List(
                  SippLandConnectedParty.TransactionDetail(
                    LocalDate.of(2020, 1, 1),
                    Yes,
                    AddressDetails("addressLine1", "addressLine2", None, None, None, None, "UK"),
                    RegistryDetails(No, None, None),
                    "acquiredFromName",
                    10.0,
                    Yes,
                    Yes,
                    None,
                    Yes,
                    Yes,
                    None,
                    10.0,
                    Yes,
                    None
                  )
                )
              )
            )
          )
        )
      )

    }

    "update LandArms data for a single member when member match is found" in {

      val testLandArmsDataRow1 = landConnectedTransaction.copy(acquiredFromName = "test2")
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpDataWithLandConnectedTx))

      result mustBe List(
        etmpDataWithLandConnectedTx.copy(
          status = SectionStatus.Changed,
          landConnectedParty = Some(
            SippLandConnectedParty(
              1,
              None,
              Some(
                List(
                  SippLandConnectedParty.TransactionDetail(
                    LocalDate.of(2020, 1, 1),
                    Yes,
                    AddressDetails("addressLine1", "addressLine2", None, None, None, None, "UK"),
                    RegistryDetails(No, None, None),
                    "test2",
                    10.0,
                    Yes,
                    Yes,
                    None,
                    Yes,
                    Yes,
                    None,
                    10.0,
                    Yes,
                    None
                  )
                )
              )
            )
          )
        )
      )

    }

    "not update the version number of the member or transaction when there is no difference in the transactions" in {
      val landConnectedTransaction1 = landConnectedTransaction

      val result =
        transformer.merge(NonEmptyList.of(landConnectedTransaction1), List(existingEtmpDataWithLandConnectedTx))

      result mustBe List(existingEtmpDataWithLandConnectedTx)
    }

    "add LandArms data with new member details for a single member when match is not found" in {
      val testLandArmsDataRow1 = landConnectedTransaction.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpDataWithLandConnectedTx))

      result mustBe List(
        etmpDataWithLandConnectedTx.copy(landConnectedParty = None, status = Deleted),
        etmpDataWithLandConnectedTx.copy(
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
        List(etmpSippMemberAndTransactions.copy(landConnectedParty = None))
      )

      result.transactions.length mustBe 0
    }
  }

  val sipp: LandOrConnectedPropertyApi.TransactionDetails = {
    import sippLandConnectedPartyTransactionDetail._

    LandOrConnectedPropertyApi.TransactionDetails(
      nameDOB = NameDOB(sippMemberDetails.firstName, sippMemberDetails.lastName, sippMemberDetails.dateOfBirth),
      nino = NinoType(sippMemberDetails.nino, sippMemberDetails.reasonNoNINO),
      acquisitionDate = acquisitionDate,
      landOrPropertyInUK = landOrPropertyInUK,
      addressDetails = AddressDetails(
        addressDetails.addressLine1,
        addressDetails.addressLine2,
        addressDetails.addressLine3,
        addressDetails.addressLine4,
        addressDetails.addressLine5,
        addressDetails.ukPostCode,
        addressDetails.countryCode
      ),
      registryDetails = RegistryDetails(
        registryDetails.registryRefExist,
        registryDetails.registryReference,
        registryDetails.noRegistryRefReason
      ),
      acquiredFromName = acquiredFromName,
      totalCost = totalCost,
      independentValuation = independentValuation,
      jointlyHeld = jointlyHeld,
      noOfPersons = noOfPersons,
      residentialSchedule29A = residentialSchedule29A,
      isLeased = isLeased,
      lesseeDetails = lesseeDetails,
      totalIncomeOrReceipts = totalIncomeOrReceipts,
      isPropertyDisposed = isPropertyDisposed,
      disposalDetails = disposalDetails,
      transactionCount = None
    )
  }
}
