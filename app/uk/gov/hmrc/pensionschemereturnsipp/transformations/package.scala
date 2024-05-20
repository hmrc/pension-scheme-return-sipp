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

package uk.gov.hmrc.pensionschemereturnsipp

import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{
  NameDOB,
  NinoType,
  RegistryDetails,
  AddressDetails => ApiAddressDetails,
  YesNo => ApiYesNo
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.MemberDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{EtmpAddress, EtmpRegistryDetails, YesNo => EtmpYesNo}

package object transformations {
  implicit class YesNoOps(val yesNo: ApiYesNo) extends AnyVal {
    def toEtmp: EtmpYesNo = yesNo match {
      case ApiYesNo.Yes => EtmpYesNo.Yes
      case ApiYesNo.No => EtmpYesNo.No
    }
  }

  implicit class AddressOps(val address: ApiAddressDetails) extends AnyVal {
    def toEtmp = EtmpAddress(
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2.get, // todo check with ETMP why it's mandatory
      addressLine3 = address.addressLine3,
      addressLine4 = address.addressLine4,
      addressLine5 = address.addressLine5,
      ukPostCode = address.ukPostCode,
      countryCode = address.countryCode
    )
  }

  implicit class RegistryOps(val registry: RegistryDetails) extends AnyVal {
    def toEtmp: EtmpRegistryDetails =
      EtmpRegistryDetails(
        registryRefExist = registry.registryRefExist.toEtmp,
        registryReference = registry.registryReference,
        noRegistryRefReason = registry.noRegistryRefReason
      )
  }

  def toMemberDetails(nameDoB: NameDOB, nino: NinoType): MemberDetails =
    MemberDetails(
      nameDoB.firstName,
      None, // todo middleName does not exist in our NameDOB
      nameDoB.lastName,
      nino.nino,
      nino.reasonNoNino,
      nameDoB.dob
    )
}
