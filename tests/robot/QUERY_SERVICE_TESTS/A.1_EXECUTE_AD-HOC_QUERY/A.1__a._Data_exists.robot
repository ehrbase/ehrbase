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

Force Tags    refactor    loaded_db



*** Variables ***
${ehr data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/data_load/ehrs/
${compo data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/data_load/compositions/



*** Test Cases ***
Main flow: execute ad-hoc QUERY where data exists I
    [Template]         execute ad-hoc query (no result comparison)
    [Tags]             

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
    [Tags]
                    retrieve OPT list
                    OPT list is empty


Establish Preconditions for Scenario: LOADED DB (PART I)
    [Tags]          

                    upload OPT      minimal/minimal_admin.opt
                    upload OPT      minimal/minimal_observation.opt
                    upload OPT      minimal/minimal_instruction.opt
                    upload OPT      minimal/minimal_evaluation.opt
                    upload OPT      minimal/minimal_action.opt

Establish Preconditions for Scenario: LOADED DB (PART II)
    [Tags]              

    # load expected-result-blueprints

    ${elist}=           Create List   @{EMPTY}

    ${A/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/100_get_ehrs.json
    ${A/100}=           Update Value To Json    ${A/100}    $.rows    ${elist}
                        Output    ${A/100}    ${QUERY RESULTS LOADED DB}/A/100
    
    ${A/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/101_get_ehrs.json
    ${A/101}=           Update Value To Json    ${A/101}    $.rows    ${elist}
                        Output    ${A/101}    ${QUERY RESULTS LOADED DB}/A/101
    
    ${A/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/102_get_ehrs.json
    ${A/102}=           Update Value To Json    ${A/102}    $.rows    ${elist}
                        Output    ${A/102}    ${QUERY RESULTS LOADED DB}/A/102

    ${A/300}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/300_get_ehrs_by_contains_any_composition.json
    ${A/300}=           Update Value To Json    ${A/300}    $.rows    ${elist}
                        Output    ${A/300}    ${QUERY RESULTS LOADED DB}/A/300

    ${A/400}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/400_get_ehrs_by_contains_composition_with_archetype.json
    ${A/400}=           Update Value To Json    ${A/400}    $.rows    ${elist}
                        Output    ${A/400}    ${QUERY RESULTS LOADED DB}/A/400
    
    ${A/401}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/401_get_ehrs_by_contains_composition_with_archetype.json
    ${A/401}=           Update Value To Json    ${A/401}    $.rows    ${elist}
                        Output    ${A/401}    ${QUERY RESULTS LOADED DB}/A/401
    
    ${A/402}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/402_get_ehrs_by_contains_composition_with_archetype.json
    ${A/402}=           Update Value To Json    ${A/402}    $.rows    ${elist}
                        Output    ${A/402}    ${QUERY RESULTS LOADED DB}/A/402

    ${A/500}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/500}=           Update Value To Json    ${A/500}    $.rows    ${elist}
                        Output    ${A/500}    ${QUERY RESULTS LOADED DB}/A/500
    
    ${A/501}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/501}=           Update Value To Json    ${A/501}    $.rows    ${elist}
                        Output    ${A/501}    ${QUERY RESULTS LOADED DB}/A/501
    
    ${A/502}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/502}=           Update Value To Json    ${A/502}    $.rows    ${elist}
                        Output    ${A/502}    ${QUERY RESULTS LOADED DB}/A/502

    ${A/503}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/503}=           Update Value To Json    ${A/503}    $.rows    ${elist}
                        Output    ${A/503}    ${QUERY RESULTS LOADED DB}/A/503

    ${A/600}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/600}=           Update Value To Json    ${A/600}    $.rows    ${elist}
                        Output    ${A/600}    ${QUERY RESULTS LOADED DB}/A/600

    ${A/601}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/601}=           Update Value To Json    ${A/601}    $.rows    ${elist}
                        Output    ${A/601}    ${QUERY RESULTS LOADED DB}/A/601
    
    ${A/602}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/602}=           Update Value To Json    ${A/602}    $.rows    ${elist}
                        Output    ${A/602}    ${QUERY RESULTS LOADED DB}/A/602
    
    ${A/603}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/603}=           Update Value To Json    ${A/603}    $.rows    ${elist}
                        Output    ${A/603}    ${QUERY RESULTS LOADED DB}/A/603



