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
Library    Collections
Library    String
Library    Process
Library    OperatingSystem

Resource    ${CURDIR}${/}../suite_settings.robot
Resource    generic_keywords.robot
Resource    template_opt1.4_keywords.robot
Resource    ehr_keywords.robot
Resource    composition_keywords.robot



*** Variables ***
${VALID QUERY DATA SETS}     ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/aql_queries_valid
${INVALID QUERY DATA SETS}   ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/aql_queries_invalid
${QUERY RESULTS LOADED DB}   ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/expected_results/loaded_db
${QUERY RESULTS EMPTY DB}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/expected_results/empty_db

${aql_queries}    ${VALID QUERY DATA SETS}



*** Keywords ***
# oooo    oooo oooooooooooo oooooo   oooo oooooo   oooooo     oooo   .oooooo.   ooooooooo.   oooooooooo.    .oooooo..o
# `888   .8P'  `888'     `8  `888.   .8'   `888.    `888.     .8'   d8P'  `Y8b  `888   `Y88. `888'   `Y8b  d8P'    `Y8
#  888  d8'     888           `888. .8'     `888.   .8888.   .8'   888      888  888   .d88'  888      888 Y88bo.
#  88888[       888oooo8       `888.8'       `888  .8'`888. .8'    888      888  888ooo88P'   888      888  `"Y8888o.
#  888`88b.     888    "        `888'         `888.8'  `888.8'     888      888  888`88b.     888      888      `"Y88b
#  888  `88b.   888       o      888           `888'    `888'      `88b    d88'  888  `88b.   888     d88' oo     .d8P
# o888o  o888o o888ooooood8     o888o           `8'      `8'        `Y8bood8P'  o888o  o888o o888bood8P'   8""88888P'
#
# [ HIGH LEVEL KEYWORDS ]

execute ad-hoc query and check result (empty DB)
    [Arguments]         ${aql_payload}
    [Documentation]     EMPTY DB

                        execute ad-hoc query    ${aql_payload}
                        check response: is positive
                        check response (EMPTY DB): returns correct content for    ${aql_payload}


execute ad-hoc query and check result (loaded DB)
    [Arguments]         ${aql_payload}    ${expected}
    [Documentation]     LOADED DB

                        execute ad-hoc query    ${aql_payload}
                        check response: is positive
                        check response (LOADED DB): returns correct content  ${expected}


execute ad-hoc query (no result comparison)
    [Arguments]         ${aql_payload}

                        execute ad-hoc query    ${aql_payload}
                        check response: is positive


execute ad-hoc query
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  AD-HOC QUERY
                        load valid query test-data-set    ${valid_test_data_set}
                        POST /query/aql    JSON


load valid query test-data-set
    [Arguments]        ${valid_test_data_set}

    ${file}=            Get File            ${VALID QUERY DATA SETS}/${valid_test_data_set}

                        Set Test Variable   ${test_data}    ${file}


load expected results-data-set (LOADED DB)
    [Arguments]        ${expected_result_data_set}

    ${file}=            Get File            ${QUERY RESULTS LOADED DB}/${expected_result_data_set}

                        Set Test Variable   ${expected_result}    ${file}


load expected results-data-set (EMPTY DB)
    [Arguments]        ${expected_result_data_set}

    ${file}=            Get File            ${QUERY RESULTS EMPTY DB}/${expected_result_data_set}

                        Set Test Variable   ${expected_result}    ${file}


# load expected result schema
#     [Arguments]         ${expected_result}
#                         Expect Response Body    ${QUERY RESULTS LOADED DB}/${expected_result}-schema.json


startup AQL SUT
    [Documentation]     used in Test Suite Setup
    ...                 this keyword overrides another one with same name
    ...                 from "generic_keywords.robot" file

    get application version
    unzip file_repo_content.zip
    empty operational_templates folder
    start ehrdb
    start openehr server


execute AQL query
    [Arguments]         ${aql_query}
    [Documentation]     Sends given AQL query via POST request.

    Log         DEPRECATION WARNING: @WLAD remove this KW - it's only used in old AQL-QUERY tests.
                ...         level=WARN

    REST.POST           ${url}/query    ${aql_query}
    Integer             response status    200







