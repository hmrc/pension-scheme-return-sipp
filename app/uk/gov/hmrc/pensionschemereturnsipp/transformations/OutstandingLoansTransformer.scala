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

import cats.data.NonEmptyList
import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoansRequest
import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoansRequest.TransactionDetail.TransformationOps
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.{EtmpMemberAndTransactions, SippLoanOutstanding}

object OutstandingLoansTransformer {
  def merge(
    updates: NonEmptyList[OutstandingLoansRequest.TransactionDetail],
    etmpData: List[EtmpMemberAndTransactions]
  ): List[EtmpMemberAndTransactions] =
    EtmpMemberAndTransactionsUpdater
      .merge[OutstandingLoansRequest.TransactionDetail, SippLoanOutstanding.TransactionDetail](
        updates,
        etmpData,
        _.toEtmp,
        (maybeTransactions, etmpMemberAndTransactions) =>
          etmpMemberAndTransactions.copy(
            loanOutstanding =
              maybeTransactions.map(transactions => SippLoanOutstanding(transactions.length, Some(transactions.toList)))
          )
      )
}
