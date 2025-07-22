/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}

trait TestPayloads {

  val unquotedSharesPayload: JsValue = Json.parse(
    """
      {
      |  "reportDetails": {
      |    "pstr": "24000020IN",
      |    "version": "001",
      |    "status": "Compiled",
      |    "periodEnd": "2023-04-05",
      |    "periodStart": "2022-04-06",
      |    "schemeName": "Open Scheme 2",
      |    "memberTransactions": "Yes"
      |  },
      |  "transactions": [
      |    {
      |      "nino": {
      |        "nino": "RC256097C"
      |      },
      |      "totalCost": 998112.43,
      |      "totalDividendsIncome": 8814140.31,
      |      "sharesDisposed": "Yes",
      |      "nameDOB": {
      |        "firstName": "Berkley",
      |        "lastName": "Fawley",
      |        "dob": "1997-11-07"
      |      },
      |      "independentValuation": "Yes",
      |      "row": 3,
      |      "sharesCompanyDetails": {
      |        "companySharesName": "Kayveo",
      |        "sharesClass": "Streamlined holistic hierarchy",
      |        "noOfShares": 759509,
      |        "companySharesCRN": "JP396032"
      |      },
      |      "acquiredFromName": "Flashpoint",
      |      "sharesDisposalDetails": {
      |        "disposedShareAmount": 447823.48,
      |        "purchasersNames": "Twitterworks",
      |        "independentValuationDisposal": "Yes",
      |        "noOfSharesSold": 15,
      |        "disposalConnectedParty": "Yes",
      |        "noOfSharesHeld": 1
      |      }
      |    },
      |    {
      |      "nino": {
      |        "nino": "CX813530D"
      |      },
      |      "totalCost": 406269.83,
      |      "totalDividendsIncome": 5268250.83,
      |      "sharesDisposed": "No",
      |      "nameDOB": {
      |        "firstName": "Gerladina",
      |        "lastName": "Tuckwood",
      |        "dob": "1998-01-05"
      |      },
      |      "independentValuation": "No",
      |      "row": 4,
      |      "sharesCompanyDetails": {
      |        "companySharesName": "Bubblemix",
      |        "sharesClass": "Automated neutral secured line",
      |        "noOfShares": 622,
      |        "companySharesCRN": "XS405832"
      |      },
      |      "acquiredFromName": "Browsezoom"
      |    }
      |  ]
      |}
      |""".stripMargin
  )

