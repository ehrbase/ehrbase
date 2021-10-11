# Updating EHRbase

This file documents any backwards-incompatible changes in EHRBase deployment and
assists users migrating to a new version.

## EHRbase 1.0.0
### Database Configuration
The creation of the DB must ensure that SQL interval type is ISO-8601 compliant. This is required to ensure proper
formatting of the resultset. 
Scripts provided ensure this encoding is done properly (see `base/db-setup`) with the following statement:
```
-- ensure INTERVAL is ISO8601 encoded
alter database ehrbase SET intervalstyle = 'iso_8601';
```