Establish Preconditions for Scenario: LOADED DB (PART III)            
    [Template]          Populate SUT with Test-Data and Prepare Expected Results
    [Tags]              

    # Load SUT with 10 EHRs each containing 15 COMPOSITIONs
    
    0    ${ehr data sets}/ehr_status_01.json
    1    ${ehr data sets}/ehr_status_02.json
    2    ${ehr data sets}/ehr_status_03.json
    3    ${ehr data sets}/ehr_status_04.json
    4    ${ehr data sets}/ehr_status_05.json
    5    ${ehr data sets}/ehr_status_06.json
    6    ${ehr data sets}/ehr_status_07.json
    7    ${ehr data sets}/ehr_status_08.json
    8    ${ehr data sets}/ehr_status_09.json
    9    ${ehr data sets}/ehr_status_10.json


Main flow: execute ad-hoc QUERY where data exists
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready

    # EHRs | THE OUTCOMMENTED ARE FAILING

    A/100_get_ehrs.json            A/100
    # A/101_get_ehrs               A/101
    A/102_get_ehrs.json            A/102
    # A/103_get_ehrs               A/103
    # A/104_get_ehrs               A/104
    # A/105_get_ehrs               A/105
    # A/106_get_ehrs               A/106
    # A/200_get_ehr_by_id
    # A/201_get_ehr_by_id
    # A/202_get_ehr_by_id
    # A/203_get_ehr_by_id
    A/300_get_ehrs_by_contains_any_composition.json               A/300
    A/400_get_ehrs_by_contains_composition_with_archetype.json    A/400
    A/401_get_ehrs_by_contains_composition_with_archetype.json    A/401
    A/402_get_ehrs_by_contains_composition_with_archetype.json    A/402
    A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/500
    A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/501
    # A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/502         # FAILS | GITHUB #61
    # A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/503         # FAILS | JIRA EHR-537
    A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/600
    A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/601
    # A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/602    # FAILS | GITHUB #61
    # A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/603    # FAILS | JIRA EHR-537

    # THE FOLLOWING REQUIRE SEPARATE FLOW

    # A/107_get_ehrs_top_5.json    A/107     # separate flow
    # A/108_get_ehrs_orderby_time-created    # separate flow    # FAILS
    # A/109_get_ehrs_within_timewindow       # separate flow    # FAILS



*** Keywords ***
Populate SUT with Test-Data and Prepare Expected Results
    [Arguments]         ${index}    ${ehr_status_object}
                        Log To Console  \nEHR RECORD ${index} ////////////////////////////////////

                        composition_keywords.start request session    Prefer=return=representation
                        create EHR record on the server    ${index}    ${ehr_status_object}

                        commit COMPO    ${compo data sets}/minimal_admin_1.composition.json
                        commit COMPO    ${compo data sets}/minimal_admin_2.composition.json
                        commit COMPO    ${compo data sets}/minimal_admin_3.composition.json
                        commit COMPO    ${compo data sets}/minimal_evaluation_1.composition.json
                        commit COMPO    ${compo data sets}/minimal_evaluation_2.composition.json
                        commit COMPO    ${compo data sets}/minimal_evaluation_3.composition.json
                        commit COMPO    ${compo data sets}/minimal_evaluation_4.composition.json
    # # commit COMPO    ${compo data sets}/minimal_instruction_1.composition.json  # GITHUB #61
    # # commit COMPO    ${compo data sets}/minimal_instruction_2.composition.json  # GITHUB #61
    # # commit COMPO    ${compo data sets}/minimal_instruction_3.composition.json  # GITHUB #61
    # # commit COMPO    ${compo data sets}/minimal_instruction_4.composition.json  # GITHUB #61
                        commit COMPO    ${compo data sets}/minimal_observation_1.composition.json
                        commit COMPO    ${compo data sets}/minimal_observation_2.composition.json
                        commit COMPO    ${compo data sets}/minimal_observation_3.composition.json
                        commit COMPO    ${compo data sets}/minimal_observation_4.composition.json


