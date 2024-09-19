# Updating EHRbase

This file documents any backwards-incompatible changes in EHRBase and assists users migrating to a new version.

## EHRbase 2.0.0

### Migrating data

EHRbase 2.0.0 comes with a completely overhauled data structure that is not automatically migrated when deploying this 
new version over an older data structure.

To support the migrating of data from systems `pre-2.0.0` to `2.0.0`, a migration tool and instructions are provided 
at https://github.com/ehrbase/migration-tool. 


## EHRbase 2.7.0

### EHR_STATUS and FOLDER consistency check

Updating an `EHR_STATUS` or `FOLDER` did not check the `If-Match header` against the DB. This allowed to pass an uid 
contained header that does not match the existing uid in the DB.
This may have lead to inconsistent data in some systems. A manual migration script is provided to, first check if a
data fix is needed and secondly run a migration to fix the uid issues.

To check if any `EHR_STATUS` or `FOLDER` is affected run [ehrbase_2.7.0_check_ehr_status_and_folder_void](scripts/db/ehrbase_2.7.0_check_ehr_status_and_folder_void.sql).
If you see an output like this `Inconsistent EHR_STATUS found` or `Inconsistent FOLDER found` run 
[ehrbase_2.7.0_fix_ehr_status_and_folder_void](scripts/db/ehrbase_2.7.0_fix_ehr_status_and_folder_void.sql), otherwise no migration is needed.
