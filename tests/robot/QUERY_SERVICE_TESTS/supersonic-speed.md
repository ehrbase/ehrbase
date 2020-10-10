


# steps to start ehrbase w/ preloaded test-data (restored db)
=============================================================

1. start postgres db container: task startdb
2. restore db from dump:
   
   ```bash
   docker exec -i ehrdb psql --username postgres ehrbase < ./dump.sql

   # OR restore all:

   docker exec -i ehrdb psql --username postgres < ./dump-all.sql
   ```

3. restore 'expected result data-sets':
   restore_cache --> tests/robot/_resources/test_data_sets/query/expected_results/loaded_db

4. start ehrbse server:         task starteb
5. run query robot tests
   
   ```bash
   robot -v SUT:DEV -e TODO -e future -L TRACE -d results --noncritical not-ready -i AQL_adhoc-queryANDloaded_db robot/QUERY_SERVICE_TESTS/
   ```




# steps to dump db (w/ ehrbase test-data) from container
========================================================

1. task startdb
2. task starteb
3. run robot tests to generate test-data and 'expected result data-sets'
   
   ```bash
   robot -v SUT:DEV -e TODO -e future -L TRACE -d results --noncritical not-ready -i AQL_adhoc-queryANDloaded_db robot/QUERY_SERVICE_TESTS/
   ```

4. dump:
   
   ```bash
   docker exec -i ehrdb pg_dump --username postgres ehrbase > ./dump.sql

   # OR dump all:

   docker exec -i ehrdb pg_dumpall --username postgres -c > ./dump-all.sql
   ```

   save 'expected result data-sets':
   save_cache --> tests/robot/_resources/test_data_sets/query/expected_results/loaded_db


# steps to create sha hash of 'expected-result-blueprints'
    ```bash
    find tests/robot/_resources/test_data_sets/query/expected_results/loaded_db/ -type f | sort | xargs cat > /tmp/expected-results-loaded_db-seed
    sha256sum /tmp/expected-results-loaded_db-seed

    >>> f5ee5a9a55c50687dafc3c3acff66089759f1577d7a5dd71aff6e60793ce91c2  /tmp/expected-results-loaded_db-seed
    ```



