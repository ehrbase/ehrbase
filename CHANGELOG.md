# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]
 ### Added
 ### Changed 
 ### Fixed 

## [0.28.0]
 ### Added

 - Added support tenant deletion ([#1146](https://github.com/ehrbase/ehrbase/pull/1146))

 ### Changed 
 - Upgrade openEHR_SDK to version 2.0.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md

 - Audit log functionality has been integrated as a plugin ([#1131](https://github.com/ehrbase/ehrbase/pull/1131))
 - update to SDK 2.0.0-SNAPSHOT ([#1141](https://github.com/ehrbase/ehrbase/pull/1141))
 - move dbencoding from SDK to ehrbase ([#1141](https://github.com/ehrbase/ehrbase/pull/1141))
 - adjust to new SDK package paths ([#1141](https://github.com/ehrbase/ehrbase/pull/1141))

 ### Fixed 

## [0.27.4]
 ### Added
 ### Changed 
 - Upgrade openEHR_SDK to version 1.29.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 ### Fixed 
 - Run directory operation in one transaction ([#1133](https://github.com/ehrbase/ehrbase/pull/1133))

## [0.27.3]
 ### Added
 ### Changed 
 - Upgrade openEHR_SDK to version 1.28.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 ### Fixed 

## [0.27.2]
 ### Added
 ### Changed 
 - Upgrade openEHR_SDK to version 1.28.0-SNAPSHOT see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 - fix flyway_checksum for script 83  ([#1130](https://github.com/ehrbase/ehrbase/pull/1130))
 ### Fixed 

## [0.27.1]
 ### Added
 ### Changed 
 - Upgrade openEHR_SDK to version 1.28.0-SNAPSHOT see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 ### Fixed 
 - Fix DB Migration scripts. Remove the tenant RLS policy during script execution ([#1127](https://github.com/ehrbase/ehrbase/pull/1127)).

## [0.27.0]
 ### Added
 - Sub-folders are retained in the response upon updating Folders. ([#1108](https://github.com/ehrbase/ehrbase/pull/1108))
 ### Changed 
 - Upgrade openEHR_SDK to version 1.27.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 - Changed namespace UUID to number-based ID and added to the primary key in each entity. ([#1100](https://github.com/ehrbase/ehrbase/pull/1100))
 ### Fixed 
 - Fixes NPE if during start up with many templates in the system.  ([#1101](https://github.com/ehrbase/ehrbase/pull/1101))

## [0.26.0]
 ### Added
 - use from sdk archie version 3.0.0 and antlr4 version 4.11.1 ([#1078](https://github.com/ehrbase/ehrbase/pull/1078))
 ### Changed 
 - Upgrade openEHR_SDK to version 1.26.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 - Respect the predefined value of the composition UID when creating new composition ([#1090](https://github.com/ehrbase/ehrbase/pull/1090))
 ### Fixed 

## [0.25.0]
 ### Added
 ### Changed 
 - Upgrade openEHR_SDK to version 1.25.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 - new Directory implementation ([#1059](https://github.com/ehrbase/ehrbase/pull/1059))
 ### Fixed 
 - error on concurrent user creation ([#1067](https://github.com/ehrbase/ehrbase/pull/1067))

## [0.24.0]

### Added

- add caches for ehr.concept, ehr.territory, ehr.language([#1007](https://github.com/ehrbase/ehrbase/pull/1007))
- add stored query text plain request support ([#1021](https://github.com/ehrbase/ehrbase/pull/1021))
- unwrap if match additional double quotes ([#1022](https://github.com/ehrbase/ehrbase/pull/1022))
- general support for tenants and authorization for plugins
- Expose management and swagger endpoints publicly([#1030](https://github.com/ehrbase/ehrbase/pull/1030))
- Include tenant information in ATNA logs([#1055](https://github.com/ehrbase/ehrbase/pull/1055))

### Changed 
 - Upgrade openEHR_SDK to version 1.24.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md

- use caffeine cache instead of ehcache as ehcache has unnecessary
  blocking([#1007](https://github.com/ehrbase/ehrbase/pull/1007))
- remove unnecessary DB queries([#1007](https://github.com/ehrbase/ehrbase/pull/1007))
- Performance improvements of the composition audit.([#1042](https://github.com/ehrbase/ehrbase/pull/1042))
- improved versioning of stored queries ([#1050](https://github.com/ehrbase/ehrbase/pull/1050))
- switch to non-privileged user for DB Access  ([#11064](https://github.com/ehrbase/ehrbase/pull/1064))

### Fixed

- Update spring-boot(spring-security), postgresql, pf4j-spring dependency version ([#1060](https://github.com/ehrbase/ehrbase/pull/1060))
- Update jackson dependency version ([#1063](https://github.com/ehrbase/ehrbase/pull/1063))
- maintain a correct history of participations([#1016](https://github.com/ehrbase/ehrbase/pull/1016))
- Fixed NullPointerException when language is missing ([#1023](https://github.com/ehrbase/ehrbase/pull/1023))
- Endpoint for storing queries gives a detailed error regarding incorrect
  version([#1032](https://github.com/ehrbase/ehrbase/pull/1032))
- Fixes NullPointerException while sending ehr status request(GET/PUT) when abac is
  enabled ([#1031](https://github.com/ehrbase/ehrbase/pull/1031))
- Fixed status update if party already exist for ehr([#1024](https://github.com/ehrbase/ehrbase/pull/1024))
- Change response code from 400 to 406 Not Acceptable on querying POST template endpoint with unsupported `Accept`
  header application/json([#1029](https://github.com/ehrbase/ehrbase/pull/1029))
- Fixed abac default tenant claims validation([#1041](https://github.com/ehrbase/ehrbase/pull/1041))
- Fixed incorrect path in Location http header([#1044](https://github.com/ehrbase/ehrbase/pull/1044))
- Fixed get deleted composition returns 500([#1048](https://github.com/ehrbase/ehrbase/pull/1048))

## [0.23.0]

### Added

- Authorization enhancements [#1002](https://github.com/ehrbase/ehrbase/pull/1002)
    - Impl. API extensions needed for authorization
    - Added annotations on all REST endpoints to support authorization decissions

### Changed

- Upgrade openEHR_SDK to version 1.24.0-SNAPSHOT see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md
- JAVA 17 baseline
- Spring Boot 2.7.4
- Upgrade openEHR_SDK to version 1.24.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md

### Fixed

## [0.22.0]

### Added

- Add spotless plugin, Add codestyle check to workflows ([#864](https://github.com/ehrbase/ehrbase/pull/864))

### Changed

- Upgrade openEHR_SDK to version 1.23.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md
- Change DB-model to save Data in a namespace ([#994](https://github.com/ehrbase/ehrbase/pull/994))

### Fixed

- Error causing a 500 Response when requesting a deleted composition via ECIS GET Composition
  Endpoint ([#875](https://github.com/ehrbase/ehrbase/pull/875))
- Update folder was not always corectly updating it items ([#974](https://github.com/ehrbase/ehrbase/pull/974))
- AuditDetails had timezone missing ([#998](https://github.com/ehrbase/ehrbase/pull/998))
- numerus AQL
  fixes ([#1001](https://github.com/ehrbase/ehrbase/pull/1001), [#1006](https://github.com/ehrbase/ehrbase/pull/1006))

## [0.21.1]

### Fixed

- Fixed update script for user consolidation ([#865](https://github.com/ehrbase/ehrbase/pull/865))

## [0.21.0]

### Added

- Implement template example endpoints ([#801](https://github.com/ehrbase/openEHR_SDK/pull/801))
- Implement EHR_STATUS.is_modifiable semantics on service
  level ([#791](https://github.com/ehrbase/openEHR_SDK/pull/791))
- use bom for dependence management  ([#820](https://github.com/ehrbase/ehrbase/pull/820))
- add Release action  ([#831](https://github.com/ehrbase/ehrbase/pull/831)
- Added hooks for the plugin system ([#816](https://github.com/ehrbase/ehrbase/pull/816))
- Added index to `party_identified` to improve performance of find EHR by
  subject-id ([857](https://github.com/ehrbase/ehrbase/pull/857)))

### Changed

- Upgrade to Spring boot 2.5.12
  see [spring-framework-rce](https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement) ([#800](https://github.com/ehrbase/ehrbase/pull/800))
  .
- Add unique constraints on `status` and `entry` ([#821](https://github.com/ehrbase/ehrbase/pull/821)).
- Removed Postgres with extensions setup ([#840](https://github.com/ehrbase/ehrbase/pull/840))
- Upgrade openEHR_SDK to version 1.19.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md

### Fixed

- Handle 4xx status code related Spring MVC Exceptions, instead of making them all a 500, and handle
  ResponseStatusException ([#803](https://github.com/ehrbase/openEHR_SDK/pull/803))
- Fix duplicate users issue ([#826](https://github.com/ehrbase/ehrbase/pull/826)).
- Fix validation errors in ECIS EHR endpoint ([#828](https://github.com/ehrbase/ehrbase/pull/828))
- Fix 400 error in ECIS EHR update ([#834](https://github.com/ehrbase/ehrbase/pull/834))

## [0.20.0] (beta)

### Added

- Add Plugins system ([#772](https://github.com/ehrbase/ehrbase/pull/772),
  [#779](https://github.com/ehrbase/ehrbase/pull/779)).
- AQL: support `ORDER BY` and `LIMIT [OFFSET]` clauses in any
  order ([#782](https://github.com/ehrbase/openEHR_SDK/pull/782)).

### Changed

- Update Archie to version 2.0.1 [#784](https://github.com/ehrbase/ehrbase/pull/784)
- Add missing database indexes [#788](https://github.com/ehrbase/ehrbase/pull/788)
  and [#796](https://github.com/ehrbase/ehrbase/pull/796)
- Upgrade openEHR_SDK to version 1.18.0 see  https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md

### Fixed

- Remove unused Operational Template cache ([#759](https://github.com/ehrbase/ehrbase/pull/759)).
- Allow update/adding/removal of feeder_audit/links on Composition ([#773](https://github.com/ehrbase/ehrbase/pull/773))
- Add default ASC direction to ORDER BY clause in AQL ([#780](https://github.com/ehrbase/ehrbase/pull/780)).
- Fix DB Migration scripts. Allow user different then ehrbase ([#795](https://github.com/ehrbase/ehrbase/pull/795)).

## [0.19.0] (beta)

### Added

- Add Flyway callback to check `IntervalStyle` configuration
  parameter ([#720](https://github.com/ehrbase/ehrbase/pull/720)).
- Validate RM types used in OPT template ([#739](https://github.com/ehrbase/ehrbase/issues/739)).

### Changed

- Upgrade to Archie 1.0.4 ([#719](https://github.com/ehrbase/ehrbase/pull/719)).
- Improve errors and exceptions logging ([#745](https://github.com/ehrbase/ehrbase/pull/745)).
- Upgrade openEHR_SDK to version 1.17.0 see  https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md

### Fixed

- Fixed SQL encoding whenever template is
  unresolved ([#723](https://github.com/ehrbase/ehrbase/issues/723))
- Modified handling of conflicting identified
  parties ([#710](https://github.com/ehrbase/ehrbase/issues/710))
- Fixes wrong status code returned by EHRbase while creating FLAT
  composition ([#726](https://github.com/ehrbase/ehrbase/pull/726))
- Fix NullPointerException while deleting unknown (or already deleted) composition
  parameter ([#722](https://github.com/ehrbase/ehrbase/pull/722)).
- Fix querying other_participations ([#707](https://github.com/ehrbase/ehrbase/issues/707))

## [0.18.3] (beta)

### Added

### Changed

- removed log4j (see https://github.com/ehrbase/ehrbase/pull/711)

### Fixed

## [0.18.2] (beta)

### Fixed

- updated log4j from 1.15.0 to 1.60.0

## [0.18.1] (beta)

### Fixed

- Fix deployment issue with Flyway migration V62__add_entry_history_missing_columns.sql

## [0.18.0] (beta)

### Added

- Migrated to Archie openEHR library version > 1.0.0, incl. its new strict invariant checks (
  see: https://github.com/ehrbase/ehrbase/pull/570)
- Support Structured format on ecis composition endpoints (
  see https://github.com/ehrbase/ehrbase/pull/648)
- Add new configuration options to customise user/admin role names when using OAuth authentication
  (see https://github.com/ehrbase/ehrbase/pull/667)
- Add configuration properties to customize CORS configuration (
  see https://github.com/ehrbase/ehrbase/pull/697)

### Changed

### Fixed

- Missing details in response returned by Directory REST API (
  see: https://github.com/ehrbase/ehrbase/pull/605)
- Add foreign key between `folder` and `ehr` tables (
  see: https://github.com/ehrbase/ehrbase/pull/616)
- Improves 'Admin Delete EHR' performance (see https://github.com/ehrbase/ehrbase/pull/626)
- many fixes to the flat support (see https://github.com/ehrbase/ehrbase/pull/627)
- Fix conversion between `DvDateTime` and `Timestamp` (
  see https://github.com/ehrbase/ehrbase/pull/634)
- Fix FLAT format does not return the archetype data if the archetype_id contains the letters "and"
- Datetime inconsistent handling (see https://github.com/ehrbase/ehrbase/pull/649)
- Fix issue using DV_DATE_TIME without time-zone (see https://github.com/ehrbase/ehrbase/pull/658)
- update lg4j version (see https://github.com/ehrbase/ehrbase/pull/702)

## [0.17.2] (beta)

### Added

- Github Action worklows to deploy multiarch images (`latest`, `next`, `version-tag`) to Docker
  Hub (
  see: https://github.com/ehrbase/ehrbase/pull/578)

### Changed

- Removes SELECT statement when PartyProxy object is empty (
  see: https://github.com/ehrbase/ehrbase/pull/581)
- Provide configuration properties for configuring context paths of openEHR REST API and Admin API (
  see: https://github.com/ehrbase/ehrbase/pull/585)

### Fixed

- `Accept` header with multiple MIME types causes an IllegalArgumentException (
  see: https://github.com/ehrbase/ehrbase/pull/583)
- Composition version Uid schema in EhrScape API (see: https://github.com/ehrbase/ehrbase/pull/520)
- Terminology Service calls from within AQL queries does not work (
  see: https://github.com/ehrbase/ehrbase/pull/572)

## [0.17.1] (beta)

### Added

- Default handling for audit metadata (see: https://github.com/ehrbase/ehrbase/pull/552)

### Changed

- Updated the SDK dependency to the latest version (
  see: https://github.com/ehrbase/ehrbase/pull/565)
- Refactored versioned object (interfaces) on service and access layer (
  see: https://github.com/ehrbase/ehrbase/pull/552)

### Fixed

- Assigner in DV_IDENTIFIER not selected in aql (see: https://github.com/ehrbase/ehrbase/pull/561)
- ehr_status.uuid not selects via aql (see: https://github.com/ehrbase/ehrbase/pull/561)
- DB migration file conflict (see: https://github.com/ehrbase/ehrbase/pull/564)
- Ddmin delete of multiple status versions (see: https://github.com/ehrbase/ehrbase/pull/552)

## [0.17.0] (beta)

### Added

- Implement validation of compositions using external FHIR TS (
  see: https://github.com/ehrbase/ehrbase/pull/493)
- Support for Attribute-based Access Control (see: https://github.com/ehrbase/ehrbase/pull/499)
- Support AQL array resolution in EHR_STATUS::other_details

### Changed

- Update paths for Admin API, Management API and `/status` endpoint (
  see: https://github.com/ehrbase/ehrbase/pull/541)

### Fixed

- Folder handling (update, delete and missing audits) (
  see: https://github.com/ehrbase/ehrbase/pull/529)
- Fixed and refactored handling of audits and versioned objects (
  see: https://github.com/ehrbase/ehrbase/pull/552/)

## [0.16.0] (beta)

### Added

- Endpoints and integration tests for VERSIONED_COMPOSITION (
  see: https://github.com/ehrbase/ehrbase/pull/448)
- ATNA Logging for composition endpoints, querying and operations on the EHR object (
  see: https://github.com/ehrbase/ehrbase/pull/452)
- EHRbase Release Checklist (see: https://github.com/ehrbase/ehrbase/pull/451)
- CACHE_ENABLED ENV to Dockerfile (see: https://github.com/ehrbase/ehrbase/pull/467)

### Changed

- Updated the SDK dependency to the latest version (
  see: https://github.com/ehrbase/ehrbase/pull/463)
- Force retrieval of operational template from DB (see: https://github.com/ehrbase/ehrbase/pull/468)

### Fixed

- WHERE field construct (see: https://github.com/ehrbase/ehrbase/pull/439)
- Inconsistent behavior in SMICS Virology Query (see: https://github.com/ehrbase/ehrbase/pull/456)
- Bunch of AQL issues (see: https://github.com/ehrbase/ehrbase/pull/461)
- AQL: Error in processing OR in Contains clause (see: https://github.com/ehrbase/ehrbase/pull/462)
- Cache issue on Startup (see: https://github.com/ehrbase/ehrbase/pull/465)

## [0.15.0] (beta)

### Added

- Adds Admin API endpoints: Del EHR, Del Composition and Del Contribution (
  see: https://github.com/ehrbase/ehrbase/pull/344)
- Add ATNA logging configuration capabilities (see https://github.com/ehrbase/ehrbase/pull/355)
- Support for EHR_STATUS and (partial) FOLDER version objects in contributions (
  see: https://github.com/ehrbase/ehrbase/pull/372)
- Add status endpoint to retrieve version information on running EHRbase instance and for heartbeat
  checks. (
  see: https://github.com/ehrbase/ehrbase/pull/393)
- Add /status/info endpoint using actuator for basic info on running app (
  see: https://github.com/ehrbase/ehrbase/pull/400)
- Add /status/health endpoint for kubernetes liveness and readiness probes (
  see: https://github.com/ehrbase/ehrbase/pull/400)
- Add /status/env endpoint for environment information (
  see: https://github.com/ehrbase/ehrbase/pull/400)
- Add /status/metrics endpoint for detailed metrics on specific topics (db connection, http
  requests, etc.) (
  see: https://github.com/ehrbase/ehrbase/pull/400)
- Add /status/prometheus endpoint for prometheus metrics (
  see: https://github.com/ehrbase/ehrbase/pull/400)
- Endpoints and integration tests for VERISONED_EHR_STATUS (
  see: https://github.com/ehrbase/ehrbase/pull/415)

### Changed

- support AQL querying on full EHR (f.e. SELECT e) (see )
- Update Dockerfile for usage with metrics and status (
  see https://github.com/ehrbase/ehrbase/pull/408)
- Refactored DB handling of contributions, removed misleading `CONTIRUBITON_HISTORY` table (
  see https://github.com/ehrbase/ehrbase/pull/416)

## [0.14.0] (beta)

### Added

- Add admin API endpoint stubs (see: https://github.com/ehrbase/ehrbase/pull/280)
- Add support for FeederAudit in Locatable. Refactored Composition Serializer for DB encoding (
  see https://github.com/ehrbase/ehrbase/tree/feature/311_feeder_audit
  , https://github.com/ehrbase/openEHR_SDK/tree/feature/311_feeder_audit)
- Change the strategy to resolve CONTAINS in AQL (https://github.com/ehrbase/ehrbase/pull/276)
- Add admin template API functionality (see: https://github.com/ehrbase/ehrbase/pull/301)
- Persist caches to java.io.tmpdir  (see: https://github.com/ehrbase/ehrbase/pull/308)
- Precalculate containment tree from OPT template (see https://github.com/ehrbase/ehrbase/pull/312)

### Changed

- Detection of duplicate directories on EHR on POST
- Using ObjectVersionId for DIRECTORY Controller and Service Layers (
  see: https://github.com/ehrbase/ehrbase/pull/297)
- Added Junit5 support via spring-boot-starter-test (https://github.com/ehrbase/ehrbase/pull/298)
- Enable cartesian products on embedded arrays in JSONB (
  see https://github.com/ehrbase/ehrbase/pull/309)
- Use new OPT-Parser from sdk (see https://github.com/ehrbase/ehrbase/pull/314)
- Add CORS config to enable clients to detect auth method (
  see https://github.com/ehrbase/ehrbase/pull/354).

### Fixed

- Detect duplicates on POST Directory (see: https://github.com/ehrbase/ehrbase/pull/281)
- Support context-less composition (see: https://github.com/ehrbase/ehrbase/pull/288)
- Fixed missing AQL level of parenthesis when using NOT in WHERE clause (
  see https://github.com/ehrbase/ehrbase/pull/293)
- Allow duplicated paths in AQL resultsets (see: https://github.com/ehrbase/ehrbase/issues/263)
- Transaction timestamps are now truncated to ms (see: https://github.com/ehrbase/ehrbase/pull/299)
- Change response code on not found directory to 412 if not found (
  see: https://github.com/ehrbase/ehrbase/pull/304)

## [0.13.0] (beta)

### Added

- Added support for various functions in AQL (aggregation, statistical, string etc.) (
  see: https://github.com/ehrbase/ehrbase/pull/223/)

### Changed

#### DIRECTORY

- PreconditionFailed error response contains proper ETag and Location headers (
  see: https://github.com/ehrbase/ehrbase/pull/183)

#### Robot Tests

- Update of AQL-Query test suite (see: https://github.com/ehrbase/ehrbase/pull/179)

### Fixed

- force a default timezone if not present for context/start_time and context/end_time if
  specified (https://github.com/ehrbase/ehrbase/pull/215)
- Representation of version uid of EHR_STATUS (see: https://github.com/ehrbase/ehrbase/pull/180)
- Refactored support of PartyProxy and ObjectId in both CRUD and AQL operations (
  see https://github.com/ehrbase/ehrbase/pull/248)
- fix support of mandatory attributes in ENTRY specialization including rm_version (
  see https://github.com/ehrbase/ehrbase/pull/247)

#### DIRECTORY

- Directory IDs from input path or If-Match header must now be in version_uid format (
  see https://github.com/ehrbase/ehrbase/pull/183)
- Folder IDs inside body are now parsed correctly (see: https://github.com/ehrbase/ehrbase/pull/183)
- PreconditionFailed error response contains proper ETag and Location headers (
  see: https://github.com/ehrbase/ehrbase/pull/183)

#### Robot Tests

- Added validation checking for other_details and ehr_status. (
  see: https://github.com/ehrbase/ehrbase/pull/207)
- Supports archetype_node_id and name for EHR_STATUS (
  see: https://github.com/ehrbase/ehrbase/pull/207)
- fixes bad canonical encoding for observation/data/origin (
  see: https://github.com/ehrbase/ehrbase/pull/213)
- POST without accept header for ehr, composition and contribution endpoints (
  see: https://github.com/ehrbase/ehrbase/pull/199)

## [0.12.0] (alpha)

### Added

- Basic Authentication as opt-in (see: https://github.com/ehrbase/ehrbase/pull/200)
- Allow Templates can now be overwritten via spring configuration (
  see: https://github.com/ehrbase/ehrbase/pull/194)

### Fixed

- Contribution endpoint checks for some invalid input combinations (
  see: https://github.com/ehrbase/ehrbase/pull/202)
- Fixes response code on /ehr PUT with invalid ID (
  see: https://github.com/ehrbase/project_management/issues/163)
- Fixes STATUS w/ empty subject bug (see: https://github.com/ehrbase/ehrbase/pull/196)
- Now querying on composition category returns the correct result (composition/category...)
- Fixes storage of party self inside compositions (see: https://github.com/ehrbase/ehrbase/pull/195)
- Added support of AQL query in the form of c/composer (
  see: https://github.com/ehrbase/ehrbase/pull/184)
- Java error with UTF-8 encoding resolved (see: https://github.com/ehrbase/ehrbase/pull/173)
- AQL refactoring and fixes to support correct canonical json representation (
  see: https://github.com/ehrbase/ehrbase/pull/201)
- fix terminal value test for non DataValue 'value' attribute (
  see: https://github.com/ehrbase/ehrbase/pull/189)

## [0.11.0] (alpha)

**Note:** Due to the transition to this changelog the following list is not complete. Starting with
the next release this file will provide a proper overview.

### Added

- Docker and docker-compose support for both application and database
- Get folder with version_at_time parameter
- Get Folder with path parameter

### Changed

- FasterXML Jackson version raised to 2.10.2
- Java version raised from 8 to 11
- Jooq version raised to 3.12.3
- Spring Boot raised to version 2

### Fixed

- Response code when composition is logically deleted (
  see: https://github.com/ehrbase/ehrbase/pull/144)
- Response and `PREFER` header handling of `/ehr` endpoints (
  see: https://github.com/ehrbase/ehrbase/pull/165)
- Deserialization of EhrStatus attributes is_modifiable and is_queryable are defaulting to `true`
  now (
  see: https://github.com/ehrbase/ehrbase/pull/158)
- Updating of composition with invalid template (e.g. completely different template than the
  previous version) (
  see: https://github.com/ehrbase/ehrbase/pull/166)
- Folder names are checked for duplicates (see: https://github.com/ehrbase/ehrbase/pull/168)
- AQL parser threw an unspecific exception when an alias was used in a WHERE
  clause (https://github.com/ehrbase/ehrbase/pull/149)
- Improved exception handling in composition validation (
  see: https://github.com/ehrbase/ehrbase/pull/147)
- Improved Reference Model validation (see: https://github.com/ehrbase/ehrbase/pull/147)
- Error when reading a composition that has a provider name set(
  see: https://github.com/ehrbase/ehrbase/pull/143)
- Allow content to be null inside a composition (see: https://github.com/ehrbase/ehrbase/pull/129)
- Fixed deletion of compositions through a contribution (
  see: https://github.com/ehrbase/ehrbase/pull/128)
- Start time of a composition was not properly updated (
  see: https://github.com/ehrbase/ehrbase/pull/137)
- Fixed validation of null values on participations (
  see: https://github.com/ehrbase/ehrbase/pull/132)
- Order by in AQL did not work properly (see: https://github.com/ehrbase/ehrbase/pull/112)
- Order of variables in AQL result was not preserved (
  see: https://github.com/ehrbase/ehrbase/pull/103)
- Validation of compositions for unsupported language(
  see: https://github.com/ehrbase/ehrbase/pull/107)
- Duplicated ehr attributes in query due to cartesian product (
  see: https://github.com/ehrbase/ehrbase/pull/106)
- Retrieve of EHR_STATUS gave Null Pointer Exception for non-existing EHRs (
  see: https://github.com/ehrbase/ehrbase/pull/136)
- Correct resolution of ehr/system_id in AQL (see: https://github.com/ehrbase/ehrbase/pull/102)
- Detection of duplicate aliases in aql select (see: https://github.com/ehrbase/ehrbase/pull/98)

## [0.10.0] (alpha)

### Added

- openEHR REST API DIRECTORY Endpoints
- openEHR REST API EHR_STATUS Endpoints (including other_details)
- Spring Transactions: EHRbase now ensures complete rollback if part of a transaction fails.
- Improved Template storage: openEHR Templates are stored inside the postgres database instead of
  the file system (
  including handling of duplicates)
- AQL queries with partial paths return data in canonical json format (including full compositions)
- Multimedia data can be correctly stored and retrieved
- Spring configuration allows setting the System ID
- Validation of openEHR Terminology (openEHR terminology codes are tested against an internal
  terminology service)

### Fixed

- Order of columns in AQL result sets are now reliably
  preserved (https://github.com/ehrbase/ehrbase/issues/37)
- Some projection issues for EHR attributes have been resolved in AQL
- Fixed error regarding DISTINCT operator in AQL (https://github.com/ehrbase/ehrbase/issues/50)
- Fixed null pointer exceptions that could occur in persistent compositions

## [0.9.0] (pre-alpha)

### Added

- openEHR REST API DIRECTORY Endpoints
- openEHR REST API EHR_STATUS Endpoints (including other_details)
- Spring Transactions: EHRbase now ensures complete rollback if part of a transaction fails.
- Improved Template storage: openEHR Templates are stored inside the postgres database instead of
  the file system (
  including handling of duplicates)
- AQL queries with partial paths return data in canonical json format (including full compositions)
- Multimedia data can be correctly stored and retrieved
- Spring configuration allows setting the System ID
- Validation of openEHR Terminology (openEHR terminology codes are tested against an internal terminology service)

### Fixed

- Order of columns in AQL result sets are now reliably preserved (https://github.com/ehrbase/ehrbase/issues/37)
- Some projection issues for EHR attributes have been resolved in AQL
- Fixed error regarding DISTINCT operator in AQL (https://github.com/ehrbase/ehrbase/issues/50)
- Fixed null pointer exceptions that could occur in persistent compositions

[0.21.1]: https://github.com/ehrbase/ehrbase/compare/v0.21.0...v0.21.1

[0.21.0]: https://github.com/ehrbase/ehrbase/compare/v0.20.0...v0.21.0

[0.20.0]: https://github.com/ehrbase/ehrbase/compare/v0.19.0...v0.20.0

[0.19.0]: https://github.com/ehrbase/ehrbase/compare/v0.18.3...v0.19.0

[0.18.3]: https://github.com/ehrbase/ehrbase/compare/v0.18.2...v0.18.3

[0.18.2]: https://github.com/ehrbase/ehrbase/compare/v0.18.1...v0.18.2

[0.18.1]: https://github.com/ehrbase/ehrbase/compare/v0.18.0...v0.18.1

[0.18.0]: https://github.com/ehrbase/ehrbase/compare/v0.17.2...v0.18.0

[0.17.2]: https://github.com/ehrbase/ehrbase/compare/v0.17.1...v0.17.2

[0.17.1]: https://github.com/ehrbase/ehrbase/compare/v0.17.0...v0.17.1

[0.17.0]: https://github.com/ehrbase/ehrbase/compare/v0.16.0...v0.17.0

[0.16.0]: https://github.com/ehrbase/ehrbase/compare/v0.15.0...v0.16.0

[0.15.0]: https://github.com/ehrbase/ehrbase/compare/v0.14.0...v0.15.0

[0.14.0]: https://github.com/ehrbase/ehrbase/compare/v0.13.0...v0.14.0

[0.13.0]: https://github.com/ehrbase/ehrbase/compare/v0.12.0...v0.13.0

[0.12.0]: https://github.com/ehrbase/ehrbase/compare/v0.11.0...v0.12.0

[0.11.0]: https://github.com/ehrbase/ehrbase/compare/v0.10.0...v0.11.0

[0.10.0]: https://github.com/ehrbase/ehrbase/compare/v0.9.0...v0.10.0

[0.9.0]: https://github.com/ehrbase/ehrbase/releases/tag/v0.9.0

[0.22.0]: https://github.com/ehrbase/ehrbase/compare/v0.21.1...v0.22.0

[0.23.0]: https://github.com/ehrbase/ehrbase/compare/v0.22.0...v0.23.0

[0.24.0]: https://github.com/ehrbase/ehrbase/compare/v0.23.0...v0.24.0
[0.25.0]: https://github.com/ehrbase/ehrbase/compare/v0.24.0...v0.25.0
[0.26.0]: https://github.com/ehrbase/ehrbase/compare/v0.25.0...v0.26.0
[0.27.0]: https://github.com/ehrbase/ehrbase/compare/v0.26.0...v0.27.0
[0.27.1]: https://github.com/ehrbase/ehrbase/compare/v0.27.0...v0.27.1
[0.27.2]: https://github.com/ehrbase/ehrbase/compare/v0.27.1...v0.27.2
[0.27.3]: https://github.com/ehrbase/ehrbase/compare/v0.27.2...v0.27.3
[0.27.4]: https://github.com/ehrbase/ehrbase/compare/v0.27.3...v0.27.4
[0.28.0]: https://github.com/ehrbase/ehrbase/compare/v0.27.4...v0.28.0
[unreleased]: https://github.com/ehrbase/ehrbase/compare/v0.28.0...HEAD
