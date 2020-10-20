
TODO @WLAD clean this up and move to dev docs

# steps to start ehrbase w/ preloaded test-data (restored db)
=============================================================

1. start postgres db container: task startdb
2. restore db from dump:
   
   ```bash
   # THIS WORKS!!! NOTE: make sure to start ehrbase AFTER restoring DB!!!
   docker exec -i ehrdb psql ehrbase --username postgres < ./dump.sql

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

   **ALTERNATIVE!!! (LIVE DB RESTORE WHILE EHRBASE IS CONNECTED TO DB)**
   1. start db: task startdb
   2. start ehrbase: task starteb
   3. restore db: docker exec -i ehrdb psql ehrbase --username postgres < ./dump.sql
      NOTE 1: IMPORTANT! the `-i` is crucial! w/o it DB is NOT respored! Strange, but true!
      NOTE 2: Precondition for this to work:
      DUMP DB with this options: `docker exec -i ehrdb pg_dump ehrbase --username postgres --if-exists --clean --verbose > ./dump.sql`
      otherwise db will not be restored properly!!!




# steps to DUMP DB (w/ ehrbase test-data) from container
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

   see pg_dump docs for details: https://www.postgresql.org/docs/11/app-pgdump.html
                                 PAY ATTENTION TO THE VERSION!!!
                                 SWITH DOCS VERSION AT THE TOP OF THE DOCS PAGE!!!

   save 'expected result data-sets':
   save_cache --> tests/robot/_resources/test_data_sets/query/expected_results/loaded_db


# steps to create sha hash of 'expected-result-blueprints'
    ```bash
    find tests/robot/_resources/test_data_sets/query/expected_results/loaded_db/ -type f ! -name *.tmp.json | sort | xargs cat > /tmp/expected-results-loaded_db-seed
    sha256sum /tmp/expected-results-loaded_db-seed

    >>> f5ee5a9a55c50687dafc3c3acff66089759f1577d7a5dd71aff6e60793ce91c2  /tmp/expected-results-loaded_db-seed
    ```

    more accurat alternative:

    dir=tests/robot/_resources/test_data_sets/query/expected_results/loaded_db/; find "$dir" -type f -exec sha256sum {} \; | sed "s~$dir~~g" | LC_ALL=C sort -d | sha256sum



# works locally
dir=robot/_resources/test_data_sets/query/expected_results/loaded_db/; find "$dir" -type f -exec sha256sum {} \; | sed "s~$dir~~g" | LC_ALL=C sort -d | sha256sum > hash-of-expected-result-data-sets.txt



# on ci
find tests/robot/_resources/test_data_sets/query/expected_results/loaded_db/ -type f | sort | xargs cat > /tmp/expected-results-loaded_db-seed

sha256sum /tmp/expected-results-loaded_db-seed | cat
>>> 9c8ba1649fb8160d9b560f3ee8c74466fdbe57cbd0980a9c28339338b3d4f100  /tmp/expected-results-loaded_db-seed


[ "$ACTUAL_HASH" = "$EXPECTED_HASH" ] && echo "Expected results have NOT changed! No need to regenerate test-data!" || DATA_CHANGED=True && export DATA_CHANGED


