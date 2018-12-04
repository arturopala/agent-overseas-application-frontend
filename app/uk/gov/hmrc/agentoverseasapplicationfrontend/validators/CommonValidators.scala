package uk.gov.hmrc.agentoverseasapplicationfrontend.validators

import play.api.data.validation._

object CommonValidators {

  private val postcodeWithoutSpacesRegex = "^[A-Z]{1,2}[0-9][0-9A-Z]?[0-9][A-Z]{2}$|BFPO[0-9]{1,5}$".r
  private val telephoneNumberRegex = "^[0-9 ()]*$"
  private val validStringRegex = "[a-zA-Z0-9,.()\\-\\!@\\s]+"
  private val emailRegex = """^[a-zA-Z0-9-.]+?@[a-zA-Z0-9-.]+$""".r

  private val nonEmptyPostcodeConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty(fieldValue) match {
      case i: Invalid =>
        i
      case Valid =>
        val error = "error.postcode.invalid"
        val fieldValueWithoutSpaces = fieldValue.replace(" ", "")
        postcodeWithoutSpacesRegex
          .unapplySeq(fieldValueWithoutSpaces)
          .map(_ => Valid)
          .getOrElse(Invalid(ValidationError(error)))
    }
  }

  private val telephoneNumberConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty(fieldValue) match {
      case i: Invalid => i
      case Valid =>
        fieldValue match {
          case value if !value.matches(telephoneNumberRegex) =>
            Invalid(ValidationError("error.telephone.invalid"))
          case _ => Valid
        }
    }
  }

  private def emailAddressConstraint: Constraint[String] = Constraint[String]("constraint.email") { e =>
    if (e == null) Invalid(ValidationError("error.email"))
    else if (e.trim.isEmpty) Invalid(ValidationError("error.email"))
    else
      emailRegex
        .findFirstMatchIn(e.trim)
        .map(_ => Valid)
        .getOrElse(Invalid("error.email"))
  }

  private val validNameConstraint: Constraint[String] = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty(fieldValue) match {
      case i @ Invalid(_) =>
        i
      case Valid =>
        if (fieldValue.matches(validStringRegex))
          Valid
        else
          Invalid(ValidationError("error.string.invalid"))
    }
  }
}
