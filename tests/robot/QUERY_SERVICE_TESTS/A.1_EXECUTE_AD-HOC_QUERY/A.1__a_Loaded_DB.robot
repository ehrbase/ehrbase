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
Metadata    Version    0.1.0
Metadata    Authors    *Wladislaw Wagner, Pablo Pazos*
Metadata    Created    2019
Metadata    Updated    2021.10.05
Metadata    TOP_TEST_SUITE    AQL
Metadata    Command    robot -d results -L TRACE -i AQL_loaded_db robot/QUERY_SERVICE_TESTS

Documentation   Main flow: execute ad-hoc QUERY where data exists
...
...     Preconditions:
...         Required data and expected result data-sets exists for each case.
...
...     Flow:
...         1. Invoke execute ad-hoc QUERY service
...         2. The result should match the expected result and cardinality
...
...     Postconditions:
...         None (system state is not altered)

Resource       ../../_resources/keywords/aql_query_keywords.robot

Suite Setup    aql_query_keywords.Establish Preconditions
# Test Setup  Establish Preconditions
# Test Teardown  restore clean SUT state
# Suite Teardown    Run Keywords    Clean DB    # Delete Temp Result-Data-Sets

Force Tags    refactor    AQL_loaded_db



*** Variables ***
${ehr data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/data_load/ehrs/
${compo data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/data_load/compositions/



*** Test Cases ***
A-100 Execute Ad-Hoc Query - Get EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]
    A/100_get_ehrs.json    A/100.tmp.json


A-101 Execute Ad-Hoc Query - Get EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/101_get_ehrs.json    A/101.tmp.json


A-102 Execute Ad-Hoc Query - Get EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]
    A/102_get_ehrs.json    A/102.tmp.json
    A/104_get_ehrs.json    A/104.tmp.json


A-103 Execute Ad-Hoc Query - Get EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/103_get_ehrs.json    A/103.tmp.json


A-105 Execute Ad-Hoc Query - Get EHRs
    [Documentation]     Execute AQL query\n\n
    ...                 SELECT e/ehr_id, e/time_created, e/system_id FROM EHR e
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/105_get_ehrs.json    A/105.tmp.json


A-106 Execute Ad-Hoc Query - Get EHRs
    [Documentation]     Execute AQL query\n\n
    ...                 SELECT e/ehr_id, e/time_created, e/system_id, e/ehr_status FROM EHR e
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/106_get_ehrs.json    A/106.tmp.json


A-107 Execute Ad-Hoc Query - Get EHRs (filtered: top 5)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/107_get_ehrs_top_5.json    A/107.tmp.json


A-108 Execute Ad-Hoc Query - Get EHRs (ordered by: time-created)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/108_get_ehrs_orderby_time-created.json    A/108.tmp.json    ignore_order=${FALSE}


A-109 Execute Ad-Hoc Query - Get EHRs (filtered: timewindow)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_timewindow    future

    A/109_get_ehrs_within_timewindow.json       A/109.tmp.json

A-200 Execute Ad-Hoc Query - Get EHR By ID
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]
    A/200_query.tmp.json    A/200.tmp.json    # blueprint: A/200_get_ehr_by_id.json
    A/201_query.tmp.json    A/201.tmp.json    # blueprint: A/201_get_ehr_by_id.json


A-202 Execute Ad-Hoc Query - Get EHR By ID
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]
    A/202_query.tmp.json    A/202.tmp.json    # blueprint: A/202_get_ehr_by_id.json
    A/203_query.tmp.json    A/203.tmp.json    # blueprint: A/203_get_ehr_by_id.json


A-300 Execute Ad-Hoc Query - Get EHRs Which Have Compositions
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/300_get_ehrs_by_contains_any_composition.json               A/300.tmp.json


A-400 Execute Ad-Hoc Query - Get EHRs Which Have Compositions
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/400_get_ehrs_by_contains_composition_with_archetype.json    A/400.tmp.json
    A/401_get_ehrs_by_contains_composition_with_archetype.json    A/401.tmp.json
    A/402_get_ehrs_by_contains_composition_with_archetype.json    A/402.tmp.json


A-500 Execute Ad-Hoc Query - Get EHRs Which Have Compositions
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]                  
    A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/500.tmp.json
    A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/501.tmp.json  
    A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/502.tmp.json
    A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/503.tmp.json