create EHR record on the server
    [Arguments]         ${index}    ${payload}

                        create new EHR with ehr_status  ${payload}
                        Integer    response status    201
                        extract ehr_id from response (JSON)

    ${ehr_id_value}=    String    response body ehr_id value
    ${time_created}     String    response body time_created
    ${system_id}        String    response body system_id value

    # ${expected}=        Update Value To Json    ${blueprint}    $.rows[${index}][0]    ${ehr_id}
    #                     Set Suite Variable  ${blueprint}  ${expected}
    #                     Set Suite Variable  ${expected}  "${expected}"

    ${A/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/100
    ${A/100}=           Add Object To Json    ${A/100}    $.rows    ${ehr_id_value}
                        Output    ${A/100}    ${QUERY RESULTS LOADED DB}/A/100

    ${A/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/101
    ${temp}=            Create List  ${ehr_id_value}[0]  ${time_created}[0]  ${system_id}[0]
    ${A/101}=           Add Object To Json    ${A/101}    $.rows    ${temp}
                        Output    ${A/101}    ${QUERY RESULTS LOADED DB}/A/101
    
    ${A/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/102
    ${A/102}=           Add Object To Json    ${A/102}    $.rows    ${ehr_id_value}
                        Output    ${A/102}    ${QUERY RESULTS LOADED DB}/A/102

    ${A/300}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/300
    ${A/300}=           Add Object To Json    ${A/300}    $.rows    ${ehr_id_value}
                        Output    ${A/300}    ${QUERY RESULTS LOADED DB}/A/300
    
    ${A/400}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/400
    ${A/400}=           Add Object To Json    ${A/400}    $.rows    ${ehr_id_value}
                        Output    ${A/400}    ${QUERY RESULTS LOADED DB}/A/400

    ${A/401}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/401
    ${A/401}=           Add Object To Json    ${A/401}    $.rows    ${ehr_id_value}
                        Output    ${A/401}    ${QUERY RESULTS LOADED DB}/A/401
    
    ${A/402}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/402
    ${A/402}=           Add Object To Json    ${A/402}    $.rows    ${ehr_id_value}
                        Output    ${A/402}    ${QUERY RESULTS LOADED DB}/A/402
    
    ${A/500}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/500
    ${A/500}=           Add Object To Json    ${A/500}    $.rows    ${ehr_id_value}
                        Output    ${A/500}    ${QUERY RESULTS LOADED DB}/A/500
    
    ${A/501}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/501
    ${A/501}=           Add Object To Json    ${A/501}    $.rows    ${ehr_id_value}
                        Output    ${A/501}    ${QUERY RESULTS LOADED DB}/A/501
    
    ${A/502}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/502
    ${A/502}=           Add Object To Json    ${A/502}    $.rows    ${ehr_id_value}
                        Output    ${A/502}    ${QUERY RESULTS LOADED DB}/A/502

    ${A/503}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/503
    ${A/503}=           Add Object To Json    ${A/503}    $.rows    ${ehr_id_value}
                        Output    ${A/503}    ${QUERY RESULTS LOADED DB}/A/503

    ${A/600}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/600
    ${A/600}=           Add Object To Json    ${A/600}    $.rows    ${ehr_id_value}
                        Output    ${A/600}    ${QUERY RESULTS LOADED DB}/A/600

    ${A/601}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/601
    ${A/601}=           Add Object To Json    ${A/601}    $.rows    ${ehr_id_value}
                        Output    ${A/601}    ${QUERY RESULTS LOADED DB}/A/601
    
    ${A/602}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/602
    ${A/602}=           Add Object To Json    ${A/602}    $.rows    ${ehr_id_value}
                        Output    ${A/602}    ${QUERY RESULTS LOADED DB}/A/602
    
    ${A/603}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/603
    ${A/603}=           Add Object To Json    ${A/603}    $.rows    ${ehr_id_value}
                        Output    ${A/603}    ${QUERY RESULTS LOADED DB}/A/603

commit COMPO
    [Arguments]         ${compo_file}
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

    &{resp}=            REST.POST    ${baseurl}/ehr/${ehr_id}/composition    ${compo_file}
                        Output Debug Info To Console
                        Integer    response status    201
                        Set Test Variable    ${response}    ${resp}
