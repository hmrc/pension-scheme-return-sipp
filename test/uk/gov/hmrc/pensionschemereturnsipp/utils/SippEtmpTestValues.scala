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

package uk.gov.hmrc.pensionschemereturnsipp.utils

import cats.data.NonEmptyList
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{LandOrConnectedPropertyRequest, ReportDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{AddressDetails, NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.EtmpConnectedOrUnconnectedType._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{RegistryDetails, YesNo}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp._
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.EtmpSippCostOrMarketType.Cost
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.New
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest
import uk.gov.hmrc.pensionschemereturnsipp.transformations.ReportDetailsOps

import java.time.LocalDate

trait SippEtmpTestValues {
  val sampleDate: LocalDate = LocalDate.of(2023, 10, 19)

  private val sippAddress: EtmpAddress = EtmpAddress(
    addressLine1 = "Flat 9, 76 Bla bla Street",
    addressLine2 = "Another long and Second one",
    addressLine3 = Some("Third Long Line"),
    addressLine4 = Some("Forth Long Line"),
    addressLine5 = Some("Fifth Long Line"),
    ukPostCode = Some("DA18 4XY"),
    countryCode = "GB"
  )

  private val sippRegistryDetails: RegistryDetails = RegistryDetails(
    registryRefExist = Yes,
    registryReference = Some("RegistryReference"),
    noRegistryRefReason = Some("I have a registry and I have entered my registry reference")
  )

  private val sippJointPropertyDetail1: EtmpSippJointPropertyDetail = EtmpSippJointPropertyDetail(
    personName = "AnotherLongName Surname",
    nino = Some("QQ123456A"),
    reasonNoNINO = Some("I have a Nino!")
  )

  private val sippJointPropertyDetail2: EtmpSippJointPropertyDetail = EtmpSippJointPropertyDetail(
    personName = "Another AgainLongName Surname",
    nino = Some("QQ123457A"),
    reasonNoNINO = Some("I have a Nino !!!!")
  )

  protected val testReportDetails: ReportDetails = ReportDetails(
    pstr = "test",
    status = EtmpPsrStatus.Compiled,
    periodStart = sampleDate,
    periodEnd = sampleDate,
    schemeName = None,
    psrVersion = None
  )

  protected val reportDetails: EtmpSippReportDetails = testReportDetails.toEtmp

  private val period: EtmpSippAccountingPeriod = EtmpSippAccountingPeriod(
    accPeriodStart = sampleDate,
    accPeriodEnd = sampleDate
  )

  protected val accountingPeriodDetails: EtmpSippAccountingPeriodDetails = EtmpSippAccountingPeriodDetails(
    version = None,
    accountingPeriods = List(period)
  )

  protected val sippMemberDetails = MemberDetails(
    firstName = "TestLongFirstName",
    middleName = Some("TestLongMiddleName"),
    lastName = "TestLongLastName",
    nino = Some("QQ123456A"),
    reasonNoNINO = Some("I have a Nino!"),
    dateOfBirth = sampleDate
  )

  private val sippSharesCompanyDetail: EtmpSippSharesCompanyDetail = EtmpSippSharesCompanyDetail(
    companySharesName = "A Long Company Name",
    companySharesCRN = Some("A1231233"),
    reasonNoCRN = Some("I have a CRN NUMBER"),
    sharesClass = "A class",
    noOfShares = 1
  )

  protected val sippSharesDisposalDetails: EtmpSippSharesDisposalDetails = EtmpSippSharesDisposalDetails(
    disposedShareAmount = 2123.22,
    disposalConnectedParty = Connected,
    purchaserName = "Some Long Purchaser Name",
    independentValutionDisposal = Yes
  )

  protected val sippLandConnectedPartyTransactionDetail: SippLandConnectedParty.TransactionDetail =
    SippLandConnectedParty.TransactionDetail(
      acquisitionDate = sampleDate,
      landOrPropertyInUK = Yes,
      addressDetails = sippAddress,
      registryDetails = sippRegistryDetails,
      acquiredFromName = "Acquired From Name",
      totalCost = 999999.99,
      independentValution = Yes,
      jointlyHeld = Yes,
      noOfPersonsIfJointlyHeld = Some(2),
      residentialSchedule29A = Yes,
      isLeased = Yes,
      noOfPersonsForLessees = Some(2),
      anyOfLesseesConnected = Some(No),
      leaseGrantedDate = Some(sampleDate),
      annualLeaseAmount = Some(999999.99),
      totalIncomeOrReceipts = 999999.99,
      isPropertyDisposed = Yes,
      disposedPropertyProceedsAmt = Some(999999.99),
      purchaserNamesIfDisposed = Some("Name1 Surname1, Name2 Surname2, Name3 Surname3"),
      anyOfPurchaserConnected = Some(No),
      independentValutionDisposal = Some(No),
      propertyFullyDisposed = Some(Yes)
    )

  protected val sippOtherAssetsConnectedPartyTransactionDetail: SippOtherAssetsConnectedParty.TransactionDetail =
    SippOtherAssetsConnectedParty.TransactionDetail(
      acquisitionDate = sampleDate,
      assetDescription = "Some Asset Description",
      acquisitionOfShares = Yes,
      sharesCompanyDetails = Some(sippSharesCompanyDetail),
      acquiredFromName = "TestLongName TestLongSurname",
      totalCost = 9999999.99,
      independentValution = Yes,
      tangibleSchedule29A = Yes,
      totalIncomeOrReceipts = 999999.99,
      isPropertyDisposed = Yes,
      disposedPropertyProceedsAmt = Some(999999.99),
      purchaserNamesIfDisposed = Some("Name1 Surname1, Name2 Surname2, Name3 Surname3"),
      anyOfPurchaserConnected = Some(No),
      disposalOfShares = No,
      independentValutionDisposal = Some(No),
      noOfSharesHeld = Some(2),
      propertyFullyDisposed = Some(Yes)
    )

  protected val sippLandArmsLengthTransactionDetail = SippLandArmsLength.TransactionDetail(
    acquisitionDate = sampleDate,
    landOrPropertyinUK = Yes,
    addressDetails = sippAddress,
    registryDetails = sippRegistryDetails,
    acquiredFromName = "Acquired From Name",
    totalCost = 999999.99,
    independentValution = Yes,
    jointlyHeld = Yes,
    noOfPersonsIfJointlyHeld = Some(2),
    residentialSchedule29A = Yes,
    isLeased = Yes,
    noOfPersonsForLessees = Some(2),
    anyOfLesseesConnected = Some(No),
    lesseesGrantedAt = Some(sampleDate),
    annualLeaseAmount = Some(999999.99),
    totalIncomeOrReceipts = 999999.99,
    isPropertyDisposed = Yes,
    disposedPropertyProceedsAmt = Some(999999.99),
    purchaserNamesIfDisposed = Some("Name1 Surname1, Name2 Surname2, Name3 Surname3"),
    anyOfPurchaserConnected = Some(No),
    independentValutionDisposal = Some(No),
    propertyFullyDisposed = Some(Yes)
  )

  protected val sippTangiblePropertyTransactionalDetail = SippTangibleProperty.TransactionDetail(
    assetDescription = "Some long asset description",
    acquisitionDate = sampleDate,
    totalCost = 9999999.99,
    acquiredFromName = "TestLongName TestLongSurname",
    independentValution = Yes,
    totalIncomeOrReceipts = 9999999.99,
    costOrMarket = Cost,
    costMarketValue = 9999999.99,
    isPropertyDisposed = Yes,
    disposedPropertyProceedsAmt = Some(999999.99),
    purchaserNamesIfDisposed = Some("Name1 Surname1, Name2 Surname2, Name3 Surname3"),
    anyOfPurchaserConnected = Some(No),
    independentValutionDisposal = Some(No),
    propertyFullyDisposed = Some(Yes)
  )

  protected val sippLoanOutstandingTransactionalDetail = SippLoanOutstanding.TransactionDetail(
    loanRecipientName = "Long Loan Recipient Name",
    dateOfLoan = sampleDate,
    amountOfLoan = 9999999.99,
    loanConnectedParty = Connected,
    repayDate = sampleDate,
    interestRate = 11.1,
    loanSecurity = Yes,
    capitalRepayments = 123123.12,
    interestPayments = 11.1,
    arrearsOutstandingPrYears = Yes,
    outstandingYearEndAmount = 12312.12
  )

  protected val sippUnquotedSharesTransactionalDetail = SippUnquotedShares.TransactionDetail(
    sharesCompanyDetails = sippSharesCompanyDetail,
    acquiredFromName = "TestLongName TestLongSurname",
    totalCost = 9999999.99,
    independentValution = Yes,
    noOfSharesSold = Some(3),
    totalDividendsIncome = 2000.2,
    sharesDisposed = Yes,
    sharesDisposalDetails = Some(sippSharesDisposalDetails),
    noOfSharesHeld = Some(2)
  )

  protected val sippLandConnectedParty = SippLandConnectedParty(
    noOfTransactions = 1,
    transactionDetails = Some(List(sippLandConnectedPartyTransactionDetail))
  )

  private val sippOtherAssetsConnectedParty = SippOtherAssetsConnectedParty(
    noOfTransactions = 1,
    transactionDetails = Some(List(sippOtherAssetsConnectedPartyTransactionDetail))
  )

  private val sippLandArmsLength = SippLandArmsLength(
    noOfTransactions = 1,
    transactionDetails = Some(List(sippLandArmsLengthTransactionDetail))
  )

  private val sippTangibleProperty = SippTangibleProperty(
    noOfTransactions = 1,
    transactionDetails = Some(List(sippTangiblePropertyTransactionalDetail))
  )

  private val sippLoanOutstanding = SippLoanOutstanding(
    noOfTransactions = 1,
    transactionDetails = Some(List(sippLoanOutstandingTransactionalDetail))
  )

  private val sippUnquotedShares = SippUnquotedShares(
    noOfTransactions = 1,
    transactionDetails = Some(List(sippUnquotedSharesTransactionalDetail))
  )

  private val etmpSippMemberAndTransactions: EtmpMemberAndTransactions = EtmpMemberAndTransactions(
    version = None,
    status = New,
    memberDetails = sippMemberDetails,
    landConnectedParty = Some(sippLandConnectedParty),
    otherAssetsConnectedParty = Some(sippOtherAssetsConnectedParty),
    landArmsLength = Some(sippLandArmsLength),
    tangibleProperty = Some(sippTangibleProperty),
    loanOutstanding = Some(sippLoanOutstanding),
    unquotedShares = Some(sippUnquotedShares)
  )

  val fullSippPsrSubmissionEtmpRequest: SippPsrSubmissionEtmpRequest = SippPsrSubmissionEtmpRequest(
    reportDetails = reportDetails,
    accountingPeriodDetails = Some(accountingPeriodDetails),
    memberAndTransactions = Some(NonEmptyList.one(etmpSippMemberAndTransactions)),
    psrDeclaration = None
  )

  val landConnectedTransaction: LandOrConnectedPropertyRequest.TransactionDetails =
    LandOrConnectedPropertyRequest.TransactionDetails(
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
      registryDetails =
        RegistryDetails(registryRefExist = YesNo.No, registryReference = None, noRegistryRefReason = None),
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
      disposalDetails = None
    )

  val etmpDataWithLandConnectedTx: EtmpMemberAndTransactions = EtmpMemberAndTransactions(
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
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

}
