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

package uk.gov.hmrc.pensionschemereturnsipp.utils

import cats.implicits.catsSyntaxFlatMapOps

import scala.concurrent.{ExecutionContext, Future}

object FutureUtils {
  implicit class FutureOps[A](val future: Future[A]) extends AnyVal {
    def tapError[B](f: Throwable => Future[B])(implicit ec: ExecutionContext): Future[A] =
      future.recoverWith(t => f(t) >> Future.failed(t))
  }
}
