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
Documentation   Alternative flow (AF): execute ad-hoc QUERY where DB is EMPTY
...
...     Preconditions:
...         DB IS EMPTY.
...
...     Flow:
...         1. Invoke execute ad-hoc QUERY service
...         2. The result should match the expected result and cardinality
...
...     Postconditions:
...         None (system state is not altered)

Resource    ${CURDIR}${/}../../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/aql_query_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/ehr_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/contribution_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/composition_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/template_opt1.4_keywords.robot

Test Setup  Establish Preconditions for Scenario: EMPTY DB
# Test Teardown  restore clean SUT state

Force Tags    refactor    empty_db



# *** Variables ***
# ${ehr data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/data_load/ehrs/
# ${compo data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/data_load/compositions/



*** Test Cases ***
Bug Case 01 - Failing Ad-Hoc Queries - Get EHR(s)
    [Documentation]     TEMP TEST CASE FOR BUG REPORTING
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              failing

    A/105_get_ehrs.json
    A/106_get_ehrs.json
    A/108_get_ehrs_orderby_time-created.json
    A/109_get_ehrs_within_timewindow.json
    A/202_get_ehr_by_id_empty_db.json
    A/203_get_ehr_by_id_empty_db.json

    [Teardown]          TRACE GITHUB ISSUE  83  not-ready


Bug Case 02 - Failing Ad-Hoc Queries - Get Composition(s)
    [Documentation]     TEMP TEST CASE FOR BUG REPORTING
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              failing

    B/102_get_compositions_orderby_name.json
    B/103_get_compositions_within_timewindow.json
    B/800_get_composition_by_uid_empty_db.json
    B/801_get_composition_by_uid_empty_db.json

    [Teardown]          TRACE GITHUB ISSUE  83  not-ready


Bug Case 03 - Failing Ad-Hoc Queries - Get Entrie(s)
    [Documentation]     TEMP TEST CASE FOR BUG REPORTING
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              failing

    C/102_get_entries_empty_db.json
    C/103_get_entries_empty_db.json

    [Teardown]          TRACE GITHUB ISSUE  83  not-ready


Bug Case 04 - Failing Ad-Hoc Queries - Get Data
    [Documentation]     TEMP TEST CASE FOR BUG REPORTING
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              failing

    D/101_select_data_values_from_all_ehrs.json
    D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json

    [Teardown]          TRACE GITHUB ISSUE  83  not-ready


AF-01 Execute Ad-Hoc Query - Get EHR(s)
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              not-ready

    A/100_get_ehrs.json
    A/101_get_ehrs.json
    A/102_get_ehrs.json
    A/103_get_ehrs.json
    A/104_get_ehrs.json
    A/107_get_ehrs_top_5.json
    A/200_get_ehr_by_id_empty_db.json
    A/201_get_ehr_by_id_empty_db.json
    A/300_get_ehrs_by_contains_any_composition.json
    A/400_get_ehrs_by_contains_composition_with_archetype.json
    A/401_get_ehrs_by_contains_composition_with_archetype.json
    A/402_get_ehrs_by_contains_composition_with_archetype.json
    A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json
    A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json
    A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json
    A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json
    A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json

    # # FAILNG QUERIES
    # A/105_get_ehrs.json
    # A/106_get_ehrs.json
    # A/108_get_ehrs_orderby_time-created.json
    # A/109_get_ehrs_within_timewindow.json
    # A/202_get_ehr_by_id_empty_db.json
    # A/203_get_ehr_by_id_empty_db.json


AF-02 Execute Ad-Hoc Query - Get Composition(s)
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              not-ready

    B/100_get_compositions_from_all_ehrs.json
    B/101_get_compositions_top_5.json
    B/200_get_compositions_from_ehr_by_id_empty_db.json
    B/300_get_compositions_with_archetype_from_all_ehrs.json
    B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json
    B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
    B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
    B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
    B/802_get_composition_by_uid_empty_db.json
    B/803_get_composition_by_uid_empty_db.json

    # # FAILING QUERIES
    # B/102_get_compositions_orderby_name.json
    # B/103_get_compositions_within_timewindow.json
    # B/800_get_composition_by_uid_empty_db.json
    # B/801_get_composition_by_uid_empty_db.json


AF-03 Execute Ad-Hoc Query - Get Entrie(s)
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              not-ready

    C/100_get_entries_empty_db.json
    C/101_get_entries_empty_db.json
    C/200_get_entries_empty_db.json
    C/300_get_entries_empty_db.json
    C/301_get_entries_empty_db.json
    C/302_get_entries_empty_db.json
    C/303_get_entries_empty_db.json
    C/400_get_entries_empty_db.json
    C/500_get_entries_empty_db.json

    # # FAILING QUERIES
    # C/102_get_entries_empty_db.json
    # C/103_get_entries_empty_db.json


AF-04 Execute Ad-Hoc Query - Get Data
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              not-ready

    D/100_select_data_values_from_all_ehrs.json
    D/200_select_data_values_from_all_ehrs_contains_composition.json
    D/201_select_data_values_from_all_ehrs_contains_composition.json
    D/300_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/301_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/302_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/303_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/304_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/305_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/308_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/309_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/310_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/400_select_data_empty_db.json
    D/401_select_data_empty_db.json
    D/402_select_data_empty_db.json
    D/403_select_data_empty_db.json
    D/404_select_data_empty_db.json
    D/405_select_data_empty_db.json
    D/500_select_data_empty_db.json
    D/501_select_data_empty_db.json
    D/502_select_data_empty_db.json
    D/503_select_data_empty_db.json

    # # FAILING QUERIES
    # D/101_select_data_values_from_all_ehrs.json
    # D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    # D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    # D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json





*** Keywords ***
Establish Preconditions for Scenario: EMPTY DB
    Check DB is empty

Check DB is empty
    [Documentation]     Connects with DB and checks that row count of given
    ...                 tables is equal to 0 (zero).
    ...                 NOTE: add more tables to check - when needed

                        Connect With DB
    ${ehr_records}=     Count Rows In DB Table    ehr.ehr
                        Should Be Equal As Integers    ${ehr_records}    ${0}
    [Teardown]          Disconnect From Database
