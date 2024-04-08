# Updating EHRbase

This file documents any backwards-incompatible changes in EHRBase and assists users migrating to a new version.

## EHRbase 2.0.0

### Migrating data

EHRbase 2.0.0 comes with a completely overhauled data structure that is not automatically migrated when deploying this 
new version over an older data structure.

To support the migrating of data from systems `pre-2.0.0` to `2.0.0`, a migration tool and instructions are provided 
at https://github.com/ehrbase/migration-tool. 