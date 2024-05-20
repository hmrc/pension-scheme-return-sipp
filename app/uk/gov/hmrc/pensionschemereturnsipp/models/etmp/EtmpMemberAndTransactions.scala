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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoan
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{RegistryDetails, YesNo}

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
)

case class MemberDetails(
  firstName: String,
  middleName: Option[String],
  lastName: String,
  nino: Option[String],
  reasonNoNINO: Option[String],
  dateOfBirth: LocalDate
)

object MemberDetails {
  implicit val formatPersonalDetails: OFormat[MemberDetails] = Json.format[MemberDetails]
}

case class SippLandConnectedParty(
  noOfTransactions: Int,
  transactionDetails: Option[List[SippLandConnectedParty.TransactionDetail]]
)

object SippLandConnectedParty {

  case class TransactionDetail(
    acquisitionDate: LocalDate,
    landOrPropertyInUK: YesNo,
    addressDetails: EtmpAddress,
    registryDetails: RegistryDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValution: YesNo, // Previous api has that Valution typo!
    jointlyHeld: YesNo,
    noOfPersonsIfJointlyHeld: Option[Int],
    residentialSchedule29A: YesNo,
    isLeased: YesNo,
    noOfPersonsForLessees: Option[Int],
    anyOfLesseesConnected: Option[YesNo],
    leaseGrantedDate: Option[LocalDate],
    annualLeaseAmount: Option[Double],
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposedPropertyProceedsAmt: Option[Double],
    purchaserNamesIfDisposed: Option[String],
    anyOfPurchaserConnected: Option[YesNo],
    independentValutionDisposal: Option[YesNo], // Previous api has that Valution typo!
    propertyFullyDisposed: Option[YesNo]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippOtherAssetsConnectedParty(
  noOfTransactions: Int,
  transactionDetails: Option[List[SippOtherAssetsConnectedParty.TransactionDetail]]
)

object SippOtherAssetsConnectedParty {
  case class TransactionDetail(
    acquisitionDate: LocalDate,
    assetDescription: String,
    acquisitionOfShares: YesNo,
    sharesCompanyDetails: Option[EtmpSippSharesCompanyDetail],
    acquiredFromName: String,
    totalCost: Double,
    independentValution: YesNo,
    tangibleSchedule29A: YesNo,
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposedPropertyProceedsAmt: Option[Double],
    purchaserNamesIfDisposed: Option[String],
    anyOfPurchaserConnected: Option[YesNo],
    independentValutionDisposal: Option[YesNo], // Previous api has that Valution typo!
    disposalOfShares: Option[YesNo],
    noOfSharesHeld: Option[Int],
    propertyFullyDisposed: Option[YesNo]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippLandArmsLength(
  noOfTransactions: Int,
  transactionDetails: Option[List[SippLandArmsLength.TransactionDetail]]
)

object SippLandArmsLength {
  case class TransactionDetail(
    acquisitionDate: LocalDate,
    landOrPropertyinUK: YesNo,
    addressDetails: EtmpAddress,
    registryDetails: RegistryDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValution: YesNo,
    jointlyHeld: YesNo,
    noOfPersonsIfJointlyHeld: Option[Int],
    residentialSchedule29A: YesNo,
    isLeased: YesNo,
    noOfPersonsForLessees: Option[Int],
    anyOfLesseesConnected: Option[YesNo],
    lesseesGrantedAt: Option[LocalDate],
    annualLeaseAmount: Option[Double],
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposedPropertyProceedsAmt: Option[Double],
    purchaserNamesIfDisposed: Option[String],
    anyOfPurchaserConnected: Option[YesNo],
    independentValutionDisposal: Option[YesNo], // Previous api has that Valution typo!
    propertyFullyDisposed: Option[YesNo]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippTangibleProperty(
  noOfTransactions: Int,
  transactionDetails: Option[List[SippTangibleProperty.TransactionDetail]]
)

object SippTangibleProperty {
  case class TransactionDetail(
    assetDescription: String,
    acquisitionDate: LocalDate,
    totalCost: Double,
    acquiredFromName: String,
    independentValution: YesNo,
    totalIncomeOrReceipts: Double,
    costOrMarket: EtmpSippCostOrMarketType,
    costMarketValue: Double,
    isPropertyDisposed: YesNo,
    disposedPropertyProceedsAmt: Option[Double],
    purchaserNamesIfDisposed: Option[String],
    anyOfPurchaserConnected: Option[YesNo],
    independentValutionDisposal: Option[YesNo], // Previous api has that Valution typo!
    propertyFullyDisposed: Option[YesNo]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
}

case class SippLoanOutstanding(
  noOfTransactions: Int,
  transactionDetails: Option[List[SippLoanOutstanding.TransactionDetail]]
)

object SippLoanOutstanding {
  case class TransactionDetail(
    loanRecipientName: String,
    dateOfLoan: LocalDate,
    amountOfLoan: Double,
    loanConnectedParty: EtmpSippConnectedOrUnconnectedType,
    repayDate: LocalDate,
    interestRate: Double,
    loanSecurity: YesNo,
    capitalRepayments: Double,
    interestPayments: Double,
    arrearsOutstandingPrYears: YesNo,
    outstandingYearEndAmount: Double
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]

  object TransactionDetail {
    implicit class TransformationOps(val transactionDetail: TransactionDetail) extends AnyVal {
      def toApi(nameDOB: NameDOB, nino: NinoType): OutstandingLoan.TransactionDetail =
        OutstandingLoan.TransactionDetail(
          nameDOB,
          nino,
          loanRecipientName = transactionDetail.loanRecipientName,
          dateOfLoan = transactionDetail.dateOfLoan,
          amountOfLoan = transactionDetail.amountOfLoan,
          loanConnectedParty =
            if (transactionDetail.loanConnectedParty == EtmpSippConnectedOrUnconnectedType.Connected) YesNo.Yes
            else YesNo.No,
          repayDate = transactionDetail.repayDate,
          interestRate = transactionDetail.interestRate,
          loanSecurity = transactionDetail.loanSecurity,
          capitalRepayments = transactionDetail.capitalRepayments,
          interestPayments = transactionDetail.interestPayments,
          arrearsOutstandingPrYears = transactionDetail.arrearsOutstandingPrYears,
          outstandingYearEndAmount = transactionDetail.outstandingYearEndAmount
        )
    }
  }
}

case class SippUnquotedShares(
  noOfTransactions: Int,
  transactionDetails: Option[List[SippUnquotedShares.TransactionDetail]]
)

object SippUnquotedShares {
  case class TransactionDetail(
    sharesCompanyDetails: EtmpSippSharesCompanyDetail,
    acquiredFromName: String,
    totalCost: Double,
    independentValution: YesNo,
    noOfSharesSold: Option[Int],
    totalDividendsIncome: Double,
    sharesDisposed: YesNo,
    sharesDisposalDetails: EtmpSippSharesDisposalDetails,
    noOfSharesHeld: Option[Int]
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
  implicit val formatOtherAssetsConnectedParty: OFormat[SippOtherAssetsConnectedParty] =
    Json.format[SippOtherAssetsConnectedParty]
  implicit val formatLandArmsLength: OFormat[SippLandArmsLength] = Json.format[SippLandArmsLength]
  implicit val formatTangibleProperty: OFormat[SippTangibleProperty] = Json.format[SippTangibleProperty]
  implicit val formatLoanOutstanding: OFormat[SippLoanOutstanding] = Json.format[SippLoanOutstanding]
  implicit val formatUnquotedShares: OFormat[SippUnquotedShares] = Json.format[SippUnquotedShares]
  implicit val format: OFormat[EtmpMemberAndTransactions] = Json.format[EtmpMemberAndTransactions]
}
