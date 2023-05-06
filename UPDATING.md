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

### Switch to native Postgres

Before 0.21.0 EHRbase offered two setups, one using the
extensions   [temporal tables](https://github.com/arkhipov/temporal_tables),
[jsquery](https://github.com/postgrespro/jsquery) and one without. With 0.21.0 EHRbase now always runs against a plain
postgres.
To migrate a postgres without those extensions run `base/db-setup/migrate_to_cloud_db_setup.sql`. This is not needed if
you used the old `base/db-setup/cloud_db_setup.sql`
or run the EHRbase Postgres docker image.

### Fix Duplicate User issue
Prior to release 0.21.0, EHRbase contained a bug that creates a new internal user for each request.

The execution of the Flyway migration script `V71__merge_duplicate_users.sql` may take its time as the duplicates are
being consolidated.

## EHRbase 0.24.0

### Switch to non-privileged user for DB Access

Prior to 0.24.0 used one user for DDL Statements and to run the application's logic. With 0.24.0 these are run with different Users with different DB Privileges.
To migrate run adjust the password in `base/db-setup/add_restricted_user.sql` and run it as DB-Admin in the ehrbase DB. 
After that adjust the ehrbase Properties:

Set the migration to use the user with DDL Privilege:
```
spring:
  flyway:
    user: ehrbase
    password: ehrbase
```
And set the application to use the restricted user
```
spring:
  datasource:
    username: ehrbase_restricted
    password: ehrbase_restricted
```

If you use the official Docker image you can also set this via

```
    environment:
      DB_URL: jdbc:postgresql://ehrdb:5432/ehrbase
      DB_USER_ADMIN: ehrbase
      DB_PASS_ADMIN: ehrbase
      DB_USER: ehrbase_restricted
      DB_PASS: ehrbase_restricted
```

see `\docker-compose.yml `

## EHRbase 0.25.0

### Switch to new directory structure

With release 0.25.0 a new Structure to store EHR directory was introduced. There is no automatic migration of old EHR
directory data into the new structure.
If you used EHR directory and are fine with losing this data you can run in postgres as admin

```
-- remove Ehr directory!!!

begin;
alter table ehr.ehr drop column if exists directory;
TRUNCATE ehr.folder, ehr.folder_hierarchy, ehr.folder_items,ehr.folder_history,ehr.folder_items_history,ehr.folder_hierarchy_history;
alter table ehr.ehr add column directory uuid references ehr.folder(id);
commit ;
```

If you need to migrate old EHR directory data please contact us.

## EHRbase 0.27.0

With release 0.27.0 the multi-tenancy implementation has been updated to allow two EHRs, compositions, 
or directories with the same ID to exist in different tenants. This was achieved by replacing the tenant UUID 
with an internal number-based ID and adding it to the primary key.

Please note that executing the Flyway migration script `V83__change_sys_tenant_to_short.sql` may take some time.