  val tangibleMoveablePropertyPayload: JsValue = Json.parse(
    """
      {
      |  "reportDetails": {
      |    "pstr": "24000020IN",
      |    "version": "001",
      |    "status": "Compiled",
      |    "periodEnd": "2023-04-05",
      |    "periodStart": "2022-04-06",
      |    "schemeName": "Open Scheme 2",
      |    "memberTransactions": "Yes"
      |  },
      |  "transactions": [
      |    {
      |      "totalCost": 3692.51,
      |      "isPropertyDisposed": "Yes",
      |      "nameDOB": {
      |        "firstName": "Udale",
      |        "lastName": "Basley",
      |        "dob": "1988-05-04"
      |      },
      |      "independentValuation": "No",
      |      "acquisitionDate": "1999-06-03",
      |      "row": 3,
      |      "costMarketValue": 5385.82,
      |      "acquiredFromName": "Udale Basley",
      |      "costOrMarket": "Cost Value",
      |      "totalIncomeOrReceipts": 7750.94,
      |      "nino": {
      |        "nino": "SH735042A"
      |      },
      |      "assetDescription": "Automated neutral open architecture",
      |      "disposalDetails": {
      |        "disposedPropertyProceedsAmt": 495.99,
      |        "purchasersNames": "Udale Basley",
      |        "propertyFullyDisposed": "Yes",
      |        "independentValuationDisposal": "Yes",
      |        "anyPurchaserConnectedParty": "Yes"
      |      }
      |    },
      |    {
      |      "totalCost": 3454.78,
      |      "isPropertyDisposed": "No",
      |      "nameDOB": {
      |        "firstName": "Celie",
      |        "lastName": "Cornwell",
      |        "dob": "1968-03-07"
      |      },
      |      "independentValuation": "No",
      |      "assetDescription": "User-friendly bi-directional system engine",
      |      "acquisitionDate": "1995-08-01",
      |      "row": 4,
      |      "costMarketValue": 9795.28,
      |      "acquiredFromName": "Celie Cornwell",
      |      "costOrMarket": "Cost Value",
      |      "totalIncomeOrReceipts": 4883.79,
      |      "nino": {
      |        "nino": "SH137044D"
      |      }
      |    }
      |  ]
      |}
      |""".stripMargin
  )
  val outstandingLoansPayload: JsValue = Json.parse(
    """
      {
      |  "reportDetails": {
      |    "pstr": "24000020IN",
      |    "version": "001",
      |    "status": "Compiled",
      |    "periodEnd": "2023-04-05",
      |    "periodStart": "2022-04-06",
      |    "schemeName": "Open Scheme 2",
      |    "memberTransactions": "Yes"
      |  },
      |  "transactions": [
      |    {
      |      "row": 3,
      |      "dateOfLoan": "2024-02-24",
      |      "repayDate": "2005-02-09",
      |      "outstandingYearEndAmount": 688668.58,
      |      "arrearsOutstandingPrYears": "Yes",
      |      "interestRate": 6.12,
      |      "amountOfLoan": 8168912.9,
      |      "loanSecurity": "Yes",
      |      "nino": {
      |        "nino": "EP200893B"
      |      },
      |      "loanRecipientName": "Humberto Kop",
      |      "arrearsOutstandingPrYearsAmt": 250,
      |      "loanConnectedParty": "No",
      |      "capitalRepayments": 796691.4,
      |      "nameDOB": {
      |        "firstName": "Humberto",
      |        "lastName": "Kop",
      |        "dob": "1928-03-13"
      |      }
      |    },
      |    {
      |      "row": 4,
      |      "dateOfLoan": "2021-10-12",
      |      "repayDate": "2020-06-02",
      |      "outstandingYearEndAmount": 186769.32,
      |      "arrearsOutstandingPrYears": "Yes",
      |      "interestRate": 16.68,
      |      "amountOfLoan": 9231508.47,
      |      "loanSecurity": "No",
      |      "nino": {
      |        "nino": "AA716047A"
      |      },
      |      "loanRecipientName": "Raddie O'Hagirtie",
      |      "arrearsOutstandingPrYearsAmt": 100,
      |      "loanConnectedParty": "No",
      |      "capitalRepayments": 267262.08,
      |      "nameDOB": {
      |        "firstName": "Raddie",
      |        "lastName": "O'Hagirtie",
      |        "dob": "1987-03-04"
      |      }
      |    }
      |  ]
      |}
      |""".stripMargin
  )
  val landOrConnectedPartyPayload: JsValue = Json.parse(
    """
      {
      |  "reportDetails": {
      |    "pstr": "24000020IN",
      |    "version": "001",
      |    "status": "Compiled",
      |    "periodEnd": "2023-04-05",
      |    "periodStart": "2022-04-06",
      |    "schemeName": "Open Scheme 2",
      |    "memberTransactions": "Yes"
      |  },
      |  "transactions": [
      |    {
      |      "totalCost": 1038.09,
      |      "isPropertyDisposed": "No",
      |      "nameDOB": {
      |        "firstName": "Natale",
      |        "lastName": "Brodeau",
      |        "dob": "1964-12-10"
      |      },
      |      "independentValuation": "Yes",
      |      "acquisitionDate": "2008-05-16",
      |      "row": 3,
      |      "registryDetails": {
      |        "registryRefExist": "Yes",
      |        "registryReference": "AB123456"
      |      },
      |      "landOrPropertyInUK": "Yes",
      |      "acquiredFromName": "Natale Brodeau",
      |      "addressDetails": {
      |        "addressLine1": "6th Floor",
      |        "addressLine4": "Bendo",
      |        "addressLine3": "622 Daystar Point",
      |        "ukPostCode": "FJ678BX",
      |        "countryCode": "GB",
      |        "addressLine2": "Room 1504"
      |      },
      |      "residentialSchedule29A": "Yes",
      |      "totalIncomeOrReceipts": 4255.31,
      |      "nino": {
      |        "nino": "SH956162C"
      |      },
      |      "jointlyHeld": "No",
      |      "isLeased": "No"
      |    },
      |    {
      |      "totalCost": 7461.26,
      |      "isPropertyDisposed": "Yes",
      |      "noOfPersons": 6,
      |      "nameDOB": {
      |        "firstName": "Price",
      |        "lastName": "Shovelin",
      |        "dob": "2003-11-02"
      |      },
      |      "independentValuation": "Yes",
      |      "disposalDetails": {
      |        "disposedPropertyProceedsAmt": 4968,
      |        "purchasersNames": "Price Shovelin",
      |        "propertyFullyDisposed": "No",
      |        "independentValuationDisposal": "No",
      |        "anyPurchaserConnectedParty": "No"
      |      },
      |      "acquisitionDate": "1994-03-27",
      |      "row": 4,
      |      "registryDetails": {
      |        "registryRefExist": "Yes",
      |        "registryReference": "CD123456"
      |      },
      |      "landOrPropertyInUK": "No",
      |      "addressDetails": {
      |        "addressLine1": "16th Floor",
      |        "addressLine4": "4 Doe Crossing Avenue",
      |        "addressLine3": "9115 Thompson Road",
      |        "countryCode": "ID",
      |        "addressLine2": "3rd Floor"
      |      },
      |      "residentialSchedule29A": "No",
      |      "totalIncomeOrReceipts": 9355.27,
      |      "nino": {
      |        "nino": "SH584060B"
      |      },
      |      "acquiredFromName": "Price Shovelin",
      |      "lesseeDetails": {
      |        "numberOfLessees": 1,
      |        "anyLesseeConnectedParty": "No",
      |        "leaseGrantedDate": "2017-10-22",
      |        "annualLeaseAmount": 5679.04
      |      },
      |      "jointlyHeld": "Yes",
      |      "isLeased": "Yes"
      |    }
      |  ]
      |}
      |""".stripMargin
  )
  val landArmsLengthPayload: JsValue = Json.parse(
    """
        {
      |  "reportDetails": {
      |    "pstr": "24000020IN",
      |    "version": "001",
      |    "status": "Compiled",
      |    "periodEnd": "2023-04-05",
      |    "periodStart": "2022-04-06",
      |    "schemeName": "Open Scheme 2",
      |    "memberTransactions": "Yes"
      |  },
      |  "transactions": [
      |    {
      |      "totalCost": 18716.7,
      |      "isPropertyDisposed": "No",
      |      "noOfPersons": 10,
      |      "nameDOB": {
      |        "firstName": "Ruthann",
      |        "lastName": "McLewd",
      |        "dob": "2018-09-10"
      |      },
      |      "independentValuation": "Yes",
      |      "acquisitionDate": "1990-11-06",
      |      "row": 3,
      |      "registryDetails": {
      |        "registryRefExist": "Yes",
      |        "registryReference": "AB123456"
      |      },
      |      "landOrPropertyInUK": "Yes",
      |      "addressDetails": {
      |        "addressLine1": "Room 916",
      |        "addressLine4": "Quinta",
      |        "addressLine3": "17 Crownhardt Point",
      |        "ukPostCode": "HS499ZX",
      |        "countryCode": "GB",
      |        "addressLine2": "Room 1589"
      |      },
      |      "residentialSchedule29A": "No",
      |      "totalIncomeOrReceipts": 23370.2,
      |      "nino": {
      |        "nino": "SH538748B"
      |      },
      |      "acquiredFromName": "Ruthann McLewd",
      |      "lesseeDetails": {
      |        "numberOfLessees": 1,
      |        "anyLesseeConnectedParty": "No",
      |        "leaseGrantedDate": "2015-11-23",
      |        "annualLeaseAmount": 2175.35
      |      },
      |      "jointlyHeld": "Yes",
      |      "isLeased": "Yes"
      |    },
      |    {
      |      "totalCost": 23548.43,
      |      "isPropertyDisposed": "Yes",
      |      "nameDOB": {
      |        "firstName": "Lancelot",
      |        "lastName": "Sanderson",
      |        "dob": "2023-12-01"
      |      },
      |      "independentValuation": "No",
      |      "disposalDetails": {
      |        "disposedPropertyProceedsAmt": 18393.57,
      |        "purchasersNames": "Lancelot Sanderson",
      |        "propertyFullyDisposed": "No",
      |        "independentValuationDisposal": "No",
      |        "anyPurchaserConnectedParty": "Yes"
      |      },
      |      "acquisitionDate": "1991-09-18",
      |      "row": 4,
      |      "registryDetails": {
      |        "registryRefExist": "Yes",
      |        "registryReference": "CD123456"
      |      },
      |      "landOrPropertyInUK": "No",
      |      "acquiredFromName": "Lancelot Sanderson",
      |      "addressDetails": {
      |        "addressLine1": "PO Box 68389",
      |        "addressLine4": "97 Prairie Rose Junction",
      |        "addressLine3": "6 Harper Alley",
      |        "countryCode": "ID",
      |        "addressLine2": "Suite 60"
      |      },
      |      "residentialSchedule29A": "No",
      |      "totalIncomeOrReceipts": 19260.3,
      |      "nino": {
      |        "nino": "SH617878A"
      |      },
      |      "jointlyHeld": "No",
      |      "isLeased": "No"
      |    }
      |  ]
      |}
      |""".stripMargin
  )

