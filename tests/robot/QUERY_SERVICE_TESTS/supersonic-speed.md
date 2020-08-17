


# steps to start ehrbase w/ preloaded test-data (restored db)
=============================================================

1. start postgres db container: task startdb
2. restore db from dump:
   docker exec -i ehrdb psql --username postgres ehrbase < ./dump.sql

   OR restore all:
   docker exec -i ehrdb psql --username postgres < ./dump-all.sql

3. start ehrbse server:         task starteb
4. run query robot tests
   robot -v SUT:DEV -e TODO -e future -L TRACE -d results --noncritical not-ready -i AQL_adhoc-queryANDloaded_db robot/QUERY_SERVICE_TESTS/




# steps to dump db (w/ ehrbase test-data) from container
========================================================

1. task startdb
2. task starteb
3. run robot tests to generate test-data and 'expected result data-sets'
   robot -v SUT:DEV -e TODO -e future -L TRACE -d results --noncritical not-ready -i AQL_adhoc-queryANDloaded_db robot/QUERY_SERVICE_TESTS/

4. dump:
   docker exec -i ehrdb pg_dump --username postgres ehrbase > ./dump.sql

   OR dump all:
   docker exec -i ehrdb pg_dumpall --username postgres -c > ./dump-all.sql

   save 'expected result data-set files':
   save-cache --> tests/robot/_resources/test_data_sets/query/expected_results/loaded_db