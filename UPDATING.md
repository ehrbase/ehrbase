# Updating EHRbase

This file documents any backwards-incompatible changes in EHRBase and
assists users migrating to a new version.

## EHRbase 0.21.0

## Switch to native Postgres

Before 0.21.0 EHRbase offered two setups, one using the
extensions   [temporal tables](https://github.com/arkhipov/temporal_tables),
[jsquery](https://github.com/postgrespro/jsquery) and one without. With 0.21.0 EHRbase now always runs against a plain
postgres.
To migrate a postgres without those extensions run `base/db-setup/migrate_to_cloud_db_setup.sql`. This is not needed if
you used the old `base/db-setup/cloud_db_setup.sql`
or run the EHRbase Postgres docker image.

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

The Flyway migration script `V71__check_duplicate_users.sql` ensures that the database does not
contain any duplicate user.

If EHRbase failed to start because of Flyway migration, please run the script `merge_duplicate_users.sql` (
located in `base/db-setup`) in order to fix the issue.