  val assetFromConnectedPartyPayload: JsValue = Json.parse("""
        {
      |  "reportDetails": {
      |    "pstr": "24000020IN",
      |    "status": "Submitted",
      |    "periodStart": "2023-01-01",
      |    "periodEnd": "2023-12-31",
      |    "schemeName": "Sample Scheme Name",
      |    "version": "1.0",
      |    "memberTransactions": "Yes"
      |  },
      |  "transactions": [
      |      {
      |        "nameDOB": {
      |          "firstName": "John",
      |          "lastName": "Doe",
      |          "dob": "1980-01-01"
      |        },
      |        "nino": {
      |          "nino": "AB123456C",
      |          "reasonNoNino": null
      |        },
      |        "acquisitionDate": "2023-05-15",
      |        "assetDescription": "Real Estate",
      |        "acquisitionOfShares": "No",
      |        "shareCompanyDetails": null,
      |        "acquiredFromName": "Jane Smith",
      |        "totalCost": 250000.00,
      |        "independentValuation": "Yes",
      |        "tangibleSchedule29A": "No",
      |        "totalIncomeOrReceipts": 5000.00,
      |        "isPropertyDisposed": "Yes",
      |        "disposalDetails": {
      |          "disposedPropertyProceedsAmt": 300000.00,
      |          "purchasersNames": "Buyer Inc.",
      |          "anyPurchaserConnectedParty": "No",
      |          "independentValuationDisposal": "Yes",
      |          "propertyFullyDisposed": "Yes"
      |        },
      |        "disposalOfShares": "No",
      |        "noOfSharesHeld": null
      |      },
      |      {
      |        "nameDOB": {
      |          "firstName": "Alice",
      |          "lastName": "Johnson",
      |          "dob": "1975-03-22"
      |        },
      |        "nino": {
      |          "nino": null,
      |          "reasonNoNino": "Not applicable"
      |        },
      |        "acquisitionDate": "2023-07-10",
      |        "assetDescription": "Company Shares",
      |        "acquisitionOfShares": "Yes",
      |        "shareCompanyDetails": {
      |          "companySharesName": "Tech Corp",
      |          "companySharesCRN": "123456789",
      |          "reasonNoCRN": null,
      |          "sharesClass": "A",
      |          "noOfShares": 100
      |        },
      |        "acquiredFromName": "Bob Brown",
      |        "totalCost": 50000.00,
      |        "independentValuation": "No",
      |        "tangibleSchedule29A": "Yes",
      |        "totalIncomeOrReceipts": 2000.00,
      |        "isPropertyDisposed": "No",
      |        "disposalDetails": null,
      |        "disposalOfShares": "No",
      |        "noOfSharesHeld": 100
      |      },
      |      {
      |        "nameDOB": {
      |          "firstName": "John",
      |          "lastName": "Doe",
      |          "dob": "1980-01-01"
      |        },
      |        "nino": {
      |          "nino": "AB123456C",
      |          "reasonNoNino": null
      |        },
      |        "acquisitionDate": "2023-07-10",
      |        "assetDescription": "Company Shares",
      |        "acquisitionOfShares": "Yes",
      |        "shareCompanyDetails": {
      |          "companySharesName": "Tech Corp",
      |          "companySharesCRN": "123456789",
      |          "reasonNoCRN": null,
      |          "sharesClass": "A",
      |          "noOfShares": 100
      |        },
      |        "acquiredFromName": "Bob Brown",
      |        "totalCost": 50000.00,
      |        "independentValuation": "No",
      |        "tangibleSchedule29A": "Yes",
      |        "totalIncomeOrReceipts": 2000.00,
      |        "isPropertyDisposed": "No",
      |        "disposalDetails": null,
      |        "disposalOfShares": "No",
      |        "noOfSharesHeld": 100
      |      },
      |    {
      |        "nameDOB": {
      |          "firstName": "John",
      |          "lastName": "Doe",
      |          "dob": "1980-01-01"
      |        },
      |        "nino": {
      |          "nino": "AB123456C",
      |          "reasonNoNino": null
      |        },
      |        "acquisitionDate": "2023-07-10",
      |        "assetDescription": "Company Shares",
      |        "acquisitionOfShares": "Yes",
      |        "shareCompanyDetails": {
      |          "companySharesName": "Tech Corp",
      |          "companySharesCRN": "123456789",
      |          "reasonNoCRN": null,
      |          "sharesClass": "A",
      |          "noOfShares": 100
      |        },
      |        "acquiredFromName": "Bob Brown",
      |        "totalCost": 50000.00,
      |        "independentValuation": "No",
      |        "tangibleSchedule29A": "Yes",
      |        "totalIncomeOrReceipts": 2000.00,
      |        "isPropertyDisposed": "No",
      |        "disposalDetails": null,
      |        "disposalOfShares": "No",
      |        "noOfSharesHeld": 100
      |      }
      |    ]
      |}
      |""".stripMargin)
}
