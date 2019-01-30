package uk.gov.hmrc.agentoverseasapplicationfrontend.validators

import org.scalatest.EitherValues
import play.api.data.{FormError, Mapping}
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators._
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.immutable.Stream
import scala.util.Random

class CommonValidatorsSpec extends UnitSpec with EitherValues {

  "saUtr bind" should {
    val utrMapping = saUtr.withPrefix("testKey")

    def bind(fieldValue: String) = utrMapping.bind(Map("testKey" -> fieldValue))

    "accept valid UTRs" in {
      bind("20000  00000") shouldBe Right("20000  00000")
    }

    "give \"error.sautr.blank\" error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", "error.sautr.blank")
    }

    "give \"error.sautr.blank\" error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", "error.sautr.blank")
    }

    "give \"error.sautr.invalid\" error" when {
      "it has more than 10 digits" in {
        bind("20000000000") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }

      "it has fewer than 10 digits" in {
        bind("200000") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }

        bind("20000000 0") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }

      "it has non-digit characters" in {
        bind("200000000B") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }

      "it has non-alphanumeric characters" in {
        bind("200000000!") should matchPattern {
          case Left(List(FormError("testKey", List("error.sautr.invalid"), _))) =>
        }
      }
    }
  }

  "emailAddress bind" should {
    val emailAddress = CommonValidators.businessEmail.withPrefix("testKey")

    def bind(fieldValue: String) = emailAddress.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern { case Left(List(FormError("testKey", List("error.email.invalid"), _))) => }

    def shouldAcceptFieldValue(fieldValue: String): Unit =
      bind(fieldValue) shouldBe Right(fieldValue)

    "reject email address" when {
      "field is not present" in {
        emailAddress.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }

      "input is empty" in {
        bind("").left.value should contain only FormError("testKey", "error.email.blank")
      }

      "input has length more than 132 characters" in {
        bind(s"${Random.alphanumeric.take(132).mkString}@example.com").left.value should contain only FormError(
          "testKey",
          "error.email.maxlength")
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", "error.email.blank")
      }

      "not a valid email" in {
        shouldRejectFieldValueAsInvalid("bademail")
      }

      "has spaces" in {
        shouldRejectFieldValueAsInvalid("bad email@example.com")
      }
    }

    "accept a valid email address" in {
      shouldAcceptFieldValue("valid+email@example.com")
      shouldAcceptFieldValue("valid@test.com")
      shouldAcceptFieldValue("valid.email@test.com")
      shouldAcceptFieldValue("valid_email@test.com")
      shouldAcceptFieldValue("valid-email@test.com")
      shouldAcceptFieldValue("valid-email.address@test.com")
      shouldAcceptFieldValue("valid-email._address@test.com")
    }
  }

  " addressLine1, 2 bind" should {
    val unprefixedAddressLine1Mapping = addressLine12(lineNumber = 1)
    val unprefixedAddressLine2Mapping = addressLine12(lineNumber = 2)

    behave like anAddressLineValidatingMapping(unprefixedAddressLine1Mapping, 1)
    behave like anAddressLineValidatingMapping(unprefixedAddressLine2Mapping, 2)

    val addressLine1Mapping = unprefixedAddressLine1Mapping.withPrefix("testKey")

    def bind(fieldValue: String) = addressLine1Mapping.bind(Map("testKey" -> fieldValue))

    "reject the line" when {
      "field is not present" in {
        addressLine1Mapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", "error.addressline.1.blank"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain(FormError("testKey", "error.addressline.1.blank"))
      }
    }
  }

  "addressLine 3 and 4 bind" should {
    def nonOptionalAddressLine34Mapping(lineNumber: Int): Mapping[String] =
      addressLine34(lineNumber).transform(_.get, Some.apply)

    behave like anAddressLineValidatingMapping(nonOptionalAddressLine34Mapping(3), 3)
    behave like anAddressLineValidatingMapping(nonOptionalAddressLine34Mapping(4), 4)

    val addressLine23Mapping = addressLine34(3).withPrefix("testKey")

    def bind(fieldValue: String) = addressLine23Mapping.bind(Map("testKey" -> fieldValue))

    def shouldAcceptFieldValue(fieldValue: String): Unit =
      if (fieldValue.isEmpty) bind(fieldValue) shouldBe Right(None)
      else bind(fieldValue) shouldBe Right(Some(fieldValue))

    "reject the line" when {
      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", "error.addressline.3.blank")
      }
    }

    "accept the line" when {
      "field is empty" in {
        shouldAcceptFieldValue("")
      }
    }
  }

  private def anAddressLineValidatingMapping(unprefixedAddressLineMapping: Mapping[String], lineNumber: Int): Unit = {

    val addressLine1Mapping = unprefixedAddressLineMapping.withPrefix("testKey")

    def bind(fieldValue: String) = addressLine1Mapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List(emptyError), _))) =>
      }

    def shouldRejectFieldValueAsTooLong(fieldValue: String): Unit =
      bind(fieldValue) shouldBe Left(List(FormError("testKey", List(s"error.addressline.$lineNumber.maxlength"))))

    def shouldAcceptFieldValue(fieldValue: String): Unit =
      if (fieldValue.isEmpty) bind(fieldValue) shouldBe Right(None)
      else bind(fieldValue) shouldBe Right(fieldValue)

    s"reject the address line $lineNumber" when {
      "there is an character that is not allowed by the DES regex" in {
        shouldRejectFieldValueAsInvalid("My Agency street<script> City~City")
      }

      "the line is too long for DES" in {
        shouldRejectFieldValueAsTooLong("123456789012345678901234567890123456")
      }
    }

    s"accept the address line $lineNumber" when {
      "there is text and numbers" in {
        shouldAcceptFieldValue("99 My Agency address")
      }

      "there are valid symbols in the input" in {
        shouldAcceptFieldValue("My - Ageny, address Street.")
        shouldAcceptFieldValue("Tester's Agency & address Street")
      }

      "there is a valid address" in {
        shouldAcceptFieldValue("My Agency address")
      }

      "it is the maximum allowable length" in {
        shouldAcceptFieldValue("12345678901234567890123456789012345")
      }
    }
  }

  "first name bind" should {
    testNameBinding("firstName")(firstName.withPrefix("testKey"))
  }

  "last name bind" should {
    testNameBinding("lastName")(lastName.withPrefix("testKey"))
  }

  def testNameBinding(nameType: String)(mapping: => Mapping[String]): Unit = {

    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    val invalidErrorMessage = s"error.$nameType.invalid"
    val maxLengthErrorMessage = s"error.$nameType.maxlength"

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List(invalidErrorMessage), _))) =>
      }

    def shouldRejectFieldValueAsTooLong(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List(maxLengthErrorMessage), _))) =>
      }

    def shouldAcceptFieldValue(fieldValue: String): Unit =
      bind(fieldValue) shouldBe Right(fieldValue)

    s"reject $nameType" when {

      "there is an ampersand character" in {
        shouldRejectFieldValueAsInvalid("My Agency & Co")
      }

      "there is a number" in {
        shouldRejectFieldValueAsInvalid("My Agency99")
      }

      "there is an invalid character" in {
        shouldRejectFieldValueAsInvalid("My Agency; His Agency #1")
        shouldRejectFieldValueAsInvalid("My Agency/ His Agency #1")
      }

      "there are more than 35 characters" in {
        shouldRejectFieldValueAsTooLong(Random.alphanumeric.take(35).mkString)
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", s"error.$nameType.blank"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", s"error.$nameType.blank")
      }

      "field is not present" in {
        mapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }
    }

    s"accept $nameType" when {
      "there are valid characters" in {
        shouldAcceptFieldValue("My Agency")
        shouldAcceptFieldValue("My--Agency")
        shouldAcceptFieldValue("My'Agency")
      }
    }
  }

  "jobTitle bind" should {

    val mapping = jobTitle.withPrefix("testKey")

    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.jobTitle.invalid"), _))) =>
      }

    def shouldRejectFieldValueAsIncorrectLength(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.jobTitle.length"), _))) =>
      }

    def shouldAcceptFieldValue(fieldValue: String): Unit =
      bind(fieldValue) shouldBe Right(fieldValue)

    s"reject job Title" when {

      "there is an ampersand character" in {
        shouldRejectFieldValueAsInvalid("Software & Dev")
      }

      "there is a number" in {
        shouldRejectFieldValueAsInvalid("Software99")
      }

      "there is an invalid character" in {
        shouldRejectFieldValueAsInvalid("Software;  #1")
        shouldRejectFieldValueAsInvalid("Softwarey/  #1")
      }

      "there are more than 50 characters" in {
        shouldRejectFieldValueAsIncorrectLength(randomString(51))
      }

      "there are less than 2 characters" in {
        shouldRejectFieldValueAsIncorrectLength("a")
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", s"error.jobTitle.blank"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", s"error.jobTitle.blank")
      }

      "field is not present" in {
        mapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }
    }

    s"accept jobTitle" when {
      "there are valid characters" in {
        shouldAcceptFieldValue("My Agency")
        shouldAcceptFieldValue("My--Agency")
        shouldAcceptFieldValue("My'Agency")
      }
    }
  }

  "tradingName bind" should {

    val tradingNameMapping = tradingName.withPrefix("testKey")

    def bind(fieldValue: String) = tradingNameMapping.bind(Map("testKey" -> fieldValue))

    def shouldRejectFieldValueAsInvalid(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.tradingName.invalid"), _))) =>
      }

    def shouldRejectFieldValueAsTooLong(fieldValue: String): Unit =
      bind(fieldValue) should matchPattern {
        case Left(List(FormError("testKey", List("error.tradingName.maxlength"), _))) =>
      }

    def shouldAcceptFieldValue(fieldValue: String): Unit =
      bind(fieldValue) shouldBe Right(fieldValue)

    "reject trading name" when {

      "there is an ampersand character" in {
        shouldRejectFieldValueAsInvalid("My Agency & Co")
      }

      "there is an apostrophe character" in {
        shouldRejectFieldValueAsInvalid("My Agency's Co")
      }

      "there is an invalid character" in {
        shouldRejectFieldValueAsInvalid("My Agency; His Agency #1")
      }

      "there are more than 40 characters" in {
        shouldRejectFieldValueAsTooLong(randomString(41))
      }

      "input is empty" in {
        bind("").left.value should contain(FormError("testKey", "error.tradingName.blank"))
      }

      "input is only whitespace" in {
        bind("    ").left.value should contain only FormError("testKey", "error.tradingName.blank")
      }

      "field is not present" in {
        tradingNameMapping.bind(Map.empty).left.value should contain only FormError("testKey", "error.required")
      }
    }

    "accept trading name" when {
      "there are valid characters" in {
        shouldAcceptFieldValue("My Agency")
        shouldAcceptFieldValue("My/Agency")
        shouldAcceptFieldValue("My,.-Agency")
      }

      "there are numbers and letters" in {
        shouldAcceptFieldValue("The 100 Agency")
      }
    }
  }

  "SA agent code binding" should {
    testAgentCode("saAgentCode")(saAgentCode.withPrefix("testKey"))
  }

  "Corporation tax agent code binding" should {
    testAgentCode("ctAgentCode")(ctAgentCode.withPrefix("testKey"))
  }

  "PAYE agent code binding" should {
    testAgentCode("payeAgentCode")(payeAgentCode.withPrefix("testKey"))
  }

  def testAgentCode(codeType: String)(mapping: => Mapping[String]) = {
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    val invalidMessage = s"error.$codeType.invalid"
    val maxLengthMessage = s"error.$codeType.maxlength"

    s"accept valid $codeType" in {
      bind("SA1234") shouldBe Right("SA1234")
    }

    s"give error.$codeType.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.$codeType.blank")
    }

    s"give error.$codeType.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.$codeType.blank")
    }

    s"give error.$codeType.maxlength error" when {
      "it has more than 6 characters" in {
        bind("SA20000000000") should matchPattern {
          case Left(List(FormError("testKey", List(maxLengthMessage), _))) =>
        }
      }

      "it has fewer than 6 characters" in {
        bind("SA200") should matchPattern {
          case Left(List(FormError("testKey", List(maxLengthMessage), _))) =>
        }
      }

      "it has no-alphanumeric characters" in {
        bind("SA**12222") should matchPattern {
          case Left(List(FormError("testKey", List(invalidMessage), _))) =>
        }
      }
    }
  }

  "VAT agent code binding" should {
    val mapping = vatAgentCode.withPrefix("testKey")
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid vatAgentCode" in {
      bind("VAT123455") shouldBe Right("VAT123455")
    }

    s"give error.vatAgentCode.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.vatAgentCode.blank")
    }

    s"give error.vatAgentCode.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.vatAgentCode.blank")
    }

    s"give error.vatAgentCode.maxlength error" when {
      "it has more than 9 characters" in {
        bind("SA20000000000") should matchPattern {
          case Left(List(FormError("testKey", List("error.vatAgentCode.maxlength"), _))) =>
        }
      }

      "it has fewer than 9 characters" in {
        bind("SA200") should matchPattern {
          case Left(List(FormError("testKey", List("error.vatAgentCode.maxlength"), _))) =>
        }
      }

      "it has no-alphanumeric characters" in {
        bind("VAT**12222") should matchPattern {
          case Left(List(FormError("testKey", List("error.vatAgentCode.invalid"), _))) =>
        }
      }
    }
  }

  "Nino validation" should {
    val ninoMapping = nino.withPrefix("testKey")

    def bind(fieldValue: String) = ninoMapping.bind(Map("testKey" -> fieldValue))

    "accept valid Nino" in {
      bind("AA980984B") shouldBe Right("AA980984B")
    }

    "accept valid Nino with random Spaces" in {
      bind("AA   9 8 0 98 4     B      ") shouldBe Right("AA   9 8 0 98 4     B      ")
    }

    "reject with error when invalid Nino" in {
      bind("AAAAAAAA0").left.value should contain only FormError("testKey", "error.nino.invalid")
    }

    "reject with error when nino field is empty" in {
      bind("").left.value should contain only FormError("testKey", "error.nino.blank")
    }

    "reject with error when nino field contain spaces only" in {
      bind("    ").left.value should contain only FormError("testKey", "error.nino.blank")
    }
  }

  "amlsBody bind" should {
    val amlsCodeMapping = amlsCode(Set("AA", "BB")).withPrefix("testKey")
    def bind(fieldValue: String) = amlsCodeMapping.bind(Map("testKey" -> fieldValue))

    "accept valid AMLS body" in {
      bind("AA") shouldBe Right("AA")
    }

    "return validation error if the field is blank" in {
      bind("").left.value should contain only FormError("testKey", "error.moneyLaunderingCompliance.amlsbody.empty")
    }

    "return validation error if the field is invalid " in {
      bind("CC").left.value should contain only FormError("testKey", "error.moneyLaunderingCompliance.amlsbody.invalid")
    }
  }

  "membershipNumber bind" should {
    val membershipNumberMapping = membershipNumber.withPrefix("testKey")
    def bind(fieldValue: String) = membershipNumberMapping.bind(Map("testKey" -> fieldValue))

    "accept valid membership number" in {
      bind("123456") shouldBe Right(Some("123456"))
    }

    "return validation error if the field is invalid" in {
      bind("**").left.value should contain only FormError("testKey", "error.membershipNumber.invalid")
    }
  }

  "Company registration number binding" should {
    val mapping = companyRegistrationNumber.withPrefix("testKey")
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid CRN" in {
      bind("12345678") shouldBe Right("12345678")
      bind("CN345678") shouldBe Right("CN345678")
    }

    s"give error.crn.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.crn.blank")
    }

    s"give error.crn.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.crn.blank")
    }

    s"give error.crn.invalid error" when {
      "it has no-alphanumeric characters" in {
        bind("VAT*$") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }
        bind("VAT**12") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }

        bind("VAT**12222") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }
      }

      "it has more than 2 letters" in {
        bind("VAT12345") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }
      }

      "it has more than 6 digits" in {
        bind("VA1234567") should matchPattern {
          case Left(List(FormError("testKey", List("error.crn.invalid"), _))) =>
        }
      }
    }
  }

  "Tax registration number binding" should {
    val mapping = taxRegistrationNumber.withPrefix("testKey")
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid TRN" in {
      bind("12345678") shouldBe Right("12345678")
      bind("TN 345678") shouldBe Right("TN 345678")
    }

    s"give error.trn.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.trn.blank")
    }

    s"give error.trn.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.trn.blank")
    }

    s"give error.trn.maxlength error when the size is more than 24 chars" in {
      bind(randomString(25)).left.value should contain only FormError("testKey", s"error.trn.maxlength")
    }

    s"give error.trn.invalid error" when {
      "it has no-alphanumeric characters" in {
        bind("VAT*$") should matchPattern {
          case Left(List(FormError("testKey", List("error.trn.invalid"), _))) =>
        }
        bind("VAT**12") should matchPattern {
          case Left(List(FormError("testKey", List("error.trn.invalid"), _))) =>
        }

        bind("VAT**12222") should matchPattern {
          case Left(List(FormError("testKey", List("error.trn.invalid"), _))) =>
        }
      }
    }
  }

  "Business telephone binding" should {
    val mapping = businessTelephone.withPrefix("testKey")
    def bind(fieldValue: String) = mapping.bind(Map("testKey" -> fieldValue))

    s"accept valid telephone number" in {
      bind("0048 605 555 555") shouldBe Right("0048 605 555 555")
    }

    s"give error.telephone.blank error when it is empty" in {
      bind("").left.value should contain only FormError("testKey", s"error.telephone.blank")
    }

    s"give error.telephone.blank error when it only contains a space" in {
      bind(" ").left.value should contain only FormError("testKey", s"error.telephone.blank")
    }

    s"give error.telephone.maxlength error when the size is more than 24 chars" in {
      bind((1 to 18).mkString).left.value should contain only FormError("testKey", s"error.telephone.maxlength")
    }

    s"give error.telephone.invalid error" when {
      "it has no-alphanumeric characters" in {
        bind("VAT$") should matchPattern {
          case Left(List(FormError("testKey", List("error.telephone.invalid"), _))) =>
        }
      }
      "it has letters in small case" in {
        bind("var") should matchPattern {
          case Left(List(FormError("testKey", List("error.telephone.invalid"), _))) =>
        }
      }
    }
  }

  def randomString(limit: Int): String = {
    def nextAlphaNum: Char = {
      val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
      chars charAt (Random.nextInt(chars.length))
    }
    (Stream.continually(nextAlphaNum)).take(limit).mkString
  }
}