package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

object FormValidators {

  def countryCode(validCountryCodes: Set[String]): Mapping[String] = text.verifying(validCountryCode(validCountryCodes))

  def radioInputSelected[T](message: String = "error.no-radio-selected"): Constraint[Option[T]] =
    Constraint[Option[T]] { fieldValue: Option[T] =>
      if (fieldValue.isDefined) Valid
      else Invalid(ValidationError(message))
    }

  private def validCountryCode(codes: Set[String]) = Constraint { fieldValue: String =>
    nonEmptyWithMessage("error.country.empty")(fieldValue.trim) match {
      case i: Invalid => i
      case Valid =>
        if (codes.contains(fieldValue.trim))
          Valid
        else
          Invalid(ValidationError("error.country.invalid"))
    }
  }

  // Same as play.api.data.validation.Constraints.nonEmpty but with a custom message instead of error.required
  private def nonEmptyWithMessage(messageKey: String): Constraint[String] = Constraint[String] { o: String =>
    if (o == null) Invalid(ValidationError(messageKey))
    else if (o.trim.isEmpty) Invalid(ValidationError(messageKey))
    else Valid
  }
}
