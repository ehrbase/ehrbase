# Configuration for running with YugaByteDB
C.Chevalley March 2022

## Installation
See https://download.yugabyte.com/local#linux (depending on your OS), or use Docker.

```
<>:~/yugabyte-2.13.0.0$ ./bin/yugabyted start
Starting yugabyted...
Found files ['yb-data'] in data dir /home/christian/var/data from possibly failed initialization. Removing...
✅ System checks

+--------------------------------------------------------------------------------------------------+
|                                            yugabyted                                             |
+--------------------------------------------------------------------------------------------------+
| Status              : Running. Leader Master is present                                          |
| Web console         : http://127.0.0.1:7000                                                      |
| JDBC                : jdbc:postgresql://127.0.0.1:5433/yugabyte?user=yugabyte&password=yugabyte  |
| YSQL                : bin/ysqlsh   -U yugabyte -d yugabyte                                       |
| YCQL                : bin/ycqlsh   -u cassandra                                                  |
| Data Dir            : /home/christian/var/data                                                   |
| Log Dir             : /home/christian/var/logs                                                   |
| Universe UUID       : b2a4288b-09d6-4c30-9ea1-9fba10f5e5e8                                       |
+--------------------------------------------------------------------------------------------------+
🚀 yugabyted started successfully! To load a sample dataset, try 'yugabyted demo'.
🎉 Join us on Slack at https://www.yugabyte.com/slack
👕 Claim your free t-shirt at https://www.yugabyte.com/community-rewards/


<>:~/yugabyte-2.13.0.0$ bin/ysqlsh -U yugabyte < ../IdeaProjects/ehrbase/base/db-setup/cloud-db-setup.sql
CREATE ROLE
CREATE DATABASE
GRANT
You are now connected to database "ehrbase" as user "yugabyte".
CREATE SCHEMA
CREATE SCHEMA
CREATE EXTENSION
ALTER DATABASE
ALTER DATABASE
GRANT
CREATE FUNCTION

For the following, please see branch feature/experimental_sharding for details on 
how to configure flyway.conf

<>:/mnt/A0E245CAE245A4FE/devel/flyway-8.4.4$ ./flyway migrate
A new version of Flyway is available
Upgrade to Flyway 8.5.4: https://rd.gt/2X0gakb
Flyway Community Edition 8.4.4 by Redgate
Database: jdbc:postgresql://localhost:5433/ehrbase (PostgreSQL 11.2)
Executing SQL callback: beforeValidate -
Successfully validated 2 migrations (execution time 00:00.019s)
Creating Schema History table "ehr"."flyway_schema_history" ...
DB: making create index for table "flyway_schema_history" nonconcurrent
Executing SQL callback: beforeMigrate -
Current version of schema "ehr": << Empty Schema >>
Migrating schema "ehr" to version "1 - empty"
Migrating schema "ehr" to version "2 - init ehrbase"
+------------+
| set_config |
+------------+
|            |
+------------+

Successfully applied 2 migrations to schema "ehr", now at version v2 (execution time 00:57.424s)

-->
post migration!
GRANT ALL ON TABLE ehr.flyway_schema_history TO ehrbase;
```
Run EHRbase as usual (after compiling it with this new flyway migration setting!). At the
moment, all integration tests are successful (including versioning).

NB1: there is an issue with indexing on UDT (not yet supported) The following SQL in `V2__init_ehrbase.sql` 
requires attention:

```sql
CREATE INDEX context_setting_idx ON ehr.event_context USING btree (setting);
```
For the time being it is commented out.

NB2: test `CartesianProductQueryIT` may fail, but this is not due to this approach and rather 
an issue with the test code, and passes if run separately.