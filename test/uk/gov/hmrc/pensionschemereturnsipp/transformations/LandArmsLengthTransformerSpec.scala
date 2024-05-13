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

import cats.implicits.catsSyntaxOptionId
import uk.gov.hmrc.pensionschemereturnsipp.models.api.LandOrConnectedProperty
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{
  AddressDetails,
  DisposalDetails,
  LesseeDetails,
  NameDOB,
  NinoType,
  RegistryDetails,
  YesNo
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpMemberAndTransactions
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

class LandArmsLengthTransformerSpec extends BaseSpec with SippEtmpDummyTestValues {

  val transformer = new LandArmsLengthTransformer
  "LandArmsLengthTransformerSpec" should {
    "transform" in {
      transformer.transform(List(sipp)) mustEqual List(
        EtmpMemberAndTransactions(
          SectionStatus.New,
          None,
          sippMemberDetails.copy(middleName = None),
          landConnectedParty = None,
          otherAssetsConnectedParty = None,
          landArmsLength = Some(sippLandArmsLengthLong),
          tangibleProperty = None,
          loanOutstanding = None,
          unquotedShares = None
        )
      )
    }
  }

  val sipp = {
    import sippLandArmsLengthTransactionDetail._
    LandOrConnectedProperty.TransactionDetails(
      nameDOB = NameDOB(sippMemberDetails.firstName, sippMemberDetails.lastName, sippMemberDetails.dateOfBirth),
      nino = NinoType(sippMemberDetails.nino, sippMemberDetails.reasonNoNINO),
      acquisitionDate = acquisitionDate,
      landOrPropertyinUK = YesNo(landOrPropertyinUK.boolean),
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
        YesNo(registryDetails.registryRefExist.boolean),
        registryDetails.registryReference,
        registryDetails.noRegistryRefReason
      ),
      acquiredFromName = acquiredFromName,
      totalCost = totalCost,
      independentValuation = YesNo(independentValution.boolean),
      jointlyHeld = YesNo(jointlyHeld.boolean),
      noOfPersons = noOfPersonsIfJointlyHeld,
      residentialSchedule29A = YesNo(residentialSchedule29A.boolean),
      isLeased = YesNo(isLeased.boolean),
      lesseeDetails = Option.when(isLeased.boolean)(
        LesseeDetails(
          noOfPersonsForLessees,
          None, // todo model diff
          YesNo(anyOfLesseesConnected.get.boolean),
          lesseesGrantedAt.get,
          annualLeaseAmount.get
        )
      ),
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
      }
    )
  }
}
