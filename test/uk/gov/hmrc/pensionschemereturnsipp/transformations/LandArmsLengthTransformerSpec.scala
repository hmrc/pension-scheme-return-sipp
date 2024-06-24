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
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.SippLandArmsLength.TransactionDetail
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{EtmpAddress, SectionStatus}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, MemberDetails, SippLandArmsLength}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

class LandArmsLengthTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  import java.time.LocalDate

  private val transformer: LandArmsLengthTransformer = new LandArmsLengthTransformer()

  val landArmsDataRow1 = LandOrConnectedPropertyApiModel.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    acquisitionDate = LocalDate.of(2020, 1, 1),
    landOrPropertyinUK = YesNo.Yes,
    addressDetails = AddressDetails(
      addressLine1 = "addressLine1",
      addressLine2 = Some("addressLine2"),
      addressLine3 = None,
      addressLine4 = None,
      addressLine5 = None,
      ukPostCode = None,
      countryCode = "UK"
    ),
    registryDetails = RegistryDetails(registryRefExist = YesNo.No, registryReference = None, noRegistryRefReason = None),
    acquiredFromName = "acquiredFromName",
    totalCost = 10,
    independentValuation = YesNo.Yes,
    jointlyHeld = YesNo.Yes,
    noOfPersons = None,
    residentialSchedule29A = YesNo.Yes,
    isLeased = YesNo.Yes,
    lesseeDetails = None,
    totalIncomeOrReceipts = 10,
    isPropertyDisposed = YesNo.Yes,
    disposalDetails = None,
    transactionCount = None
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
    landArmsLength = Some(
      SippLandArmsLength(
        1,
        Some(
          List(
            TransactionDetail(
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
    ),
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

  "merge" should {
    "update LandArms data for a single member when member match is found" in {
      val testEtmpData = etmpData.copy(landArmsLength = None)

      val result = transformer.merge(NonEmptyList.of(landArmsDataRow1), List(testEtmpData))

      result mustBe List(
        etmpData.copy(
          landArmsLength = Some(
            SippLandArmsLength(
              1,
              Some(
                List(
                  TransactionDetail(
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

      val testLandArmsDataRow1 = landArmsDataRow1.copy(acquiredFromName = "test2")
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(
          landArmsLength = Some(
            SippLandArmsLength(
              1,
              Some(
                List(
                  TransactionDetail(
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
      val testLandArmsDataRow1 = landArmsDataRow1.copy(nino = NinoType(Some("otherNino"), None))
      val result = transformer.merge(NonEmptyList.of(testLandArmsDataRow1), List(etmpData))

      result mustBe List(
        etmpData.copy(landArmsLength = None),
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

  lazy val sipp = {
    import sippLandArmsLengthTransactionDetail._
    LandOrConnectedPropertyApiModel.TransactionDetails(
      nameDOB = NameDOB(sippMemberDetails.firstName, sippMemberDetails.lastName, sippMemberDetails.dateOfBirth),
      nino = NinoType(sippMemberDetails.nino, sippMemberDetails.reasonNoNINO),
      acquisitionDate = acquisitionDate,
      landOrPropertyinUK = landOrPropertyinUK,
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
          lesseesGrantedAt.get,
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