# oooooooooooo ooooo      ooo oooooooooo.   ooooooooo.     .oooooo.   ooooo ooooo      ooo ooooooooooooo  .oooooo..o
# `888'     `8 `888b.     `8' `888'   `Y8b  `888   `Y88.  d8P'  `Y8b  `888' `888b.     `8' 8'   888   `8 d8P'    `Y8
#  888          8 `88b.    8   888      888  888   .d88' 888      888  888   8 `88b.    8       888      Y88bo.
#  888oooo8     8   `88b.  8   888      888  888ooo88P'  888      888  888   8   `88b.  8       888       `"Y8888o.
#  888    "     8     `88b.8   888      888  888         888      888  888   8     `88b.8       888           `"Y88b
#  888       o  8       `888   888     d88'  888         `88b    d88'  888   8       `888       888      oo     .d8P
# o888ooooood8 o8o        `8  o888bood8P'   o888o         `Y8bood8P'  o888o o8o        `8      o888o     8""88888P'
#
# [ HTTP METHODS / ENDPOINTS ]



# oooo            .       .                                                     .
# `888          .o8     .o8                                                   .o8
#  888 .oo.   .o888oo .o888oo oo.ooooo.       oo.ooooo.   .ooooo.   .oooo.o .o888oo
#  888P"Y88b    888     888    888' `88b       888' `88b d88' `88b d88(  "8   888
#  888   888    888     888    888   888       888   888 888   888 `"Y88b.    888
#  888   888    888 .   888 .  888   888       888   888 888   888 o.  )88b   888 .
# o888o o888o   "888"   "888"  888bod8P'       888bod8P' `Y8bod8P' 8""888P'   "888"
#                              888             888
#                             o888o           o888o
#
# [ HTTP POST ]

POST /query/aql
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method POST on /query/aql endpoint
    ...                 DEPENDENCY: following variables have to be in test-level scope:
    ...                 `${test_data}`

                        prepare query request session    ${format}
    ${resp}=            Post Request        ${SUT}   /query/aql
                        ...                 data=${test_data}
                        ...                 headers=${headers}
                        Set Test Variable   ${response}    ${resp}
                        Set Test Variable   ${response body}    ${resp.content}
                        Output Debug Info:  POST /query/aql
    
    # UNCOMMENT NEXT BLOCK FOR DEBUGGING (BETTER OUTPUT IN CONSOLE)
    # TODO: rm/comment when test stable
    &{resp}=            REST.POST  /query/aql  body=${test_data}  headers=${headers}
                        Log To Console  \n//////////// ACTUAL //////////////////////////////
    ${body}=            Output  response body
                        Integer    response status    200


POST /query/{qualified_query_name}/{version}
    No Operation





# oooo            .       .                                            .
# `888          .o8     .o8                                          .o8
#  888 .oo.   .o888oo .o888oo oo.ooooo.        .oooooooo  .ooooo.  .o888oo
#  888P"Y88b    888     888    888' `88b      888' `88b  d88' `88b   888
#  888   888    888     888    888   888      888   888  888ooo888   888
#  888   888    888 .   888 .  888   888      `88bod8P'  888    .o   888 .
# o888o o888o   "888"   "888"  888bod8P'      `8oooooo.  `Y8bod8P'   "888"
#                              888            d"     YD
#                             o888o           "Y88888P'
#
# [ HTTP GET ]


GET /query/{qualified_query_name}
    No Operation


GET /query/{qualified_query_name}/?ehr_id
    No Operation


GET /query/{qualified_query_name}/?query_parameter
    No Operation


GET /query/{qualified_query_name}/?ehr_id?query_parameter
    No Operation


GET /query/{qualified_query_name}/{version}
    No Operation


GET /query/{qualified_query_name}/{version}?ehr_id
    No Operation


GET /query/{qualified_query_name}/{version}?query_parameter
    No Operation


GET /query/{qualified_query_name}/{version}?ehr_id?query_parameter
    No Operation







