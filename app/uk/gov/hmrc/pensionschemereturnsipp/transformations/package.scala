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

import uk.gov.hmrc.pensionschemereturnsipp.models.api.{AccountingPeriod, AccountingPeriodDetails, ReportDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{
  NameDOB,
  NinoType,
  UnquotedShareDisposalDetail,
  AddressDetails => ApiAddressDetails
}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{EtmpAddress, EtmpSippSharesDisposalDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{
  EtmpSippAccountingPeriodDetails,
  EtmpSippReportDetails,
  MemberDetails
}

package object transformations {
  implicit class AddressOps(val address: ApiAddressDetails) extends AnyVal {
    def toEtmp = EtmpAddress(
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      addressLine3 = address.addressLine3,
      addressLine4 = address.addressLine4,
      addressLine5 = address.addressLine5,
      ukPostCode = address.ukPostCode,
      countryCode = address.countryCode
    )
  }

  implicit class AddressEtmpOps(val address: EtmpAddress) extends AnyVal {
    def fromEtmp: ApiAddressDetails = ApiAddressDetails(
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      addressLine3 = address.addressLine3,
      addressLine4 = address.addressLine4,
      addressLine5 = address.addressLine5,
      ukPostCode = address.ukPostCode,
      countryCode = address.countryCode
    )
  }

  implicit class EtmpReportDetailsOps(val report: EtmpSippReportDetails) extends AnyVal {
    def toApi = ReportDetails(
      pstr = report.pstr.getOrElse(throw new IllegalArgumentException("pstr was missing in the ETMP response")), // todo check with ETMP why it's not mandatory
      status = report.status,
      periodStart = report.periodStart,
      periodEnd = report.periodEnd,
      schemeName = report.schemeName,
      psrVersion = report.psrVersion
    )
  }

  implicit class EtmpAccountingPeriodOps(val details: EtmpSippAccountingPeriodDetails) extends AnyVal {
    def toApi: AccountingPeriodDetails = AccountingPeriodDetails(
      version = details.version,
      accountingPeriods = details.accountingPeriods.map(aP => AccountingPeriod(aP.accPeriodStart, aP.accPeriodEnd))
    )
  }

  implicit class ReportDetailsOps(val report: ReportDetails) extends AnyVal {
    def toEtmp = EtmpSippReportDetails(
      pstr = Some(report.pstr),
      status = report.status,
      periodStart = report.periodStart,
      periodEnd = report.periodEnd,
      memberTransactions = YesNo.Yes,
      schemeName = report.schemeName,
      psrVersion = report.psrVersion
    )
  }

  def toEtmp(shareDetails: UnquotedShareDisposalDetail): EtmpSippSharesDisposalDetails =
    EtmpSippSharesDisposalDetails(
      disposedShareAmount = shareDetails.totalAmount,
      disposalConnectedParty = shareDetails.purchaserConnectedParty,
      purchaserName = shareDetails.nameOfPurchaser,
      independentValutionDisposal = shareDetails.independentValuationDisposal
    )

  def toMemberDetails(nameDoB: NameDOB, nino: NinoType): MemberDetails =
    MemberDetails(
      nameDoB.firstName,
      None, // todo middleName does not exist in our NameDOB
      nameDoB.lastName,
      nino.nino,
      nino.reasonNoNino,
      nameDoB.dob
    )

  def toNameDOB(memberDetails: MemberDetails): NameDOB =
    NameDOB(
      memberDetails.firstName,
      memberDetails.lastName,
      memberDetails.dateOfBirth
    )

  def toNinoType(memberDetails: MemberDetails): NinoType =
    NinoType(
      memberDetails.nino,
      memberDetails.reasonNoNINO
    )
}
