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

import org.scalacheck.Gen
import uk.gov.hmrc.pensionschemereturnsipp.models.api.OutstandingLoan
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common.{NameDOB, NinoType, YesNo}

import java.time.{Instant, LocalDate, ZoneId}
import scala.util.Random

object Generators {
  val localDate = Gen.chooseNum(LocalDate.MIN.toEpochDay, LocalDate.MAX.toEpochDay).map(LocalDate.ofEpochDay)

  val yesNo: Gen[YesNo] = Gen.oneOf(YesNo.Yes, YesNo.No)
  val ninoType: Gen[NinoType] = Gen.option(Gen.asciiPrintableStr).flatMap {
    case Some(value) => Gen.const(NinoType(Some(value), None))
    case None => Gen.asciiPrintableStr.map(reason => NinoType(None, Some(reason)))
  }

  def amount(value: Double = Random.between(0.0, 999999.99)): Gen[Double] = Gen.const(value)

  val genNameDOB: Gen[NameDOB] = for {
    firstName <- Gen.asciiPrintableStr.map(_.take(20))
    lastName <- Gen.asciiPrintableStr.map(_.take(20))
    dob <- localDate
  } yield NameDOB(firstName, lastName, dob)

  val outstandingLoanGenerator: Gen[OutstandingLoan.TransactionDetail] =
    for {
      nameDOB <- genNameDOB
      nino <- ninoType
      loanRecipientName <- Gen.asciiPrintableStr
      dateOfLoan <- localDate
      amountOfLoan <- amount()
      loanConnectedParty <- yesNo
      repayDate <- localDate
      interestRate <- amount()
      loanSecurity <- yesNo
      capitalRepayments <- amount()
      interestPayments <- amount()
      arrearsOutstandingPrYears <- yesNo
      outstandingYearEndAmount <- amount()
    } yield OutstandingLoan.TransactionDetail(
      nameDOB,
      nino,
      loanRecipientName,
      dateOfLoan,
      amountOfLoan,
      loanConnectedParty,
      repayDate,
      interestRate,
      loanSecurity,
      capitalRepayments,
      interestPayments,
      arrearsOutstandingPrYears,
      outstandingYearEndAmount
    )
}
