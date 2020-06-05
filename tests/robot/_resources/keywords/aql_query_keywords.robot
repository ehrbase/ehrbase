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

Set Smoke Test Status
    ${SMOKE_TEST_PASSED} =    Set Variable if    "${TESTSTATUS}"=="PASS"    ${TRUE}    ${FALSE}
    Set Global Variable    ${SMOKE_TEST_PASSED}    ${SMOKE_TEST_PASSED}


Establish Preconditions
    Preconditions (PART 1) - Load Blueprints of Queries and Expected-Results
    Preconditions (PART 2) - Generate Test-Data and Expected-Results


execute ad-hoc query and check result (empty DB)
    [Arguments]         ${aql_payload}
    [Documentation]     EMPTY DB

                        execute ad-hoc query    ${aql_payload}
                        check response: is positive
                        check response (EMPTY DB): returns correct content for    ${aql_payload}


execute invalid ad-hoc query and check result (empty DB)
    [Arguments]         ${aql_payload}    ${error_message}
    [Documentation]     EMPTY DB
                        execute invalid ad-hoc query    ${aql_payload}
                        check response: is negative
                        check response: contains error message    ${error_message}


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


execute invalid ad-hoc query
    [Arguments]         ${invalid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  AD-HOC QUERY
                        load invalid query test-data-set  ${invalid_test_data_set}
                        POST /query/aql    JSON


load valid query test-data-set
    [Arguments]        ${valid_test_data_set}

    ${file} =           Load JSON From File    ${VALID QUERY DATA SETS}/${valid_test_data_set}
                        Set Test Variable      ${test_data}    ${file}


load invalid query test-data-set
    [Arguments]        ${invalid_test_data_set}

    ${file} =           Load JSON From File    ${INVALID QUERY DATA SETS}/${invalid_test_data_set}
                        Set Test Variable      ${test_data}    ${file}


load expected results-data-set (LOADED DB)
    [Arguments]        ${expected_result_data_set}

    ${file}=            Load JSON From File    ${QUERY RESULTS LOADED DB}/${expected_result_data_set}
                        Set Test Variable      ${expected_result}    ${file}


load expected results-data-set (EMPTY DB)
    [Arguments]        ${expected_result_data_set}

    ${file}=            Load JSON From File    ${QUERY RESULTS EMPTY DB}/${expected_result_data_set}
                        Set Test Variable      ${expected_result}    ${file}







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

                        prepare new request session    ${format}
    ${resp}=            Post Request        ${SUT}   /query/aql
                        ...                 data=${test_data}
                        ...                 headers=${headers}
                        Set Test Variable   ${response}    ${resp}
                        Set Test Variable   ${response body}    ${resp.content}
                        # Output Debug Info:  POST /query/aql
    
    # UNCOMMENT NEXT BLOCK FOR DEBUGGING (BETTER OUTPUT IN CONSOLE)
    # TODO: rm/comment it out when test stable
                        Log To Console  \n//////////// ACTUAL //////////////////////////////
                        Output    ${response.json()}


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

                        Log To Console  \n/////////// EXPECTED //////////////////////////////
                        Output    ${expected result}

    &{diff}             compare_jsons_ignoring_properties    ${response body}    ${expected result}
    # ...                 meta    path    foo        # comment: example of how to add additional
                                                     #          properties to be ignored
    ...                 report_repetition=${TRUE}

                        Should Be Empty  ${diff}  msg=DIFF DETECTED!


# check response (LOADED DB): returns correct filtered content for
#     [Arguments]         ${aql_payload}
#                         load expected results-data-set (LOADED DB)    ${aql_payload}
#     &{diff}=            compare json-strings  ${response body}  ${expected result}  exclude_paths=root['meta']


check response (LOADED DB): returns correct ordered content
    [Documentation]     expected result is generated at runtime and saved as .tmp.json
    ...                 in the same folder as the related blueprint
    [Arguments]         ${path_to_expected_result}
                        load expected results-data-set (LOADED DB)    ${path_to_expected_result}

                        Log To Console  \n/////////// EXPECTED //////////////////////////////
                        Output    ${expected result}
    
    # TODO: probably need to sort the expected result before comparison
    
    &{diff}             compare_jsons_ignoring_properties    ${response body}    ${expected result}
    ...                 meta    path
    ...                 ignore_order=${FALSE}
    ...                 report_repetition=${TRUE}

                        Should Be Empty    ${diff}    msg=DIFF DETECTED!


check response (EMPTY DB): returns correct content for
    [Arguments]         ${aql_payload}

                        load expected results-data-set (EMPTY DB)    ${aql_payload}
                        
                        Log To Console  \n/////////// EXPECTED //////////////////////////////
                        Output    ${expected result}

    &{diff}=            compare_jsons_ignoring_properties  ${response body}  ${expected result}
                        Should Be Empty  ${diff}  msg=DIFF DETECTED!


# [ NEGATIVE RESPONSE CHECKS ]
check response: is negative
    Should Be Equal As Strings   ${response.status_code}   400


check response: contains error message
    [Arguments]         ${error_message}
                        # Log    ${response body}
    ${body} =           Convert To String    ${response body}
                        Should Contain    ${body}    ${error_message}    ignore_case=True







# ooooo   ooooo oooooooooooo       .o.       oooooooooo.   oooooooooooo ooooooooo.    .oooooo..o
# `888'   `888' `888'     `8      .888.      `888'   `Y8b  `888'     `8 `888   `Y88. d8P'    `Y8
#  888     888   888             .8"888.      888      888  888          888   .d88' Y88bo.
#  888ooooo888   888oooo8       .8' `888.     888      888  888oooo8     888ooo88P'   `"Y8888o.
#  888     888   888    "      .88ooo8888.    888      888  888    "     888`88b.         `"Y88b
#  888     888   888       o  .8'     `888.   888     d88'  888       o  888  `88b.  oo     .d8P
# o888o   o888o o888ooooood8 o88o     o8888o o888bood8P'   o888ooooood8 o888o  o888o 8""88888P'
#
# [ HTTP HEADERS ]

# NOTE: All request header settings are handled from generic_keywords.robot resource file.

Available keywords:
    generic_keywords.prepare new request session
    generic_keywords.set request headers






# ooooooooooooo oooooooooooo  .oooooo..o ooooooooooooo      oooooooooo.         .o.       ooooooooooooo       .o.       
# 8'   888   `8 `888'     `8 d8P'    `Y8 8'   888   `8      `888'   `Y8b       .888.      8'   888   `8      .888.      
#      888       888         Y88bo.           888            888      888     .8"888.          888          .8"888.     
#      888       888oooo8     `"Y8888o.       888            888      888    .8' `888.         888         .8' `888.    
#      888       888    "         `"Y88b      888            888      888   .88ooo8888.        888        .88ooo8888.   
#      888       888       o oo     .d8P      888            888     d88'  .8'     `888.       888       .8'     `888.  
#     o888o     o888ooooood8 8""88888P'      o888o          o888bood8P'   o88o     o8888o     o888o     o88o     o8888o 
#                                                                                                                       
# [ TEST DATA & EXPECTED RESULTS GENERATION ]

Preconditions (PART 1) - Load Blueprints of Queries and Expected-Results
    [Documentation]     Loads expected-result-blueprints and creates local copies for temporary use.
    ...                 Temporary copies are not git versioned. They are overwritten between test runs
    ...                 and are automatically removed after test execution.
    ...                 NOTE: turn off automatic removal by commenting out related part of "Suite Teardown"
    ...                       to do a manual comparison between 'blueprint' and generated 'expected result set'
    ...                       see example in line below:
    ...                 Suite Teardown    Run Keywords    Clean DB    # Delete Temp Result-Data-Sets

    ${elist}=           Create List   @{EMPTY}
                        Set Suite Variable    @{empty_list}    @{EMPTY}

    # comment: creates temp copies of expectectd-result-blueprints
    @{blueprints}=      Create List    A/100_get_ehrs.json
                        ...            A/101_get_ehrs.json
                        ...            A/102_get_ehrs.json
                        ...            A/103_get_ehrs.json
                        ...            A/104_get_ehrs.json
                        ...            A/105_get_ehrs.json
                        ...            A/106_get_ehrs.json
                        ...            A/107_get_ehrs_top_5.json
                        ...            A/108_get_ehrs_orderby_time-created.json
                        ...            A/109_get_ehrs_within_timewindow.json
                        ...            A/300_get_ehrs_by_contains_any_composition.json
                        ...            A/400_get_ehrs_by_contains_composition_with_archetype.json
                        ...            A/401_get_ehrs_by_contains_composition_with_archetype.json
                        ...            A/402_get_ehrs_by_contains_composition_with_archetype.json
                        ...            A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json
                        ...            A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json
                        ...            A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json
                        ...            A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json
                        ...            A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
                        ...            A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
                        ...            A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
                        ...            A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
                        ...            B/100_get_compositions_from_all_ehrs.json
                        ...            B/101_get_compositions_top_5.json
                        ...            B/102_get_compositions_orderby_name.json
                        ...            B/103_get_compositions_within_timewindow.json
                        ...            B/300_get_compositions_with_archetype_from_all_ehrs.json
                        ...            B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json
                        ...            B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
                        ...            B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
                        ...            B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
                        ...            B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
                        ...            B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
                        ...            B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
                        ...            B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
                        ...            B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
                        ...            B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
                        ...            B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
                        ...            B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
                        ...            D/200_select_data_values_from_all_ehrs_contains_composition.json
                        ...            D/201_select_data_values_from_all_ehrs_contains_composition.json
                        ...            D/300_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/301_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/302_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/303_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/304_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/308_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/309_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/310_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
                        ...            D/312_select_data_values_from_all_ehrs_contains_composition_with_archetype_top_5.json
                    
                    FOR     ${blueprint}    IN    @{blueprints}

                            Set Suite Variable    ${blueprint}    ${blueprint}
                            Make Temp Copy of Expected Result Blueprint
                    END
                            Set Suite Variable    @{blueprint}    @{EMPTY}

    # comment: For the data-sets below copies of query-blueprints AND expected-result-blueprints are required
    @{blueprints}=      Create List    A/200_get_ehr_by_id.json
                        ...            A/201_get_ehr_by_id.json
                        ...            A/202_get_ehr_by_id.json
                        ...            A/203_get_ehr_by_id.json
                        ...            B/200_get_compositions_from_ehr_by_id.json
                        ...            B/800_get_composition_by_uid.json
                        ...            B/801_get_composition_by_uid.json
                        ...            B/802_get_composition_by_uid.json
                        ...            B/803_get_composition_by_uid.json
                        ...            C/300_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
                        ...            C/301_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
                        ...            C/302_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
                        ...            C/303_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
                        ...            C/400_get_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
                        ...            C/500_get_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs_condition.json
                        ...            C/100_get_entries_from_ehr_with_uid_contains_compositions_from_all_ehrs.json
                        ...            C/101_get_entries_top_5.json
                        ...            C/102_get_entries_orderby_name.json
                        ...            C/103_get_entries_within_timewindow.json
                        ...            C/200_get_entries_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
                        ...            D/400_select_data_values_from_all_compositions_in_ehr.json
                        ...            D/401_select_data_values_from_all_compositions_in_ehr.json
                        ...            D/402_select_data_values_from_all_compositions_in_ehr.json
                        ...            D/403_select_data_values_from_all_compositions_in_ehr.json
                        ...            D/404_select_data_values_from_all_compositions_in_ehr.json
                        ...            D/405_select_data_values_from_all_compositions_in_ehr.json
                        ...            D/500_select_data_values_from_compositions_with_given_archetype_in_ehr.json
                        ...            D/501_select_data_values_from_compositions_with_given_archetype_in_ehr.json
                        ...            D/502_select_data_values_from_compositions_with_given_archetype_in_ehr.json
                        ...            D/503_select_data_values_from_compositions_with_given_archetype_in_ehr.json

                FOR     ${blueprint}    IN    @{blueprints}

                        Set Suite Variable    ${blueprint}    ${blueprint}
                        Make Temp Copy of Expected Result Blueprint
                        Make Temp Copy of Query Blueprint
                END
                        Set Suite Variable    @{blueprint}    @{EMPTY}


Preconditions (PART 2) - Generate Test-Data and Expected-Results
    [Documentation]     Uploads required test data to server/db and generates expected results
    ...                 during that process. This involves
    ...                 - creating EHR records with ehr_status
    ...                 - committing Compositions for each EHR record
    ...                 - injecting real data into 'expected result sets'

    upload OPT      minimal/minimal_admin.opt
    upload OPT      minimal/minimal_observation.opt
    upload OPT      minimal/minimal_instruction.opt
    upload OPT      minimal/minimal_evaluation.opt
    upload OPT      minimal/minimal_action.opt
    upload OPT      minimal/minimal_action_2.opt
    upload OPT      all_types/Test_all_types.opt

    Populate SUT with Test-Data and Prepare Expected Results    1    ${ehr data sets}/ehr_status_01.json
    Populate SUT with Test-Data and Prepare Expected Results    2    ${ehr data sets}/ehr_status_02.json
    Populate SUT with Test-Data and Prepare Expected Results    3    ${ehr data sets}/ehr_status_03.json
    Populate SUT with Test-Data and Prepare Expected Results    4    ${ehr data sets}/ehr_status_04.json
    Populate SUT with Test-Data and Prepare Expected Results    5    ${ehr data sets}/ehr_status_05.json
    Populate SUT with Test-Data and Prepare Expected Results    6    ${ehr data sets}/ehr_status_06.json
    Populate SUT with Test-Data and Prepare Expected Results    7    ${ehr data sets}/ehr_status_07.json
    Populate SUT with Test-Data and Prepare Expected Results    8    ${ehr data sets}/ehr_status_08.json
    Populate SUT with Test-Data and Prepare Expected Results    9    ${ehr data sets}/ehr_status_09.json
    Populate SUT with Test-Data and Prepare Expected Results   10    ${ehr data sets}/ehr_status_10.json


Populate SUT with Test-Data and Prepare Expected Results
    [Arguments]         ${ehr_index}    ${ehr_status_object}
    Log To Console  \nEHR RECORD ${ehr_index} ////////////////////////////////////

    Create EHR Record On The Server    ${ehr_index}    ${ehr_status_object}

    Commit Compo     1    ${ehr_index}    ${compo data sets}/minimal_admin_1.composition.json
    Commit Compo     2    ${ehr_index}    ${compo data sets}/minimal_admin_2.composition.json
    Commit Compo     3    ${ehr_index}    ${compo data sets}/minimal_admin_3.composition.json

    Commit Compo     4    ${ehr_index}    ${compo data sets}/minimal_evaluation_1.composition.json
    Commit Compo     5    ${ehr_index}    ${compo data sets}/minimal_evaluation_2.composition.json
    Commit Compo     6    ${ehr_index}    ${compo data sets}/minimal_evaluation_3.composition.json
    Commit Compo     7    ${ehr_index}    ${compo data sets}/minimal_evaluation_4.composition.json
    Commit Compo     8    ${ehr_index}    ${compo data sets}/all_types.composition.json

    Commit Compo     9    ${ehr_index}    ${compo data sets}/minimal_instruction_1.composition.json
    Commit Compo    10    ${ehr_index}    ${compo data sets}/minimal_instruction_2.composition.json
    Commit Compo    11    ${ehr_index}    ${compo data sets}/minimal_instruction_3.composition.json
    Commit Compo    12    ${ehr_index}    ${compo data sets}/minimal_instruction_4.composition.json

    Commit Compo    13    ${ehr_index}    ${compo data sets}/minimal_observation_1.composition.json
    Commit Compo    14    ${ehr_index}    ${compo data sets}/minimal_observation_2.composition.json
    Commit Compo    15    ${ehr_index}    ${compo data sets}/minimal_observation_3.composition.json
    Commit Compo    16    ${ehr_index}    ${compo data sets}/minimal_observation_4.composition.json

    Commit Compo    17    ${ehr_index}    ${compo data sets}/minimal_action2_1.composition.json
    Commit Compo    18    ${ehr_index}    ${compo data sets}/minimal_action2_2.composition.json
    Commit Compo    19    ${ehr_index}    ${compo data sets}/minimal_action2_3.composition.json



Create EHR Record On The Server
    [Arguments]         ${ehr_index}    ${payload}

                        prepare new request session    Prefer=return=representation
                        Set Suite Variable    ${ehr_index}    ${ehr_index}

                        create new EHR with ehr_status  ${payload}
                        Integer    response status    201
                        # extract ehr_id from response (JSON)    # TODOO: remove

    ${ehr_id_obj}=      Object    response body ehr_id
    ${ehr_id_value}=    String    response body ehr_id value
                        Set Suite Variable    ${ehr_id_value}    ${ehr_id_value}
                        Set Suite Variable    ${ehr_id_obj}    ${ehr_id_obj}
                        # comment: ATTENTION - RESTinstance lib returns a LIST!
                        #          The value is at index 0 in that list
                        Set Suite Variable    ${ehr_id}    ${ehr_id_value}[0]

    # TODO: BUG - time_created should be an object! Update when iplementation is fixed
        # this is how it should look like:
        # ${time_created_obj}  Object    response body time_created
        # ${time_created}     String    response body time_created value
    ${time_created}=    String    response body time_created
                        Set Suite Variable    ${time_created}    ${time_created}

    ${system_id_obj}=   Object    response body system_id
    ${system_id}=       String    response body system_id value
                        Set Suite Variable    ${system_id_obj}    ${system_id_obj}
                        Set Suite Variable    ${system_id}    ${system_id}

    ${ehr_status_obj}=  Object    response body ehr_status
                        set suite Variable    ${ehr_status_obj}    ${ehr_status_obj}

    ##########################################################################################
    #                                                                                        #
    # FOR EACH EHR RECORD IN DB DATA IS EXTRACTED AND POPULATATED INTO EXPECTED RESULTS SETS #
    #                                                                                        #
    ##########################################################################################

    A/100
    A/101
    A/102
    A/103
    A/104
    A/105
    A/106
    A/107
    A/108
    A/109
    A/200
    A/201
    A/202
    A/203
    A/300
    A/400
    A/401
    A/402
    A/500
    A/501
    A/502
    A/503
    A/600
    A/601
    A/602
    A/603


Commit Compo
    [Arguments]         ${compo_index}    ${ehr_index}    ${compo_file}
    [Documentation]     Commits a Composition and exposes data from response as variables
    ...                 to test-suite level scope.
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

        Log To Console  \nCOMPOSITION ${compo_index}(${ehr_index}) ////////////////////////////////////

                        Set Suite Variable    ${compo_index}    ${compo_index}
                        # Set Suite Variable    ${ehr_index}    ${ehr_index}    # TODO: @WLAD REMOVE

    &{resp}=            REST.POST    ${baseurl}/ehr/${ehr_id}/composition    ${compo_file}
                        Output Debug Info To Console
                        Integer    response status    201
                        Set Suite Variable    ${response}    ${resp}

    # comment: This returns the object from response body wrapped in a list []
    #          That's exactly what we need to inject into expected-result blueprint most of the time.
    #          In cases where you need the oject itself use index 0 on that list: ${compo_in_list}[0]
    @{body}=            Object     response body
                        Set Suite Variable    ${compo_in_list}    ${body}
                        Set Suite Variable    ${compo_uid_value}    ${response.body.uid.value}
                        Set Suite Variable    ${compo_uid}    ${response.body.uid}    
                        Set Suite Variable    ${compo_name_value}    ${response.body.name.value}
                        Set Suite Variable    ${compo_name}    ${response.body.name}
                        Set Suite Variable    ${compo_archetype_id_value}     ${response.body.archetype_details.archetype_id.value}
                        Set Suite Variable    ${compo_archetype_id}     ${response.body.archetype_details.archetype_id}
                        Set Suite Variable    ${compo_archetype_node_id}    ${response.body.archetype_node_id}
                        Set Suite Variable    ${compo_content_archetype_node_id}    ${response.body.content[0].archetype_node_id}
                        Set Suite Variable    ${compo_content_type}    ${response.body.content[0]._type}
                        Set Suite Variable    ${compo_language}    ${response.body.language}
                        Set Suite Variable    ${compo_territory}    ${response.body.territory}
                        Set Suite Variable    ${compo_category_value}    ${response.body.category.value}
                        Set Suite Variable    ${compo_category}    ${response.body.category}

    Run Keyword If      "${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1"    Run Keywords
    ...                 Set Suite Variable    ${compo_data_origin_value}    ${response.body.content[0].data.origin.value}    AND
    ...                 Set Suite Variable    ${compo_data_origin}    ${response.body.content[0].data.origin}    AND
    ...                 Set Suite Variable    ${compo_events_time_value}    ${response.body.content[0].data.events[0].time.value}    AND
    ...                 Set Suite Variable    ${compo_events_time}    ${response.body.content[0].data.events[0].time}    AND
    ...                 Set Suite Variable    ${observ_items}    ${response.body.content[0].data.events[0].data["items"]}    AND
    ...                 Set Suite Variable    ${compo_events_items_value_value}    ${observ_items[0].value.value}    AND
    ...                 Set Suite Variable    ${compo_events_items_value}    ${observ_items[0].value}
                        # NOTE: above lines contain a workaround to set "{content[0].data.events[0].data.items[0].value.value}"
                        #       which normaly fails cause Robot/Python considers 'items' to be a method/function

    ###########################################################################################
    #                                                                                         #
    # FOR EACH COMPOSITION IN DB DATA IS EXTRACTED AND POPULATATED INTO EXPECTED RESULTS SETS #
    #                                                                                         #
    ###########################################################################################

    B/100
    B/102
    B/200
    B/300
    B/400
    B/500
    B/501
    B/502
    B/503
    B/600
    B/601
    B/700 701 702    B/700
    B/700 701 702    B/701
    B/700 701 702    B/702
    
    # B/800     # comment: future feature
    # B/801     # NOTE: for details check --> https://github.com/ehrbase/project_management/issues/109#issuecomment-605975468
                # TODO: @WLAD reactive when becomes available
    B/802
    B/803

    # # FUTURE FEATURE
    # # TODO: @WLAD reactive when becomes available
    # C/100
    # C/101
    # C/102
    # C/103
    # C/200
    # C/300
    # C/301
    # C/302
    # C/303
    # C/400
    # C/500

    D/200
    D/201
    D/300
    D/301
    D/302
    D/303
    D/304
    D/306
    D/307
    D/308
    D/309
    D/310

    # D/311     # comment: future feature
                # NOTE: for details check -->  https://github.com/ehrbase/ehrbase/pull/179/files/4c2253f04e69c9e72986a74b464b9d20f9c43d70#r401266691
                # TODO: @WLAD reactivate when becomes available
    D/312
    D/400
    D/401
    D/402
    D/403
    D/404
    D/405
    D/500
    D/501
    D/502
    D/503


    # BACKLOG / DATA GENERATION NOT READY/POSSIBLE OR NOT CLEAR HOW TO DO
    # ===================================================================

    # # NOT READY - not clear yet what is definition of TOP 5 COMPOSITIONS (GITHUB #103)
    # ${B/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/101.tmp.json
    # ${B/101}=           Add Object To Json     ${B/101}    $.rows    ${response.body}
    #                     Output    ${B/101}     ${QUERY RESULTS LOADED DB}/B/101.tmp.json

    # # NOT READY - "TIMEWINDOW" not implemented (GITHUB #106)
    # ${B/103}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/103.tmp.json
    # ${B/103}=           Add Object To Json     ${B/103}    $.rows    ${response.body}
    #                     Output    ${B/103}     ${QUERY RESULTS LOADED DB}/B/103.tmp.json

        # # FAILS because requiered compos fail to commit (instruction compositions)
        # ${B/602}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/602.tmp.json
        #                     Set Suite Variable    ${B/602}    ${B/602}
        #                     Run Keyword If    "${compo_content_archetype_node_id}"=="openEHR-EHR-INSTRUCTION.minimal.v1"    B/602



    # # HAS NO expected result set (because is a future feature - as of 14/03/2020)
    # ${C/100_query}=
    # ${C/101_query}=
    # ${C/102_query}=
    # ${C/103_query}=
    # ${C/200_query}=

    # # FAILS because requiered INSTRUSCTION compos fail to commit
    # ${C/302}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/C/302.tmp.json
    #                     Set Suite Variable    ${C/302}    ${C/302}
    #                     Run Keyword If    ${ehr_index}==1 and "${compo_content_type}"=="INSTRUCTION"   C/302

    # # FAILS because requiered ACTION compos fail to commit
    # ${C/303}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/C/303.tmp.json
    #                     Set Suite Variable    ${C/303}    ${C/303}
    #                     Run Keyword If    ${ehr_index}==1 and "${compo_content_type}"=="ACTION"   C/303


Make Temp Copy of Query Blueprint
                        # Log To Console    Temp Copy of Query-Blueprint: ${blueprint[0:5]}_query.tmp.json
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/${blueprint}
                        Output    ${query}    ${VALID QUERY DATA SETS}/${blueprint[0:5]}_query.tmp.json


Make Temp Copy of Expected Result Blueprint
                        # Log To Console    Expected Result Blueprint: ${blueprint}
                        # Log To Console    Temp Copy: ${blueprint[0:5]}.tmp.json
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/${blueprint}
                        Update Value To Json    ${expected}    $.rows    ${empty_list}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/${blueprint[0:5]}.tmp.json


Delete Temp Result-Data-Sets
    Remove Files        ${QUERY RESULTS LOADED DB}/*/*.tmp.json
    Remove Files        ${VALID QUERY DATA SETS}//*/*.tmp.json


Load Temp Query-Data-Set
    [Documentation]     Exposes {query} to test-suite level scope.
    [Arguments]         ${dataset}
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/${dataset}_query.tmp.json
                        Set Suite Variable    ${query}    ${query}


Update Temp Query-Data-Set
    [Documentation]     Updates 'q' with real ehr_id
    [Arguments]         ${dataset}
                        Load Temp Query-Data-Set    ${dataset}
    ${q}=               Get Value From Json    ${query}    $.q
    ${q}=               Replace String    ${q}[0]    __MODIFY_EHR_ID_1__    ${ehr_id}
                        Update Value To Json   ${query}    $.q    ${q}
                        Output    ${query}    ${VALID QUERY DATA SETS}/${dataset}_query.tmp.json


Update Query-Parameter in Temp Query-Data-Set
    [Documentation]     Exposes {q} to test-suite level scope.
    [Arguments]         ${dataset}
                        Load Temp Query-Data-Set    ${dataset}
    ${q_param}=         Get Value From Json    ${query}    $.query_parameters.ehr_id
    ${q_param}=         Replace String    ${q_param}[0]    __MODIFY_EHR_ID_1__    ${ehr_id}
                        Update Value To Json   ${query}    $.query_parameters.ehr_id    ${q_param}
                        Output    ${query}    ${VALID QUERY DATA SETS}/${dataset}_query.tmp.json


Load Temp Result-Data-Set
    [Documentation]     Exposes {expected} to test-suite level scope.
    [Arguments]         ${dataset}
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/${dataset}.tmp.json
                        Set Suite Variable    ${expected}    ${expected}


Update 'rows' in Temp Result-Data-Set
    [Arguments]         ${dataset}
                        Load Temp Result-Data-Set    ${dataset}    
                        Add Object To Json     ${expected}    $.rows    ${rows_content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/${dataset}.tmp.json

                    
Update 'q' and 'meta' in Temp Result-Data-Set
    [Documentation]     Updates and exposes {expected} to test-suite level scope.
    ...                 Condition ensures the update is not repeated unnecessary.

    ${q}=               Get Value From Json    ${expected}    $.q
    ${q}=               Replace String    ${q}[0]    __MODIFY_EHR_ID_1__    ${ehr_id}
                        Update Value To Json   ${expected}    $.q    ${q}
                        Update Value To Json   ${expected}    $.meta._executed_aql    ${q}
                        Set Suite Variable    ${expected}    ${expected}


Update 'rows', 'q' and 'meta' in Temp Result-Data-Set
    [Arguments]         ${dataset}
                        Load Temp Result-Data-Set    ${dataset}

                        Update 'q' and 'meta' in Temp Result-Data-Set

                        Add Object To Json     ${expected}    $.rows    ${rows_content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/${dataset}.tmp.json
                        Clean Up Suite Variables


Create Temp List
    [Documentation]     Exposes {rows_content} to test-suite level scope.
    [Arguments]         @{list_items}   
                        Log Many  ${list_items} 

    @{rows_content}=    Set Variable    ${list_items}
                        Set Suite Variable    ${rows_content}    ${rows_content}







#       .o.            88                                              oooo      .            
#      .888.          .8'                                              `888    .o8            
#     .8"888.        .8'       oooo d8b  .ooooo.   .oooo.o oooo  oooo   888  .o888oo  .oooo.o 
#    .8' `888.      .8'        `888""8P d88' `88b d88(  "8 `888  `888   888    888   d88(  "8 
#   .88ooo8888.    .8'          888     888ooo888 `"Y88b.   888   888   888    888   `"Y88b.  
#  .8'     `888.  .8'           888     888    .o o.  )88b  888   888   888    888 . o.  )88b 
# o88o     o8888o 88           d888b    `Y8bod8P' 8""888P'  `V88V"V8P' o888o   "888" 8""888P' 
#                                                                     
# [ Expected Result Generation Flows ]

A/100
    ${A/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/100.tmp.json
    ${A/100}=           Add Object To Json    ${A/100}    $.rows    ${ehr_id_value}
                        Output    ${A/100}    ${QUERY RESULTS LOADED DB}/A/100.tmp.json

A/101
    ${A/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/101.tmp.json
    ${temp}=            Create List  ${ehr_id_value}[0]  ${time_created}[0]  ${system_id}[0]
    ${A/101}=           Add Object To Json    ${A/101}    $.rows    ${temp}
                        Output    ${A/101}    ${QUERY RESULTS LOADED DB}/A/101.tmp.json

A/102
    ${A/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/102.tmp.json
    ${A/102}=           Add Object To Json    ${A/102}    $.rows    ${ehr_id_value}
                        Output    ${A/102}    ${QUERY RESULTS LOADED DB}/A/102.tmp.json

A/103
    ${A/103}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/103.tmp.json
    ${temp}=            Create List  ${ehr_id_value}[0]  ${time_created}[0]  ${system_id}[0]
    ${A/103}=           Add Object To Json    ${A/103}    $.rows    ${temp}
                        Output    ${A/103}    ${QUERY RESULTS LOADED DB}/A/103.tmp.json

A/104
    ${A/104}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/104.tmp.json
    ${A/104}=           Add Object To Json    ${A/104}    $.rows    ${ehr_id_obj}
                        Output    ${A/104}    ${QUERY RESULTS LOADED DB}/A/104.tmp.json

A/105
    ${A/105}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/105.tmp.json
    # TODO: here time_created_obj has to be used, but it is not implemented yet
    #       this is how is should look like:
    #       ${temp}=            Create List  ${ehr_id_obj}[0]  ${time_created_obj}[0]  ${system_id_obj}[0]
    ${temp}=            Create List  ${ehr_id_obj}[0]  ${time_created}[0]  ${system_id_obj}[0]
    ${A/105}=           Add Object To Json    ${A/105}    $.rows    ${temp}
                        Output    ${A/105}    ${QUERY RESULTS LOADED DB}/A/105.tmp.json

A/106
    ${A/106}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/106.tmp.json
    # TODO: same as above! update to use time_created_obj
    ${temp}=            Create List  ${ehr_id_obj}[0]  ${time_created}[0]  ${system_id_obj}[0]  ${ehr_status_obj}[0]
    ${A/106}=           Add Object To Json    ${A/106}    $.rows    ${temp}
                        Output    ${A/106}    ${QUERY RESULTS LOADED DB}/A/106.tmp.json

A/107
                        Return From Keyword If    ${ehr_index}<6   NOT IN TOP 5!
    ${A/107}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/107.tmp.json
    ${A/107}=           Add Object To Json    ${A/107}    $.rows    ${ehr_id_value}
                        Output    ${A/107}    ${QUERY RESULTS LOADED DB}/A/107.tmp.json

A/108
    ${A/108}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/108.tmp.json
    ${A/108}=           Add Object To Json    ${A/108}    $.rows    ${ehr_id_value}
    ${A/108}=           Add Object To Json    ${A/108}    $.rows    EHRs SHOULD BE ORDERED BY TIME-CREATED!  #TODO: rm when fixed
                        Output    ${A/108}    ${QUERY RESULTS LOADED DB}/A/108.tmp.json

A/109
    ${A/109}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/109.tmp.json
    ${A/109}=           Add Object To Json    ${A/109}    $.rows    ${ehr_id_value}
    ${A/109}=           Add Object To Json    ${A/109}    $.rows    ONLY EHRs IN SPECIFIED TIME-WINDOW SHOULD BE HERE!  #TODO: rm when fixed
                        Output    ${A/109}    ${QUERY RESULTS LOADED DB}/A/109.tmp.json

A/200
    Return From Keyword If    not (${ehr_index}==1)    NOTHING TO DO HERE!
    ${A/200}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/200.tmp.json

                        # updates the query
    ${A/200_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/A/200_query.tmp.json
                        Update Value To Json   ${A/200_query}    $.q    SELECT e/ehr_id/value FROM EHR e [ehr_id/value='${ehr_id}']
                        Output    ${A/200_query}    ${VALID QUERY DATA SETS}/A/200_query.tmp.json

                        # updates expected result set
    ${A/200}=           Update Value To Json   ${A/200}    $.q    SELECT e/ehr_id/value FROM EHR e [ehr_id/value='${ehr_id}']
                        Update Value To Json   ${A/200}    $.meta._executed_aql    SELECT e/ehr_id/value FROM EHR e [ehr_id/value='${ehr_id}']
                        Add Object To Json     ${A/200}    $.rows    ${ehr_id_value}
                        Output    ${A/200}     ${QUERY RESULTS LOADED DB}/A/200.tmp.json

A/201
    Return From Keyword If    not (${ehr_index}==1)    NOTHING TO DO HERE!
    ${A/201}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/201.tmp.json
                        # updates the query
    ${A/201_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/A/201_query.tmp.json
                        Update Value To Json   ${A/201_query}    $.q    select e/ehr_id/value from EHR e WHERE e/ehr_id/value = '${ehr_id}'
                        Output    ${A/201_query}    ${VALID QUERY DATA SETS}/A/201_query.tmp.json

                        # updates expected result set
    ${A/201}=           Update Value To Json   ${A/201}    $.q    select e/ehr_id/value from EHR e WHERE e/ehr_id/value = '${ehr_id}'
                        Update Value To Json   ${A/201}    $.meta._executed_aql    select e/ehr_id/value from EHR e WHERE e/ehr_id/value = '${ehr_id}'
                        Add Object To Json     ${A/201}    $.rows    ${ehr_id_value}
                        Output    ${A/201}     ${QUERY RESULTS LOADED DB}/A/201.tmp.json

A/202
    Return From Keyword If    not (${ehr_index}==1)    NOTHING TO DO HERE!
    ${A/202}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/202.tmp.json
                        # updates the query
    ${A/202_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/A/202_query.tmp.json
                        Update Value To Json   ${A/202_query}    $.q    SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value='${ehr_id}'
                        Output    ${A/202_query}    ${VALID QUERY DATA SETS}/A/202_query.tmp.json

                        # updates expected result set
    ${A/202}=           Update Value To Json   ${A/202}    $.q    SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value='${ehr_id}'
                        Update Value To Json   ${A/202}    $.meta._executed_aql    SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value='${ehr_id}'
                        Add Object To Json     ${A/202}    $.rows    ${ehr_id_value}
                        Output    ${A/202}     ${QUERY RESULTS LOADED DB}/A/202.tmp.json

A/203
    Return From Keyword If    not (${ehr_index}==1)    NOTHING TO DO HERE!
    ${A/203}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/203.tmp.json
                        # updates the query
    ${A/203_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/A/203_query.tmp.json
                        Update Value To Json   ${A/203_query}    $.q    SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value matches {'${ehr_id}'}
                        Output    ${A/203_query}    ${VALID QUERY DATA SETS}/A/203_query.tmp.json

                        # updates expected result set
    ${A/203}=           Update Value To Json   ${A/203}    $.q    SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value matches {'${ehr_id}'}
                        Update Value To Json   ${A/203}    $.meta._executed_aql    SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value matches {'${ehr_id}'}
                        Add Object To Json     ${A/203}    $.rows    ${ehr_id_value}
                        Output    ${A/203}     ${QUERY RESULTS LOADED DB}/A/203.tmp.json

A/300
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/300.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/300.tmp.json

A/400
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/400.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/400.tmp.json

A/401
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/401.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/401.tmp.json

A/402
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/402.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/402.tmp.json

A/500
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/500.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/500.tmp.json

A/501
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/501.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/501.tmp.json

A/502
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/502.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/502.tmp.json

A/503
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/503.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/503.tmp.json

A/600
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/600.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/600.tmp.json

A/601
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/601.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/601.tmp.json

A/602
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/602.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/602.tmp.json

A/603
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/A/603.tmp.json
                        Add Object To Json    ${expected}    $.rows    ${ehr_id_value}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/A/603.tmp.json



# oooooooooo.       88                                              oooo      .            
# `888'   `Y8b     .8'                                              `888    .o8            
#  888     888    .8'       oooo d8b  .ooooo.   .oooo.o oooo  oooo   888  .o888oo  .oooo.o 
#  888oooo888'   .8'        `888""8P d88' `88b d88(  "8 `888  `888   888    888   d88(  "8 
#  888    `88b  .8'          888     888ooo888 `"Y88b.   888   888   888    888   `"Y88b.  
#  888    .88P .8'           888     888    .o o.  )88b  888   888   888    888 . o.  )88b 
# o888bood8P'  88           d888b    `Y8bod8P' 8""888P'  `V88V"V8P' o888o   "888" 8""888P'
#
# [ Expected Result Generation Flows ]

B/100
    ${B/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/100.tmp.json
    ${B/100}=           Add Object To Json     ${B/100}    $.rows    ${compo_in_list}
                        Output    ${B/100}     ${QUERY RESULTS LOADED DB}/B/100.tmp.json

B/102
    Return From Keyword If    (${ehr_index}>=5)    nothing to do here!
    ${B/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/102.tmp.json
    ${B/102}=           Add Object To Json     ${B/102}    $.rows    ${compo_in_list}
                        Output    ${B/102}     ${QUERY RESULTS LOADED DB}/B/102.tmp.json

B/200
    Return From Keyword If    not (${ehr_index}==1)    nothing to do here!
                        # updates the query
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/B/200_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT c FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c
                        Output    ${query}    ${VALID QUERY DATA SETS}/B/200_query.tmp.json

                        # updates expected result set
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/200.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT c FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT c FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/200.tmp.json

B/300
    Return From Keyword If    not ("${compo_archetype_id_value}"=="openEHR-EHR-COMPOSITION.minimal.v1")    nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/300.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/300.tmp.json

B/400
    Return From Keyword If    not ("${compo_archetype_id_value}"=="openEHR-EHR-COMPOSITION.test_all_types.v1")    nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/400.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/400.tmp.json

B/500
    Return From Keyword If    not ("${compo_content_type}"=="OBSERVATION")    nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/500.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/500.tmp.json

B/501
    Return From Keyword If    not ("${compo_content_type}"=="EVALUATION")    nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/501.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/501.tmp.json

B/502
    Return From Keyword If    not ("${compo_content_type}"=="INSTRUCTION")    nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/502.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/502.tmp.json

B/503
    Return From Keyword If    not ("${compo_content_type}"=="ACTION")    nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/503.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/503.tmp.json

B/600
    Return From Keyword If    not ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")   nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/600.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/600.tmp.json

B/601
    Return From Keyword If    not ("${compo_content_archetype_node_id}"=="openEHR-EHR-EVALUATION.minimal.v1")   nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/601.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/601.tmp.json

B/602
    Return From Keyword If    not ("${compo_content_archetype_node_id}"=="openEHR-EHR-INSTRUCTION.minimal.v1")   nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/602.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/602.tmp.json

B/603
    Return From Keyword If    not ("${compo_content_archetype_node_id}"=="openEHR-EHR-ACTION.minimal_2.v1")   nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/603.tmp.json
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/603.tmp.json

B/700 701 702
    [Arguments]         ${dataset}
    Return From Keyword If    not ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")    nothing to do here!
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/${dataset}.tmp.json
    ${items}            Set Variable    ${response.body.content[0].data.events[0].data["items"]}
    ${obs_value}        Set Variable    ${items[0].value.value}
                        Run Keyword If    "${obs_value}"=="first value"    Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Run Keyword Unless    "${obs_value}"=="first value"    Return From Keyword
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/${dataset}.tmp.json

B/800
    Return From Keyword If    not (${compo_index}==1 and ${ehr_index}==1)    nothing to do here!
                        # comment: updates the query
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/B/800_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT c FROM COMPOSITION c [uid/value='${compo_uid_value}']
                        Output    ${query}    ${VALID QUERY DATA SETS}/B/800_query.tmp.json

                        # comment: updates expected result set
    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/800.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT c FROM COMPOSITION c [uid/value='${compo_uid_value}']
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT c FROM COMPOSITION c [uid/value='${compo_uid_value}']
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/800.tmp.json

B/801
    Return From Keyword If    not (${compo_index}==1 and ${ehr_index}==1)    nothing to do here!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/B/801_query.tmp.json
                        Update Value To Json   ${query}    $.query_parameters['uid']    ${compo_uid_value}
                        Output    ${query}    ${VALID QUERY DATA SETS}/B/801_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/801.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT c FROM COMPOSITION c [uid/value='${compo_uid_value}']
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT c FROM COMPOSITION c [uid/value='${compo_uid_value}']
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/801.tmp.json

B/802
    Return From Keyword If    not (${compo_index}==1 and ${ehr_index}==1)    nothing to do here!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/B/802_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT c FROM COMPOSITION c WHERE c/uid/value='${compo_uid_value}'
                        Output    ${query}    ${VALID QUERY DATA SETS}/B/802_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/802.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT c FROM COMPOSITION c WHERE c/uid/value='${compo_uid_value}'
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT c FROM COMPOSITION c WHERE c/uid/value='${compo_uid_value}'
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/802.tmp.json

B/803
    Return From Keyword If    not (${compo_index}==1 and ${ehr_index}==1)    nothing to do here!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/B/803_query.tmp.json
                        Update Value To Json   ${query}    $.query_parameters['uid']    ${compo_uid_value}
                        Output    ${query}    ${VALID QUERY DATA SETS}/B/803_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/B/803.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT c FROM COMPOSITION c WHERE c/uid/value='${compo_uid_value}'
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT c FROM COMPOSITION c WHERE c/uid/value='${compo_uid_value}'
                        Add Object To Json     ${expected}    $.rows    ${compo_in_list}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/B/803.tmp.json



#   .oooooo.        88                                              oooo      .            
#  d8P'  `Y8b      .8'                                              `888    .o8            
# 888             .8'       oooo d8b  .ooooo.   .oooo.o oooo  oooo   888  .o888oo  .oooo.o 
# 888            .8'        `888""8P d88' `88b d88(  "8 `888  `888   888    888   d88(  "8 
# 888           .8'          888     888ooo888 `"Y88b.   888   888   888    888   `"Y88b.  
# `88b    ooo  .8'           888     888    .o o.  )88b  888   888   888    888 . o.  )88b 
#  `Y8bood8P'  88           d888b    `Y8bod8P' 8""888P'  `V88V"V8P' o888o   "888" 8""888P'
#
# [ Expected Result Generation Flows ]                                                                                            
                                                              
C/100
    Return From Keyword If    not (${compo_index}==1)    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/100_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/100_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/100.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry
                        Add Object To Json     ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/C/100.tmp.json

C/101
    Return From Keyword If    not (${compo_index}==1 and ${ehr_index}<6)    NOT IN TOP 5!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/101_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT TOP 5 entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/101_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/101.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT TOP 5 entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT TOP 5 entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry
                        Add Object To Json     ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/C/101.tmp.json

C/102
    Return From Keyword If    not (${compo_index}==1)    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/102_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry ORDER BY entry/name/value ASC
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/102_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/102.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry ORDER BY entry/name/value ASC
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry ORDER BY entry/name/value ASC
                        Add Object To Json     ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/C/102.tmp.json

C/103
    Return From Keyword If    not (${compo_index}==1)    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/103_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry TIMEWINDOW PT12H/2019-10-24
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/103_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/103.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry TIMEWINDOW PT12H/2019-10-24
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c CONTAINS ENTRY entry TIMEWINDOW PT12H/2019-10-24
                        Add Object To Json     ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/C/103.tmp.json

C/200
    Return From Keyword If    not (${compo_index}==1)    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/200_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/200_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/200.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry
                        Add Object To Json     ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/C/200.tmp.json

C/300
    Return From Keyword If    not (${ehr_index}==1 and "${compo_content_type}"=="OBSERVATION")    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/300_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION entry
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/300_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/300.tmp.json
                        Update Value To Json   ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION entry
                        Update Value To Json   ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION entry
                        Add Object To Json     ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/C/300.tmp.json

C/301
    Return From Keyword If    not (${ehr_index}==1 and "${compo_content_type}"=="EVALUATION")    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/301_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS EVALUATION entry
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/301_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/301.tmp.json
                        Update Value To Json    ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS EVALUATION entry
                        Update Value To Json    ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS EVALUATION entry
                        Add Object To Json    ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/C/301.tmp.json

C/302
    Return From Keyword If    not (${ehr_index}==1 and "${compo_content_type}"=="INSTRUCTION")    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/302_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS INSTRUCTION entry
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/302_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/302.tmp.json
                        Update Value To Json    ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS INSTRUCTION entry
                        Update Value To Json    ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS INSTRUCTION entry
                        Add Object To Json    ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/C/302.tmp.json

C/303
    Return From Keyword If    not (${ehr_index}==1 and "${compo_content_type}"=="ACTION")    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/303_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ACTION entry
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/303_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/303.tmp.json
                        Update Value To Json    ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ACTION entry
                        Update Value To Json    ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ACTION entry
                        Add Object To Json    ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/C/303.tmp.json

C/400
    Return From Keyword If    not ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/400_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry [openEHR-EHR-OBSERVATION.minimal.v1]
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/400_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/400.tmp.json
                        Update Value To Json    ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry [openEHR-EHR-OBSERVATION.minimal.v1]
                        Update Value To Json    ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry [openEHR-EHR-OBSERVATION.minimal.v1]
                        Add Object To Json    ${expected}    $.rows    ${response.body.content}
                        Output    ${expected}    ${QUERY RESULTS LOADED DB}/C/400.tmp.json

C/500
    Return From Keyword If    not ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")    NOTHING TO DO HERE!
    ${query}=           Load JSON From File    ${VALID QUERY DATA SETS}/C/500_query.tmp.json
                        Update Value To Json   ${query}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry [openEHR-EHR-OBSERVATION.minimal.v1] WHERE entry/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'first value'
                        Output    ${query}    ${VALID QUERY DATA SETS}/C/500_query.tmp.json

    ${expected}=        Load JSON From File    ${QUERY RESULTS LOADED DB}/C/500.tmp.json
                        Update Value To Json    ${expected}    $.q    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry [openEHR-EHR-OBSERVATION.minimal.v1] WHERE entry/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'first value'
                        Update Value To Json    ${expected}    $.meta._executed_aql    SELECT entry FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ENTRY entry [openEHR-EHR-OBSERVATION.minimal.v1] WHERE entry/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'first value'
    ${items}            Set Variable    ${response.body.content[0].data.events[0].data["items"]}
    ${obs_value}        Set Variable    ${items[0].value.value}
                        Run Keyword If    "${obs_value}"=="first value"    Run Keywords
                        ...   Add Object To Json     ${expected}    $.rows    ${response.body.content}    AND
                        ...   Output    ${expected}    ${QUERY RESULTS LOADED DB}/C/500.tmp.json
                        # # comment: this is the same as the last 3 line above
                        # Run Keyword If    "${obs_value}"=="first value"    Add Object To Json     ${C/500}    $.rows    ${response.body.content}
                        # Run Keyword Unless    "${obs_value}"=="first value"    Return From Keyword
                        # Output    ${C/500}    ${QUERY RESULTS LOADED DB}/C/500.tmp.json



# oooooooooo.        88                                              oooo      .            
# `888'   `Y8b      .8'                                              `888    .o8            
#  888      888    .8'       oooo d8b  .ooooo.   .oooo.o oooo  oooo   888  .o888oo  .oooo.o 
#  888      888   .8'        `888""8P d88' `88b d88(  "8 `888  `888   888    888   d88(  "8 
#  888      888  .8'          888     888ooo888 `"Y88b.   888   888   888    888   `"Y88b.  
#  888     d88' .8'           888     888    .o o.  )88b  888   888   888    888 . o.  )88b 
# o888bood8P'   88           d888b    `Y8bod8P' 8""888P'  `V88V"V8P' o888o   "888" 8""888P'
# 
# [ Expected Result Generation Flows ] 

D/200
    [Documentation]     Condition here makes sure that content in 'rows' of expected result-data-set
    ...                 is not filled with dublicates on every interation which takes place for each Composition. 
    ...                 Same flow as A/101
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
                        Update 'rows' in Temp Result-Data-Set    D/200

D/201
    [Documentation]     same flow as A/105
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        # TODO-NOTE: same problem with {time_created} as in data-set A/105 --> check comment there!!!
                        Create Temp List    ${ehr_id_obj}[0]  ${time_created}[0]  ${system_id_obj}[0]
                        Update 'rows' in Temp Result-Data-Set    D/201

D/300
    [Documentation]     same flow as D/200
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
                        Update 'rows' in Temp Result-Data-Set    D/300

D/301
    [Documentation]     same flow as D/201
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        # TODO-NOTE: same problem with {time_created} as in data-set A/105 --> check comment there!!!
                        Create Temp List    ${ehr_id_obj}[0]  ${time_created}[0]  ${system_id_obj}[0]
                        Update 'rows' in Temp Result-Data-Set    D/301

D/302
    [Documentation]     same flow as D/200
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
                        Update 'rows' in Temp Result-Data-Set    D/302

D/303
    [Documentation]     same flow as D/201
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        # TODO-NOTE: same problem with {time_created} as in data-set A/105 --> check comment there!!!
                        Create Temp List    ${ehr_id_obj}[0]  ${time_created}[0]  ${system_id_obj}[0]
                        Update 'rows' in Temp Result-Data-Set    D/303

D/304
    [Documentation]     same flow as D/200
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
                        Update 'rows' in Temp Result-Data-Set    D/304

D/306
    [Documentation]     same flow as D/200
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
                        Update 'rows' in Temp Result-Data-Set    D/306

D/307
    [Documentation]     same flow as D/201
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        # TODO-NOTE: same problem with {time_created} as in data-set A/105 --> check comment there!!!
                        Create Temp List    ${ehr_id_obj}[0]  ${time_created}[0]  ${system_id_obj}[0]
                        Update 'rows' in Temp Result-Data-Set    D/307

D/308
    [Documentation]     same flow as D/200
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
                        Update 'rows' in Temp Result-Data-Set    D/308

D/309
    [Documentation]     same flow as D/201
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Load Temp Result-Data-Set    D/309
                        # TODO-NOTE: same problem with {time_created} as in data-set A/105 --> check comment there!!!
                        Create Temp List    ${ehr_id_obj}[0]  ${time_created}[0]  ${system_id_obj}[0]
                        Update 'rows' in Temp Result-Data-Set    D/309

D/310
    [Documentation]     same flow as D/200
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
                        Update 'rows' in Temp Result-Data-Set    D/310

D/311
    [Documentation]     same flow as D/201
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        # TODO-NOTE: same problem with {time_created} as in data-set A/105 --> check comment there!!!
                        Create Temp List    ${ehr_id_obj}[0]    ${time_created}[0]    ${system_id_obj}[0]
                        Update 'rows' in Temp Result-Data-Set    D/311

D/312
    [Documentation]     Same flow as D/200 but have to limit result to TOP 5
                        Return From Keyword If    ${compo_index}!=1    NOTHING TO DO HERE!
                        Return From Keyword If    ${ehr_index}>5   NOT IN TOP 5!
                        Create Temp List    ${ehr_id_value}[0]
                        ...                 ${time_created}[0]
                        ...                 ${system_id}[0]
                        ...                 TODOO: CLARIFY w/ @PABLO - TOP5 newest OR oldest?
                        Update 'rows' in Temp Result-Data-Set    D/312

D/400
    [Documentation]     Conditions are set to meet reqs of expected result-data-set.
    ...                 Here we are interested only in EHR record number 1 and all of it's compositions.
    ...                 1. Don't do anything if {ehr_index} is not 1.
    ...                 2. Update query-data-set ONLY on FIRST iteration to avoid unnecessary repetitions.
                        Return From Keyword If    not ${ehr_index}==1    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==1    Update Temp Query-Data-Set    D/400
                        Create Temp List    ${compo_uid_value}
                        ...                 ${compo_name_value}
                        ...                 ${compo_archetype_node_id}
                        ...                 ${compo_language}
                        ...                 ${compo_territory}
                        ...                 ${compo_category_value}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/400

D/401
    [Documentation]     Same flow as D/400
                        Return From Keyword If    not ${ehr_index}==1    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==1    Update Temp Query-Data-Set    D/401
                        Create Temp List    ${compo_uid_value}
                        ...                 ${compo_name_value}
                        ...                 ${compo_archetype_node_id}
                        ...                 ${compo_language}
                        ...                 ${compo_territory}
                        ...                 ${compo_category_value}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/401

D/402
    [Documentation]     Same flow as D/400
                        Return From Keyword If    not ${ehr_index}==1    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==1    Update Temp Query-Data-Set    D/402
                        Create Temp List    ${compo_uid_value}
                        ...                 ${compo_name_value}
                        ...                 ${compo_archetype_node_id}
                        ...                 ${compo_language}
                        ...                 ${compo_territory}
                        ...                 ${compo_category_value}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/402

D/403
    [Documentation]     Same flow as D/402 with different content in 'rows'
                        Return From Keyword If    not ${ehr_index}==1    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==1    Update Temp Query-Data-Set    D/403
                        Create Temp List    ${compo_uid}
                        ...                 ${compo_name}
                        ...                 ${compo_archetype_node_id}
                        ...                 ${compo_language}
                        ...                 ${compo_territory}
                        ...                 ${compo_category}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/403

D/404
    [Documentation]     Same flow as D/403
                        Return From Keyword If    not ${ehr_index}==1    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==1    Update Temp Query-Data-Set    D/404
                        Create Temp List    ${compo_uid}
                        ...                 ${compo_name}
                        ...                 ${compo_archetype_node_id}
                        ...                 ${compo_language}
                        ...                 ${compo_territory}
                        ...                 ${compo_category}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/404

D/405
    [Documentation]     Same flow as D/403
                        Return From Keyword If    not ${ehr_index}==1    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==1    Update Temp Query-Data-Set    D/405
                        Create Temp List    ${compo_uid}
                        ...                 ${compo_name}
                        ...                 ${compo_archetype_node_id}
                        ...                 ${compo_language}
                        ...                 ${compo_territory}
                        ...                 ${compo_category}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/405

D/500
    [Documentation]     Condition: compo must be an Observation and must belong to first EHR record.
    ${is_observation}=  Set Variable if  ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")  ${TRUE}
    ${is_from_ehr_1}=   Set Variable If  ${ehr_index}==1  ${TRUE}
                        Return From Keyword If    not (${is_observation} and ${is_from_ehr_1})    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==13    Update Temp Query-Data-Set    D/500
                        Create Temp List    ${compo_uid_value}
                        ...                 ${compo_name_value}
                        ...                 ${compo_data_origin_value}
                        ...                 ${compo_events_time_value}
                        ...                 ${compo_events_items_value_value}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/500
                    
D/501
    [Documentation]     Same flow as D/500, different content in 'rows'
    ${is_observation}=  Set Variable if  ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")  ${TRUE}
    ${is_from_ehr_1}=   Set Variable If  ${ehr_index}==1  ${TRUE}
                        Return From Keyword If    not (${is_observation} and ${is_from_ehr_1})    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==13    Update Temp Query-Data-Set    D/501
                        Create Temp List    ${compo_uid}
                        ...                 ${compo_name}
                        ...                 ${compo_data_origin}
                        ...                 ${compo_events_time}
                        ...                 ${compo_events_items_value}
                        Update 'rows', 'q' and 'meta' in Temp Result-Data-Set    D/501

D/502
    [Documentation]     Similar flow as D/500, but different element is replaced in query-data-set.
    ...                 Condition: compo must be an Observation and must belong to first EHR record.
    ${is_observation}=  Set Variable if  ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")  ${TRUE}
    ${is_from_ehr_1}=   Set Variable If  ${ehr_index}==1  ${TRUE}
                        Return From Keyword If    not (${is_observation} and ${is_from_ehr_1})    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==13    Update Query-Parameter in Temp Query-Data-Set    D/502
                        Create Temp List    ${compo_uid_value}
                        ...                 ${compo_name_value}
                        ...                 ${compo_data_origin_value}
                        ...                 ${compo_events_time_value}
                        ...                 ${compo_events_items_value_value}
                        Load Temp Result-Data-Set    D/502
    ${meta_exec_aql}=   Get Value From Json    ${expected}    $.meta._executed_aql
    ${meta_exec_aql}=   Replace String    ${meta_exec_aql}[0]    __MODIFY_EHR_ID_1__    ${ehr_id}
                        Update Value To Json   ${expected}    $.meta._executed_aql    ${meta_exec_aql}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/D/502.tmp.json
                        Update 'rows' in Temp Result-Data-Set    D/502

D/503
    [Documentation]     Similar flow as D/500, but different element is replaced in query-data-set.
    ...                 Condition: compo must be an Observation and must belong to first EHR record.
    ${is_observation}=  Set Variable if  ("${compo_content_archetype_node_id}"=="openEHR-EHR-OBSERVATION.minimal.v1")  ${TRUE}
    ${is_from_ehr_1}=   Set Variable If  ${ehr_index}==1  ${TRUE}
                        Return From Keyword If    not (${is_observation} and ${is_from_ehr_1})    NOTHING TO DO HERE!
                        Run Keyword if    ${ehr_index}==1 and ${compo_index}==13    Update Query-Parameter in Temp Query-Data-Set    D/503
                        Create Temp List    ${compo_uid}
                        ...                 ${compo_name}
                        ...                 ${compo_data_origin}
                        ...                 ${compo_events_time}
                        ...                 ${compo_events_items_value}
                        Load Temp Result-Data-Set    D/503
    ${meta_exec_aql}=   Get Value From Json    ${expected}    $.meta._executed_aql
    ${meta_exec_aql}=   Replace String    ${meta_exec_aql}[0]    __MODIFY_EHR_ID_1__    ${ehr_id}
                        Update Value To Json   ${expected}    $.meta._executed_aql    ${meta_exec_aql}
                        # Set Suite Variable    ${expected}    ${expected}
                        Output    ${expected}     ${QUERY RESULTS LOADED DB}/D/503.tmp.json
                        Update 'rows' in Temp Result-Data-Set    D/503







# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]

# *** Keywords ***
# prepare query request session
#     [Arguments]         ${format}=JSON    &{extra_headers}
#     [Documentation]     Prepares request settings for usage with RequestLibrary
#     ...                 :format: JSON (default) / XML
#     ...                 :extra_headers: optional - e.g. Prefer=return=representation
#     ...                                            e.g. If-Match={ehrstatus_uid}

#                         # case: JSON
#                         Run Keyword If      $format=='JSON'    set request headers
#                         ...                 content=application/json
#                         ...                 accept=application/json
#                         ...                 &{extra_headers}

#                         # case: XML
#                         Run Keyword If      $format=='XML'    set request headers
#                         ...                 content=application/xml
#                         ...                 accept=application/xml
#                         ...                 &{extra_headers}


# set request headers
#     [Arguments]         ${content}=application/json  ${accept}=application/json  &{extra_headers}
#     [Documentation]     Sets the headers of a request
#     ...                 :content: application/json (default) / application/xml
#     ...                 :accept: application/json (default) / application/xml
#     ...                 :extra_headers: optional

#                         Log Many            ${content}  ${accept}  ${extra_headers}

#     &{headers}=         Create Dictionary   Content-Type=${content}
#                         ...                 Accept=${accept}

#                         Run Keyword If      ${extra_headers}    Set To Dictionary    ${headers}    &{extra_headers}

#                         Create Session      ${SUT}    ${${SUT}.URL}
#                         ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True

#                         Set Test Variable   ${headers}    ${headers}


# Example of how to properly escape regex expressions to use with DeepDiff lib
#    ${exclude_paths}    Create List    root\\['meta'\\]    \\['columns'\\]\\[\\d+\\]\\['path'\\]
#                        ...            \\['_type'\\]
#    &{diff}             compare json-strings    ${response body}    ${expected result}
#                        ...                     exclude_regex_paths=${exclude_paths}
#                        ...                     ignore_order=False
