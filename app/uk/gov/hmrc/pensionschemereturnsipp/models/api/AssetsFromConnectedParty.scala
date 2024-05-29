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

package uk.gov.hmrc.pensionschemereturnsipp.models.api

import cats.data.NonEmptyList
import play.api.libs.json._
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo

import java.time.LocalDate

case class AssetsFromConnectedParty(
  noOfTransactions: Int,
  transactionDetails: Option[NonEmptyList[AssetsFromConnectedParty.TransactionDetails]]
)

object AssetsFromConnectedParty {
  case class TransactionDetails(
    nameDOB: NameDOB,
    nino: NinoType,
    acquisitionDate: LocalDate,
    assetDescription: String,
    acquisitionOfShares: YesNo,
    shareCompanyDetails: Option[SharesCompanyDetails],
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    tangibleSchedule29A: YesNo,
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails],
    disposalOfShares: YesNo,
    noOfSharesHeld: Option[Int]
  ) extends MemberKey

  object TransactionDetails {
    implicit val format: OFormat[TransactionDetails] = Json.format[TransactionDetails]
  }

  implicit val format: OFormat[AssetsFromConnectedParty] = Json.format[AssetsFromConnectedParty]
}