A-600 Execute Ad-Hoc Query - Get EHRs Which Have Compositions
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/600.tmp.json
    A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/601.tmp.json
    A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/602.tmp.json
    A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/603.tmp.json


B-100 Execute Ad-Hoc Query - Get Compositions From All EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/100_get_compositions_from_all_ehrs.json    B/100.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-102 Execute Ad-Hoc Query - Get Compositions (ordered by: name)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/102_get_compositions_orderby_name.json    B/102.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-103 Execute Ad-Hoc Query - Get Compositions (filtered: timewindow)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_timewindow    future
    B/103_get_compositions_within_timewindow.json    B/103.tmp.json


B-104 Get Compositions (filtered: top 5, ordered by: start_time ASC)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              635    586
    B/104_get_compositions_top_5_ordered_by_starttime_asc.json    B/104.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  635


B-105 Get Compositions (filtered: top 5, ordered by: start_time DESC)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586
    B/105_get_compositions_top_5_ordered_by_starttime_desc.json    B/105.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-106 Get Compositions (filtered: top 5, ordered by: start_time value ASC)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586
    B/106_get_compositions_top_5_ordered_by_starttimevalue_asc.json    B/106.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-200 Execute Ad-Hoc Query - Get Compositions From All EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/200_query.tmp.json    B/200.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-300 Execute Ad-Hoc Query - Get Compositions From All EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/300_get_compositions_with_archetype_from_all_ehrs.json    B/300.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-400 Execute Ad-Hoc Query - Get Composition(s)
    [Documentation]     Test w/ "all_types.composition.json" commit
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    587    not-ready
    B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json    B/400.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-500 Execute Ad-Hoc Query - Get Composition(s)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/500.tmp.json
    B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/501.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-502 Execute Ad-Hoc Query - Get Composition(s)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/502.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-503 Execute Ad-Hoc Query - Get Composition(s)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/503.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-600 Execute Ad-Hoc Query - Get Composition(s)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/600.tmp.json
    B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/601.tmp.json
    B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/602.tmp.json
    B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/603.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-700 Execute Ad-Hoc Query - Get Composition(s)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/700.tmp.json
    B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/701.tmp.json
    B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/702.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-800 Execute Ad-Hoc Query - Get Compositions By UID
    [Documentation]     B/800: SELECT c FROM COMPOSITION c [uid/value='123::node.name.com::1']
    ...                 B/801: SELECT c FROM COMPOSITION c [uid/value=$uid]
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              109    future
    B/800_query.tmp.json    B/800.tmp.json
    B/801_query.tmp.json    B/801.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  109  bug  still blocked by


B-802 Execute Ad-Hoc Query - Get Compositions By UID
    [Documentation]     B/802: SELECT c FROM COMPOSITION c WHERE c/uid/value='123::node.name.com::1'
    ...                 B/803: SELECT c FROM COMPOSITION c WHERE c/uid/value=$uid
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              [Tags]              586    not-ready
    B/802_query.tmp.json    B/802.tmp.json
    B/803_query.tmp.json    B/803.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


D-200 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/200_select_data_values_from_all_ehrs_contains_composition.json    D/200.tmp.json


D-201 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/201_select_data_values_from_all_ehrs_contains_composition.json    D/201.tmp.json


D-300 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/300_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/300.tmp.json

D-301 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/301_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/301.tmp.json


D-302 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/302_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/302.tmp.json


D-303 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/303_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/303.tmp.json


D-304 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/304_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/304.tmp.json


D-306 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/306.tmp.json


D-307 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/307.tmp.json


D-308 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/308_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/308.tmp.json


D-309 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/309_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/309.tmp.json


D-310 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/310_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/310.tmp.json


D-311 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/311.tmp.json


D-312 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query. \n\n
    ...                 select TOP 5 e/ehr_id/value, e/time_created/value, e/system_id/value from EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1]
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/312_select_data_values_from_all_ehrs_contains_composition_with_archetype_top_5.json    D/312.tmp.json


D-400 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/400_query.tmp.json    D/400.tmp.json


D-401 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/401_query.tmp.json    D/401.tmp.json


D-402 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/402_query.tmp.json    D/402.tmp.json


D-403 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/403_query.tmp.json    D/403.tmp.json


D-404 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/404_query.tmp.json    D/404.tmp.json


D-405 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/405_query.tmp.json    D/405.tmp.json


