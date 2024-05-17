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

import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoan
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.YesNo
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.{EtmpSippConnectedOrUnconnectedType, SectionStatus}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, SippLoanOutstanding}

import javax.inject.{Inject, Singleton}

@Singleton
class OutstandingLoanTransformer @Inject() {
  def transform(list: List[OutstandingLoan.TransactionDetail]): List[EtmpMemberAndTransactions] =
    list
      .groupMap(p => p.nameDOB -> p.nino)(transformSingle)
      .map {
        case ((nameDoB, nino), transactions) =>
          val loanOutstanding = SippLoanOutstanding(transactions.length, Some(transactions).filter(_.nonEmpty))
          EtmpMemberAndTransactions(
            status = SectionStatus.New,
            version = None,
            memberDetails = toMemberDetails(nameDoB, nino),
            landConnectedParty = None,
            otherAssetsConnectedParty = None,
            landArmsLength = None,
            tangibleProperty = None,
            loanOutstanding = Some(loanOutstanding),
            unquotedShares = None
          )
      }
      .toList

  private def transformSingle(
    property: OutstandingLoan.TransactionDetail
  ): SippLoanOutstanding.TransactionDetail =
    SippLoanOutstanding.TransactionDetail(
      loanRecipientName = property.loanRecipientName,
      dateOfLoan = property.dateOfLoan,
      amountOfLoan = property.amountOfLoan,
      loanConnectedParty =
        if (property.loanConnectedParty == YesNo.Yes) EtmpSippConnectedOrUnconnectedType.Connected
        else EtmpSippConnectedOrUnconnectedType.Unconnected, //TODO change api type
      repayDate = property.repayDate,
      interestRate = property.interestRate,
      loanSecurity = property.loanSecurity.toEtmp,
      capitalRepayments = property.capitalRepayments,
      interestPayments = property.interestPayments,
      arrearsOutstandingPrYears = property.arrearsOutstandingPrYears.toEtmp,
      outstandingYearEndAmount = property.outstandingYearEndAmount
    )
}
