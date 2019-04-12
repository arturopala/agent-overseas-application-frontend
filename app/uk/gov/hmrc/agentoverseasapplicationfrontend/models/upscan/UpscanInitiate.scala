/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentoverseasapplicationfrontend.models.upscan

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes, _}

case class UpscanInitiate(reference: String, uploadRequest: UploadRequest)

case class UploadRequest(href: String, fields: Fields)

case class Fields(
  xAmzMetaCallbackUrl: String,
  xAmzDate: String,
  xAmzCredential: String,
  xAmzAlgorithm: String,
  key: String,
  acl: String,
  xAmzSignature: String,
  xAmzMetaConsumingService: String,
  policy: String
)

object Fields {
  implicit val writes = new Writes[Fields] {
    def writes(fields: Fields) = Json.obj(
      "x-amz-meta-callback-url"      -> fields.xAmzMetaCallbackUrl,
      "x-amz-date"                   -> fields.xAmzDate,
      "x-amz-credential"             -> fields.xAmzCredential,
      "x-amz-algorithm"              -> fields.xAmzAlgorithm,
      "key"                          -> fields.key,
      "acl"                          -> fields.acl,
      "x-amz-signature"              -> fields.xAmzSignature,
      "x-amz-meta-consuming-service" -> fields.xAmzMetaConsumingService,
      "policy"                       -> fields.policy
    )
  }
  implicit val reads: Reads[Fields] = ((__ \ "x-amz-meta-callback-url").read[String] and
    (__ \ "x-amz-date").read[String] and
    (__ \ "x-amz-credential").read[String] and
    (__ \ "x-amz-algorithm").read[String] and
    (__ \ "key").read[String] and
    (__ \ "acl").read[String] and
    (__ \ "x-amz-signature").read[String] and
    (__ \ "x-amz-meta-consuming-service").read[String] and
    (__ \ "policy").read[String])(Fields.apply _)
}

object UploadRequest {
  implicit val writes = new Writes[UploadRequest] {
    def writes(uploadRequest: UploadRequest) = Json.obj(
      "href"   -> uploadRequest.href,
      "fields" -> uploadRequest.fields
    )
  }
  implicit val reads: Reads[UploadRequest] = ((__ \ "href").read[String] and
    (__ \ "fields").read[Fields])(UploadRequest.apply _)
}

object UpscanInitiate {
  implicit val writes = new Writes[UpscanInitiate] {
    def writes(upscan: UpscanInitiate) = Json.obj(
      "reference"     -> upscan.reference,
      "uploadRequest" -> upscan.uploadRequest
    )
  }
  implicit val reads: Reads[UpscanInitiate] = ((__ \ "reference").read[String] and
    (__ \ "uploadRequest").read[UploadRequest])(UpscanInitiate.apply _)
}
