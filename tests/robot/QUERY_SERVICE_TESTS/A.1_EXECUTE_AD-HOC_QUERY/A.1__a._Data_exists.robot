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
Documentation   Main flow: execute ad-hoc QUERY where data exists
...
...     Preconditions:
...         Required data exists for each case.
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

# Test Setup  start openehr server
# Test Teardown  restore clean SUT state

Force Tags    refactor



*** Variables ***
${ehr data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/data_load/ehrs/
${compo data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/data_load/compositions/




*** Test Cases ***
Main flow: execute ad-hoc QUERY where data exists I
    [Template]         execute ad-hoc query (no result comparison)

    # EHRs
    A/100_get_ehrs.json
    A/101_get_ehrs.json
    A/102_get_ehrs.json
    A/103_get_ehrs.json
    A/104_get_ehrs.json
    A/105_get_ehrs.json
    A/106_get_ehrs.json
    A/107_get_ehrs_top_5.json
    A/108_get_ehrs_orderby_time-created.json
    A/109_get_ehrs_within_timewindow.json
    A/200_get_ehr_by_id.json
    A/201_get_ehr_by_id.json
    A/202_get_ehr_by_id.json
    A/203_get_ehr_by_id.json
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

    # COMPOSITIONs
    B/100_get_compositions_from_all_ehrs.json
    B/101_get_compositions_top_5.json
    B/102_get_compositions_orderby_name.json
    B/103_get_compositions_within_timewindow.json
    B/200_get_compositions_from_ehr_by_id.json
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
    B/800_get_composition_by_uid.json
    B/801_get_composition_by_uid.json
    B/802_ge_composition_by_uid.json
    B/803_get_composition_by_uid.json

    # ENTRIEs
    C/100_get_entries_from_ehr_with_uid_contains_compositions_from_all_ehrs.json
    C/101_get_entries_top_5.json
    C/102_get_entries_orderby_name.json
    C/103_get_entries_within_timewindow.json
    C/200_get_entries_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
    C/300_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
    C/301_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
    C/302_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
    C/303_get_entries_with_type_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
    C/400_get_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json
    C/500_get_entries_with_archetype_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs_condition.json

    # DATA
    D/100_select_data_values_from_all_ehrs.json
    D/101_select_data_values_from_all_ehrs.json
    D/200_select_data_values_from_all_ehrs_contains_composition.json
    D/201_select_data_values_from_all_ehrs_contains_composition.json
    D/300_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/301_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/302_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/303_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/304_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/305_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/308_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/309_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/310_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
    D/400_select_data_values_from_all_compositions_in_ehr.json
    D/401_select_data_values_from_all_compositions_in_ehr.json
    D/402_select_data_values_from_all_compositions_in_ehr.json
    D/403_select_data_values_from_all_compositions_in_ehr.json
    D/404_select_data_values_from_all_compositions_in_ehr.json
    D/405_select_data_values_from_all_compositions_in_ehr.json
    D/500_select_data_values_from_compositions_with_given_archetype_in_ehr.json
    D/501_select_data_values_from_compositions_with_given_archetype_in_ehr.json
    D/502_select_data_values_from_compositions_with_given_archetype_in_ehr.json
    D/503_select_data_values_from_compositions_with_given_archetype_in_ehr.json

    [Teardown]      TRACE JIRA BUG    NO-JIRA-ID    not-ready    Some AQL QUERIES fail!


Check DB is empty
    [Tags]              xxx
    retrieve OPT list
    OPT list is empty


Load SUT with Test-Data
    [Tags]
    [Template]          create EHR records on the server
    ${ehr data sets}/ehr_status_01.json
    ${ehr data sets}/ehr_status_02.json
    ${ehr data sets}/ehr_status_03.json
    ${ehr data sets}/ehr_status_04.json
    ${ehr data sets}/ehr_status_05.json
    ${ehr data sets}/ehr_status_06.json
    ${ehr data sets}/ehr_status_07.json
    ${ehr data sets}/ehr_status_08.json
    ${ehr data sets}/ehr_status_09.json
    ${ehr data sets}/ehr_status_10.json



Main flow: execute ad-hoc QUERY where data exists
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready   xxx

    # EHRs
    A/100_get_ehrs


Alternative flow: execute ad-hoc QUERY where DB is empty
    [Template]          execute ad-hoc query and check result (empty DB)
    [Tags]              not-ready

    # EHRs
    A/100_get_ehrs.json



*** Keywords ***
create EHR records on the server
    [Arguments]         ${payload}
                        create new EHR with ehr_status  ${payload}
                        Integer    response status    201
                        # extract ehr_id from response (JSON)
