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

import cats.Order
import cats.data.NonEmptyList
import cats.implicits.{catsKernelStdOrderForOption, catsKernelStdOrderForString, catsKernelStdOrderForTuple4}
import uk.gov.hmrc.pensionschemereturnsipp.models.api.MemberKey
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.EtmpMemberAndTransactions
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus
import uk.gov.hmrc.pensionschemereturnsipp.models.etmp.common.SectionStatus.{Changed, Deleted}

import java.time.LocalDate
import scala.util.chaining.scalaUtilChainingOps

object EtmpMemberAndTransactionsUpdater {
  def merge[T <: MemberKey, EtmpType](
    updates: NonEmptyList[T],
    etmpData: List[EtmpMemberAndTransactions],
    transformer: T => EtmpType,
    extractor: EtmpMemberAndTransactions => List[EtmpType],
    modifier: (Option[NonEmptyList[EtmpType]], EtmpMemberAndTransactions) => EtmpMemberAndTransactions
  ): List[EtmpMemberAndTransactions] = {
    implicit val localDateOrder: Order[LocalDate] = (x, y) => x.compareTo(y)

    val updatesByMember = updates.groupBy(
      k => (k.nameDOB.firstName, k.nameDOB.lastName, k.nameDOB.dob, k.nino.nino)
    )

    val etmpDataByMember =
      etmpData
        .groupBy(
          k => (k.memberDetails.firstName, k.memberDetails.lastName, k.memberDetails.dateOfBirth, k.memberDetails.nino)
        )
        .view
        .mapValues(_.head)

    val updatedEtmpDataByMember = etmpDataByMember.map {
      case (memberKey, etmpTxsByMember) =>
        val update: Option[NonEmptyList[EtmpType]] = updatesByMember
          .get(memberKey)
          .map(_.map(transformer))

        val extracted = extractor(etmpTxsByMember)
        val updatedList = update.asList

        if (extracted.diff(updatedList).nonEmpty || updatedList.diff(extracted).nonEmpty) {
          modifier(update, etmpTxsByMember)
            .pipe { modified =>
              if (areJourneysEmpty(modified))
                modified.copy(status = Deleted, version = None)
              else
                modified.copy(status = Changed, version = None)
            }
        } else {
          etmpTxsByMember // No changes to transactions, leaving `status` as is
        }
    }.toList

    val newMembers = updatesByMember.keySet.diff(etmpDataByMember.keySet)

    val newEtmpDataByMember =
      newMembers.toList
        .flatMap(memberKey => updatesByMember.get(memberKey))
        .map { values =>
          val updated = values.head
          val memberDetails = toMemberDetails(updated.nameDOB, updated.nino)
          modifier(
            Some(values.map(transformer)),
            EtmpMemberAndTransactions.empty(SectionStatus.New, memberDetails)
          )
        }

    updatedEtmpDataByMember ++ newEtmpDataByMember
  }

  private def areJourneysEmpty(trx: EtmpMemberAndTransactions): Boolean =
    trx.landConnectedParty.forall(_.noOfTransactions == 0) &&
      trx.landArmsLength.forall(_.noOfTransactions == 0) &&
      trx.loanOutstanding.forall(_.noOfTransactions == 0) &&
      trx.tangibleProperty.forall(_.noOfTransactions == 0) &&
      trx.unquotedShares.forall(_.noOfTransactions == 0) &&
      trx.otherAssetsConnectedParty.forall(_.noOfTransactions == 0)

  implicit class FlattenOps[A](val maybeNonEmptyList: Option[NonEmptyList[A]]) {
    def asList: List[A] = maybeNonEmptyList.toList.flatMap(_.toList)
  }
}
