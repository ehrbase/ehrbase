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

Updating an `EHR_STATUS` or `FOLDER` did not check the `If-Match header` against the DB. This allowed to pass in an 
invalid identifier that does not match the existing in the DB. This may have lead to inconsistent data in some systems. 

To check if any `EHR_STATUS` or `FOLDER` is affected run [ehrbase_2.7.0_check_ehr_status_and_folder_void](db_scripts/ehrbase_2.7.0_check_ehr_status_and_folder_void.sql).
In case you see an output like:
```text
Inconsistent EHR_STATUS found
Inconsistent FOLDER found
```
Please open an issue so that a fix can be provided.

## EHRbase 2.10.0

Starting from version 2.0.0 the ehrscape API was deprecated. 
With the release of version 2.10.0, the API is now disabled by default,
but can still be enabled by setting the configuration property or environment variable `ehrbase.rest.ehrscape.enabled` to `true`.

A validation that compositions only contain nodes that are defined by the template has been added.
This behavior can be disabled by setting the configuration property or environment variable `ehrbase.validation.checkForExtraNodes` to `false`.

## EHRbase 2.11.0

The new data model for FOLDER items requires a full migration of the ehr_folder_data table, which may take a while.
As only local VERSIONED_COMPOSITION references are supported, existing data is rewritten accordingly.
If entries of FOLDER.items.id.value exist that do not comply with the UUID format, the migration will fail.
These entries will have to be fixed manually.

## EHRbase 2.13.0

The openEHR terminology has been update to version 2.4.0.
As the code for the composition category 'episodic' has changed from 435 to 451, existing templates and compositions may be inconsistent.
For smaller systems this can be checked via AQL:
`SELECT c/archetype_details/template_id/value, count(*) FROM COMPOSITION c WHERE c/category/code/value = '435'`.
Please open an issue on github in case you need instructions on correcting the data.
