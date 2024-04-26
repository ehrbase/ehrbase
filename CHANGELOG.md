# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]
### Added
### Changed
### Fixed

## [2.1.0]
 ### Added
* Added `STORED_QUERY_CACHE` ([#1258](https://github.com/ehrbase/ehrbase/pull/1258))
* Added new config option `ehrbase.security.management.endpoints.web.csrf-validation-enabled` ([#1294](https://github.com/ehrbase/ehrbase/pull/1294),[#1297](https://github.com/ehrbase/ehrbase/pull/1297))
* Added AQL debug support ([#1296](https://github.com/ehrbase/ehrbase/pull/1296))
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
[unreleased]: https://github.com/ehrbase/ehrbase/compare/v2.1.0...HEAD
