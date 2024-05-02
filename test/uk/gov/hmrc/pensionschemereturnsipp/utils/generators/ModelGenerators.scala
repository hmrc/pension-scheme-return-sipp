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

package uk.gov.hmrc.pensionschemereturnsipp.utils.generators

import org.scalacheck.Gen
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.{PsaId, PspId}
import uk.gov.hmrc.pensionschemereturnsipp.models.SchemeId.{Pstr, Srn}
import uk.gov.hmrc.pensionschemereturnsipp.models.SchemeStatus.{
  Deregistered,
  Open,
  Pending,
  PendingInfoReceived,
  PendingInfoRequired,
  Rejected,
  RejectedUnderAppeal,
  WoundUp
}
import uk.gov.hmrc.pensionschemereturnsipp.models.cache.PensionSchemeUser
import uk.gov.hmrc.pensionschemereturnsipp.models.cache.PensionSchemeUser.{Administrator, Practitioner}
import uk.gov.hmrc.pensionschemereturnsipp.models._

import java.time.LocalDate

trait ModelGenerators extends BasicGenerators {

  lazy val minimalDetailsGen: Gen[MinimalDetails] =
    for {
      email <- email
      isSuspended <- boolean
      orgName <- Gen.option(nonEmptyString)
      individual <- Gen.option(individualDetailsGen)
      rlsFlag <- boolean
      deceasedFlag <- boolean
    } yield MinimalDetails(email, isSuspended, orgName, individual, rlsFlag, deceasedFlag)

  lazy val individualDetailsGen: Gen[IndividualDetails] =
    for {
      firstName <- nonEmptyString
      middleName <- Gen.option(nonEmptyString)
      lastName <- nonEmptyString
    } yield IndividualDetails(firstName, middleName, lastName)

  val validSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Open,
      WoundUp,
      Deregistered
    )

  val invalidSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Pending,
      PendingInfoRequired,
      PendingInfoReceived,
      Rejected,
      RejectedUnderAppeal
    )

  val schemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(validSchemeStatusGen, invalidSchemeStatusGen)

  val schemeDetailsGen: Gen[SchemeDetails] =
    for {
      name <- nonEmptyString
      pstr <- nonEmptyString
      status <- schemeStatusGen
      authorisingPsa <- Gen.option(nonEmptyString)
    } yield SchemeDetails(name, pstr, status, authorisingPsa)

  val pensionSchemeUserGen: Gen[PensionSchemeUser] =
    Gen.oneOf(Administrator, Practitioner)

  val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId)
  val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId)

  val srnGen: Gen[Srn] = nonEmptyString.map(Srn)
  val pstrGen: Gen[Pstr] = nonEmptyString.map(Pstr)

  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1, 28)
    month <- Gen.choose(1, 12)
    year <- Gen.choose(1990, 2000)
  } yield LocalDate.of(year, month, day)

  val psaOrPspGen: Gen[String] = Gen.oneOf(Seq("PSA", "PSP"))

  val schemeIdGen: Gen[SchemeId] = Gen.oneOf(srnGen, pstrGen)
}
