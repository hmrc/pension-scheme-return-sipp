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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{
  AssetsFromConnectedPartyApi,
  LandOrConnectedPropertyApi,
  OutstandingLoansApi,
  ReportDetails,
  TangibleMoveablePropertyApi,
  UnquotedShareApi
}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.CostOrMarketType.{CostValue, MarketValue}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{
  AccountingPeriod,
  AccountingPeriodDetails,
  AddressDetails,
  DisposalDetails,
  LesseeDetails,
  RegistryDetails,
  SharesCompanyDetails,
  UnquotedShareDisposalDetails
}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.ConnectionStatus._
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.New
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.requests.SippPsrSubmissionEtmpRequest

import java.time.LocalDate
import scala.annotation.unused
import io.scalaland.chimney.dsl._
import uk.gov.hmrc.pensionschemereturnsipp.transformations.reportDetailsApiToEtmp

trait SippEtmpTestValues {
  val sampleDate: LocalDate = LocalDate.of(2023, 10, 19)

  private val sippAddress: AddressDetails = AddressDetails(
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

  @unused
  private val sippJointPropertyDetail1: EtmpSippJointPropertyDetail = EtmpSippJointPropertyDetail(
    personName = "AnotherLongName Surname",
    nino = Some("QQ123456A"),
    reasonNoNINO = Some("I have a Nino!")
  )

  @unused
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
    None,
    None
  )

  protected val reportDetails: EtmpSippReportDetails = testReportDetails.transformInto[EtmpSippReportDetails]

  private val period: AccountingPeriod = AccountingPeriod(
    accPeriodStart = sampleDate,
    accPeriodEnd = sampleDate
  )

  protected val accountingPeriodDetails: AccountingPeriodDetails = AccountingPeriodDetails(
    version = None,
    accountingPeriods = NonEmptyList.one(period)
  )

  protected val sippMemberDetails = MemberDetails(
    firstName = "TestLongFirstName",
    lastName = "TestLongLastName",
    nino = Some("QQ123456A"),
    reasonNoNINO = Some("I have a Nino!"),
    dateOfBirth = sampleDate
  )

  private val sippSharesCompanyDetail: SharesCompanyDetails = SharesCompanyDetails(
    companySharesName = "A Long Company Name",
    companySharesCRN = Some("A1231233"),
    reasonNoCRN = Some("I have a CRN NUMBER"),
    sharesClass = "A class",
    noOfShares = 1
  )

  protected val sippSharesDisposalDetails: UnquotedShareDisposalDetails = UnquotedShareDisposalDetails(
    disposedShareAmount = 2123.22,
    purchasersNames = "Some Long Purchaser Name",
    disposalConnectedParty = Yes,
    independentValuationDisposal = Yes,
    noOfSharesSold = 1,
    noOfSharesHeld = 1
  )

  protected val sippLandConnectedPartyTransactionDetail: SippLandConnectedParty.TransactionDetail =
    SippLandConnectedParty.TransactionDetail(
      acquisitionDate = sampleDate,
      landOrPropertyInUK = Yes,
      addressDetails = sippAddress,
      registryDetails = sippRegistryDetails,
      acquiredFromName = "Acquired From Name",
      totalCost = 999999.99,
      independentValuation = Yes,
      jointlyHeld = Yes,
      noOfPersons = Some(2),
      residentialSchedule29A = Yes,
      isLeased = Yes,
      lesseeDetails = Some(LesseeDetails(2, No, sampleDate, 999999.99)),
      totalIncomeOrReceipts = 999999.99,
      isPropertyDisposed = Yes,
      disposalDetails = Some(
        DisposalDetails(999999.99, "Name1 Surname1, Name2 Surname2, Name3 Surname3", No, No, Yes)
      )
    )

  protected val sippOtherAssetsConnectedPartyTransactionDetail: SippOtherAssetsConnectedParty.TransactionDetails =
    SippOtherAssetsConnectedParty.TransactionDetails(
      acquisitionDate = sampleDate,
      assetDescription = "Some Asset Description",
      acquisitionOfShares = Yes,
      sharesCompanyDetails = Some(sippSharesCompanyDetail),
      acquiredFromName = "TestLongName TestLongSurname",
      totalCost = 9999999.99,
      independentValuation = Yes,
      tangibleSchedule29A = Yes,
      totalIncomeOrReceipts = 999999.99,
      isPropertyDisposed = Yes,
      disposalDetails = Some(
        DisposalDetails(999999.99, "Name1 Surname1, Name2 Surname2, Name3 Surname3", No, No, Yes)
      ),
      disposalOfShares = Some(No),
      noOfSharesHeld = Some(2)
    )