D-500 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              628    not-ready
    D/500_query.tmp.json    D/500.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  628  bug


D-501 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              628    not-ready
    D/501_query.tmp.json    D/501.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  628  bug


D-502 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              628    not-ready
    D/502_query.tmp.json    D/502.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  628  bug


D-503 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              628    not-ready
    D/503_query.tmp.json    D/503.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  628  bug


D-504 Execute Ad-HOc Query - Get archetype_details
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              
    D/504_query.tmp.json    D/504.tmp.json





# oooooooooooo ooooo     ooo ooooooooooooo ooooo     ooo ooooooooo.   oooooooooooo 
# `888'     `8 `888'     `8' 8'   888   `8 `888'     `8' `888   `Y88. `888'     `8 
#  888          888       8       888       888       8   888   .d88'  888         
#  888oooo8     888       8       888       888       8   888ooo88P'   888oooo8    
#  888    "     888       8       888       888       8   888`88b.     888    "    
#  888          `88.    .8'       888       `88.    .8'   888  `88b.   888       o 
# o888o           `YbodP'        o888o        `YbodP'    o888o  o888o o888ooooood8
#
# [ Things that are not expected to work yet ]

C-100 Execute Ad-Hoc Query - Get Entries from EHR
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    future
    C/100_query.tmp.json    C/100.tmp.json


C-101 Execute Ad-Hoc Query - Get Entries (filtered: top 5)
    [Documentation]     get_entries_top_5
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    future
    C/101_query.tmp.json    C/101.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  101  bug  blocked by


C-102 Execute Ad-Hoc Query - Get Entries (ordered by: name)
    [Documentation]     get_entries_orderby_name
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    future
    C/102_query.tmp.json    C/102.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  101  bug  blocked by


C-103 Execute Ad-Hoc Query - Get Entries (filtered: timewindow)
    [Documentation]     get_entries_within_timewindow
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    AQL_timewindow    future
    C/103_query.tmp.json    C/103.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  101  bug  reladed


C-200 Execute Ad-Hoc Query - Get Entries from EHR
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    future
    C/200_query.tmp.json    C/200.tmp.json


C-300 Execute Ad-Hoc Query - Get Entries from EHR
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    future
    C/300_query.tmp.json    C/300.tmp.json
    C/301_query.tmp.json    C/301.tmp.json
    C/302_query.tmp.json    C/302.tmp.json
    C/303_query.tmp.json    C/303.tmp.json


C-400 Execute Ad-Hoc Query - Get Entries from EHR
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    future
    C/400_query.tmp.json    C/400.tmp.json


C-500 Execute Ad-Hoc Query - Get Entries from EHR
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              AQL_entry    future
    C/500_query.tmp.json    C/500.tmp.json

D-505 Execute Ad-Hoc Query - Get all names values
    [Tags]
    [Documentation]     Checks if IndexOutOfBoundsException is not returned
    execute ad-hoc query    D/505_select_data_without_composition_and_ehr_criteria.json
    check response: is positive
    ${resultedNameValuesRows}     Get Value From Json     ${response body}        $.rows..name.value
    ${length}       Get Length              ${resultedNameValuesRows}
    Log                 ${length}           console=yes
    Should Be True      ${length}>5
    [Teardown]
    ...     Log     Test for bug: https://github.com/ehrbase/ehrbase/issues/808     console=yes


# SPECIAL CASES / REGRESSION TEST (RT) QUERIES

RT-001 - Query For Not-Existing Composition Should Return Empty Result
    [Tags]    
    Query For Not-Existing Composition Name

    # comment: validate response
    Integer    response status    200
    Array      $.rows    []





*** Keywords ***
Query For Not-Existing Composition Name
        # comment: create AQL string and execute AQL query
        ${query1}=    Catenate
        ...           SELECT
        ...             c/uid/value, c/name/value, c/archetype_node_id, c/composer/name
        ...           FROM
        ...             EHR e
        ...           CONTAINS
        ...             COMPOSITION c [openEHR-EHR-COMPOSITION.asdfsomegibberish.v1]
        Set Test Variable    ${payload}    {"q": "${query1}"}
        POST /query/aql (REST)     JSON








# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]

# *** Test Cases ***
# Main Flow: Execute Ad-Hoc Queries (Loaded DB)
#     [Template]         execute ad-hoc query (no result comparison)
#     [Tags]

