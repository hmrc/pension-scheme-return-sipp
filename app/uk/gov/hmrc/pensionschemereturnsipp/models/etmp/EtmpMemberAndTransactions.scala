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

package uk.gov.hmrc.pensionschemereturnsipp.models.etmp

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{
  AddressDetails,
  ConnectionStatus,
  CostOrMarketType,
  DisposalDetails,
  LesseeDetails,
  RegistryDetails,
  SharesCompanyDetails,
  UnquotedShareDisposalDetails,
  YesNo
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus

import java.time.LocalDate

case class EtmpMemberAndTransactions(
  status: SectionStatus,
  version: Option[String],
  memberDetails: MemberDetails,
  landConnectedParty: Option[SippLandConnectedParty],
  otherAssetsConnectedParty: Option[SippOtherAssetsConnectedParty],
  landArmsLength: Option[SippLandArmsLength],
  tangibleProperty: Option[SippTangibleProperty],
  loanOutstanding: Option[SippLoanOutstanding],
  unquotedShares: Option[SippUnquotedShares]
) extends VersionedAsset

case class PersonalDetails(
  firstName: String,
  lastName: String,
  nino: Option[String],
  reasonNoNINO: Option[String],
  dateOfBirth: LocalDate
)

object PersonalDetails {
  implicit val personalDetailsFormat: OFormat[PersonalDetails] = Json.format[PersonalDetails]
}

case class MemberDetails(personalDetails: PersonalDetails) {
  def firstName: String = personalDetails.firstName
  def lastName: String = personalDetails.lastName
  def nino: Option[String] = personalDetails.nino
  def reasonNoNINO: Option[String] = personalDetails.reasonNoNINO
  def dateOfBirth: LocalDate = personalDetails.dateOfBirth
}

object MemberDetails {
  def apply(
    firstName: String,
    lastName: String,
    nino: Option[String],
    reasonNoNINO: Option[String],
    dateOfBirth: LocalDate
  ): MemberDetails =
    MemberDetails(PersonalDetails(firstName, lastName, nino, reasonNoNINO, dateOfBirth))

  def compare(p1: PersonalDetails, p2: PersonalDetails): Boolean =
    p1.firstName == p2.firstName &&
      p1.lastName == p2.lastName &&
      p1.dateOfBirth == p2.dateOfBirth &&
      p1.nino == p2.nino

  implicit val memberDetailsFormat: OFormat[MemberDetails] = Json.format[MemberDetails]
}

sealed trait VersionedAsset {
  def version: Option[String]
}

case class SippLandConnectedParty(
  noOfTransactions: Int,
  version: Option[String],
  transactionDetails: Option[List[SippLandConnectedParty.TransactionDetail]]
) extends VersionedAsset

object SippLandConnectedParty {

  case class TransactionDetail(
    acquisitionDate: LocalDate,
    landOrPropertyInUK: YesNo,
    addressDetails: AddressDetails,
    registryDetails: RegistryDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    jointlyHeld: YesNo,
    noOfPersons: Option[Int],
    residentialSchedule29A: YesNo,
    isLeased: YesNo,
    lesseeDetails: Option[LesseeDetails],
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippOtherAssetsConnectedParty(
  noOfTransactions: Int,
  version: Option[String],
  transactionDetails: Option[List[SippOtherAssetsConnectedParty.TransactionDetails]]
) extends VersionedAsset

object SippOtherAssetsConnectedParty {
  case class TransactionDetails(
    acquisitionDate: LocalDate,
    assetDescription: String,
    acquisitionOfShares: YesNo,
    sharesCompanyDetails: Option[SharesCompanyDetails],
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    tangibleSchedule29A: YesNo,
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails],
    disposalOfShares: Option[YesNo],
    noOfSharesHeld: Option[Int]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetails] = Json.format[TransactionDetails]
}

case class SippLandArmsLength(
  noOfTransactions: Int,
  version: Option[String],
  transactionDetails: Option[List[SippLandArmsLength.TransactionDetail]]
) extends VersionedAsset

object SippLandArmsLength {
  case class TransactionDetail(
    acquisitionDate: LocalDate,
    landOrPropertyInUK: YesNo,
    addressDetails: AddressDetails,
    registryDetails: RegistryDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    jointlyHeld: YesNo,
    noOfPersons: Option[Int],
    residentialSchedule29A: YesNo,
    isLeased: YesNo,
    lesseeDetails: Option[LesseeDetails],
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippTangibleProperty(
  noOfTransactions: Int,
  version: Option[String],
  transactionDetails: Option[List[SippTangibleProperty.TransactionDetail]]
) extends VersionedAsset

object SippTangibleProperty {
  case class TransactionDetail(
    assetDescription: String,
    acquisitionDate: LocalDate,
    totalCost: Double,
    acquiredFromName: String,
    independentValuation: YesNo,
    totalIncomeOrReceipts: Double,
    costOrMarket: CostOrMarketType,
    costMarketValue: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippLoanOutstanding(
  noOfTransactions: Int,
  version: Option[String],
  transactionDetails: Option[List[SippLoanOutstanding.TransactionDetail]]
) extends VersionedAsset

object SippLoanOutstanding {
  case class TransactionDetail(
    loanRecipientName: String,
    dateOfLoan: LocalDate,
    amountOfLoan: Double,
    loanConnectedParty: ConnectionStatus,
    repayDate: LocalDate,
    interestRate: Double,
    loanSecurity: YesNo,
    capitalRepayments: Double,
    arrearsOutstandingPrYears: YesNo,
    arrearsOutstandingPrYearsAmt: Option[Double],
    outstandingYearEndAmount: Double
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippUnquotedShares(
  noOfTransactions: Int,
  version: Option[String],
  transactionDetails: Option[List[SippUnquotedShares.TransactionDetail]]
) extends VersionedAsset

object SippUnquotedShares {
  case class TransactionDetail(
    sharesCompanyDetails: SharesCompanyDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    totalDividendsIncome: Double,
    sharesDisposed: YesNo,
    sharesDisposalDetails: Option[UnquotedShareDisposalDetails]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

object EtmpMemberAndTransactions {
  def empty(sectionStatus: SectionStatus, memberDetails: MemberDetails) = new EtmpMemberAndTransactions(
    sectionStatus,
    version = None,
    memberDetails = memberDetails,
    landConnectedParty = None,
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

  implicit val formatMemberDetails: OFormat[MemberDetails] = Json.format[MemberDetails]
  implicit val formatLandConnectedParty: OFormat[SippLandConnectedParty] = Json.format[SippLandConnectedParty]
  implicit val formatOtherAssets: OFormat[SippOtherAssetsConnectedParty] = Json.format[SippOtherAssetsConnectedParty]
  implicit val formatLandArmsLength: OFormat[SippLandArmsLength] = Json.format[SippLandArmsLength]
  implicit val formatTangibleProperty: OFormat[SippTangibleProperty] = Json.format[SippTangibleProperty]
  implicit val formatLoanOutstanding: OFormat[SippLoanOutstanding] = Json.format[SippLoanOutstanding]
  implicit val formatUnquotedShares: OFormat[SippUnquotedShares] = Json.format[SippUnquotedShares]
  implicit val formatMemberAndTrx: OFormat[EtmpMemberAndTransactions] = Json.format[EtmpMemberAndTransactions]
}
