# agent-overseas-application Frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-overseas-application-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agents-overseas-application-frontend/_latestVersion)

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENT_MTD -f
    sm --stop AGENTS_OVERSEAS_APPLICATION_FRONTEND
    sbt run

It should then be listening on port 9404

    browse http://localhost:9404/agent-services/apply-from-outside-uk

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