  protected val sippLandArmsLengthTransactionDetail = SippLandArmsLength.TransactionDetail(
    acquisitionDate = sampleDate,
    landOrPropertyInUK = Yes,
    addressDetails = sippAddress,
    registryDetails = sippRegistryDetails,
    acquiredFromName = "Acquired From Name",
    totalCost = 999999.99,
    independentValuation = Yes,
    jointlyHeld = Yes,
    noOfPersons = Some(2),
    residentialSchedule29A = Yes,
    isLeased = Yes,
    lesseeDetails = Some(LesseeDetails(2, No, sampleDate, 999999.99)),
    totalIncomeOrReceipts = 999999.99,
    isPropertyDisposed = Yes,
    disposalDetails = Some(DisposalDetails(999999.99, "Name1 Surname1, Name2 Surname2, Name3 Surname3", No, No, Yes))
  )

  protected val sippTangiblePropertyTransactionalDetail = SippTangibleProperty.TransactionDetail(
    assetDescription = "Some long asset description",
    acquisitionDate = sampleDate,
    totalCost = 9999999.99,
    acquiredFromName = "TestLongName TestLongSurname",
    independentValuation = Yes,
    totalIncomeOrReceipts = 9999999.99,
    costOrMarket = CostValue,
    costMarketValue = 9999999.99,
    isPropertyDisposed = Yes,
    disposalDetails = Some(
      DisposalDetails(999999.99, "Name1 Surname1, Name2 Surname2, Name3 Surname3", No, No, No)
    )
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
    arrearsOutstandingPrYears = Yes,
    arrearsOutstandingPrYearsAmt = Some(11.1),
    outstandingYearEndAmount = 12312.12
  )

  protected val sippUnquotedSharesTransactionalDetail = SippUnquotedShares.TransactionDetail(
    sharesCompanyDetails = sippSharesCompanyDetail,
    acquiredFromName = "TestLongName TestLongSurname",
    totalCost = 9999999.99,
    independentValuation = Yes,
    totalDividendsIncome = 2000.2,
    sharesDisposed = Yes,
    sharesDisposalDetails = Some(sippSharesDisposalDetails)
  )

  protected val sippLandConnectedParty = SippLandConnectedParty(
    noOfTransactions = 1,
    version = None,
    transactionDetails = Some(List(sippLandConnectedPartyTransactionDetail))
  )

  private val sippOtherAssetsConnectedParty = SippOtherAssetsConnectedParty(
    noOfTransactions = 1,
    version = None,
    transactionDetails = Some(List(sippOtherAssetsConnectedPartyTransactionDetail))
  )

  private val sippLandArmsLength = SippLandArmsLength(
    noOfTransactions = 1,
    version = None,
    transactionDetails = Some(List(sippLandArmsLengthTransactionDetail))
  )

  private val sippTangibleProperty = SippTangibleProperty(
    noOfTransactions = 1,
    version = None,
    transactionDetails = Some(List(sippTangiblePropertyTransactionalDetail))
  )

  private val sippLoanOutstanding = SippLoanOutstanding(
    noOfTransactions = 1,
    version = None,
    transactionDetails = Some(List(sippLoanOutstandingTransactionalDetail))
  )

  private val sippUnquotedShares = SippUnquotedShares(
    noOfTransactions = 1,
    version = None,
    transactionDetails = Some(List(sippUnquotedSharesTransactionalDetail))
  )

