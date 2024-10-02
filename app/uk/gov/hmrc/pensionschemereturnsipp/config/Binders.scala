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

package uk.gov.hmrc.pensionschemereturnsipp.config
import cats.implicits.toBifunctorOps
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.pensionschemereturnsipp.models.{Journey, JourneyType}

object Binders {

  implicit def journeyTypeBindable(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[JourneyType] = new QueryStringBindable[JourneyType] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, JourneyType]] =
      stringBinder.bind(key, params).map {
        case Right(value) => JourneyType.withNameInsensitiveEither(value).leftMap(_ => s"Invalid journey type: $value")
        case Left(error) => Left(error)
      }

    def unbind(key: String, journeyType: JourneyType): String =
      stringBinder.unbind(key, journeyType.entryName)
  }

  implicit def journeyBindable(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[Journey] = new QueryStringBindable[Journey] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Journey]] =
      stringBinder.bind(key, params).map {
        case Right(value) => Journey.withNameInsensitiveEither(value).leftMap(_ => s"Invalid journey: $value")
        case Left(error) => Left(error)
      }

    def unbind(key: String, journey: Journey): String =
      stringBinder.unbind(key, journey.entryName)
  }
}
