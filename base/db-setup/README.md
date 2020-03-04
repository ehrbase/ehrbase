# Notes on Deploying EHRBASE DB on managed PostgreSQL servers (Cloud Configuration)

C. Chevalley 23/09/2019

Most, if not all, cloud providers provide PostgreSQL as managed DB server, integrating commonly supported extensions. 
However, more specific extensions including 'jsquery' and 'temporal_tables' are generally not supported by these
vendors.

EHRbase does support deployment in the cloud with the following provisions:

1. Extension 'temporal_tables' is replaced by a compatible plpg/sql function: versioning()
2. Extension 'jsquery' is bypassed at AQL processing level by a configuration parameter

## Deployment on a Cloud Service
Assuming a managed database for PostgreSQL (10+) is available, the DB can be configured with the following scripts:

`# cd base/db-setup`

`sudo -u postgres psql < cloud-db-setup.sql`

This configures ehrbase db with the following output:

```markdown
CREATE DATABASE
GRANT
You are now connected to database "ehrbase" as user "postgres".
CREATE SCHEMA
CREATE SCHEMA
CREATE EXTENSION
CREATE EXTENSION
ALTER DATABASE
GRANT
CREATE FUNCTION
```

The DB is now ready to be configured using the flyway migrations:

`# cd ..`

`mvn flyway:migrate`

The process configures tables, indexes, triggers as required for ehrbase.

## Configuring the Application to Enable SQL Querying Without Extension
To direct EHRbase to perform AQL querying without `jsquery` edit the YAML application configuration.

For a cloud deployment, use `application-cloud.yml` (in `application/resources`) that read:

```markdown
server:
  ...
  aql:
    use-jsquery: false
```

This can be achieved during test by adding the application parameter specifying the configuration to use:

`--spring.config.location=classpath:application-cloud.yml`

For example:

`java -jar application/target/application-0.8.0.jar --spring.config.location=classpath:application-cloud.yml`

Alternatively, the parameter can be passed as an argument to the command line:

`java -jar application/target/application-0.8.0.jar -server.aql.use-jsquery=true`

By default, `server.aql.use-jsquery` is set to `true`, setting it to false indicate to the AQL processor to use standard
json path resolution instead of jsquery resolution in a WHERE clause. For more details on this see 

- https://github.com/postgrespro/jsquery
- https://pgxn.org/dist/jsquery/
- https://www.postgresql.org/docs/current/functions-json.html


