# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]
 ### Added
* Added `STORED_QUERY_CACHE` ([#1258](https://github.com/ehrbase/ehrbase/pull/1258))
 ### Changed 
* Changed `StoredQueryRepository` methods to only accept `StoredQueryQualifiedName` as arguments ([#1258](https://github.com/ehrbase/ehrbase/pull/1258))
 ### Fixed 
* Fixed an issue with AQL, which caused NPEs when the query required adding filtering subqueries on a DV_ORDERED path ([#1293](https://github.com/ehrbase/ehrbase/pull/1293))
* Delete Contribution now returns a 501 Not Implemented instead of 500 as it's not supported since 2.0.0 ([#1278](https://github.com/ehrbase/ehrbase/pull/1278))

## [2.0.0]
  Welcome to EHRbase 2.0.0. This major release contains a complete overhaul of the data structure and 
  the Archetype Query Language (AQL) engine.
  
  See [UPDATING.md](./UPDATING.md) for details on how to update to the new release.

[unreleased]: https://github.com/ehrbase/ehrbase/compare/v2.0.0...HEAD
