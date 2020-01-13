# EHRbase

EHRbase is an [openEHR](openehr.org) Clinical Data Repository, providing a standard-based backend for interoperable clinical applications. It implements the latest version of the openEHR Reference Model (RM 1.0.4) and version 1.4 of the Archetype Definition Language (ADL). Applications can use the capabilities of EHRbase through the latest version of the [openEHR REST API](https://specifications.openehr.org/releases/ITS-REST/latest/) and model-based queries using the [Archetype Query Language](https://specifications.openehr.org/releases/QUERY/latest/AQL.html).

## Release Notes
##### 2019, October 10
This initial release is an alpha version. This means that most endpoints of the official openEHR REST API are implemented. DIRECTORY and EHR_STATUS will become available within a few couple of weeks. There are known issues and we are heavily working to get to beta status. Please see [EHRbase Documentation](https://docs.ehrbase.org/en/latest/) for further details. For now, we encourage the use of EHRbase for the purpose of development of openEHR applications and to make trial implementations of changes applicable to the openEHR Specification. Deployment in production with real patient data is not recommended yet.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. Please read these instructions carefully. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

You will need Java JDK/JRE 8 (preferably openJDK: e.g. from https://adoptopenjdk.net/)

You will need a Postgres Database (Docker image or local installation). We recommend the Docker image to get started quickly.

When installing locally, the Postgres Database (at least Version 10.4) needs the flowing extensions:
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

> NOTE: Building EHRbase requires a proper and running DB for the following steps.

Run `./db-setup/createdb.sql` as `postgres` User.

You can also use this Docker image which is a preconfigured  Postgres database:
```
    docker run --name ehrdb -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 ehrbaseorg/ehrbase-database-docker:11.5
```
#### 2. Setup Maven environment

Edit the database properties in  `./pom.xml` if necessary

#### 3. Build EHRbase
Run `mvn package`

#### 4. Run EHRbase

Replace the * with the current version, e.g. `application/target/application-0.8.0.jar`

`java -jar application/target/application-*.jar`


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

EHRbase heavily relies on the openEHR Reference Model implementation ([Archie](https://github.com/openEHR/archie)) made by Nedap. Many thanks to Pieter Bos and his team for this marvellous piece of software!

EHRbase is jointly developed by [Vitasystems GmbH](https://www.vitagroup.ag/de_DE/Ueber-uns/vitasystems) and [Peter L. Reichertz Institute for Medical Informatics of TU Braunschweig and Hannover Medical School](plri.de) 
