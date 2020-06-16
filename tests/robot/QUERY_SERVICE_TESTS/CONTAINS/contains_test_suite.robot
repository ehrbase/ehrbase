# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Documentation   Main flow: this test suite is focused on testing different combinations of CONTAINS expressions in queries.

Library    REST    ${baseurl}    #ssl_verify=false
Library    RequestsLibrary  WITH NAME  R
Library    String
Library    Collections
Library    OperatingSystem
Library    Process
Library    XML
Library    JSONLibrary
Library    DateTime
# Library    ${CURDIR}${/}../_resources/libraries/dockerlib.py
# Library    ${CURDIR}${/}../_resources/libraries/jsonlib.py
# Resource   ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
# Resource   ${CURDIR}${/}../_resources/keywords/db_keywords.robot

Library    ${EXECDIR}/robot/_resources/libraries/dockerlib.py
Library    ${EXECDIR}/robot/_resources/libraries/jsonlib.py

Resource   ${EXECDIR}/robot/_resources/keywords/composition_keywords.robot


*** Variables ***
${hip_baseurl_v1}     http://localhost:8080/ehrbase/rest/ecis/v1
${template_id}    IDCR%20-%20Immunisation%20summary.v0
${invalid_ehr_id}    123
${CODE_COVERAGE}    False
${NODOCKER}         False

${PROJECT_ROOT}  ${EXECDIR}${/}..
${POM_FILE}    ${PROJECT_ROOT}${/}pom.xml
${FIXTURES}     ${PROJECT_ROOT}/tests/robot/_resources/fixtures
${SMOKE_TEST_PASSED}    ${TRUE}



# Your System Under Test (SUT) configuration goes here!
# Check tests/README.md for details.
${SUT}                  TEST    # DEFAULT

# local test environment: for development
&{DEV}                  URL=http://localhost:8080/ehrbase/rest/openehr/v1
...                     HEARTBEAT=http://localhost:8080/ehrbase/
...                     CREDENTIALS=${devcreds}
...                     AUTH={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
                        # comment: nodename is actually "CREATING_SYSTEM_ID" and can be set from cli
                        #          when starting server .jar
                        #          i.e. java -jar application.jar --server.nodename=some.foobar.baz
                        #          EHRbase's default is local.ehrbase.org
...                     NODENAME=local.ehrbase.org
...                     CONTROL=manual
${devcreds}             None

# testing environment: used on CI pipeline
&{TEST}                 URL=http://localhost:8080/ehrbase/rest/openehr/v1
...                     HEARTBEAT=http://localhost:8080/ehrbase/
...                     CREDENTIALS=${testcreds}
...                     AUTH={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
...                     NODENAME=local.ehrbase.org
...                     CONTROL=docker
${testcreds}            None

# staging environment
&{STAGE}                URL=http://localhost:8080/ehrbase/rest/openehr/v1
...                     HEARTBEAT=http://localhost:8080/ehrbase/
...                     CREDENTIALS=${stagecreds}
...                     AUTH={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
...                     NODENAME=stage.ehrbase.org
...                     CONTROL=docker
${stagecreds}           None

# pre production environment
&{PREPROD}              URL=http://localhost:8080/ehrbase/rest/openehr/v1
...                     HEARTBEAT=http://localhost:8080/ehrbase/
...                     CREDENTIALS=${preprodcreds}
...                     AUTH={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
...                     NODENAME=preprod.ehrbase.org
...                     CONTROL=docker
${preprodcreds}         None


# # Basic Auth example
# &{AIRBASE}              URL=https://domain.com/rest/openehr/v1
# ...                     CREDENTIALS=@{aircreds}
# ...                     AUTH={"Authorization": "Basic Auth-String"}
# @{aircreds}             username    password

${BASEURL}              ${${SUT}.URL}
${HEARTBEAT_URL}        ${${SUT}.HEARTBEAT}
${AUTHORIZATION}        ${${SUT}.AUTH}
${CREATING_SYSTEM_ID}   ${${SUT}.NODENAME}
${CONTROL_MODE}         ${${SUT}.CONTROL}


*** Variables ***
${CONTAINS QUERY DATA SETS}     ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/aql_queries_valid/test_contains

*** Keywords ***
Execute ad-hoc query contains
    [Arguments]         ${aql_payload}
    [Documentation]     AQL CONTAINS

                        # create EHR and get ehr_id
                        create EHR      JSON
                        Log To Console    ${ehr_id}

                        #execute ad-hoc query    ${aql_payload}
                        #load valid query test-data-set    ${valid_test_data_set}
                        ${file}=            Load JSON From File    ${CONTAINS QUERY DATA SETS}/${aql_payload}

                        # set ehr_id query parameter for the json query payload
                        ${file}=            Update Value To Json   ${file}   $.query_parameters.ehr_id   ${ehr_id}

                        # the next keyword expects ${test_data}
                                            Set Test Variable      ${test_data}    ${file}

                        POST /query/aql    JSON

                        #check response: is positive
                        Should Be Equal As Strings   ${response.status_code}   200
                        #check response (EMPTY DB): returns correct content for    ${aql_payload}

*** Test Cases ***
Ad-hoc query contains
    [Template]          Execute ad-hoc query contains
    [Tags]
    nested_3_levels_contains_anyehr.json
    nested_3_levels_contains_nocompoarchid_anyehr.json
    nested_3_levels_contains_nocompoarchid.json
    nested_3_levels_contains.json
    nested_4_levels_contains_anyehr.json
    nested_4_levels_contains_noarchid_1.json
