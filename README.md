# EHRbase 
![Maven Central](https://img.shields.io/maven-central/v/org.ehrbase.openehr/server) ![Docker Image Version (latest semver)](https://img.shields.io/docker/v/ehrbase/ehrbase?sort=semver) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ehrbase_ehrbase&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ehrbase_ehrbase) [![Documentation Status](https://readthedocs.org/projects/ehrbase/badge/?version=latest)](https://ehrbase.readthedocs.io/en/latest/?badge=latest) [![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md) 

[![EHRbase Logo](ehrbase.png)](ehrbase.png) 

EHRbase is an [openEHR](https://www.openehr.org/) Clinical Data Repository, providing a standard-based backend for interoperable clinical applications. It implements the latest version of the openEHR Reference Model (RM 1.1.0) and version 1.4 of the Archetype Definition Language (ADL). Applications can use the capabilities of EHRbase through the latest version of the [openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest/) and model-based queries using the [Archetype Query Language](https://specifications.openehr.org/releases/QUERY/latest/AQL.html).

----

## Release notes

Please check the [CHANGELOG](https://github.com/ehrbase/ehrbase/blob/develop/CHANGELOG.md) and / or   [EHRbase Documentation](https://ehrbase.readthedocs.io/en/latest/) for more details.

## Documentation
[EHRbase Documentation](https://ehrbase.readthedocs.io/en/latest/) is build with Sphinx and hosted on [Read the Docs](https://readthedocs.org/).

## Quick Start: Run EHRbase with Docker
See our [Run EHRbase + DB with Docker-Compose](https://ehrbase.readthedocs.io/en/latest/03_development/04_docker_images/01_ehrbase/02_use_image/index.html#run-ehrbase-db-with-docker-compose) documentation page for a quick start.

## Acknowledgments

EHRbase contains code and derived code from EtherCIS (ethercis.org) which has been developed by Christian Chevalley (ADOC Software Development Co.,Ltd).
Dr. Tony Shannon and Phil Berret of the [Ripple Foundation CIC Ltd, UK](https://ripple.foundation/) and Dr. Ian McNicoll (FreshEHR Ltd.) greatly contributed to EtherCIS. 

EHRbase heavily relies on the openEHR Reference Model implementation ([Archie](https://github.com/openEHR/archie)) made by Nedap. Many thanks to Pieter Bos and his team for their work!

EHRbase is jointly developed by [Vitasystems GmbH](https://www.vitagroup.ag/de_DE/Ueber-uns/vitasystems) and [Peter L. Reichertz Institute for Medical Informatics of TU Braunschweig and Hannover Medical School](https://www.plri.de/)

----

## Building and Installing EHRbase
These instructions will get you a copy of the project up and running on your local machine **for development and testing purposes**. Please read these instructions carefully. See [deployment](#deployment) for notes on how to deploy the project on a live system.

### Prerequisites

You will need Java JDK/JRE 17 (preferably openJDK: e.g. from https://adoptopenjdk.net/)

You will need a Postgres Database (at least Version 10.4, Version 13 recommended) (Docker image or local installation).
We recommend the Docker
image to get started quickly.


### Installing

#### 1. Setup database

> NOTE: Building EHRbase requires a properly set up and running DB for the following steps.

Run `./base/db-setup/createdb.sql` as `postgres` User.

You can also use this Docker image which is a preconfigured Postgres database:
```shell
    docker network create ehrbase-net
    docker run --name ehrdb --network ehrbase-net -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 ehrbase/ehrbase-postgres:13.4.v2
```

(For a preconfigured EHRbase application Docker image and its usage see the [documentation](https://ehrbase.readthedocs.io/en/latest/03_development/04_docker_images/index.html))
#### 2. Setup Maven environment

Edit the database properties in  `./pom.xml` if necessary

#### 3. Build EHRbase
Run `mvn package`

#### 4. Run EHRbase

Replace the * with the current version, e.g. `application/target/application-0.9.0.jar`

`java -jar application/target/application-*.jar`

### Authentication Types

#### 1. Basic Auth

EHRbase can use Basic Authentication for all resources. This means you have to send an 'Authorization' header
set with keyword `Basic` followed by the authentication information in Base64 encoded username and password. To
generate the Base64 encoded username and password combination create the string after the following schema:
`username:password`.

The Basic Auth mechanism is implemented as "opt-in" and can be activated either by providing an environment variable
`SECURITY_AUTHTYPE=BASIC` with the start command or by adding the value into the target application.yml file.

Currently we have support one user with password which can be set via environment variables `SECURITY_AUTHUSER` and
`SECURITY_AUTHPASSWORD`. By default these values are set with `ehrbase-user` and `authPassword=SuperSecretPassword`
and can be overridden by environment values. Alternatively you can set them inside the corresponding application.yml
file.

The same applies to the *admin* user, via `SECURITY_AUTHADMINUSER`, `SECURITY_AUTHADMINPASSWORD` 
and their default values of `ehrbase-admin` and `EvenMoreSecretPassword`.

#### 2. OAuth2

Environment variable `SECURITY_AUTHTYPE=OAUTH` is enabling OAuth2 authentication.

Additionally, setting the following variable to point to the existing OAuth2 server and realm is necessary:
`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI=http://localhost:8081/auth/realms/ehrbase`


Two roles are available: a user role, and admin role. By default, these roles are expected to be named `USER` and
`ADMIN`. The names of these roles can be customised through the `SECURITY_OAUTH2USERROLE` and `SECURITY_OAUTH2ADMINROLE`
environment variables. Users should have their roles assigned accordingly, either in the `realm_access.roles` or `scope`
claim of the JWT used for authentication.

## Contributing

### Codestyle/Formatting
EHRbase java sourcecode is using [palantir-java-format](https://github.com/palantir/palantir-java-format) codestyle. 
The formatting is checked and applied using the [spotless-maven-plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven). 
To apply the codestyle run the `com.diffplug.spotless:spotless-maven-plugin:apply` maven goal in the root directory of the project.
To check if the code conforms to the codestyle run the `com.diffplug.spotless:spotless-maven-plugin:check` maven goal in the root directory of the project.
These maven goals can also be run for a single module by running them in the modules' subdirectory.

To make sure all code conforms to the codestyle, the "check-codestyle" check is run on all pull requests. 
Pull requests not passing this check shall not be merged.

If you wish to automatically apply the formatting on commit for *.java files, a simple pre-commit hook script "pre-commit.sh" is available in the root directory of this repository.
To enable the hook you can either copy the script to or create a symlink for it at `.git/hooks/pre-commit`. 
The git hook will run the "apply" goal for the whole project, but formatting changes will only be staged for already staged files, to avoid including unrelated changes.

In case there is a section of code that you carefully formatted in a special way the formatting can be turned off for that section like this:
```
everything here will be reformatted..

// @formatter:off

    This is not affected by spotless-plugin reformatting...
            And will stay as is it is!

// @formatter:on

everything here will be reformatted..
```
Please be aware that `@formatter:off/on` should only be used on rare occasions to increase readability of complex code and shall be looked at critically when reviewing merge requests.

## Running the tests

For integration tests please refer to the [integration-test](https://github.com/ehrbase/integration-tests) repository

## Deployment

 1. `java -jar application/target/application-*.jar` You can override the application properties (like database settings) using the normal spring boot mechanism: [Command-Line Arguments in Spring Boot](https://www.baeldung.com/spring-boot-command-line-arguments)
 2. Browse to Swagger UI --> http://localhost:8080/ehrbase/swagger-ui.html

## Updating

Before updating to a new version of EHRBase check [UPDATING.md](UPDATING.md) for any backwards-incompatible changes and additional
steps needed in EHRBase. New Releases may introduce DB changes. It is thus recommend to make a DB backup before
updating.

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management


----

## License

EHRbase uses the Apache License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0)



## Stargazers over time

[![Stargazers over time](https://starchart.cc/ehrbase/ehrbase.svg)](https://starchart.cc/ehrbase/ehrbase)
