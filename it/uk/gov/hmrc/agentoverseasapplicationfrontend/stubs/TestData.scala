package uk.gov.hmrc.agentoverseasapplicationfrontend.stubs

object TestData {

 val allRejected =
  """[
    |    {
    |        "applicationReference": {
    |            "value": "8c61f8dc"
    |        },
    |        "applicationCreationDate": "2019-01-15",
    |        "application": {
    |            "amls": {
    |                "supervisoryBody": "Association of Chartered Certified Accountants (ACCA)",
    |                "supervisionMemberId": "1662309"
    |            },
    |            "contactDetails": {
    |                "firstName": "Firstname",
    |                "lastName": "Lastname",
    |                "jobTitle": "Jobtitle",
    |                "businessTelephone": "000-0000",
    |                "businessEmail": "email@domain.com"
    |            },
    |            "businessDetail": {
    |                "tradingName": "Tradingname",
    |                "businessAddress": {
    |                    "addressLine1": "Somestreet",
    |                    "addressLine2": "Somewhere",
    |                    "countryCode": "USA"
    |                },
    |                "extraInfo": {
    |                    "isUkRegisteredTaxOrNino": {
    |                        "str": "no"
    |                    },
    |                    "isHmrcAgentRegistered": {
    |                        "str": "no"
    |                    }
    |                }
    |            }
    |        },
    |        "status": {
    |            "typeIdentifier": "rejected"
    |        },
    |        "relatedAuthProviderIds": [
    |            "newuser"
    |        ],
    |        "maintainerReviewedOn": "2019-01-15",
    |        "reviewerPid": "test StrideId",
    |        "rejectedBecause": [
    |            {
    |                "rejectReason": "test reason"
    |            }
    |        ]
    |    },
    |
    |    {
    |        "applicationReference": {
    |            "value": "8c61f8dc"
    |        },
    |        "applicationCreationDate": "2019-01-08",
    |        "application": {
    |            "amls": {
    |                "supervisoryBody": "Association of Chartered Certified Accountants (ACCA)",
    |                "supervisionMemberId": "1662309"
    |            },
    |            "contactDetails": {
    |                "firstName": "Firstname",
    |                "lastName": "Lastname",
    |                "jobTitle": "Jobtitle",
    |                "businessTelephone": "000-0000",
    |                "businessEmail": "email@domain.com"
    |            },
    |            "businessDetail": {
    |                "tradingName": "Tradingname",
    |                "businessAddress": {
    |                    "addressLine1": "Somestreet",
    |                    "addressLine2": "Somewhere",
    |                    "countryCode": "USA"
    |                },
    |                "extraInfo": {
    |                    "isUkRegisteredTaxOrNino": {
    |                        "str": "no"
    |                    },
    |                    "isHmrcAgentRegistered": {
    |                        "str": "no"
    |                    }
    |                }
    |            }
    |        },
    |        "status": {
    |            "typeIdentifier": "rejected"
    |        },
    |        "relatedAuthProviderIds": [
    |            "newuser"
    |        ],
    |        "maintainerReviewedOn": "2019-01-08",
    |        "reviewerPid": "test StrideId",
    |        "rejectedBecause": [
    |            {
    |                "rejectReason": "test reason"
    |            }
    |        ]
    |    },
    |     {
    |        "applicationReference": {
    |            "value": "8c61f8dc"
    |        },
    |        "applicationCreationDate": "2019-01-22",
    |        "application": {
    |            "amls": {
    |                "supervisoryBody": "Association of Chartered Certified Accountants (ACCA)",
    |                "supervisionMemberId": "1662309"
    |            },
    |            "contactDetails": {
    |                "firstName": "Firstname",
    |                "lastName": "Lastname",
    |                "jobTitle": "Jobtitle",
    |                "businessTelephone": "000-0000",
    |                "businessEmail": "email@domain.com"
    |            },
    |            "businessDetail": {
    |                "tradingName": "Tradingname",
    |                "businessAddress": {
    |                    "addressLine1": "Somestreet",
    |                    "addressLine2": "Somewhere",
    |                    "countryCode": "USA"
    |                },
    |                "extraInfo": {
    |                    "isUkRegisteredTaxOrNino": {
    |                        "str": "no"
    |                    },
    |                    "isHmrcAgentRegistered": {
    |                        "str": "no"
    |                    }
    |                }
    |            }
    |        },
    |        "status": {
    |            "typeIdentifier": "rejected"
    |        },
    |        "relatedAuthProviderIds": [
    |            "newuser"
    |        ],
    |        "maintainerReviewedOn": "2019-01-22",
    |        "reviewerPid": "test StrideId",
    |        "rejectedBecause": [
    |            {
    |                "rejectReason": "test reason"
    |            }
    |        ]
    |    }
    |]""".stripMargin

