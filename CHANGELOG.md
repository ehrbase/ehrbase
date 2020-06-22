# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

## [0.13.0] (beta)

### Added
- added support for various functions in AQL (aggregation, statistical, string etc.) (see: https://github.com/ehrbase/ehrbase/pull/223/)

### Changed
- Update of AQL-Query test suite (see: https://github.com/ehrbase/ehrbase/pull/179)

### Fixed
- Added validation checking for other_details and ehr_status. (see: https://github.com/ehrbase/ehrbase/pull/207)
- Supports archetype_node_id and name for EHR_STATUS (see: https://github.com/ehrbase/ehrbase/pull/207)
- fixes bad canonical encoding for observation/data/origin (see: https://github.com/ehrbase/ehrbase/pull/213)
- POST without accept header for ehr, composition and contribution endpoints (see: https://github.com/ehrbase/ehrbase/pull/199)
- force a default timezone if not present for context/start_time and context/end_time if specified (https://github.com/ehrbase/ehrbase/pull/215)
- Representation of version uid of EHR_STATUS (see: https://github.com/ehrbase/ehrbase/pull/180)
- Refactored support of PartyProxy and ObjectId in both CRUD and AQL operations (see https://github.com/ehrbase/ehrbase/pull/248)


## [0.12.0] (alpha)

### Added

- Basic Authentication as opt-in (see: https://github.com/ehrbase/ehrbase/pull/200)
- Allow Templates can now be overwritten via spring configuration (see: https://github.com/ehrbase/ehrbase/pull/194)

### Fixed

- Contribution endpoint checks for some invalid input combinations (see: https://github.com/ehrbase/ehrbase/pull/202)
- Fixes response code on /ehr PUT with invalid ID (see: https://github.com/ehrbase/project_management/issues/163)
- Fixes STATUS w/ empty subject bug (see: https://github.com/ehrbase/ehrbase/pull/196)
- Now querying on composition category returns the correct result (composition/category...)
- Fixes storage of party self inside compositions (see: https://github.com/ehrbase/ehrbase/pull/195)
- Added support of AQL query in the form of c/composer (see: https://github.com/ehrbase/ehrbase/pull/184)
- Java error with UTF-8 encoding resolved (see: https://github.com/ehrbase/ehrbase/pull/173)
- AQL refactoring and fixes to support correct canonical json representation (see: https://github.com/ehrbase/ehrbase/pull/201)
- fix terminal value test for non DataValue 'value' attribute (see: https://github.com/ehrbase/ehrbase/pull/189)

## [0.11.0] (alpha)

**Note:** Due to the transition to this changelog the following list is not complete. 
Starting with the next release this file will provide a proper overview.

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

- Response code when composition is logically deleted (see: https://github.com/ehrbase/ehrbase/pull/144)
- Response and `PREFER` header handling of `/ehr` endpoints (see: https://github.com/ehrbase/ehrbase/pull/165)
- Deserialization of EhrStatus attributes is_modifiable and is_queryable are defaulting to `true` now (see: https://github.com/ehrbase/ehrbase/pull/158)
- Updating of composition with invalid template (e.g. completely different template than the previous version) (see: https://github.com/ehrbase/ehrbase/pull/166)
- Folder names are checked for duplicates (see: https://github.com/ehrbase/ehrbase/pull/168)
- AQL parser threw an unspecific exception when an alias was used in a WHERE clause (https://github.com/ehrbase/ehrbase/pull/149)
- Improved exception handling in composition validation (see: https://github.com/ehrbase/ehrbase/pull/147)
- Improved Reference Model validation (see: https://github.com/ehrbase/ehrbase/pull/147)
- Error when reading a composition that has a provider name set(see: https://github.com/ehrbase/ehrbase/pull/143)
- Allow content to be null inside a composition (see: https://github.com/ehrbase/ehrbase/pull/129)
- Fixed deletion of compositions through a contribution (see: https://github.com/ehrbase/ehrbase/pull/128)
- Start time of a composition was not properly updated (see: https://github.com/ehrbase/ehrbase/pull/137)
- Fixed validation of null values on participations (see: https://github.com/ehrbase/ehrbase/pull/132)
- Order by in AQL did not work properly (see: https://github.com/ehrbase/ehrbase/pull/112)
- Order of variables in AQL result was not preserved (see: https://github.com/ehrbase/ehrbase/pull/103)
- Validation of compositions for unsupported language(see: https://github.com/ehrbase/ehrbase/pull/107)
- Duplicated ehr attributes in query due to cartesian product (see: https://github.com/ehrbase/ehrbase/pull/106)
- Retrieve of EHR_STATUS gave Null Pointer Exception for non-existing EHRs (see: https://github.com/ehrbase/ehrbase/pull/136)
- Correct resolution of ehr/system_id in AQL (see: https://github.com/ehrbase/ehrbase/pull/102)
- Detection of duplicate aliases in aql select (see: https://github.com/ehrbase/ehrbase/pull/98)


## [0.10.0] (alpha)

### Added

- openEHR REST API DIRECTORY Endpoints
- openEHR REST API EHR_STATUS Endpoints (including other_details)
- Spring Transactions: EHRbase now ensures complete rollback if part of a transaction fails.
- Improved Template storage: openEHR Templates are stored inside the postgres database instead of the file system (including handling of duplicates)
- AQL queries with partial paths return data in canonical json format (including full compositions)
- Multimedia data can be correctly stored and retrieved
- Spring configuration allows setting the System ID
- Validation of openEHR Terminology (openEHR terminology codes are tested against an internal terminology service)

###Fixed

- Order of columns in AQL result sets are now reliably preserved (https://github.com/ehrbase/ehrbase/issues/37)
- Some projection issues for EHR attributes have been resolved in AQL
- Fixed error regarding DISTINCT operator in AQL (https://github.com/ehrbase/ehrbase/issues/50)
- Fixed null pointer exceptions that could occur in persistent compositions

## [0.9.0] (pre-alpha)

### Added

- openEHR REST API DIRECTORY Endpoints
- openEHR REST API EHR_STATUS Endpoints (including other_details)
- Spring Transactions: EHRbase now ensures complete rollback if part of a transaction fails.
- Improved Template storage: openEHR Templates are stored inside the postgres database instead of the file system (including handling of duplicates)
- AQL queries with partial paths return data in canonical json format (including full compositions)
- Multimedia data can be correctly stored and retrieved
- Spring configuration allows setting the System ID
- Validation of openEHR Terminology (openEHR terminology codes are tested against an internal terminology service)

###Fixed

- Order of columns in AQL result sets are now reliably preserved (https://github.com/ehrbase/ehrbase/issues/37)
- Some projection issues for EHR attributes have been resolved in AQL
- Fixed error regarding DISTINCT operator in AQL (https://github.com/ehrbase/ehrbase/issues/50)
- Fixed null pointer exceptions that could occur in persistent compositions

[unreleased]: https://github.com/ehrbase/ehrbase/compare/v0.13.0...HEAD
[0.13.0]: https://github.com/ehrbase/ehrbase/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/ehrbase/ehrbase/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/ehrbase/ehrbase/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/ehrbase/ehrbase/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/ehrbase/ehrbase/releases/tag/v0.9.0

