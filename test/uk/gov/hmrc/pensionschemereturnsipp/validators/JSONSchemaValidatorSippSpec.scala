/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.pensionschemereturnsipp.validators

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.pensionschemereturnsipp.utils.{SippEtmpDummyTestValues, SippEtmpTestValues}

import java.io.{BufferedWriter, FileWriter}

class JSONSchemaValidatorSippSpec
    extends AnyWordSpec
    with Matchers
    with JsonFileReader
    with SippEtmpTestValues
    with SippEtmpDummyTestValues {

  "validateJson for new SIPP Json" must {

    // TODO! - Add a functional test - That test is creating request test data as a file!
    "Create Request Data" in {
//      val json = Json.toJson(fullSippPsrSubmissionRequestLong)
//
//      val filePath = "/Users/tolgahmrc/Documents/Test-RequestPayload-v2/test-7.json"
//
//      val writer = new BufferedWriter(new FileWriter(filePath))
//
//      val jsonString: String = Json.stringify(json)
//      writer.write(jsonString)
//      writer.close()
    }
  }
}
