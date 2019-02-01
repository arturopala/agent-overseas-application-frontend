package uk.gov.hmrc.agentoverseasapplicationfrontend.stubs

object StubsTestData {

 def pendingApplication(appCreateDate: String) =
   s"""[
      |{
      |    "applicationReference" : {
      |        "value" : "25eaab89"
      |    },
      |    "applicationCreationDate" : "$appCreateDate",
      |    "application" : {
      |        "amls" : {
      |            "supervisoryBody" : "International Association of Bookkeepers (IAB)",
      |            "supervisionMemberId" : "0987654321"
      |        },
      |        "contactDetails" : {
      |            "firstName" : "Testing",
      |            "lastName" : "Agent",
      |            "jobTitle" : "Tester",
      |            "businessTelephone" : "011565438754",
      |            "businessEmail" : "test@test.com"
      |        },
      |        "businessDetail" : {
      |            "tradingName" : "Testing Agency",
      |            "businessAddress" : {
      |                "addressLine1" : "addressLine1",
      |                "addressLine2" : "addressLine2",
      |                "addressLine3" : "addressLine3",
      |                "addressLine4" : "addressLine4",
      |                "countryCode" : "TN"
      |            },
      |            "extraInfo" : {
      |                "isUkRegisteredTaxOrNino" : {
      |                    "str" : "yes"
      |                },
      |                "isHmrcAgentRegistered" : {
      |                    "str" : "yes"
      |                },
      |                "saAgentCode" : "KOOH67",
      |                "regNo" : "regNumber here",
      |                "utr" : "4000000009",
      |                "nino" : "AA000000A",
      |                "taxRegNo" : [
      |                    "anotherTaxRegNumber here",
      |                    "taxRegNumber here"
      |                ]
      |            }
      |        }
      |    },
      |    "status" : {
      |        "typeIdentifier" : "pending"
      |    },
      |    "relatedAuthProviderIds" : [
      |        "9865690"
      |    ]
      |}
      |]""".stripMargin


 def acceptedApplication =
  s"""[
     |{
     |    "applicationReference" : {
     |        "value" : "25eaab89"
     |    },
     |    "applicationCreationDate" : "2019-02-20",
     |    "application" : {
     |        "amls" : {
     |            "supervisoryBody" : "International Association of Bookkeepers (IAB)",
     |            "supervisionMemberId" : "0987654321"
     |        },
     |        "contactDetails" : {
     |            "firstName" : "Testing",
     |            "lastName" : "Agent",
     |            "jobTitle" : "Tester",
     |            "businessTelephone" : "011565438754",
     |            "businessEmail" : "test@test.com"
     |        },
     |        "businessDetail" : {
     |            "tradingName" : "Testing Agency",
     |            "businessAddress" : {
     |                "addressLine1" : "addressLine1",
     |                "addressLine2" : "addressLine2",
     |                "addressLine3" : "addressLine3",
     |                "addressLine4" : "addressLine4",
     |                "countryCode" : "TN"
     |            },
     |            "extraInfo" : {
     |                "isUkRegisteredTaxOrNino" : {
     |                    "str" : "yes"
     |                },
     |                "isHmrcAgentRegistered" : {
     |                    "str" : "yes"
     |                },
     |                "saAgentCode" : "KOOH67",
     |                "regNo" : "regNumber here",
     |                "utr" : "4000000009",
     |                "nino" : "AA000000A",
     |                "taxRegNo" : [
     |                    "anotherTaxRegNumber here",
     |                    "taxRegNumber here"
     |                ]
     |            }
     |        }
     |    },
     |    "status" : {
     |        "typeIdentifier" : "accepted"
     |    },
     |    "relatedAuthProviderIds" : [
     |        "9865690"
     |    ],
     |    "maintainerReviewedOn" : "2019-02-18",
     |    "reviewerPid" : "ID53421"
     |}
     |]""".stripMargin

 def applicationInRedirectStatus(redirectStatus: String) =
  s"""[
     |{
     |    "applicationReference" : {
     |        "value" : "25eaab89"
     |    },
     |    "applicationCreationDate" : "2019-02-20",
     |    "application" : {
     |        "amls" : {
     |            "supervisoryBody" : "International Association of Bookkeepers (IAB)",
     |            "supervisionMemberId" : "0987654321"
     |        },
     |        "contactDetails" : {
     |            "firstName" : "Testing",
     |            "lastName" : "Agent",
     |            "jobTitle" : "Tester",
     |            "businessTelephone" : "011565438754",
     |            "businessEmail" : "test@test.com"
     |        },
     |        "businessDetail" : {
     |            "tradingName" : "Testing Agency",
     |            "businessAddress" : {
     |                "addressLine1" : "addressLine1",
     |                "addressLine2" : "addressLine2",
     |                "addressLine3" : "addressLine3",
     |                "addressLine4" : "addressLine4",
     |                "countryCode" : "TN"
     |            },
     |            "extraInfo" : {
     |                "isUkRegisteredTaxOrNino" : {
     |                    "str" : "yes"
     |                },
     |                "isHmrcAgentRegistered" : {
     |                    "str" : "yes"
     |                },
     |                "saAgentCode" : "KOOH67",
     |                "regNo" : "regNumber here",
     |                "utr" : "4000000009",
     |                "nino" : "AA000000A",
     |                "taxRegNo" : [
     |                    "anotherTaxRegNumber here",
     |                    "taxRegNumber here"
     |                ]
     |            }
     |        }
     |    },
     |    "status" : {
     |        "typeIdentifier" : "$redirectStatus"
     |    },
     |    "relatedAuthProviderIds" : [
     |        "9865690"
     |    ],
     |    "maintainerReviewedOn" : "2019-02-18",
     |    "reviewerPid" : "ID53421"
     |}
     |]""".stripMargin


 def allRejected =
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
    |        "applicationCreationDate": "2019-01-21",
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

 def notAllRejected =

  """[
    |    {
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
    |            "typeIdentifier": "pending"
    |        },
    |        "relatedAuthProviderIds": [
    |            "newuser"
    |        ]
    |    },
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
    |             {
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
