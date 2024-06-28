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
import uk.gov.hmrc.pensionschemereturnsipp.models.api.common._
import uk.gov.hmrc.pensionschemereturnsipp.models.api.{
  AssetsFromConnectedPartyApi,
  AssetsFromConnectedPartyResponse,
  LandOrConnectedPropertyApi,
  LandOrConnectedPropertyResponse,
  OutstandingLoansApi,
  OutstandingLoansResponse,
  PSRSubmissionResponse,
  ReportDetails,
  TangibleMoveablePropertyApi,
  TangibleMoveablePropertyResponse,
  UnquotedShareApi,
  UnquotedShareResponse
}
import uk.gov.hmrc.pensionschemereturnsipp.models.common.CostOrMarketType.MarketValue
import uk.gov.hmrc.pensionschemereturnsipp.models.common.RegistryDetails
import uk.gov.hmrc.pensionschemereturnsipp.models.common.YesNo.{No, Yes}
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

      resultApiResponse mustBe PSRSubmissionResponse(
        ReportDetails(
          "12345678AA",
          Compiled,
          LocalDate.parse("2022-04-06"),
          LocalDate.parse("2023-04-05"),
          Some("PSR Scheme"),
          Some("001")
        ),
        Some(
          NonEmptyList(
            LandOrConnectedPropertyApi.TransactionDetails(
              NameDOB("firstName", "lastName", LocalDate.parse("2020-01-01")),
              NinoType(Some("nino"), None),
              LocalDate.parse("2020-01-01"),
              Yes,
              AddressDetails("addressLine1", Some("addressLine2"), None, None, None, None, "UK"),
              RegistryDetails(No, None, None),
              "acquiredFromName",
              10.0,
              Yes,
              Yes,
              None,
              Yes,
              Yes,
              None,
              10.0,
              Yes,
              None,
              None
            ),
            List()
          )
        ),
        None,
        Some(
          NonEmptyList(
            LandOrConnectedPropertyApi.TransactionDetails(
              NameDOB("firstName", "lastName", LocalDate.parse("2020-01-01")),
              NinoType(Some("nino"), None),
              LocalDate.parse("2020-01-01"),
              Yes,
              AddressDetails("addressLine1", Some("addressLine2"), None, None, None, None, "UK"),
              RegistryDetails(No, None, None),
              "acquiredFromName",
              10.0,
              Yes,
              Yes,
              None,
              Yes,
              Yes,
              None,
              10.0,
              Yes,
              None,
              None
            ),
            List()
          )
        ),
        None,
        None,
        None
      )
    }

  }

}
