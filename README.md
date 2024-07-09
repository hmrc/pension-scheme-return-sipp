# pension-scheme-return-sipp

Pension scheme return sipp is the backend service for pension scheme return sipp frontend which is a feature on manage
your sipp pension.

***

## Technical documentation

### Before running the app

Run the following command to start all of the related services for this project:

```bash
sm2 -start PSR_ALL
```

Included in the above command is `PENSION_SCHEME_RETURN_SIPP`, which is this repository's most recent release.
If you want to run your local version of this code instead, run:

```bash
sm2 -stop PENSION_SCHEME_RETURN_SIPP
```

then:

```bash
sbt 'run'
```

Note: this service runs on port 10704 by default, but a different port (e.g. 17000) may be specified, as shown in the
example below:

```bash
sbt 'run 17000'
```

***

### Running the test suite

```bash
sbt clean compile coverage it/test test coverageReport
```

or

You can execute the [runtests.sh](runtests.sh) file to run the tests and generate coverage report easily.

```bash
/bin/bash ./runtests.sh
```

### Useful links

- [Confluence](https://confluence.tools.tax.service.gov.uk/display/PSR/Pension+Scheme+Return+Home)

***

## API Mapping

In down below all the API modelled with API version 1.0.3 | 27-10-2023. There is no guarantee to have exact match. That
part will be updated when API swagger file updated by ETMP development team.

### Interest in Land or Property

 Questions	                                                                                                                                        | Etmp Request Model                                                             | 	Notes                                                                               
---------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------- 
 First name of scheme member                                                                                                                       | memberDetails.firstName                                                        
 Last name of scheme member                                                                                                                        | memberDetails.lastName                                                         
 Member date of birth                                                                                                                              | memberDetails.dateOfBirth                                                      
 Member National Insurance number                                                                                                                  | memberDetails.nino                                                             
 If no National Insurance number for member, give reason                                                                                           | memberDetails.reasonNoNINO                                                     
 How many land or property transactions did the member make during the tax year and not reported in a previous return for this member?             | landConnectedParty.noOfTransactions                                            
 What is the date of acquisition?                                                                                                                  | landConnectedParty.transactionDetails[X].acquisitionDate                       
 Is the land or property in the UK?                                                                                                                | landConnectedParty.transactionDetails[X].landOrPropertyInUK                    
 Enter the UK address line 1 of the land or property                                                                                               | landConnectedParty.transactionDetails[X].addressDetails.addressLine1           
 Enter UK address line 2 of the land or property                                                                                                   | landConnectedParty.transactionDetails[X].addressDetails.addressLine2           
 Enter UK address line 3 of the land or property                                                                                                   | landConnectedParty.transactionDetails[X].addressDetails.addressLine3           
 Enter name UK town or city of the land or property                                                                                                | landConnectedParty.transactionDetails[X].addressDetails.addressLine4           | or addressLine5 ???                                                                  
 Enter post code of the land or property                                                                                                           | landConnectedParty.transactionDetails[X].addressDetails.ukPostCode             
 Enter the non-UK address line 1 of the land or property                                                                                           | landConnectedParty.transactionDetails[X].addressDetails.addressLine1           
 Enter non-UK address line 2 of the land or property                                                                                               | landConnectedParty.transactionDetails[X].addressDetails.addressLine2           
 Enter non-UK address line 3 of the land or property                                                                                               | landConnectedParty.transactionDetails[X].addressDetails.addressLine3           
 Enter non-UK address line 4 of the land or property                                                                                               | landConnectedParty.transactionDetails[X].addressDetails.addressLine4           
 Enter non-UK country name of the land or property                                                                                                 | landConnectedParty.transactionDetails[X].addressDetails.countryCode            
 Is there a Land Registry reference in respect of the land or property?                                                                            | landConnectedParty.transactionDetails[X].registryDetails.registryRefExist      | If yes, we don't have reference                                                      
 If no Land Registry reference, enter reason                                                                                                       | landConnectedParty.transactionDetails[X].registryReference.noRegistryRefReason 
 Who was the land or property acquired from?                                                                                                       | landConnectedParty.transactionDetails[X].acquiredFromName                      
 What is the total cost of the land or property acquired?                                                                                          | landConnectedParty.transactionDetails[X].totalCost                             
 Is the transaction supported by an Independent Valuation?                                                                                         | landConnectedParty.transactionDetails[X].independentValuation                  
 Is the property held jointly?                                                                                                                     | landConnectedParty.transactionDetails[X].jointlyHeld                           
 How many persons jointly own the property?                                                                                                        | landConnectedParty.transactionDetails[X].noOfPersonsIfJointlyHeld              
 Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?                                             | landConnectedParty.transactionDetails[X].residentialSchedule29A                
 Is the land or property leased?                                                                                                                   | landConnectedParty.transactionDetails[X].isLeased                              
 How many lessees are there for the land or property?                                                                                              | landConnectedParty.transactionDetails[X].noOfPersonsForLessees                 
 Are any of the lessees a connected party?                                                                                                         | landConnectedParty.transactionDetails[X].anyOfLesseesConnected                 
 What date was the lease granted?                                                                                                                  | landConnectedParty.transactionDetails[X].leaseGrantedDate                      | leaseGrantedDate & annualLeaseAmount can be in an object? Will see when Api is ready 
 What is the annual lease amount?                                                                                                                  | landConnectedParty.transactionDetails[X].annualLeaseAmount                     
 What is the total amount of income and receipts in respect of the land or property during tax year                                                | landConnectedParty.transactionDetails[X].totalIncomeOrReceipts                 
 Were any disposals made on this?                                                                                                                  | landConnectedParty.transactionDetails[X].isPropertyDisposed                    
 What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land | landConnectedParty.transactionDetails[X].disposedPropertyProceedsAmt           | All can be in an object -  we will see when Api is ready                             
 If disposals were made on this, what are the names of the purchasers?                                                                             | landConnectedParty.transactionDetails[X].purchaserNamesIfDisposed              
 Are any of the purchasers connected parties?                                                                                                      | landConnectedParty.transactionDetails[X].anyOfPurchaserConnected               
 Is the transaction supported by an independent valuation                                                                                          | landConnectedParty.transactionDetails[X].independentValuationDisposal          | Previous version of API has that Valuation typo                                       
 Has the land or property been fully disposed of?                                                                                                  | landConnectedParty.transactionDetails[X].propertyFullyDisposed                 

### Arms Length Land or Property

 Questions                                                                                                                                         | 	Etmp Request Model                                                         | 	Notes                                                                               
---------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|--------------------------------------------------------------------------------------
 First name of scheme member                                                                                                                       | 	memberDetails.firstName                                                    
 Last name of scheme member                                                                                                                        | 	memberDetails.lastName                                                     
 Member date of birth                                                                                                                              | 	memberDetails.dateOfBirth                                                  
 Member National Insurance number                                                                                                                  | 	memberDetails.nino                                                         
 If no National Insurance number for member, give reason                                                                                           | 	memberDetails.reasonNoNINO                                                 
 How many land or property transactions were made during the tax year and not reported in a previous return for this member?                       | 	landArmsLength.noOfTransactions                                            
 What is the date of acquisition?                                                                                                                  | 	landArmsLength.transactionDetails[X].acquisitionDate                       
 Is the land or property in the UK?                                                                                                                | 	landArmsLength.transactionDetails[X].landOrPropertyInUK                    
 Enter the UK address line 1 of the land or property                                                                                               | 	landArmsLength.transactionDetails[X].addressDetails.addressLine1           
 Enter UK address line 2 of the land or property                                                                                                   | 	landArmsLength.transactionDetails[X].addressDetails.addressLine2           
 Enter UK address line 3 of the land or property                                                                                                   | 	landArmsLength.transactionDetails[X].addressDetails.addressLine3           
 Enter name UK town or city of the land or property                                                                                                | 	landArmsLength.transactionDetails[X].addressDetails.addressLine4           | or addressLine5 ???                                                                  
 Enter post code of the land or property                                                                                                           | 	landArmsLength.transactionDetails[X].addressDetails.ukPostCode             
 Enter the non-UK address line 1 of the land or property                                                                                           | 	landArmsLength.transactionDetails[X].addressDetails.addressLine1           
 Enter non-UK address line 2 of the land or property                                                                                               | 	landArmsLength.transactionDetails[X].addressDetails.addressLine2           
 Enter non-UK address line 3 of the land or property                                                                                               | 	landArmsLength.transactionDetails[X].addressDetails.addressLine3           
 Enter non-UK address line 4 of the land or property                                                                                               | 	landArmsLength.transactionDetails[X].addressDetails.addressLine4           
 Enter non-UK country name of the land or property                                                                                                 | 	landArmsLength.transactionDetails[X].addressDetails.countryCode            
 Is there a Land Registry reference in respect of the land or property?                                                                            | 	landArmsLength.transactionDetails[X].registryDetails.registryRefExist      | 	If yes, we don't have reference                                                     
 If no Land Registry reference, enter reason                                                                                                       | 	landArmsLength.transactionDetails[X].registryReference.noRegistryRefReason 
 Who was the land or property acquired from?                                                                                                       | 	landArmsLength.transactionDetails[X].acquiredFromName                      
 What is the total cost of the land or property acquired?                                                                                          | 	landArmsLength.transactionDetails[X].totalCost                             
 Is the transaction supported by an Independent Valuation?                                                                                         | 	landArmsLength.transactionDetails[X].independentValuation                  
 Is the property held jointly?                                                                                                                     | 	landArmsLength.transactionDetails[X].jointlyHeld                           
 How many persons jointly own the property?                                                                                                        | 	landArmsLength.transactionDetails[X].noOfPersonsIfJointlyHeld              
 Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?                                             | 	landArmsLength.transactionDetails[X].residentialSchedule29A                
 Is the land or property leased?                                                                                                                   | 	landArmsLength.transactionDetails[X].isLeased                              
 What are the names of the lessees?                                                                                                                | 	landArmsLength.transactionDetails[X].noOfPersonsForLessees                 
 Are any of the lessees a connected party?                                                                                                         | 	landArmsLength.transactionDetails[X].anyOfLesseesConnected                 
 What date was the lease granted?                                                                                                                  | 	landArmsLength.transactionDetails[X].leaseGrantedDate	                     | leaseGrantedDate & annualLeaseAmount can be in an object? Will see when Api is ready 
 What is the annual lease amount?                                                                                                                  | 	landArmsLength.transactionDetails[X].annualLeaseAmount                     
 What is the total amount of income and receipts in respect of the land or property during tax year                                                | 	landArmsLength.transactionDetails[X].totalIncomeOrReceipts                 
 Was any disposal of the land or property made during the year?                                                                                    | 	landArmsLength.transactionDetails[X].isPropertyDisposed                    
 What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land | 	landArmsLength.transactionDetails[X].disposedPropertyProceedsAmt           | 	All can be in an object? Will see when Api is ready                                 
 If disposals were made on this, what are the names of the purchasers?                                                                             | 	landArmsLength.transactionDetails[X].purchaserNamesIfDisposed              
 Are any of the purchasers connected parties?                                                                                                      | 	landArmsLength.transactionDetails[X].anyOfPurchaserConnected               
 Is the transaction supported by an independent valuation                                                                                          | 	landArmsLength.transactionDetails[X].independentValuationDisposal                                                 
 Has the land or property been fully disposed of?                                                                                                  | 	landArmsLength.transactionDetails[X].propertyFullyDisposed                 

### Tangible moveable property

 Questions                                                                                                                                             | 	Etmp Request Model                                                                   | 	Notes                                               
-------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|------------------------------------------------------
 First name of scheme member                                                                                                                           | 	memberDetails.firstName                                                              
 Last name of scheme member                                                                                                                            | 	memberDetails.lastName                                                               
 Member date of birth                                                                                                                                  | 	memberDetails.dateOfBirth                                                            
 Member National Insurance number                                                                                                                      | 	memberDetails.nino                                                                   
 If no National Insurance number for member, give reason                                                                                               | 	memberDetails.reasonNoNINO                                                           
 How many transactions of tangible moveable property were made during the tax year and not reported in a previous return for this member?              | 	tangibleProperty.noOfTransactions                                                    
 Description of asset                                                                                                                                  | 	tangibleProperty.transactionDetails[X].assetDescription                              
 What was the date of acquisiiton of the asset?                                                                                                        | 	tangibleProperty.transactionDetails[X].acquisitionDate                               
 What was the total cost of the asset acquired?                                                                                                        | 	tangibleProperty.transactionDetails[X].totalCost                                     
 Who was the asset acquired from?                                                                                                                      | 	tangibleProperty.transactionDetails[X].acquiredFromName                              
 Is this transaction supported by an Independent Valuation?                                                                                            | 	tangibleProperty.transactionDetails[X].independentValuation                           
 What is the total amount of income and receipts received in respect of the asset during tax year?                                                     | 	tangibleProperty.transactionDetails[X].totalIncomeOrReceipts                         
 Is the total cost value or market value of the asset?                                                                                                 | 	tangibleProperty.transactionDetails[X].costOrMarket                                  
 What is the total cost value or market value of the asset, as at 6 April [of the tax year that you are submitting]                                    | 	tangibleProperty.transactionDetails[X].costMarketValue                               
 During the year was there any disposal of the tangible moveable property made?                                                                        | 	tangibleProperty.transactionDetails[X].isPropertyDisposed                            
 If yes, there was disposal of tangible moveable property - what is the total amount of consideration received from the sale or disposal of the asset? | 	tangibleProperty.transactionDetails[X].disposedPropertyProceedsAmt                   | 	All can be in an object? Will see when Api is ready 
 Names of purchasers                                                                                                                                   | 	tangibleProperty.transactionDetails[X].purchaserNamesIfDisposed                      
 Are any of the purchasers connected parties?                                                                                                          | 	tangibleProperty.transactionDetails[X].anyOfPurchaserConnected                       
 Was the transaction supported by an independent valuation?                                                                                            | 	tangibleProperty.transactionDetails[X].independentValuationDisposal	Again that typo ! 
 Is any part of the asset still held?                                                                                                                  | 	tangibleProperty.transactionDetails[X].propertyFullyDisposed                         

### Outstanding Loans

 Questions                                                                                                                   | 	Etmp Request Model                                              | 	Notes 
-----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|--------
 First name of scheme member                                                                                                 | 	memberDetails.firstName                                         
 Last name of scheme member                                                                                                  | 	memberDetails.lastName                                          
 Member date of birth                                                                                                        | 	memberDetails.dateOfBirth                                       
 Member National Insurance number                                                                                            | 	memberDetails.nino                                              
 If no National Insurance number for member, give reason                                                                     | 	memberDetails.reasonNoNINO                                      
 How many separate loans were made or outstanding during the tax year and not reported in a previous return for this member? | 	loanOutstanding.noOfTransactions                                
 What is the name of the recipient of loan?                                                                                  | 	loanOutstanding.transactionDetails[X].loanRecipientName         
 What was the date of loan?                                                                                                  | 	loanOutstanding.transactionDetails[X].dateOfLoan                
 What is the amount of the loan?                                                                                             | 	loanOutstanding.transactionDetails[X].amountOfLoan              
 Is the loan associated with a connected party?                                                                              | 	loanOutstanding.transactionDetails[X].loanConnectedParty        
 What is the repayment date of the loan?                                                                                     | 	loanOutstanding.transactionDetails[X].repayDate                 
 What is the Interest Rate for the loan?                                                                                     | 	loanOutstanding.transactionDetails[X].interestRate              
 Has security been given for the loan?                                                                                       | 	loanOutstanding.transactionDetails[X].loanSecurity              
 In respect of this loan, what Capital Repayments have been received by the scheme during the year?                          | 	loanOutstanding.transactionDetails[X].capitalRepayments         
 In respect of this loan, what interest payments have been received by the scheme during the year?                           | 	loanOutstanding.transactionDetails[X].interestPayments          
 In respect of this loan, are there any arrears outstanding from previous years                                              | 	loanOutstanding.transactionDetails[X].arrearsOutstandingPrYears 
 In respect of this loan, what is the amount outstanding at the year end?                                                    | 	loanOutstanding.transactionDetails[X].outstandingYearEndAmount  

### Unquoted Shares

 Questions                                                                                                                         | 	Etmp Request Model                                                                     | 	Notes                                                                               
-----------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------
 First name of scheme member                                                                                                       | 	memberDetails.firstName                                                                
 Last name of scheme member                                                                                                        | 	memberDetails.lastName                                                                 
 Member date of birth                                                                                                              | 	memberDetails.dateOfBirth                                                              
 Member National Insurance number                                                                                                  | 	memberDetails.nino                                                                     
 If no National Insurance number for member, give reason                                                                           | 	memberDetails.reasonNoNINO                                                             
 How many share transactions were made during the tax year and not reported in a previous return for this member?                  | 	unquotedShares.noOfTransactions                                                        
 What is the name of the company to which the shares relate?                                                                       | 	unquotedShares.transactionDetails[X].sharesCompanyDetails.companySharesName            
 What is the CRN of the company to which the shares relate?                                                                        | 	unquotedShares.transactionDetails[X].sharesCompanyDetails.companySharesCRN             
 If no CRN of the company to which the shares relate, enter reason                                                                 | 	unquotedShares.transactionDetails[X].sharesCompanyDetails.reasonNoCRN                  
 What are the class of shares acquired?                                                                                            | 	unquotedShares.transactionDetails[X].sharesCompanyDetails.sharesClass                  
 What are the total number of shares acquired?                                                                                     | 	unquotedShares.transactionDetails[X].sharesCompanyDetails.noOfShares                   
 Who were the shares acquired from?                                                                                                | 	unquotedShares.transactionDetails[X].acquiredFromName                                  
 What was the total cost of shares acquired, or subscribed for?                                                                    | 	unquotedShares.transactionDetails[X].totalCost                                         
 In respect of of these shares, was this transaction supported by an independent valuation?                                        | 	unquotedShares.transactionDetails[X].independentValuation                               
 If the transaction was supported by an independent valuation, what was the number of shares sold?                                 | 	unquotedShares.transactionDetails[X].noOfSharesSold                                    
 What was the total amount of dividends or other income received during the year?                                                  | 	unquotedShares.transactionDetails[X].totalDividendsIncome                              
 Was any disposal of shares made during the tax year                                                                               | 	unquotedShares.transactionDetails[X].sharesDisposed                                    
 If disposal of shares were made during the tax year - what is the total amount of consideration received from the sale of shares? | 	unquotedShares.transactionDetails[X].sharesDisposalDetails.disposedShareAmount         
 What was the name of the purchaser of the shares?                                                                                 | 	unquotedShares.transactionDetails[X].sharesDisposalDetails.purchaserName               
 Was the disposal made to a connected party or connected parties?                                                                  | 	unquotedShares.transactionDetails[X].sharesDisposalDetails.disposalConnectedParty      
 Was the transaction supported by an independent valuation?                                                                        | 	unquotedShares.transactionDetails[X].sharesDisposalDetails.independentValuationDisposal 
 What is the total number of shares now held?                                                                                      | 	unquotedShares.transactionDetails[X].noOfSharesHeld                                    | 	Maybe it can be in that sharesDisposalDetails in future but now it is outside of it 

### Any Asset Other than Land or Property

 Questions                                                                                                         | 	Etmp Request Model                                                                     | 	Notes                                             
-------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|----------------------------------------------------
 First name of scheme member                                                                                       | 	memberDetails.firstName                                                                
 Last name of scheme member                                                                                        | 	memberDetails.lastName                                                                 
 Member date of birth                                                                                              | 	memberDetails.dateOfBirth                                                              
 Member National Insurance number                                                                                  | 	memberDetails.nino                                                                     
 If no National Insurance number for member, give reason                                                           | 	memberDetails.reasonNoNINO                                                             
 How many asset transactions were made during the tax year and not reported in a previous return for this member?  | 	otherAssetsConnectedParty.noOfTransactions                                             
 What was the date the asset was acquired?                                                                         | 	otherAssetsConnectedParty.transactionDetails[X].acquisitionDate                        
 Description of asset                                                                                              | 	otherAssetsConnectedParty.transactionDetails[X].assetDescription                       
 Was this an acquisition of shares?                                                                                | 	otherAssetsConnectedParty.transactionDetails[X].acquisitionOfShares                    
 If yes, What is the name of the company to which the shares relate?                                               | 	otherAssetsConnectedParty.transactionDetails[X].sharesCompanyDetails.companySharesName 
 What is the CRN of the company that has the shares?                                                               | 	otherAssetsConnectedParty.transactionDetails[X].sharesCompanyDetails.companySharesCRN  
 If no CRN, enter reason you do not have this                                                                      | 	otherAssetsConnectedParty.transactionDetails[X].sharesCompanyDetails.reasonNoCRN       
 What are the class of shares?                                                                                     | 	otherAssetsConnectedParty.transactionDetails[X].sharesCompanyDetails.sharesClass       
 What are the total number of shares acquired or received/held in respect of this transaction?                     | 	otherAssetsConnectedParty.transactionDetails[X].sharesCompanyDetails.noOfShares        
 Who was the asset acquired from?                                                                                  | 	otherAssetsConnectedParty.transactionDetails[X].acquiredFromName                       
 What was the total cost of the asset acquired?                                                                    | 	otherAssetsConnectedParty.transactionDetails[X].totalCost                              
 Is this transaction supported by an Independent Valuation                                                         | 	otherAssetsConnectedParty.transactionDetails[X].independentValuation                    
 Is any part of the asset Tangible Moveable Property as defined by Schedule 29a Finance Act 2004?                  | 	otherAssetsConnectedParty.transactionDetails[X].tangibleSchedule29A                    
 What is the total amount of income and receipts received in respect of the asset during tax year                  | 	otherAssetsConnectedParty.transactionDetails[X].totalIncomeOrReceipts                  
 During the year was any disposal of the asset made?                                                               | 	otherAssetsConnectedParty.transactionDetails[X].isPropertyDisposed                     
 If disposals were made what is the total amount of consideration received from the sale or disposal of the asset? | 	otherAssetsConnectedParty.transactionDetails[X].disposedPropertyProceedsAmt            | 	Again all can be in single object, we will see :) 
 Names of purchasers                                                                                               | 	otherAssetsConnectedParty.transactionDetails[X].purchaserNamesIfDisposed               
 Are any of the purchasers connected parties?                                                                      | 	otherAssetsConnectedParty.transactionDetails[X].anyOfPurchaserConnected                
 Was the transaction supported by an independent valuation?                                                        | 	otherAssetsConnectedParty.transactionDetails[X].independentValuationDisposal            
 Was there disposal of shares?                                                                                     | 	otherAssetsConnectedParty.transactionDetails[X].disposalOfShares                       
 If there were disposals of shares what is the total number of shares now held                                     | 	otherAssetsConnectedParty.transactionDetails[X].noOfSharesHeld                         
 If no disposals of shares were made has the asset been fully disposed of?                                         | 	otherAssetsConnectedParty.transactionDetails[X].propertyFullyDisposed                  

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").