 val notAllRejected =

  """[
    |    {
    |        "applicationReference": {
    |            "value": "8c61f8dc"
    |        },
    |        "applicationCreationDate": "2019-01-15",
    |        "application": {
    |            "amls": {
    |                "supervisoryBody": "Association of Chartered Certified Accountants (ACCA)",
    |                "supervisionMemberId": "1662309"
    |            },
    |            "contactDetails": {
    |                "firstName": "Firstname",
    |                "lastName": "Lastname",
    |                "jobTitle": "Jobtitle",
    |                "businessTelephone": "000-0000",
    |                "businessEmail": "email@domain.com"
    |            },
    |            "businessDetail": {
    |                "tradingName": "Tradingname",
    |                "businessAddress": {
    |                    "addressLine1": "Somestreet",
    |                    "addressLine2": "Somewhere",
    |                    "countryCode": "USA"
    |                },
    |                "extraInfo": {
    |                    "isUkRegisteredTaxOrNino": {
    |                        "str": "no"
    |                    },
    |                    "isHmrcAgentRegistered": {
    |                        "str": "no"
    |                    }
    |                }
    |            }
    |        },
    |        "status": {
    |            "typeIdentifier": "rejected"
    |        },
    |        "relatedAuthProviderIds": [
    |            "newuser"
    |        ],
    |        "maintainerReviewedOn": "2019-01-15",
    |        "reviewerPid": "test StrideId",
    |        "rejectedBecause": [
    |            {
    |                "rejectReason": "test reason"
    |            }
    |        ]
    |    },
    |
    |    {
    |        "applicationReference": {
    |            "value": "8c61f8dc"
    |        },
    |        "applicationCreationDate": "2019-01-08",
    |        "application": {
    |            "amls": {
    |                "supervisoryBody": "Association of Chartered Certified Accountants (ACCA)",
    |                "supervisionMemberId": "1662309"
    |            },
    |            "contactDetails": {
    |                "firstName": "Firstname",
    |                "lastName": "Lastname",
    |                "jobTitle": "Jobtitle",
    |                "businessTelephone": "000-0000",
    |                "businessEmail": "email@domain.com"
    |            },
    |            "businessDetail": {
    |                "tradingName": "Tradingname",
    |                "businessAddress": {
    |                    "addressLine1": "Somestreet",
    |                    "addressLine2": "Somewhere",
    |                    "countryCode": "USA"
    |                },
    |                "extraInfo": {
    |                    "isUkRegisteredTaxOrNino": {
    |                        "str": "no"
    |                    },
    |                    "isHmrcAgentRegistered": {
    |                        "str": "no"
    |                    }
    |                }
    |            }
    |        },
    |        "status": {
    |            "typeIdentifier": "pending"
    |        },
    |        "relatedAuthProviderIds": [
    |            "newuser"
    |        ]
    |    },
    |     {
    |        "applicationReference": {
    |            "value": "8c61f8dc"
    |        },
    |        "applicationCreationDate": "2019-01-22",
    |        "application": {
    |            "amls": {
    |                "supervisoryBody": "Association of Chartered Certified Accountants (ACCA)",
    |                "supervisionMemberId": "1662309"
    |            },
    |            "contactDetails": {
    |                "firstName": "Firstname",
    |                "lastName": "Lastname",
    |                "jobTitle": "Jobtitle",
    |                "businessTelephone": "000-0000",
    |                "businessEmail": "email@domain.com"
    |            },
    |            "businessDetail": {
    |                "tradingName": "Tradingname",
    |                "businessAddress": {
    |                    "addressLine1": "Somestreet",
    |                    "addressLine2": "Somewhere",
    |                    "countryCode": "USA"
    |                },
    |                "extraInfo": {
    |                    "isUkRegisteredTaxOrNino": {
    |                        "str": "no"
    |                    },
    |                    "isHmrcAgentRegistered": {
    |                        "str": "no"
    |                    }
    |                }
    |            }
    |        },
    |        "status": {
    |            "typeIdentifier": "rejected"
    |        },
    |        "relatedAuthProviderIds": [
    |            "newuser"
    |        ],
    |        "maintainerReviewedOn": "2019-01-22",
    |        "reviewerPid": "test StrideId",
    |        "rejectedBecause": [
    |            {
    |                "rejectReason": "test reason"
    |            }
    |        ]
    |    }
    |]""".stripMargin

}
