/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasapplicationfrontend.config

import scala.util.{Failure, Success, Try}

object AMLSLoader {
  def load(path: String): Map[String, String] =
    Try {
      require(path.nonEmpty, "AMLS file path cannot be empty")
      require(path.endsWith(".csv"), "AMLS file should be a csv file")

      scala.io.Source
        .fromInputStream(this.getClass.getResourceAsStream(path), "utf-8")
        .getLines()
        .drop(1)
        .toSeq
        .map { line =>
          line.split(",").map(_.trim) match {
            case Array(code, bodyName) => (code, bodyName)
            case _                     => sys.error("Strange line in AMLS csv file")
          }
        }
        .toMap
    } match {
      case Success(amlsCodesToNames) if amlsCodesToNames.nonEmpty => amlsCodesToNames
      case Failure(ex)                                            => sys.error(ex.getMessage)
      case _                                                      => sys.error("No amls entries found in the csv")
    }
}
