# EHRbase

EHRbase is an [openEHR](openehr.org) Clinical Data Repository, providing a standard-based backend for interoperable clinical applications. It implements the latest version of the openEHR Reference Model (RM 1.0.4) and version 1.4 of the Archetype Definition Language (ADL). Applications can use the capabilities of EHRbase through the latest version of the [openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest/) and model-based queries using the [Archetype Query Language](https://specifications.openehr.org/releases/QUERY/latest/AQL.html).

## Release Notes

##### 2021, February 25
v0.15.0 - third **beta** release. Please see [EHRbase Documentation](https://ehrbase.readthedocs.io/en/latest/) for further details.


##### 2020, October 1
v0.14.0 - second **beta** release. Please see [EHRbase Documentation](https://ehrbase.readthedocs.io/en/latest/) for further details.


##### 2020, May 14
This release of EHRbase (v0.13.0) is the first **beta** release. Please see [EHRbase Documentation](https://ehrbase.readthedocs.io/en/latest/) for further details.


## 📝 Documentation
[EHRbase Documentation](https://ehrbase.readthedocs.io/en/latest/) is build with Sphinx and hosted on [Read the Docs](https://readthedocs.org/).

## Quick Start: Run EHRbase with Docker
See our [Run EHRbase + DB with Docker-Compose](https://ehrbase.readthedocs.io/en/latest/03_development/04_docker_images/01_ehrbase/02_use_image/index.html#run-ehrbase-db-with-docker-compose) documentation page for a quick start.

## Building and Installing EHRbase
These instructions will get you a copy of the project up and running on your local machine **for development and testing purposes**. Please read these instructions carefully. See [deployment](#deployment) for notes on how to deploy the project on a live system.

### Prerequisites

You will need Java JDK/JRE 11 (preferably openJDK: e.g. from https://adoptopenjdk.net/)

You will need a Postgres Database (Docker image or local installation). We recommend the Docker image to get started quickly.

When installing locally, the Postgres Database (at least Version 10.4) needs the following extensions:
 * [temporal tables](https://github.com/arkhipov/temporal_tables) 
    ```bash
     git clone https://github.com/arkhipov/temporal_tables.git
     make
     sudo make install
     make installcheck
     ```
 * [jsquery](https://github.com/postgrespro/jsquery) 
     ```bash
     git clone https://github.com/postgrespro/jsquery.git
     cd jsquery
     make USE_PGXS=1
     sudo make USE_PGXS=1 install
     make USE_PGXS=1 installcheck
    ```

### Installing

#### 1. Setup database

> NOTE: Building EHRbase requires a properly set up and running DB for the following steps.

Run `./db-setup/createdb.sql` as `postgres` User.

You can also use this Docker image which is a preconfigured Postgres database:
```shell
    docker network create ehrbase-net
    docker run --name ehrdb --network ehrbase-net -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 ehrbase/ehrbase-postgres:latest
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

In the given OAuth2 server configuration the roles `user` and `admin` are expected to be configured. 
Users should have their roles assigned accordingly.

## Running the tests

This command will run all tests from `tests/robot` folder.
DB and server application will be started/stopped by the tests accordingly. You *must not* start them by hand.

> NOTE: Make sure you meet the PREREQUISITES mentioned in tests/README.md prior to test execution.
>
> Please Check the README in `tests` folder for more details.

```bash
cd tests
./run_local_tests.sh
```


## Deployment

 1. `java -jar application/target/application-*.jar` You can override the application properties (like database settings) using the normal spring boot mechanism: [Command-Line Arguments in Spring Boot](https://www.baeldung.com/spring-boot-command-line-arguments)
 2. Browse to Swagger UI --> http://localhost:8080/ehrbase/swagger-ui.html



## Built With

* [Maven](https://maven.apache.org/) - Dependency Management



## License

EHRbase uses the Apache License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0)

## Acknowledgments

EHRbase contains code and derived code from EtherCIS (ethercis.org) which has been developed by Christian Chevalley (ADOC Software Development Co.,Ltd).
Dr. Tony Shannon and Phil Berret of the [Ripple Foundation CIC Ltd, UK](https://ripple.foundation/) and Dr. Ian McNicoll (FreshEHR Ltd.) greatly contributed to EtherCIS. 

EHRbase heavily relies on the openEHR Reference Model implementation ([Archie](https://github.com/openEHR/archie)) made by Nedap. Many thanks to Pieter Bos and his team for their work!

EHRbase is jointly developed by [Vitasystems GmbH](https://www.vitagroup.ag/de_DE/Ueber-uns/vitasystems) and [Peter L. Reichertz Institute for Medical Informatics of TU Braunschweig and Hannover Medical School](plri.de)


## Stargazers over time

[![Stargazers over time](https://starchart.cc/ehrbase/ehrbase.svg)](https://starchart.cc/ehrbase/ehrbase)
