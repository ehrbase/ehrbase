# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.19.0]
 ### Added
 ### Changed 
- Publishing artifacts directly to central.sonatype.com [#1512](https://github.com/ehrbase/ehrbase/pull/1512)
 ### Fixed 

## [2.18.0]
 ### Added
 ### Changed 
 ### Fixed 
- Add missing test dependency scope to assertj [#1502](https://github.com/ehrbase/ehrbase/pull/1502)
- Allow for retrieving COMPOSITIONs containing ELEMENTs with FEEDER_AUDIT [#1506](https://github.com/ehrbase/ehrbase/pull/1506)

## [2.17.0]
 ### Added
 ### Changed 
 ### Fixed 

## [2.16.0]
 ### Added
 ### Changed 
 ### Fixed 
 - Parsing of invalid temporal aql parameters [#1489](https://github.com/ehrbase/ehrbase/pull/1489)

## [2.15.0]
 ### Added
 ### Changed 
 ### Fixed
 - Prevent concurrent creation of multiple EHRs with same patient reference [#1475](https://github.com/ehrbase/ehrbase/pull/1475)

## [2.14.0]
 ### Added
 ### Changed 
 ### Fixed 
- fix difference in Web template between openEhr API and etercis API  [#1470](https://github.com/ehrbase/ehrbase/pull/1470)

## [2.13.0]
 ### Added
 ### Changed 
 ### Fixed 
* Fix AQL handling for "FOLDER CONTAINS VERSION CONTAINS COMPOSITION" [#1459](https://github.com/ehrbase/ehrbase/pull/1459)

## [2.12.0]
 ### Added
 ### Changed 
* Added check for duplicate version IDs during Folder creation as well as check for Folder uid and if-match header id during update ([#1410](https://github.com/ehrbase/ehrbase/pull/1410))
* Remove EHR from AQL if not needed for the query ([#1448](https://github.com/ehrbase/ehrbase/pull/1448))
* Update flyway to V11 to Gracefully exit in case installed flyway DB is newer than supported ([#1450](https://github.com/ehrbase/ehrbase/pull/1450))
 ### Fixed 
* Allow Template overwrite with renamed property from `system.allow-template-overwrite` to `ehrbase.template.allow-overwrite` ([#1440](https://github.com/ehrbase/ehrbase/pull/1440))

## [2.11.0]
 ### Added
 ### Changed 
* EHR directory: Restriction of FOLDER items to only contain local VERSIONED_COMPOSITION references ([#1433](https://github.com/ehrbase/ehrbase/pull/1433))
 ### Fixed 

## [2.10.0]
 ### Added
* Revived option to pre-fill template cache (`ehrbase.cache.template-init-on-startup`, default: `false`) ([#1394](https://github.com/ehrbase/ehrbase/pull/1394))
* Added experimental `AQL` support for `FOLDER` (`ehrbase.aql.experimental.aqlOnFolder`, default: `false`) ([#1401](https://github.com/ehrbase/ehrbase/pull/1401))
 ### Changed
* Feature toggle for ehrscape API (`ehrbase.rest.ehrscape.enabled`, default: `false`) ([#1415](https://github.com/ehrbase/ehrbase/pull/1415))
 ### Fixed
* Validate that compositions only contain nodes that are defined by the template (`ehrbase.validation.checkForExtraNodes`, default: `true`)  ([#1424](https://github.com/ehrbase/ehrbase/pull/1424))

## [2.9.0]
 ### Added
 ### Changed 
 ### Fixed
* Improved transaction-awareness of caches ([#1407](https://github.com/ehrbase/ehrbase/pull/1407))

## [2.8.1]
 ### Added
 ### Changed 
 ### Fixed 
* Fixed HTTP 500 in case stored query does not exist [#1409](https://github.com/ehrbase/ehrbase/pull/1409)

## [2.8.0]
 ### Added
* AQL: Support query parameter with multiple values ([1399](https://github.com/ehrbase/ehrbase/pull/1399))
 ### Changed 
* Enable user access to the welcome page while authentication is enabled ([#1400](https://github.com/ehrbase/ehrbase/pull/1400))
 ### Fixed 
* Stored AQL queries: Correctly clean up query cache when storing queries ([#1405](https://github.com/ehrbase/ehrbase/pull/1405))
* Updating an `EHR_STATUS` or `FOLDER` did not check the `If-Match header` ([1398](https://github.com/ehrbase/ehrbase/pull/1398))

## [2.7.0]
 ### Added
* Experimental ItemTag REST endpoints for EHR_STATUS and COMPOSITION (configs: `ehrbase.rest.experimental.tags.*`) ([1343](https://github.com/ehrbase/ehrbase/pull/1343))
* CLI runner with support for flyway pre-migrations ([1387](https://github.com/ehrbase/ehrbase/pull/1387))
 ### Changed 
 ### Fixed 
* Require EHR_STATUS `is_queryable` and `is_modifiable` to be present ([#1377](https://github.com/ehrbase/ehrbase/pull/1377)) 

## [2.6.0]
 ### Added
 ### Changed 
* Improved data structure for hierarchy of versioned objects ([#1359](https://github.com/ehrbase/ehrbase/pull/1359))
* Upgrade openEHR_SDK to version 2.15.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md
 ### Fixed 

## [2.5.0]
 ### Added
* Create a `ehrbase` user to run the Docker container ([#1336](https://github.com/ehrbase/ehrbase/pull/1336))
 ### Changed
* Deprecate plugin aspects ([#1344](https://github.com/ehrbase/ehrbase/pull/1344))
* Add simplified JSON-based “web template” format support for GET Template ADL 1.4 using header `Accept: application/openehr.wt+json` ([1334](https://github.com/ehrbase/ehrbase/pull/1334))
* Improved AQL performance ([#1358](https://github.com/ehrbase/ehrbase/pull/1358))
* Upgrade openEHR_SDK to version 2.14.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md

 ### Fixed
* Return `201` instead of `204` for EHR creation ([1371](https://github.com/ehrbase/ehrbase/pull/1371))
* Fixed AQL predicate reduction logic ([#1358](https://github.com/ehrbase/ehrbase/pull/1358))
* Respect AQL root predicates ([#1358](https://github.com/ehrbase/ehrbase/pull/1358))

## [2.4.0]
 ### Added
- Configurable flyway migration strategy
- Configurable fetch limit checks + default limit for AQL queries
- Configurable fetch limit precedence strategy for AQL queries
 ### Changed 
 - Upgrade openEHR_SDK to version 2.13.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
 ### Fixed 

## [2.3.0]
 ### Added
 ### Changed 
 - Upgrade openEHR_SDK to version 2.12.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md
* AQL-Performance: paths containing non-locatable structure attributes (EVENT_CONTEXT, FEEDER_AUDIT) ([#1341](https://github.com/ehrbase/ehrbase/pull/1341))
* Removed `@Schema(MediaType.class)` Header declaration from swagger UI ([#1333](https://github.com/ehrbase/ehrbase/pull/1333))
 ### Fixed

## [2.2.0]
### Added
* Added AQL debug support ([#1296](https://github.com/ehrbase/ehrbase/pull/1296))
### Changed 
 - Upgrade openEHR_SDK to version 2.11.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md
* The field `q` of AQL query responses now contain the requested, and not the executed, query string  ([#1296](https://github.com/ehrbase/ehrbase/pull/1296))
* The field `meta._schema_version` of AQL query responses has been changed to `1.0.3`  ([#1296](https://github.com/ehrbase/ehrbase/pull/1296))
* Return HTTP 422 Unprocessable Content in case fetch or offset is defined inside the aql query and as parameter ([#1325](https://github.com/ehrbase/ehrbase/pull/1325)).
### Fixed

## [2.1.0]
 ### Added
* Added `STORED_QUERY_CACHE` ([#1258](https://github.com/ehrbase/ehrbase/pull/1258))
* Added new config option `ehrbase.security.management.endpoints.web.csrf-validation-enabled` ([#1294](https://github.com/ehrbase/ehrbase/pull/1294),[#1297](https://github.com/ehrbase/ehrbase/pull/1297))
 ### Changed 
 - Upgrade openEHR_SDK to version 2.10.0 see https://github.com/ehrbase/openEHR_SDK/blob/develop/CHANGELOG.md 
* Changed `StoredQueryRepository` methods to only accept `StoredQueryQualifiedName` as arguments ([#1258](https://github.com/ehrbase/ehrbase/pull/1258))
 ### Fixed 
* Fixed an issue with AQL, which caused NPEs when the query required adding filtering subqueries on a DV_ORDERED path ([#1293](https://github.com/ehrbase/ehrbase/pull/1293))
* Delete Contribution now returns a 501 Not Implemented instead of 500 as it's not supported since 2.0.0 ([#1278](https://github.com/ehrbase/ehrbase/pull/1278))

## [2.0.0]
  Welcome to EHRbase 2.0.0. This major release contains a complete overhaul of the data structure and 
  the Archetype Query Language (AQL) engine.
  
  See [UPDATING.md](./UPDATING.md) for details on how to update to the new release.

[2.1.0]: https://github.com/ehrbase/ehrbase/compare/v2.0.0...v2.1.0
[2.2.0]: https://github.com/ehrbase/ehrbase/compare/v2.1.0...v2.2.0
[2.3.0]: https://github.com/ehrbase/ehrbase/compare/v2.2.0...v2.3.0
[2.4.0]: https://github.com/ehrbase/ehrbase/compare/v2.3.0...v2.4.0
[2.5.0]: https://github.com/ehrbase/ehrbase/compare/v2.4.0...v2.5.0
[2.6.0]: https://github.com/ehrbase/ehrbase/compare/v2.5.0...v2.6.0
[2.7.0]: https://github.com/ehrbase/ehrbase/compare/v2.6.0...v2.7.0
[2.8.0]: https://github.com/ehrbase/ehrbase/compare/v2.7.0...v2.8.0
[2.8.1]: https://github.com/ehrbase/ehrbase/compare/v2.8.0...v2.8.1
[2.9.0]: https://github.com/ehrbase/ehrbase/compare/v2.8.1...v2.9.0
[2.10.0]: https://github.com/ehrbase/ehrbase/compare/v2.9.0...v2.10.0
[2.11.0]: https://github.com/ehrbase/ehrbase/compare/v2.10.0...v2.11.0
[2.12.0]: https://github.com/ehrbase/ehrbase/compare/v2.11.0...v2.12.0
[2.13.0]: https://github.com/ehrbase/ehrbase/compare/v2.12.0...v2.13.0
[2.14.0]: https://github.com/ehrbase/ehrbase/compare/v2.13.0...v2.14.0
[2.15.0]: https://github.com/ehrbase/ehrbase/compare/v2.14.0...v2.15.0
[2.16.0]: https://github.com/ehrbase/ehrbase/compare/v2.15.0...v2.16.0
[2.17.0]: https://github.com/ehrbase/ehrbase/compare/v2.16.0...v2.17.0
[2.18.0]: https://github.com/ehrbase/ehrbase/compare/v2.17.0...v2.18.0
[2.19.0]: https://github.com/ehrbase/ehrbase/compare/v2.18.0...v2.19.0
