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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedProperty
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedProperty.TransactionDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{
  AddressDetails,
  DisposalDetails,
  LesseeDetails,
  NameDOB,
  NinoType,
  RegistryDetails,
  YesNo => ApiYesNo
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{EtmpAddress, EtmpRegistryDetails, SectionStatus, YesNo}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpMemberAndTransactions,
  MemberDetails,
  SippLandConnectedParty
}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

import java.time.LocalDate

class LandConnectedPartyTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  private val transformer: LandConnectedPartyTransformer = new LandConnectedPartyTransformer()

  val landArmsDataRow1 = TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    acquisitionDate = LocalDate.of(2020, 1, 1),
    landOrPropertyinUK = ApiYesNo.Yes,
    addressDetails = AddressDetails(
      addressLine1 = "addressLine1",
      addressLine2 = Some("addressLine2"),
      addressLine3 = None,
      addressLine4 = None,
      addressLine5 = None,
      ukPostCode = None,
      countryCode = "UK"
    ),
    registryDetails =
      RegistryDetails(registryRefExist = ApiYesNo.No, registryReference = None, noRegistryRefReason = None),
    acquiredFromName = "acquiredFromName",
    totalCost = 10,
    independentValuation = ApiYesNo.Yes,
    jointlyHeld = ApiYesNo.Yes,
    noOfPersons = None,
    residentialSchedule29A = ApiYesNo.Yes,
    isLeased = ApiYesNo.Yes,
    lesseeDetails = None,
    totalIncomeOrReceipts = 10,
    isPropertyDisposed = ApiYesNo.Yes,
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
    landConnectedParty = Some(
      SippLandConnectedParty(
        1,
        Some(
          List(
            SippLandConnectedParty.TransactionDetail(
              LocalDate.of(2020, 1, 1),
              YesNo.Yes,
              EtmpAddress("addressLine1", "addressLine2", None, None, None, None, "UK"),
              EtmpRegistryDetails(YesNo.No, None, None),
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
    ),
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

  "merge" should {
    "update LandArms data for a single member when member match is found" in {
      val testEtmpData = etmpData.copy(landConnectedParty = None)

      val result = transformer.merge(NonEmptyList.of(landArmsDataRow1), List(testEtmpData))

      result mustBe List(
        etmpData.copy(
          landConnectedParty = Some(
            SippLandConnectedParty(
              1,
              Some(
                List(
                  SippLandConnectedParty.TransactionDetail(
                    LocalDate.of(2020, 1, 1),
                    YesNo.Yes,
                    EtmpAddress("addressLine1", "addressLine2", None, None, None, None, "UK"),
                    EtmpRegistryDetails(YesNo.No, None, None),
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

      val testLandArmsDataRow1 = landArmsDataRow1.copy(acquiredFromName = "test2")
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(
          landConnectedParty = Some(
            SippLandConnectedParty(
              1,
              Some(
                List(
                  SippLandConnectedParty.TransactionDetail(
                    LocalDate.of(2020, 1, 1),
                    YesNo.Yes,
                    EtmpAddress("addressLine1", "addressLine2", None, None, None, None, "UK"),
                    EtmpRegistryDetails(YesNo.No, None, None),
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
      val testLandArmsDataRow1 = landArmsDataRow1.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(landConnectedParty = None),
        etmpData.copy(
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

  val sipp = {
    import sippLandConnectedPartyTransactionDetail._

    LandOrConnectedProperty.TransactionDetails(
      nameDOB = NameDOB(sippMemberDetails.firstName, sippMemberDetails.lastName, sippMemberDetails.dateOfBirth),
      nino = NinoType(sippMemberDetails.nino, sippMemberDetails.reasonNoNINO),
      acquisitionDate = acquisitionDate,
      landOrPropertyinUK = ApiYesNo(landOrPropertyInUK.boolean),
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
        ApiYesNo(registryDetails.registryRefExist.boolean),
        registryDetails.registryReference,
        registryDetails.noRegistryRefReason
      ),
      acquiredFromName = acquiredFromName,
      totalCost = totalCost,
      independentValuation = ApiYesNo(independentValution.boolean),
      jointlyHeld = ApiYesNo(jointlyHeld.boolean),
      noOfPersons = noOfPersonsIfJointlyHeld,
      residentialSchedule29A = ApiYesNo(residentialSchedule29A.boolean),
      isLeased = ApiYesNo(isLeased.boolean),
      lesseeDetails = Option.when(isLeased.boolean)(
        LesseeDetails(
          noOfPersonsForLessees,
          None, // todo model diff
          ApiYesNo(anyOfLesseesConnected.get.boolean),
          leaseGrantedDate.get,
          annualLeaseAmount.get
        )
      ),
      totalIncomeOrReceipts = totalIncomeOrReceipts,
      isPropertyDisposed = ApiYesNo(isPropertyDisposed.boolean),
      disposalDetails = Option.when(isPropertyDisposed.boolean) {
        DisposalDetails(
          disposedPropertyProceedsAmt.get,
          purchaserNamesIfDisposed.get,
          ApiYesNo(anyOfPurchaserConnected.get.boolean),
          ApiYesNo(independentValutionDisposal.get.boolean),
          ApiYesNo(propertyFullyDisposed.get.boolean)
        )
      }
    )

  }

}