# ooooooooo.   oooooooooooo  .oooooo..o ooooooooo.     .oooooo.   ooooo      ooo  .oooooo..o oooooooooooo  .oooooo..o
# `888   `Y88. `888'     `8 d8P'    `Y8 `888   `Y88.  d8P'  `Y8b  `888b.     `8' d8P'    `Y8 `888'     `8 d8P'    `Y8
#  888   .d88'  888         Y88bo.       888   .d88' 888      888  8 `88b.    8  Y88bo.       888         Y88bo.
#  888ooo88P'   888oooo8     `"Y8888o.   888ooo88P'  888      888  8   `88b.  8   `"Y8888o.   888oooo8     `"Y8888o.
#  888`88b.     888    "         `"Y88b  888         888      888  8     `88b.8       `"Y88b  888    "         `"Y88b
#  888  `88b.   888       o oo     .d8P  888         `88b    d88'  8       `888  oo     .d8P  888       o oo     .d8P
# o888o  o888o o888ooooood8 8""88888P'  o888o         `Y8bood8P'  o8o        `8  8""88888P'  o888ooooood8 8""88888P'
#
# [ POSITIVE RESPONSE CHECKS ]

check response: is positive
    Should Be Equal As Strings   ${response.status_code}   200


check response (LOADED DB): returns correct content
    [Arguments]         ${path_to_expected}

                        load expected results-data-set (LOADED DB)    ${path_to_expected}

    &{diff}=            compare json-strings  ${response body}  ${expected result}
    ...                 report_repetition=True
    ...                 exclude_paths=root['meta']
                        Should Be Empty  ${diff}  msg=DIFF DETECTED!


# check response (LOADED DB): returns correct filtered content for
#     [Arguments]         ${aql_payload}
#                         load expected results-data-set (LOADED DB)    ${aql_payload}
#     &{diff}=            compare json-strings  ${response body}  ${expected result}  exclude_paths=root['meta']


# check response (LOADED DB): returns correct ordered content for
#     [Arguments]         ${aql_payload}
#                         load expected results-data-set (LOADED DB)    ${aql_payload}
#     &{diff}=            compare json-strings  ${response body}  ${expected result}  exclude_paths=root['meta']


check response (EMPTY DB): returns correct content for
    [Arguments]         ${aql_payload}

                        load expected results-data-set (EMPTY DB)    ${aql_payload}
                        
                        Log To Console  \n/////////// EXPECTED //////////////////////////////
                        Output    ${expected result}

    &{diff}=            compare json-strings  ${response body}  ${expected result}  exclude_paths=root['meta']
                        Should Be Empty  ${diff}  msg=DIFF DETECTED!







# ooooo   ooooo oooooooooooo       .o.       oooooooooo.   oooooooooooo ooooooooo.    .oooooo..o
# `888'   `888' `888'     `8      .888.      `888'   `Y8b  `888'     `8 `888   `Y88. d8P'    `Y8
#  888     888   888             .8"888.      888      888  888          888   .d88' Y88bo.
#  888ooooo888   888oooo8       .8' `888.     888      888  888oooo8     888ooo88P'   `"Y8888o.
#  888     888   888    "      .88ooo8888.    888      888  888    "     888`88b.         `"Y88b
#  888     888   888       o  .8'     `888.   888     d88'  888       o  888  `88b.  oo     .d8P
# o888o   o888o o888ooooood8 o88o     o8888o o888bood8P'   o888ooooood8 o888o  o888o 8""88888P'
#
# [ HTTP HEADERS ]

prepare query request session
    [Arguments]         ${format}=JSON    &{extra_headers}
    [Documentation]     Prepares request settings for usage with RequestLibrary
    ...                 :format: JSON (default) / XML
    ...                 :extra_headers: optional - e.g. Prefer=return=representation
    ...                                            e.g. If-Match={ehrstatus_uid}

                        # case: JSON
                        Run Keyword If      $format=='JSON'    set request headers
                        ...                 content=application/json
                        ...                 accept=application/json
                        ...                 &{extra_headers}

                        # case: XML
                        Run Keyword If      $format=='XML'    set request headers
                        ...                 content=application/xml
                        ...                 accept=application/xml
                        ...                 &{extra_headers}


set request headers
    [Arguments]         ${content}=application/json  ${accept}=application/json  &{extra_headers}
    [Documentation]     Sets the headers of a request
    ...                 :content: application/json (default) / application/xml
    ...                 :accept: application/json (default) / application/xml
    ...                 :extra_headers: optional

                        Log Many            ${content}  ${accept}  ${extra_headers}

    &{headers}=         Create Dictionary   Content-Type=${content}
                        ...                 Accept=${accept}

                        Run Keyword If      ${extra_headers}    Set To Dictionary    ${headers}    &{extra_headers}

                        Create Session      ${SUT}    ${${SUT}.URL}
                        ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True

                        Set Test Variable   ${headers}    ${headers}