  protected val etmpSippMemberAndTransactions: EtmpMemberAndTransactions = EtmpMemberAndTransactions(
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

  protected val fullSippPsrSubmissionEtmpRequest: SippPsrSubmissionEtmpRequest = SippPsrSubmissionEtmpRequest(
    reportDetails = reportDetails,
    accountingPeriodDetails = Some(accountingPeriodDetails),
    memberAndTransactions = Some(NonEmptyList.one(etmpSippMemberAndTransactions)),
    psrDeclaration = None
  )

  protected val landConnectedTransaction: LandOrConnectedPropertyApi.TransactionDetails =
    LandOrConnectedPropertyApi.TransactionDetails(
      nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
      nino = NinoType(nino = Some("nino"), reasonNoNino = None),
      acquisitionDate = LocalDate.of(2020, 1, 1),
      landOrPropertyInUK = Yes,
      addressDetails = AddressDetails(
        addressLine1 = "addressLine1",
        addressLine2 = "addressLine2",
        addressLine3 = None,
        addressLine4 = None,
        addressLine5 = None,
        ukPostCode = None,
        countryCode = "UK"
      ),
      registryDetails = RegistryDetails(registryRefExist = No, registryReference = None, noRegistryRefReason = None),
      acquiredFromName = "acquiredFromName",
      totalCost = 10,
      independentValuation = Yes,
      jointlyHeld = Yes,
      noOfPersons = None,
      residentialSchedule29A = Yes,
      isLeased = Yes,
      lesseeDetails = None,
      totalIncomeOrReceipts = 10,
      isPropertyDisposed = Yes,
      disposalDetails = None,
      transactionCount = None
    )

  val etmpDataWithLandConnectedTx: EtmpMemberAndTransactions = EtmpMemberAndTransactions(
    status = SectionStatus.New,
    version = None,
    memberDetails = MemberDetails(
      firstName = "firstName",
      lastName = "lastName",
      nino = Some("nino"),
      reasonNoNINO = None,
      dateOfBirth = LocalDate.of(2020, 1, 1)
    ),
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
    ),
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

  val existingEtmpDataWithLandConnectedTx: EtmpMemberAndTransactions = EtmpMemberAndTransactions(
    status = SectionStatus.New,
    version = Some("001"),
    memberDetails = MemberDetails(
      firstName = "firstName",
      lastName = "lastName",
      nino = Some("nino"),
      reasonNoNINO = None,
      dateOfBirth = LocalDate.of(2020, 1, 1)
    ),
    landConnectedParty = Some(
      SippLandConnectedParty(
        1,
        Some("001"),
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
    ),
    otherAssetsConnectedParty = None,
    landArmsLength = None,
    tangibleProperty = None,
    loanOutstanding = None,
    unquotedShares = None
  )

  lazy val sippAssetsFromConnectedPartyApi: AssetsFromConnectedPartyApi.TransactionDetails = {
    import sippOtherAssetsConnectedPartyTransactionDetail._

    AssetsFromConnectedPartyApi.TransactionDetails(
      nameDOB = NameDOB(
        sippMemberDetails.firstName,
        sippMemberDetails.lastName,
        sippMemberDetails.dateOfBirth
      ),
      nino = NinoType(sippMemberDetails.nino, sippMemberDetails.reasonNoNINO),
      acquisitionDate = acquisitionDate,
      assetDescription = assetDescription,
      acquisitionOfShares = acquisitionOfShares,
      sharesCompanyDetails = sharesCompanyDetails,
      acquiredFromName = acquiredFromName,
      totalCost = totalCost,
      independentValuation = independentValuation,
      tangibleSchedule29A = tangibleSchedule29A,
      totalIncomeOrReceipts = totalIncomeOrReceipts,
      isPropertyDisposed = isPropertyDisposed,
      disposalDetails = disposalDetails,
      disposalOfShares = disposalOfShares,
      noOfSharesHeld = noOfSharesHeld,
      transactionCount = None
    )
  }

  val sippTangibleApi = TangibleMoveablePropertyApi.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    acquisitionDate = LocalDate.of(2020, 1, 1),
    assetDescription = "Asset Description",
    acquiredFromName = "acquiredFromName",
    totalCost = 20.0,
    independentValuation = Yes,
    totalIncomeOrReceipts = 20.0,
    costOrMarket = MarketValue,
    costMarketValue = 20.0,
    isPropertyDisposed = No,
    disposalDetails = None,
    transactionCount = None
  )

  val sippUnquotedShareApi = UnquotedShareApi.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    sharesCompanyDetails = SharesCompanyDetails(
      companySharesName = "test",
      companySharesCRN = None,
      reasonNoCRN = None,
      sharesClass = "test",
      noOfShares = 1
    ),
    acquiredFromName = "test",
    totalCost = 1.0,
    independentValuation = Yes,
    totalDividendsIncome = 1.00,
    sharesDisposed = Yes,
    sharesDisposalDetails = None,
    transactionCount = None
  )

  val sippOutstandingLoansApi = OutstandingLoansApi.TransactionDetails(
    nameDOB = NameDOB(firstName = "firstName", lastName = "lastName", dob = LocalDate.of(2020, 1, 1)),
    nino = NinoType(nino = Some("nino"), reasonNoNino = None),
    loanRecipientName = "test",
    dateOfLoan = LocalDate.of(2020, 1, 1),
    amountOfLoan = 1,
    loanConnectedParty = Yes,
    repayDate = LocalDate.of(2020, 1, 1),
    interestRate = 1,
    loanSecurity = Yes,
    capitalRepayments = 1,
    arrearsOutstandingPrYears = Yes,
    arrearsOutstandingPrYearsAmt = Some(1),
    outstandingYearEndAmount = 1,
    transactionCount = None
  )

}
