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

package uk.gov.hmrc.pensionschemereturnsipp.models.etmp

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.SubmittedBy
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpSippPsrDeclaration.Declaration

case class EtmpSippPsrDeclaration(
  submittedBy: SubmittedBy,
  submitterID: String,
  psaID: Option[String],
  psaDeclaration: Option[Declaration],
  pspDeclaration: Option[Declaration]
)

object EtmpSippPsrDeclaration {
  case class Declaration(declaration1: Boolean, declaration2: Boolean)

  implicit val formatDeclaration: OFormat[Declaration] = Json.format[Declaration]
  implicit val format: OFormat[EtmpSippPsrDeclaration] = Json.format[EtmpSippPsrDeclaration]
}
