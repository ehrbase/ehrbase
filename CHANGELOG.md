# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

## [0.11.0] (alpha)

### Added

- Docker and docker-compose support for both application and database
- Get folder with version_at_time parameter
- Get Folder with path parameter

### Changed

- FasterXML Jackson version raised to 2.10.2

### Fixed

- Response code when composition is logically deleted (see: https://github.com/ehrbase/ehrbase/pull/144)
- Response and `PREFER` header handling of `/ehr` endpoints (see: https://github.com/ehrbase/ehrbase/pull/165)
- Deserialization of EhrStatus attributes is_modifiable and is_queryable are defaulting to `true` now (see: https://github.com/ehrbase/ehrbase/pull/158)
- Updating of composition with invalid template (e.g. completely different template than the previous version) (see: https://github.com/ehrbase/ehrbase/pull/166)
- Folder names are checked for duplicates (see: https://github.com/ehrbase/ehrbase/pull/168)

## [0.10.0] (alpha)


## [0.9.0] (pre-alpha)


[unreleased]: https://github.com/ehrbase/ehrbase/compare/v0.10.0...HEAD
[0.10.0]: https://github.com/ehrbase/ehrbase/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/ehrbase/ehrbase/releases/tag/v0.9.0