#     No Operation

#     # EHRs
#     A/100_get_ehrs.json
#     A/101_get_ehrs.json
#     A/102_get_ehrs.json
#     A/103_get_ehrs.json
#     A/104_get_ehrs.json
#     A/105_get_ehrs.json
#     A/106_get_ehrs.json
#     A/107_get_ehrs_top_5.json
#     A/108_get_ehrs_orderby_time-created.json
#     A/109_get_ehrs_within_timewindow.json
#     A/200_get_ehr_by_id.json
#     A/201_get_ehr_by_id.json
#     A/202_get_ehr_by_id.json
#     A/203_get_ehr_by_id.json
#     A/300_get_ehrs_by_contains_any_composition.json
#     A/400_get_ehrs_by_contains_composition_with_archetype.json
#     A/401_get_ehrs_by_contains_composition_with_archetype.json
#     A/402_get_ehrs_by_contains_composition_with_archetype.json
#     A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json
#     A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json
#     A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json
#     A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json
#     A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
#     A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
#     A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
#     A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json

#     # COMPOSITIONs
#     B/100_get_compositions_from_all_ehrs.json
#     B/102_get_compositions_orderby_name.json
#     B/103_get_compositions_within_timewindow.json
#     B/200_get_compositions_from_ehr_by_id.json
#     B/300_get_compositions_with_archetype_from_all_ehrs.json
#     B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json
#     B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
#     B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
#     B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
#     B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
#     B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
#     B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
#     B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
#     B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
#     B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
#     B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
#     B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
#     B/800_get_composition_by_uid.json
#     B/801_get_composition_by_uid.json
#     B/802_ge_composition_by_uid.json
#     B/803_get_composition_by_uid.json

#     # ENTRIEs
#     C/100_get_entries_from_ehr_with_uid_contains_compositions_from_all_ehrs.json
#     C/101_get_entries_top_5.json
#     C/102_get_entries_orderby_name.json
#     C/103_get_entries_within_timewindow.json
#     C/200_get_entries_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
#     C/300_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
#     C/301_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
#     C/302_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
#     C/303_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
#     C/400_get_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
#     C/500_get_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs_condition.json

#     # DATA
#     D/100_select_data_values_from_all_ehrs.json
#     D/101_select_data_values_from_all_ehrs.json
#     D/200_select_data_values_from_all_ehrs_contains_composition.json
#     D/201_select_data_values_from_all_ehrs_contains_composition.json
#     D/300_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/301_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/302_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/303_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/304_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/308_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/309_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/310_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
#     D/400_select_data_values_from_all_compositions_in_ehr.json
#     D/401_select_data_values_from_all_compositions_in_ehr.json
#     D/402_select_data_values_from_all_compositions_in_ehr.json
#     D/403_select_data_values_from_all_compositions_in_ehr.json
#     D/404_select_data_values_from_all_compositions_in_ehr.json
#     D/405_select_data_values_from_all_compositions_in_ehr.json
#     D/500_select_data_values_from_compositions_with_given_archetype_in_ehr.json
#     D/501_select_data_values_from_compositions_with_given_archetype_in_ehr.json
#     D/502_select_data_values_from_compositions_with_given_archetype_in_ehr.json
#     D/503_select_data_values_from_compositions_with_given_archetype_in_ehr.json

#     [Teardown]      TRACE GITHUB ISSUE  123  bug  Some AQL QUERIES fail!



# *** Keywords ***
# # NOTE: below two KWs do exactly the same
# D/200
#     Run keyword if      ${compo_index}==1   Run Keywords
#     ...                 Load Temp Result-Data-Set    D/200
#     ...          AND    Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
#     ...          AND    Add Object To Json    ${expected}    $.rows    ${rows_content}
#     ...          AND    Output    ${expected}    ${QUERY RESULTS LOADED DB}/D/200.tmp.json
#     ...          AND    Clean Up Suite Variables

# D/200 alternative
#     Return From Keyword If    ${compo_index}!=1    nothing to do here!
#     Load Temp Result-Data-Set    D/200
#     Create Temp List    ${ehr_id_value}[0]    ${time_created}[0]    ${system_id}[0]
#     Add Object To Json    ${expected}    $.rows    ${rows_content}
#     Output    ${expected}    ${QUERY RESULTS LOADED DB}/D/200.tmp.json
#     Clean Up Suite Variables
