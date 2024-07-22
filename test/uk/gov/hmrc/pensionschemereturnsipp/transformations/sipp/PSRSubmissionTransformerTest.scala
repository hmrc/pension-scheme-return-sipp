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

package uk.gov.hmrc.pensionschemereturnsipp.transformations.sipp;

import cats.data.NonEmptyList
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{
  AssetsFromConnectedPartyResponse,
  LandOrConnectedPropertyResponse,
  OutstandingLoansResponse,
  ReportDetails,
  TangibleMoveablePropertyResponse,
  UnquotedShareResponse
}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.{AccountingPeriod, AccountingPeriodDetails}
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpPsrStatus.Compiled
import uk.gov.hmrc.pensionschemereturnsipp.transformations.{
  AssetsFromConnectedPartyTransformer,
  LandArmsLengthTransformer,
  LandConnectedPartyTransformer,
  OutstandingLoansTransformer,
  TangibleMoveablePropertyTransformer,
  UnquotedSharesTransformer
}
import uk.gov.hmrc.pensionschemereturnsipp.utils.{BaseSpec, SippEtmpDummyTestValues}

import java.time.LocalDate;

class PSRSubmissionTransformerTest extends BaseSpec with SippEtmpDummyTestValues {

  private val assetsFromConnectedPartyTransformer = mock[AssetsFromConnectedPartyTransformer]
  private val landArmsLengthTransformer = mock[LandArmsLengthTransformer]
  private val landConnectedPartyTransformer = mock[LandConnectedPartyTransformer]
  private val outstandingLoansTransformer = mock[OutstandingLoansTransformer]
  private val tangibleMoveablePropertyTransformer = mock[TangibleMoveablePropertyTransformer]
  private val unquotedSharesTransformer = mock[UnquotedSharesTransformer]

  private val transformer: PSRSubmissionTransformer = new PSRSubmissionTransformer(
    assetsFromConnectedPartyTransformer,
    landArmsLengthTransformer,
    landConnectedPartyTransformer,
    outstandingLoansTransformer,
    tangibleMoveablePropertyTransformer,
    unquotedSharesTransformer
  )

  "transform" should {

    "transform ETMP response to API response" in {
      when(assetsFromConnectedPartyTransformer.transformToResponse(any()))
        .thenReturn(AssetsFromConnectedPartyResponse(List(sippAssetsFromConnectedPartyApi)))
      when(landArmsLengthTransformer.transformToResponse(any()))
        .thenReturn(LandOrConnectedPropertyResponse(List(landConnectedTransaction)))
      when(landConnectedPartyTransformer.transformToResponse(any()))
        .thenReturn(LandOrConnectedPropertyResponse(List(landConnectedTransaction)))
      when(outstandingLoansTransformer.transformToResponse(any()))
        .thenReturn(OutstandingLoansResponse(List(sippOutstandingLoansApi)))
      when(tangibleMoveablePropertyTransformer.transformToResponse(any()))
        .thenReturn(TangibleMoveablePropertyResponse(List(sippTangibleApi)))
      when(unquotedSharesTransformer.transformToResponse(any()))
        .thenReturn(UnquotedShareResponse(List(sippUnquotedShareApi)))

      val resultApiResponse = transformer.transform(sampleSippPsrSubmissionEtmpResponse)

      resultApiResponse.details mustBe ReportDetails(
        "12345678AA",
        Compiled,
        LocalDate.parse("2022-04-06"),
        LocalDate.parse("2023-04-05"),
        Some("PSR Scheme"),
        Some("001")
      )

      resultApiResponse.accountingPeriodDetails mustBe AccountingPeriodDetails(
        Some("002"),
        NonEmptyList.of(
          AccountingPeriod(LocalDate.parse("2022-04-06"), LocalDate.parse("2022-12-31")),
          AccountingPeriod(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-04-05"))
        )
      )

      resultApiResponse.landConnectedParty.nonEmpty mustBe true
      resultApiResponse.otherAssetsConnectedParty.nonEmpty mustBe true
      resultApiResponse.unquotedShares.nonEmpty mustBe true
      resultApiResponse.landArmsLength.nonEmpty mustBe true
      resultApiResponse.loanOutstanding.nonEmpty mustBe true
      resultApiResponse.tangibleProperty.nonEmpty mustBe true
    }
  }
}
