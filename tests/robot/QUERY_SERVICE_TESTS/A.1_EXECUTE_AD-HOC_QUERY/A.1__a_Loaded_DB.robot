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
Documentation   Main flow (MF): execute ad-hoc QUERY where data exists
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

Suite Setup    Establish Preconditions
# Test Setup  Establish Preconditions
# Test Teardown  restore clean SUT state
Suite Teardown    Run Keywords    Clean DB    Delete Temp Result Sets

Force Tags    refactor    loaded_db



*** Variables ***
${ehr data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/data_load/ehrs/
${compo data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/data_load/compositions/



*** Test Cases ***
# Check DB is empty
#     [Tags]          
#                     retrieve OPT list
#                     OPT list is empty

MF-001 Execute Ad-Hoc Query - Get EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready

    # EHRs | THE OUTCOMMENTED ARE FAILING

    A/100_get_ehrs.json            A/100.tmp.json
    # A/101_get_ehrs               A/101.tmp.json
    A/102_get_ehrs.json            A/102.tmp.json
    # A/103_get_ehrs               A/103.tmp.json
    # A/104_get_ehrs               A/104.tmp.json
    # A/105_get_ehrs               A/105.tmp.json
    # A/106_get_ehrs               A/106.tmp.json

# MF-002 Execute Ad-Hoc Query - Get EHR By ID
#     [Template]          execute ad-hoc query and check result (loaded DB)
#     [Tags]              not-ready

#     No Operation

#     # A/200_get_ehr_by_id
#     # A/201_get_ehr_by_id
#     # A/202_get_ehr_by_id
#     # A/203_get_ehr_by_id

MF-003 Execute Ad-Hoc Query - Get EHRs Which Have Compositions
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready

    A/300_get_ehrs_by_contains_any_composition.json               A/300.tmp.json
    A/400_get_ehrs_by_contains_composition_with_archetype.json    A/400.tmp.json
    A/401_get_ehrs_by_contains_composition_with_archetype.json    A/401.tmp.json
    A/402_get_ehrs_by_contains_composition_with_archetype.json    A/402.tmp.json
    A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/500.tmp.json
    A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/501.tmp.json
    A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/600.tmp.json
    A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/601.tmp.json

    # # FAILS | GITHUB #61
    # A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/502.tmp.json
    # A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/602.tmp.json

    # # FAILS | JIRA EHR-537
    # A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/503.tmp.json
    # A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/603.tmp.json


# MF-004 Execute Ad-Hoc Query - Get EHRs (filtered: top 5)
#     [Template]          execute ad-hoc query and check result (loaded DB)
#     [Tags]              not-ready

#     No Operation

#     # THE FOLLOWING REQUIRE SEPARATE FLOW

#     A/107_get_ehrs_top_5.json    A/107.tmp.json              # separate flow
#     A/108_get_ehrs_orderby_time-created    A/108.tmp.json    # separate flow    # FAILS
#     A/109_get_ehrs_within_timewindow       A/109.tmp.json    # separate flow    # FAILS



MF-005 Execute Ad-Hoc Query - Get Compositions From All EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready

    B/100_get_compositions_from_all_ehrs.json    B/100.tmp.json


MF-006 Execute Ad-Hoc Query - Get Compositions From EHR By ID
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready

    B/200_query.tmp.json    B/200.tmp.json


MF-007 Execute Ad-Hoc Query - Get Composition(s)
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready
    
    B/300_get_compositions_with_archetype_from_all_ehrs.json    B/300.tmp.json
    B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/500.tmp.json
    B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/501.tmp.json
    B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/600.tmp.json
    B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/601.tmp.json
    B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/700.tmp.json
    B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/701.tmp.json
    B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/702.tmp.json

    # # BACKLOG / DATA GENERATION FOR THIS QUERIES NOT READY OR NOT POSSIBLE YET
    # B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json    B/400.tmp.json
    # B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/502.tmp.json
    # B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/503.tmp.json
    # B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/602.tmp.json
    # B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/603.tmp.json


# MF-008 Execute Ad-Hoc Query - Get Compositions (filtered: i.e. top 5)
#     [Template]          execute ad-hoc query and check result (loaded DB)
#     [Tags]              not-ready

#     B/101_get_compositions_top_5.json    B/101.tmp.json
#     B/102_get_compositions_orderby_name.json    B/102.tmp.json
#     B/103_get_compositions_within_timewindow.json    B/103.tmp.json

MF-009 Execute Ad-Hoc Query - Get Compositions By UID
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready

    B/800_query.tmp.json    B/800.tmp.json
    B/801_query.tmp.json    B/801.tmp.json
    B/802_query.tmp.json    B/802.tmp.json
    B/803_query.tmp.json    B/803.tmp.json





*** Keywords ***
Delete Temp Result Sets
    Remove Files    ${QUERY RESULTS LOADED DB}/*/*.tmp.json
    Remove Files    ${VALID QUERY DATA SETS}//*/*.tmp.json


Establish Preconditions

    Preconditions (PART 1) - Upload Required OPTs
    Preconditions (PART 2) - Load Result-Blueprints
    Preconditions (PART 3) - Populate SUT with Test-Data and Prepare Expected Results


Preconditions (PART 1) - Upload Required OPTs

                        upload OPT      minimal/minimal_admin.opt
                        upload OPT      minimal/minimal_observation.opt
                        upload OPT      minimal/minimal_instruction.opt
                        upload OPT      minimal/minimal_evaluation.opt
                        upload OPT      minimal/minimal_action.opt
                        upload OPT      all_types/Test_all_types.opt


Preconditions (PART 2) - Load Result-Blueprints
    [Documentation]     Loads expected-result-blueprints and creates local copies for temporary use.
    ...                 Temporary copies are not git versioned. They are overwritten between test runs
    ...                 automatically and can be ignored or removed after test.

    ${elist}=           Create List   @{EMPTY}

    # EHR(S)

    ${A/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/100_get_ehrs.json
    ${A/100}=           Update Value To Json    ${A/100}    $.rows    ${elist}
                        Output    ${A/100}    ${QUERY RESULTS LOADED DB}/A/100.tmp.json
    
    ${A/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/101_get_ehrs.json
    ${A/101}=           Update Value To Json    ${A/101}    $.rows    ${elist}
                        Output    ${A/101}    ${QUERY RESULTS LOADED DB}/A/101.tmp.json
    
    ${A/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/102_get_ehrs.json
    ${A/102}=           Update Value To Json    ${A/102}    $.rows    ${elist}
                        Output    ${A/102}    ${QUERY RESULTS LOADED DB}/A/102.tmp.json

    ${A/300}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/300_get_ehrs_by_contains_any_composition.json
    ${A/300}=           Update Value To Json    ${A/300}    $.rows    ${elist}
                        Output    ${A/300}    ${QUERY RESULTS LOADED DB}/A/300.tmp.json

    ${A/400}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/400_get_ehrs_by_contains_composition_with_archetype.json
    ${A/400}=           Update Value To Json    ${A/400}    $.rows    ${elist}
                        Output    ${A/400}    ${QUERY RESULTS LOADED DB}/A/400.tmp.json
    
    ${A/401}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/401_get_ehrs_by_contains_composition_with_archetype.json
    ${A/401}=           Update Value To Json    ${A/401}    $.rows    ${elist}
                        Output    ${A/401}    ${QUERY RESULTS LOADED DB}/A/401.tmp.json
    
    ${A/402}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/402_get_ehrs_by_contains_composition_with_archetype.json
    ${A/402}=           Update Value To Json    ${A/402}    $.rows    ${elist}
                        Output    ${A/402}    ${QUERY RESULTS LOADED DB}/A/402.tmp.json

    ${A/500}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/500}=           Update Value To Json    ${A/500}    $.rows    ${elist}
                        Output    ${A/500}    ${QUERY RESULTS LOADED DB}/A/500.tmp.json
    
    ${A/501}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/501}=           Update Value To Json    ${A/501}    $.rows    ${elist}
                        Output    ${A/501}    ${QUERY RESULTS LOADED DB}/A/501.tmp.json
    
    ${A/502}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/502}=           Update Value To Json    ${A/502}    $.rows    ${elist}
                        Output    ${A/502}    ${QUERY RESULTS LOADED DB}/A/502.tmp.json

    ${A/503}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json
    ${A/503}=           Update Value To Json    ${A/503}    $.rows    ${elist}
                        Output    ${A/503}    ${QUERY RESULTS LOADED DB}/A/503.tmp.json

    ${A/600}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/600}=           Update Value To Json    ${A/600}    $.rows    ${elist}
                        Output    ${A/600}    ${QUERY RESULTS LOADED DB}/A/600.tmp.json

    ${A/601}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/601}=           Update Value To Json    ${A/601}    $.rows    ${elist}
                        Output    ${A/601}    ${QUERY RESULTS LOADED DB}/A/601.tmp.json
    
    ${A/602}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/602}=           Update Value To Json    ${A/602}    $.rows    ${elist}
                        Output    ${A/602}    ${QUERY RESULTS LOADED DB}/A/602.tmp.json
    
    ${A/603}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json
    ${A/603}=           Update Value To Json    ${A/603}    $.rows    ${elist}
                        Output    ${A/603}    ${QUERY RESULTS LOADED DB}/A/603.tmp.json


    # COMPOSITION(S)

    ${B/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/100_get_compositions_from_all_ehrs.json
    ${B/100}=           Update Value To Json   ${B/100}    $.rows    ${elist}
                        Output    ${B/100}     ${QUERY RESULTS LOADED DB}/B/100.tmp.json

    ${B/200_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/B/200_get_compositions_from_ehr_by_id.json
                        Output    ${B/200_query}    ${VALID QUERY DATA SETS}/B/200_query.tmp.json
    ${B/200}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/200_get_compositions_from_ehr_by_id.json
    ${B/200}=           Update Value To Json   ${B/200}    $.rows    ${elist}
                        Output    ${B/200}     ${QUERY RESULTS LOADED DB}/B/200.tmp.json
                        
    ${B/300}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/300_get_compositions_with_archetype_from_all_ehrs.json
    ${B/300}=           Update Value To Json   ${B/300}    $.rows    ${elist}
                        Output    ${B/300}     ${QUERY RESULTS LOADED DB}/B/300.tmp.json
            
    ${B/500}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    ${B/500}=           Update Value To Json   ${B/500}    $.rows    ${elist}
                        Output    ${B/500}     ${QUERY RESULTS LOADED DB}/B/500.tmp.json
                        
    ${B/501}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    ${B/501}=           Update Value To Json   ${B/501}    $.rows    ${elist}
                        Output    ${B/501}     ${QUERY RESULTS LOADED DB}/B/501.tmp.json
                        
    ${B/600}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    ${B/600}=           Update Value To Json   ${B/600}    $.rows    ${elist}
                        Output    ${B/600}     ${QUERY RESULTS LOADED DB}/B/600.tmp.json
                        
    ${B/601}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    ${B/601}=           Update Value To Json   ${B/601}    $.rows    ${elist}
                        Output    ${B/601}     ${QUERY RESULTS LOADED DB}/B/601.tmp.json
                        
    ${B/700}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
    ${B/700}=           Update Value To Json   ${B/700}    $.rows    ${elist}
                        Output    ${B/700}     ${QUERY RESULTS LOADED DB}/B/700.tmp.json
                        
    ${B/701}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
    ${B/701}=           Update Value To Json   ${B/701}    $.rows    ${elist}
                        Output    ${B/701}     ${QUERY RESULTS LOADED DB}/B/701.tmp.json
                        
    ${B/702}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json
    ${B/702}=           Update Value To Json   ${B/702}    $.rows    ${elist}
                        Output    ${B/702}     ${QUERY RESULTS LOADED DB}/B/702.tmp.json

    ${B/800_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/B/800_get_composition_by_uid.json
                        Output    ${B/800_query}    ${VALID QUERY DATA SETS}/B/800_query.tmp.json            
    ${B/800}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/800_get_composition_by_uid.json
    ${B/800}=           Update Value To Json   ${B/800}    $.rows    ${elist}
                        Output    ${B/800}     ${QUERY RESULTS LOADED DB}/B/800.tmp.json

    ${B/801_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/B/801_get_composition_by_uid.json
                        Output    ${B/801_query}    ${VALID QUERY DATA SETS}/B/801_query.tmp.json               
    ${B/801}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/801_get_composition_by_uid.json
    ${B/801}=           Update Value To Json   ${B/801}    $.rows    ${elist}
                        Output    ${B/801}     ${QUERY RESULTS LOADED DB}/B/801.tmp.json

    ${B/802_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/B/802_get_composition_by_uid.json
                        Output    ${B/802_query}    ${VALID QUERY DATA SETS}/B/802_query.tmp.json           
    ${B/802}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/802_get_composition_by_uid.json
    ${B/802}=           Update Value To Json   ${B/802}    $.rows    ${elist}
                        Output    ${B/802}     ${QUERY RESULTS LOADED DB}/B/802.tmp.json

    ${B/803_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/B/803_get_composition_by_uid.json
                        Output    ${B/803_query}    ${VALID QUERY DATA SETS}/B/803_query.tmp.json             
    ${B/803}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/803_get_composition_by_uid.json
    ${B/803}=           Update Value To Json   ${B/803}    $.rows    ${elist}
                        Output    ${B/803}     ${QUERY RESULTS LOADED DB}/B/803.tmp.json


    # BACKLOG / DATA GENERATION FOR THIS QUERIES NOT READY OR NOT POSSIBLE YET

    # ${B/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/101_get_compositions_top_5.json
    # ${B/101}=           Update Value To Json   ${B/101}    $.rows    ${elist}
    #                     Output    ${B/101}     ${QUERY RESULTS LOADED DB}/B/101.tmp.json
                        
    # ${B/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/102_get_compositions_orderby_name.json
    # ${B/102}=           Update Value To Json   ${B/102}    $.rows    ${elist}
    #                     Output    ${B/102}     ${QUERY RESULTS LOADED DB}/B/102.tmp.json
                        
    # ${B/103}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/103_get_compositions_within_timewindow.json
    # ${B/103}=           Update Value To Json   ${B/103}    $.rows    ${elist}
    #                     Output    ${B/103}     ${QUERY RESULTS LOADED DB}/B/103.tmp.json
                
    # ${B/400}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json
    # ${B/400}=           Update Value To Json   ${B/400}    $.rows    ${elist}
    #                     Output    ${B/400}     ${QUERY RESULTS LOADED DB}/B/400.tmp.json

    # # FAILS because requiered compos fail to commit (instruction compositions)             
    # ${B/502}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    # ${B/502}=           Update Value To Json   ${B/502}    $.rows    ${elist}
    #                     Output    ${B/502}     ${QUERY RESULTS LOADED DB}/B/502.tmp.json

    # # FAILS because we have no ACTION compositions yet                    
    # ${B/503}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json
    # ${B/503}=           Update Value To Json   ${B/503}    $.rows    ${elist}
    #                     Output    ${B/503}     ${QUERY RESULTS LOADED DB}/B/503.tmp.json

    # # FAILS because requiered compos fail to commit (instruction compositions)              
    # ${B/602}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    # ${B/602}=           Update Value To Json   ${B/602}    $.rows    ${elist}
    #                     Output    ${B/602}     ${QUERY RESULTS LOADED DB}/B/602.tmp.json

    # # FAILS because we have no ACTION compositions yet             
    # ${B/603}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json
    # ${B/603}=           Update Value To Json   ${B/603}    $.rows    ${elist}
    #                     Output    ${B/603}     ${QUERY RESULTS LOADED DB}/B/603.tmp.json


Preconditions (PART 3) - Populate SUT with Test-Data and Prepare Expected Results                
    [Documentation]     Creates EHR records each containing 15 COMPOSITIONs
    
    Populate SUT with Test-Data and Prepare Expected Result    1    ${ehr data sets}/ehr_status_01.json
    Populate SUT with Test-Data and Prepare Expected Result    2    ${ehr data sets}/ehr_status_02.json
    # Populate SUT with Test-Data and Prepare Expected Result    3    ${ehr data sets}/ehr_status_03.json
    # Populate SUT with Test-Data and Prepare Expected Result    4    ${ehr data sets}/ehr_status_04.json
    # Populate SUT with Test-Data and Prepare Expected Result    5    ${ehr data sets}/ehr_status_05.json
    # Populate SUT with Test-Data and Prepare Expected Result    6    ${ehr data sets}/ehr_status_06.json
    # Populate SUT with Test-Data and Prepare Expected Result    7    ${ehr data sets}/ehr_status_07.json
    # Populate SUT with Test-Data and Prepare Expected Result    8    ${ehr data sets}/ehr_status_08.json
    # Populate SUT with Test-Data and Prepare Expected Result    9    ${ehr data sets}/ehr_status_09.json
    # Populate SUT with Test-Data and Prepare Expected Result   10    ${ehr data sets}/ehr_status_10.json


Populate SUT with Test-Data and Prepare Expected Result
    [Arguments]         ${ehr_index}    ${ehr_status_object}
    Log To Console  \nEHR RECORD ${ehr_index} ////////////////////////////////////

    prepare new request session    Prefer=return=representation

    Create EHR Record On The Server    ${ehr_index}    ${ehr_status_object}

    Commit Compo    1    ${ehr_index}    ${compo data sets}/minimal_admin_1.composition.json
    Commit Compo    2    ${ehr_index}    ${compo data sets}/minimal_admin_2.composition.json
    Commit Compo    3    ${ehr_index}    ${compo data sets}/minimal_admin_3.composition.json

    Commit Compo    4    ${ehr_index}    ${compo data sets}/minimal_evaluation_1.composition.json
    Commit Compo    5    ${ehr_index}    ${compo data sets}/minimal_evaluation_2.composition.json
    Commit Compo    6    ${ehr_index}    ${compo data sets}/minimal_evaluation_3.composition.json
    Commit Compo    7    ${ehr_index}    ${compo data sets}/minimal_evaluation_4.composition.json

    # # FAILS - TODO: report issue
    # Commit Compo    8    ${ehr_index}    ${compo data sets}/all_types.composition.json

    # # FAILS - GITHUB #61
    # Commit Compo    9    ${ehr_index}    ${compo data sets}/minimal_instruction_1.composition.json
    # Commit Compo   10    ${ehr_index}    ${compo data sets}/minimal_instruction_2.composition.json
    # Commit Compo   11    ${ehr_index}    ${compo data sets}/minimal_instruction_3.composition.json
    # Commit Compo   12    ${ehr_index}    ${compo data sets}/minimal_instruction_4.composition.json

    Commit Compo   13    ${ehr_index}    ${compo data sets}/minimal_observation_1.composition.json
    Commit Compo   14    ${ehr_index}    ${compo data sets}/minimal_observation_2.composition.json
    Commit Compo   15    ${ehr_index}    ${compo data sets}/minimal_observation_3.composition.json
    Commit Compo   16    ${ehr_index}    ${compo data sets}/minimal_observation_4.composition.json


Create EHR Record On The Server
    [Arguments]         ${index}    ${payload}

                        create new EHR with ehr_status  ${payload}
                        Integer    response status    201
                        extract ehr_id from response (JSON)

    ${ehr_id_value}=    String    response body ehr_id value
    ${time_created}     String    response body time_created
    ${system_id}        String    response body system_id value
    
    ##########################################################################################
    #                                                                                        #
    # FOR EACH EHR RECORD IN DB DATA IS EXTRACTED AND POPULATATED INTO EXPECTED RESULTS SETS #     
    #                                                                                        #
    ##########################################################################################

    ${A/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/100.tmp.json
    ${A/100}=           Add Object To Json    ${A/100}    $.rows    ${ehr_id_value}
                        Output    ${A/100}    ${QUERY RESULTS LOADED DB}/A/100.tmp.json

    ${A/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/101.tmp.json
    ${temp}=            Create List  ${ehr_id_value}[0]  ${time_created}[0]  ${system_id}[0]
    ${A/101}=           Add Object To Json    ${A/101}    $.rows    ${temp}
                        Output    ${A/101}    ${QUERY RESULTS LOADED DB}/A/101.tmp.json
    
    ${A/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/102.tmp.json
    ${A/102}=           Add Object To Json    ${A/102}    $.rows    ${ehr_id_value}
                        Output    ${A/102}    ${QUERY RESULTS LOADED DB}/A/102.tmp.json

    ${A/300}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/300.tmp.json
    ${A/300}=           Add Object To Json    ${A/300}    $.rows    ${ehr_id_value}
                        Output    ${A/300}    ${QUERY RESULTS LOADED DB}/A/300.tmp.json
    
    ${A/400}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/400.tmp.json
    ${A/400}=           Add Object To Json    ${A/400}    $.rows    ${ehr_id_value}
                        Output    ${A/400}    ${QUERY RESULTS LOADED DB}/A/400.tmp.json

    ${A/401}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/401.tmp.json
    ${A/401}=           Add Object To Json    ${A/401}    $.rows    ${ehr_id_value}
                        Output    ${A/401}    ${QUERY RESULTS LOADED DB}/A/401.tmp.json
    
    ${A/402}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/402.tmp.json
    ${A/402}=           Add Object To Json    ${A/402}    $.rows    ${ehr_id_value}
                        Output    ${A/402}    ${QUERY RESULTS LOADED DB}/A/402.tmp.json
    
    ${A/500}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/500.tmp.json
    ${A/500}=           Add Object To Json    ${A/500}    $.rows    ${ehr_id_value}
                        Output    ${A/500}    ${QUERY RESULTS LOADED DB}/A/500.tmp.json
    
    ${A/501}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/501.tmp.json
    ${A/501}=           Add Object To Json    ${A/501}    $.rows    ${ehr_id_value}
                        Output    ${A/501}    ${QUERY RESULTS LOADED DB}/A/501.tmp.json
    
    ${A/502}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/502.tmp.json
    ${A/502}=           Add Object To Json    ${A/502}    $.rows    ${ehr_id_value}
                        Output    ${A/502}    ${QUERY RESULTS LOADED DB}/A/502.tmp.json

    ${A/503}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/503.tmp.json
    ${A/503}=           Add Object To Json    ${A/503}    $.rows    ${ehr_id_value}
                        Output    ${A/503}    ${QUERY RESULTS LOADED DB}/A/503.tmp.json

    ${A/600}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/600.tmp.json
    ${A/600}=           Add Object To Json    ${A/600}    $.rows    ${ehr_id_value}
                        Output    ${A/600}    ${QUERY RESULTS LOADED DB}/A/600.tmp.json

    ${A/601}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/601.tmp.json
    ${A/601}=           Add Object To Json    ${A/601}    $.rows    ${ehr_id_value}
                        Output    ${A/601}    ${QUERY RESULTS LOADED DB}/A/601.tmp.json
    
    ${A/602}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/602.tmp.json
    ${A/602}=           Add Object To Json    ${A/602}    $.rows    ${ehr_id_value}
                        Output    ${A/602}    ${QUERY RESULTS LOADED DB}/A/602.tmp.json
    
    ${A/603}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/A/603.tmp.json
    ${A/603}=           Add Object To Json    ${A/603}    $.rows    ${ehr_id_value}
                        Output    ${A/603}    ${QUERY RESULTS LOADED DB}/A/603.tmp.json



Commit Compo
    [Arguments]         ${compo_index}    ${ehr_index}    ${compo_file}
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

        Log To Console  \nCOMPOSITION ${ehr_index} ////////////////////////////////////

    &{resp}=            REST.POST    ${baseurl}/ehr/${ehr_id}/composition    ${compo_file}
                        Output Debug Info To Console
                        Integer    response status    201
    &{body}=            Output     response body

                        Set Suite Variable    ${response}    ${resp}

                        Set Suite Variable    ${composition_uid}    ${response.body.uid.value}
                        Set Suite Variable    ${archtype_id}    ${response.body.archetype_details.archetype_id.value}
                        Set Suite Variable    ${content_type}    ${response.body.content[0]._type}
    
    ${archtype_id_of_content}=    Set Variable    ${response.body.content[0].archetype_node_id}

    ###########################################################################################
    #                                                                                         #
    # FOR EACH COMPOSITION IN DB DATA IS EXTRACTED AND POPULATATED INTO EXPECTED RESULTS SETS #     
    #                                                                                         #
    ###########################################################################################

    ${B/100}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/100.tmp.json
    ${B/100}=           Add Object To Json     ${B/100}    $.rows    ${response.body}
                        Output    ${B/100}     ${QUERY RESULTS LOADED DB}/B/100.tmp.json

    ${B/200}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/200.tmp.json
                        Set Suite Variable    ${B/200}    ${B/200}
                        Run Keyword If    ${ehr_index}==1    B/200
    
    ${B/300}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/300.tmp.json
                        Set Suite Variable    ${B/300}    ${B/300}
                        Run Keyword If    "${archtype_id}"=="openEHR-EHR-COMPOSITION.minimal.v1"    B/300
    
    ${B/500}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/500.tmp.json
                        Set Suite Variable    ${B/500}    ${B/500}
                        Run Keyword If    "${content_type}"=="OBSERVATION"    B/500
    
    ${B/501}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/501.tmp.json
                        Set Suite Variable    ${B/501}    ${B/501}
                        Run Keyword If    "${content_type}"=="EVALUATION"    B/501
    
    ${B/600}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/600.tmp.json
                        Set Suite Variable    ${B/600}    ${B/600}
                        Run Keyword If    "${archtype_id_of_content}"=="openEHR-EHR-OBSERVATION.minimal.v1"    B/600

    ${B/601}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/601.tmp.json
                        Set Suite Variable    ${B/601}    ${B/601}
                        Run Keyword If    "${archtype_id_of_content}"=="openEHR-EHR-EVALUATION.minimal.v1"    B/601
    
    ${B/700}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/700.tmp.json
                        Set Suite Variable    ${B/700}    ${B/700}
                        Run Keyword If    "${archtype_id_of_content}"=="openEHR-EHR-OBSERVATION.minimal.v1"    
                        ...                B/700 701 702    B/700
                        
    ${B/701}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/701.tmp.json
                        Set Suite Variable    ${B/701}    ${B/701}
                        Run Keyword If    "${archtype_id_of_content}"=="openEHR-EHR-OBSERVATION.minimal.v1"
                        ...                B/700 701 702    B/701

    ${B/702}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/702.tmp.json
                        Set Suite Variable    ${B/702}    ${B/702}
                        Run Keyword If    "${archtype_id_of_content}"=="openEHR-EHR-OBSERVATION.minimal.v1"
                        ...                B/700 701 702    B/702

    ${B/800}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/800.tmp.json
                        Set Suite Variable    ${B/800}    ${B/800}
                        Run Keyword If    ${compo_index}==1    B/800

    ${B/801}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/801.tmp.json
                        Set Suite Variable    ${B/801}    ${B/801}
                        Run Keyword If    ${compo_index}==1    B/801
    
    ${B/802}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/802.tmp.json
                        Set Suite Variable    ${B/802}    ${B/802}
                        Run Keyword If    ${compo_index}==1    B/802

    ${B/803}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/803.tmp.json
                        Set Suite Variable    ${B/803}    ${B/803}
                        Run Keyword If    ${compo_index}==1    B/803



    # BACKLOG / DATA GENERATION NOT READY/POSSIBLE OR NOT CLEAR HOW TO DO
    # ===================================================================
    
    # # NOT READY - not clear yet what is definition of TOP 5 COMPOSITIONS
    # ${B/101}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/101.tmp.json
    # ${B/101}=           Add Object To Json     ${B/101}    $.rows    ${response.body}
    #                     Output    ${B/101}     ${QUERY RESULTS LOADED DB}/B/101.tmp.json

    # # NOT READY - research how order Composisions by name
    # ${B/102}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/102.tmp.json
    # ${B/102}=           Add Object To Json     ${B/102}    $.rows    ${response.body}
    #                     Output    ${B/102}     ${QUERY RESULTS LOADED DB}/B/102.tmp.json
    
    # # NOT READY - not clear yet what is definition of "TIMEWINDOW"
    # ${B/103}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/103.tmp.json
    # ${B/103}=           Add Object To Json     ${B/103}    $.rows    ${response.body}
    #                     Output    ${B/103}     ${QUERY RESULTS LOADED DB}/B/103.tmp.json

    # # FAILS because requiered compo fail to commit (all_types composition)
    # ${B/400}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/400.tmp.json
    #                     Set Suite Variable    ${B/400}    ${B/400}
    #                     Run Keyword If    "${archtype_id}"=="openEHR-EHR-SECTION.test_all_types.v1"    B/400

    # # FAILS because requiered compos fail to commit (instruction compositions)             
    # ${B/502}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/502.tmp.json
    #                     Set Suite Variable    ${B/502}    ${B/502}
    #                     Run Keyword If    "${content_type}"=="INSTRUCTION"    B/502

    # # FAILS because we have no ACTION compositions yet                    
    # ${B/503}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/503.tmp.json
    #                     Set Suite Variable    ${B/503}    ${B/503}
    #                     Run Keyword If    "${content_type}"=="ACTION"    B/503            

    # # FAILS because requiered compos fail to commit (instruction compositions)              
    # ${B/602}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/602.tmp.json
    #                     Set Suite Variable    ${B/602}    ${B/602}
    #                     Run Keyword If    "${archtype_id_of_content}"=="openEHR-EHR-INSTRUCTION.minimal.v1"    B/602          

    # # FAILS because we have no ACTION compositions yet             
    # ${B/603}=           Load JSON From File    ${QUERY RESULTS LOADED DB}/B/603.tmp.json
    #                     Set Suite Variable    ${B/603}    ${B/603}
    #                     Run Keyword If    "${archtype_id_of_content}"=="openEHR-EHR-ACTION.minimal.v1"    B/603           



B/200
                        # updates the query
    ${B/200_query}=     Load JSON From File    ${VALID QUERY DATA SETS}/B/200_query.tmp.json
                        Update Value To Json   ${B/200_query}    $.q    SELECT c FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c
                        Output    ${B/200_query}    ${VALID QUERY DATA SETS}/B/200_query.tmp.json

                        # updates expected result set
    ${B/200}=           Update Value To Json   ${B/200}    $.q    SELECT c FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c
                        Update Value To Json   ${B/200}    $.meta._executed_aql    SELECT c FROM EHR e [ehr_id/value='${ehr_id}'] CONTAINS COMPOSITION c
                        Add Object To Json     ${B/200}    $.rows    ${response.body}
                        Output    ${B/200}     ${QUERY RESULTS LOADED DB}/B/200.tmp.json

B/300
    ${B/300}=           Add Object To Json     ${B/300}    $.rows    ${response.body}
                        Output    ${B/300}     ${QUERY RESULTS LOADED DB}/B/300.tmp.json

B/400
    ${B/400}=           Add Object To Json     ${B/400}    $.rows    ${response.body}
                        Output    ${B/400}     ${QUERY RESULTS LOADED DB}/B/400.tmp.json

B/500
    ${B/500}=           Add Object To Json     ${B/500}    $.rows    ${response.body}
                        Output    ${B/500}     ${QUERY RESULTS LOADED DB}/B/500.tmp.json

B/501
    ${B/501}=           Add Object To Json     ${B/501}    $.rows    ${response.body}
                        Output    ${B/501}     ${QUERY RESULTS LOADED DB}/B/501.tmp.json

B/502
    ${B/502}=           Add Object To Json     ${B/502}    $.rows    ${response.body}
                        Output    ${B/502}     ${QUERY RESULTS LOADED DB}/B/502.tmp.json

B/503
    ${B/503}=           Add Object To Json     ${B/503}    $.rows    ${response.body}
                        Output    ${B/503}     ${QUERY RESULTS LOADED DB}/B/503.tmp.json

B/600
    ${B/600}=           Add Object To Json     ${B/600}    $.rows    ${response.body}
                        Output    ${B/600}     ${QUERY RESULTS LOADED DB}/B/600.tmp.json

B/601
    ${B/601}=           Add Object To Json     ${B/601}    $.rows    ${response.body}
                        Output    ${B/601}     ${QUERY RESULTS LOADED DB}/B/601.tmp.json

B/602
    ${B/602}=           Add Object To Json     ${B/602}    $.rows    ${response.body}
                        Output    ${B/602}     ${QUERY RESULTS LOADED DB}/B/602.tmp.json

B/603
    ${B/603}=           Add Object To Json     ${B/603}    $.rows    ${response.body}
                        Output    ${B/603}     ${QUERY RESULTS LOADED DB}/B/603.tmp.json

B/700 701 702
    [Arguments]         ${dataset}
    ${items}            Set Variable    ${response.body.content[0].data.events[0].data["items"]}
    ${obs_value}        Set Variable    ${items[0].value.value}
                        Run Keyword If    "${obs_value}"=="first value"    Add Object To Json     ${${dataset}}    $.rows    ${response.body}
                        Run Keyword Unless    "${obs_value}"=="first value"    Return From Keyword
                        Output    ${${dataset}}    ${QUERY RESULTS LOADED DB}/${dataset}.tmp.json

B/800
                        # updates the query
    ${temp}=            Load JSON From File    ${VALID QUERY DATA SETS}/B/800_query.tmp.json
                        Update Value To Json   ${temp}    $.q    SELECT c FROM COMPOSITION c [uid/value='${composition_uid}']
                        Output    ${temp}    ${VALID QUERY DATA SETS}/B/800_query.tmp.json

                        # updates expected result set
    ${B/800}=           Update Value To Json   ${B/800}    $.q    SELECT c FROM COMPOSITION c [uid/value='${composition_uid}']
                        Update Value To Json   ${B/800}    $.meta._executed_aql    SELECT c FROM COMPOSITION c [uid/value='${composition_uid}']
                        Add Object To Json     ${B/800}    $.rows    ${response.body}
                        Output    ${B/800}     ${QUERY RESULTS LOADED DB}/B/800.tmp.json

B/801
                        # updates the query
    ${temp}=            Load JSON From File    ${VALID QUERY DATA SETS}/B/801_query.tmp.json
                        Update Value To Json   ${temp}    $.query_parameters['uid']    ${composition_uid}
                        Output    ${temp}    ${VALID QUERY DATA SETS}/B/801_query.tmp.json

                        # updates expected result set
    ${B/801}=           Update Value To Json   ${B/801}    $.q    SELECT c FROM COMPOSITION c [uid/value='${composition_uid}']
                        Update Value To Json   ${B/801}    $.meta._executed_aql    SELECT c FROM COMPOSITION c [uid/value='${composition_uid}']
                        Add Object To Json     ${B/801}    $.rows    ${response.body}
                        Output    ${B/801}     ${QUERY RESULTS LOADED DB}/B/801.tmp.json

B/802
                        # updates the query
    ${temp}=            Load JSON From File    ${VALID QUERY DATA SETS}/B/802_query.tmp.json
                        Update Value To Json   ${temp}    $.q    SELECT c FROM COMPOSITION c WHERE c/uid/value='${composition_uid}'
                        Output    ${temp}    ${VALID QUERY DATA SETS}/B/802_query.tmp.json

                        # updates expected result set
    ${B/802}=           Update Value To Json   ${B/802}    $.q    SELECT c FROM COMPOSITION c WHERE c/uid/value='${composition_uid}'
                        Update Value To Json   ${B/802}    $.meta._executed_aql    SELECT c FROM COMPOSITION c WHERE c/uid/value='${composition_uid}'
                        Add Object To Json     ${B/802}    $.rows    ${response.body}
                        Output    ${B/802}     ${QUERY RESULTS LOADED DB}/B/802.tmp.json

B/803
                        # updates the query
    ${temp}=            Load JSON From File    ${VALID QUERY DATA SETS}/B/803_query.tmp.json
                        Update Value To Json   ${temp}    $.query_parameters['uid']    ${composition_uid}
                        Output    ${temp}    ${VALID QUERY DATA SETS}/B/803_query.tmp.json

                        # updates expected result set
    ${B/803}=           Update Value To Json   ${B/803}    $.q    SELECT c FROM COMPOSITION c WHERE c/uid/value='${composition_uid}'
                        Update Value To Json   ${B/803}    $.meta._executed_aql    SELECT c FROM COMPOSITION c WHERE c/uid/value='${composition_uid}'
                        Add Object To Json     ${B/803}    $.rows    ${response.body}
                        Output    ${B/803}     ${QUERY RESULTS LOADED DB}/B/803.tmp.json














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
#     B/101_get_compositions_top_5.json
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
#     D/305_select_data_values_from_all_ehrs_contains_composition_with_archetype.json
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

#     [Teardown]      TRACE GITHUB ISSUE  GITHUB_ISSUE  not-ready  Some AQL QUERIES fail!
