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

trait PsrTestData {
  val assetFromConnectedPartyPayload: JsValue = Json.parse(
    """
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
