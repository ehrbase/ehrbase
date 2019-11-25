# aql_queries_valid

Contains sample queries in four groups:

- A. get EHRs
- B. get COMPOSITIONs
- C. get ENTRIES
- D. get DATA_VALUEs

Some queries are the same query written in different ways, trying to cover what is allowed by the AQL syntax.


# expected_results/empty_db

Contains expected results for each query, considering the database is empty: there are no COMPOSITIONs or EHRs.

Those examples were created by using EHRSCAPE as the reference implementation. Some of the queries are not processed correctly by EHRSCAPE and the result doesn't give any context about the errors. The failing queries are:

- A/109
- B/103
- B/702
- B/800
- B/801
- B/803
- C/100
- C/101
- C/102
- C/103
- C/200
- C/300
- C/301
- C/302
- C/303
- D/306
- D/307
- D/308
- D/309
- D/310
- D/311
- D/502
- D/503


# expected_results/loaded_db

Contains expected request for each query, considering the database has pre-lodaded data, which is provided in the folder ../data_load (there are 10 EHRs and 15 COMPOSITIONs to load into each EHR). And the required templates are in ../valid_templates/minimal

The failing queries in EHRSCAPE are the same for these expected results, so we need to create the expected results by hand for those.
