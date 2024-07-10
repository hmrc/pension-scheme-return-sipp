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

package uk.gov.hmrc.pensionschemereturnsipp

import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaNumChar, alphaNumStr}
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.{PsaId, PspId}
import uk.gov.hmrc.pensionschemereturnsipp.models.{IndividualDetails, MinimalDetails, PensionSchemeId}

object Generators {
  def nonEmptyString: Gen[String] =
    for {
      c <- alphaNumChar
      s <- alphaNumStr
    } yield s"$c$s"

  val boolean: Gen[Boolean] =
    Gen.oneOf(true, false)

  val topLevelDomain: Gen[String] =
    Gen.oneOf("com", "gov.uk", "co.uk", "net", "org", "io")

  val emailGen: Gen[String] =
    for {
      username <- nonEmptyString
      domain <- nonEmptyString
      topDomain <- topLevelDomain
    } yield s"$username@$domain.$topDomain"

  val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId)
  val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId)
  val pensionSchemeIdGen: Gen[PensionSchemeId] = Gen.oneOf(psaIdGen, pspIdGen)

  lazy val minimalDetailsGen: Gen[MinimalDetails] =
    for {
      email <- emailGen
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
}
