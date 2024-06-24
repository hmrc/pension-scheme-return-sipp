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
import cats.implicits.catsSyntaxOptionId
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedPropertyApiModel
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{RegistryDetails, YesNo}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.EtmpAddress
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{MemberDetails, SippLandConnectedParty}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

import java.time.LocalDate

class LandConnectedPartyTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  private val transformer: LandConnectedPartyTransformer = new LandConnectedPartyTransformer()

  "merge" should {
    "update LandArms data for a single member when member match is found" in {
      val testEtmpData = etmpDataWithLandConnectedTx.copy(landConnectedParty = None)

      val result = transformer.merge(NonEmptyList.of(landConnectedTransaction), List(testEtmpData))

      result mustBe List(
        etmpDataWithLandConnectedTx.copy(
          landConnectedParty = Some(
            SippLandConnectedParty(
              1,
              Some(
                List(
                  SippLandConnectedParty.TransactionDetail(
                    LocalDate.of(2020, 1, 1),
                    YesNo.Yes,
                    EtmpAddress("addressLine1", "addressLine2", None, None, None, None, "UK"),
                    RegistryDetails(YesNo.No, None, None),
                    "acquiredFromName",
                    10.0,
                    YesNo.Yes,
                    YesNo.Yes,
                    None,
                    YesNo.Yes,
                    YesNo.Yes,
                    None,
                    None,
                    None,
                    None,
                    10.0,
                    YesNo.Yes,
                    None,
                    None,
                    None,
                    None,
                    None
                  )
                )
              )
            )
          )
        )
      )

    }

    "replace LandArms data for a single member when member match is found" in {

      val testLandArmsDataRow1 = landConnectedTransaction.copy(acquiredFromName = "test2")
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpDataWithLandConnectedTx))

      result mustBe List(
        etmpDataWithLandConnectedTx.copy(
          landConnectedParty = Some(
            SippLandConnectedParty(
              1,
              Some(
                List(
                  SippLandConnectedParty.TransactionDetail(
                    LocalDate.of(2020, 1, 1),
                    YesNo.Yes,
                    EtmpAddress("addressLine1", "addressLine2", None, None, None, None, "UK"),
                    RegistryDetails(YesNo.No, None, None),
                    "test2",
                    10.0,
                    YesNo.Yes,
                    YesNo.Yes,
                    None,
                    YesNo.Yes,
                    YesNo.Yes,
                    None,
                    None,
                    None,
                    None,
                    10.0,
                    YesNo.Yes,
                    None,
                    None,
                    None,
                    None,
                    None
                  )
                )
              )
            )
          )
        )
      )

    }

    "add LandArms data with new member details for a single member when match is not found" in {
      val testLandArmsDataRow1 = landConnectedTransaction.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpDataWithLandConnectedTx))

      result mustBe List(
        etmpDataWithLandConnectedTx.copy(landConnectedParty = None),
        etmpDataWithLandConnectedTx.copy(
          memberDetails = MemberDetails(
            firstName = "firstName",
            middleName = None,
            lastName = "lastName",
            nino = Some("otherNino"),
            reasonNoNINO = None,
            dateOfBirth = LocalDate.of(2020, 1, 1)
          )
        )
      )
    }
  }

  val sipp: LandOrConnectedPropertyApiModel.TransactionDetails = {
    import sippLandConnectedPartyTransactionDetail._

    LandOrConnectedPropertyApiModel.TransactionDetails(
      nameDOB = NameDOB(sippMemberDetails.firstName, sippMemberDetails.lastName, sippMemberDetails.dateOfBirth),
      nino = NinoType(sippMemberDetails.nino, sippMemberDetails.reasonNoNINO),
      acquisitionDate = acquisitionDate,
      landOrPropertyinUK = landOrPropertyInUK,
      addressDetails = AddressDetails(
        addressDetails.addressLine1,
        addressDetails.addressLine2.some,
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
      independentValuation = independentValution,
      jointlyHeld = jointlyHeld,
      noOfPersons = noOfPersonsIfJointlyHeld,
      residentialSchedule29A = residentialSchedule29A,
      isLeased = isLeased,
      lesseeDetails = Option.when(isLeased.boolean)(
        LesseeDetails(
          noOfPersonsForLessees,
          None, // todo model diff
          anyOfLesseesConnected.get,
          leaseGrantedDate.get,
          annualLeaseAmount.get
        )
      ),
      totalIncomeOrReceipts = totalIncomeOrReceipts,
      isPropertyDisposed = isPropertyDisposed,
      disposalDetails = Option.when(isPropertyDisposed.boolean) {
        DisposalDetails(
          disposedPropertyProceedsAmt.get,
          purchaserNamesIfDisposed.get,
          anyOfPurchaserConnected.get,
          independentValutionDisposal.get,
          propertyFullyDisposed.get
        )
      },
      transactionCount = None
    )
  }
}
