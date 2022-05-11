# Updating EHRbase

This file documents any backwards-incompatible changes in EHRBase and
assists users migrating to a new version.

## EHRbase 0.19.0

### Database Configuration

The creation of the DB must ensure that SQL interval type is ISO-8601 compliant. This is required to ensure proper
formatting of the resultset.
Scripts provided ensure this encoding is done properly (see `base/db-setup`) with the following statement:

```
-- ensure INTERVAL is ISO8601 encoded
alter database ehrbase SET intervalstyle = 'iso_8601';
```

If an old version of the scripts was used this statement needs to be run manually.

## EHRbase 0.21.0

Prior to release 0.21.0, EHRbase contained a bug that creates a new internal user for each request.

The execution of the Flyway migration script `V70__merge_duplicate_users.sql` may take its time as the duplicates are being consolidated.