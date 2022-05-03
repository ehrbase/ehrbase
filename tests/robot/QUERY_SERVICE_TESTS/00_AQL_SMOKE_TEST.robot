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
Documentation   AQL QUERY SMOKE TEST

Resource       ../_resources/keywords/aql_query_keywords.robot

# Suite Setup    aql_query_keywords.Establish Preconditions
# Test Setup  Establish Preconditions
# Test Teardown  restore clean SUT state
# Suite Teardown    Run Keywords    Clean DB    # Delete Temp Result-Data-Sets
# Suite Teardown    dump db

Force Tags    AQL_smoke_loaded_db    AQL_temp



*** Variables ***
${ehr data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/data_load/ehrs/
${compo data sets}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/data_load/compositions/



*** Test Cases ***
AQL LOADED DB SMOKE TEST - Preconditions
    [Tags]    AQL_smoke

    Establish Preconditions

    [Teardown]    Set Smoke Test Status


AQL LOADED DB SMOKE TEST - Queries
    [Tags]          AQL_smoke

    # COMMENT: QUERY EXECUTION
    #////////////////////////////////////////////////////////////////
    #//                                                           ///
    #//  UNCOMMENT ONE LINE AT A TIME TO TEST RELATED QUERY ONLY  ///
    #//                                                           ///
    #////////////////////////////////////////////////////////////////

    execute ad-hoc query and check result (loaded DB)  A/100_get_ehrs.json    A/100.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/101_get_ehrs.json    A/101.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/102_get_ehrs.json    A/102.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/103_get_ehrs.json    A/103.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/104_get_ehrs.json    A/104.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/105_get_ehrs.json    A/105.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/106_get_ehrs.json    A/106.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/107_get_ehrs_top_5.json    A/107.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/108_get_ehrs_orderby_time-created.json    A/108.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/109_get_ehrs_within_timewindow.json       A/109.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/200_query.tmp.json    A/200.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/201_query.tmp.json    A/201.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/202_query.tmp.json    A/202.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/203_query.tmp.json    A/203.tmp.json
    execute ad-hoc query and check result (loaded DB)  A/300_get_ehrs_by_contains_any_composition.json               A/300.tmp.json
    execute ad-hoc query and check result (loaded DB)  A/400_get_ehrs_by_contains_composition_with_archetype.json    A/400.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/401_get_ehrs_by_contains_composition_with_archetype.json    A/401.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/402_get_ehrs_by_contains_composition_with_archetype.json    A/402.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/500.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/501.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/502.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/503.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/600.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/601.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/602.tmp.json
    # execute ad-hoc query and check result (loaded DB)  A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/603.tmp.json

    # execute ad-hoc query and check result (loaded DB)  B/100_get_compositions_from_all_ehrs.json    B/100.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/102_get_compositions_orderby_name.json    B/102.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/103_get_compositions_within_timewindow.json    B/103.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/200_get_compositions_from_ehr_by_id.json    B/200.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/300_get_compositions_with_archetype_from_all_ehrs.json    B/300.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json    B/400.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/500.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/501.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/502.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/503.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/600.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/601.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/602.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/603.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/700.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/701.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/702.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/802_query.tmp.json    B/802.tmp.json
    # execute ad-hoc query and check result (loaded DB)  B/803_query.tmp.json    B/803.tmp.json

    # execute ad-hoc query and check result (loaded DB)  D/200_select_data_values_from_all_ehrs_contains_composition.json    D/200.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/201_select_data_values_from_all_ehrs_contains_composition.json    D/201.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/300_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/300.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/301_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/301.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/302_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/302.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/303_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/303.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/304_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/304.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/306.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/307.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/308_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/308.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/309_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/309.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/310_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/310.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/312_select_data_values_from_all_ehrs_contains_composition_with_archetype_top_5.json    D/312.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/400_query.tmp.json    D/400.tmp.json   
    # execute ad-hoc query and check result (loaded DB)  D/401_query.tmp.json    D/401.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/402_query.tmp.json    D/402.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/403_query.tmp.json    D/403.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/404_query.tmp.json    D/404.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/405_query.tmp.json    D/405.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/500_query.tmp.json    D/500.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/501_query.tmp.json    D/501.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/502_query.tmp.json    D/502.tmp.json
    # execute ad-hoc query and check result (loaded DB)  D/503_query.tmp.json    D/503.tmp.json

    ## FUTURE FEATURE: DON'T USE YET!
        # execute ad-hoc query and check result (loaded DB)  B/800_query.tmp.json    B/800.tmp.json    # GITHUB ISSUE #109
        # execute ad-hoc query and check result (loaded DB)  B/801_query.tmp.json    B/801.tmp.json    # GITHUB ISSUE #109
        # execute ad-hoc query and check result (loaded DB)  C/100_query.tmp.json    C/100.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/101_query.tmp.json    C/101.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/102_query.tmp.json    C/102.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/103_query.tmp.json    C/103.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/200_query.tmp.json    C/200.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/300_query.tmp.json    C/300.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/301_query.tmp.json    C/301.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/302_query.tmp.json    C/302.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/303_query.tmp.json    C/303.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/400_query.tmp.json    C/400.tmp.json
        # execute ad-hoc query and check result (loaded DB)  C/500_query.tmp.json    C/500.tmp.json
        # execute ad-hoc query and check result (loaded DB)  D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/311.tmp.json







#////////////////////////////////////////////////////////////////////////
#///                                                                  ///
#///         TEMP TCs for issue tracking and debugging only!          ///
#///       (they are duplicates of TCs in original test suite)        ///
#///      TODO: @WLAD REMOVE THIS TCs FROM HERE WHEN THEY PASS!       ///
#///                                                                  ///
#////////////////////////////////////////////////////////////////////////

B-100 Execute Ad-Hoc Query - Get Compositions From All EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              not-ready    not-ready_test-issue
    B/100_get_compositions_from_all_ehrs.json    B/100.tmp.json


B-200 Execute Ad-Hoc Query - Get Compositions From All EHRs
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              357  358  359
    B/200_query.tmp.json    B/200.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  357  bug


B-400 Execute Ad-Hoc Query - Get Composition(s)
    [Documentation]     Test w/ "all_types.composition.json" commit
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              586    not-ready
    B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json    B/400.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  586  bug


B-800 Execute Ad-Hoc Query - Get Compositions By UID
    [Documentation]     B/800: SELECT c FROM COMPOSITION c [uid/value='123::node.name.com::1']
    ...                 B/801: SELECT c FROM COMPOSITION c [uid/value=$uid]
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              109    future
    B/800_query.tmp.json    B/800.tmp.json
    B/801_query.tmp.json    B/801.tmp.json
    [Teardown]          TRACE GITHUB ISSUE  109  bug  still blocked by


D-312 Execute Ad-HOc Query - Get Data
    [Documentation]     Get Data related query.
    [Template]          execute ad-hoc query and check result (loaded DB)
    [Tags]              TODO  # @WLAD implement a flow w/ TOP5 + ORDERED BY time_created
    D/312_select_data_values_from_all_ehrs_contains_composition_with_archetype_top_5.json    D/312.tmp.json


CLEAN UP SUT
    [Documentation]     ATTENTION: ALWAYS INCLUDE '-i AQL_smoke' and '-i AQL_tempANDissue_id'
    ...                 when you run test from this suite!!!
    [Tags]              AQL_smoke
    # Pass Execution  DONT DUMP THIS TIME
    dump db
    # db_keywords.Delete All templates
    # db_keywords.Delete All EHR Records







*** Keywords ***
# oooo    oooo oooooooooooo oooooo   oooo oooooo   oooooo     oooo   .oooooo.   ooooooooo.   oooooooooo.    .oooooo..o
# `888   .8P'  `888'     `8  `888.   .8'   `888.    `888.     .8'   d8P'  `Y8b  `888   `Y88. `888'   `Y8b  d8P'    `Y8
#  888  d8'     888           `888. .8'     `888.   .8888.   .8'   888      888  888   .d88'  888      888 Y88bo.
#  88888[       888oooo8       `888.8'       `888  .8'`888. .8'    888      888  888ooo88P'   888      888  `"Y8888o.
#  888`88b.     888    "        `888'         `888.8'  `888.8'     888      888  888`88b.     888      888      `"Y88b
#  888  `88b.   888       o      888           `888'    `888'      `88b    d88'  888  `88b.   888     d88' oo     .d8P
# o888o  o888o o888ooooood8     o888o           `8'      `8'        `Y8bood8P'  o888o  o888o o888bood8P'   8""88888P'
#
# [ THIS KWs OVERIDE EXISTING KWs IN RESOURCE FILE TO WORK PROPERLY IN SMOKE TEST ]


Establish Preconditions
    # comment: WHEN TEST-DATA HAS NOT CHANGED RESTORE DB FROM DUMP AND SKIP REST OF THIS KW!
    ${data-changed}     Run Keyword And Return Status    File Should Exist    /tmp/DATA_CHANGED_NOTICE
                        Run Keyword And Return If    not ${data-changed}    db_keywords.restore db from dump
   
    # comment: WHEN DATA_CHANGED ENV EXIST DO THIS!
    Preconditions (PART 1) - Load Blueprints of Queries and Expected-Results
    Preconditions (PART 2) - Generate Test-Data and Expected-Results (MINIMAL SET)


Preconditions (PART 2) - Generate Test-Data and Expected-Results (MINIMAL SET)
    [Documentation]     Generates Test-Data and Expected-Results   
    ...                 NOTE: This is a short version of A.1.a__Loaded_DB.robot TC.
    ...                 Uploads required test data to server/db and generates expected results
    ...                 during that process. This involves
    ...                 - creating ONE EHR record with ehr_status
    ...                 - committing one Composition of EACH TYPE: admin, evaluation,
    ...                   instruction, observation, action
    ...                 - injecting live data into 'expected result blueprint'
    ...                 - injecting live data into some 'query blueprints'

    # comment: Populate SUT with Test-Data and Prepare Expected Results
    Upload OPT    minimal/minimal_admin.opt
    Upload OPT    minimal/minimal_observation.opt
    Upload OPT    minimal/minimal_instruction.opt
    Upload OPT    minimal/minimal_evaluation.opt
    Upload OPT    minimal/minimal_action.opt
    Upload OPT    minimal/minimal_action_2.opt
    ### REL TO https://github.com/ehrbase/ehrbase/issues/643
    ###upload OPT    all_types/Test_all_types.opt
    Upload OPT    all_types/Test_all_types_v2.opt

    Create EHR Record On The Server    1    ${ehr data sets}/ehr_status_01.json
    Commit Compo     1    1    ${compo data sets}/minimal_admin_1.composition.json
    Commit Compo     2    1    ${compo data sets}/minimal_evaluation_1.composition.json
    Commit Compo     3    1    ${compo data sets}/minimal_instruction_1.composition.json
    Commit Compo     4    1    ${compo data sets}/minimal_observation_1.composition.json
    Commit Compo     5    1    ${compo data sets}/minimal_action2_1.composition.json
    ### REL TO https://github.com/ehrbase/ehrbase/issues/643
    ###Commit Compo     6    1    ${compo data sets}/all_types.composition.json
    Commit Compo     6    1    ${compo data sets}/all_types_v2.composition.json

    Create EHR Record On The Server    2    ${ehr data sets}/ehr_status_01.json
    Commit Compo     1    2    ${compo data sets}/minimal_admin_1.composition.json
    Commit Compo     2    2    ${compo data sets}/minimal_evaluation_1.composition.json
    Commit Compo     3    2    ${compo data sets}/minimal_instruction_1.composition.json
    Commit Compo     4    2    ${compo data sets}/minimal_observation_1.composition.json
    Commit Compo     5    2    ${compo data sets}/minimal_action2_1.composition.json
    Commit Compo     6    2    ${compo data sets}/all_types.composition.json

    #///////////////////////////////////////////////////
    #//                                              ///
    #//  UNCOMMENT MORE COMPOS TO EXTEND SMOKE TEST  ///
    #//                                              ///
    #///////////////////////////////////////////////////

    # Commit Compo    7    1    ${compo data sets}/minimal_admin_2.composition.json
    # Commit Compo    8    1    ${compo data sets}/minimal_admin_3.composition.json

    # Commit Compo    9    1    ${compo data sets}/minimal_evaluation_2.composition.json
    # Commit Compo   10    1    ${compo data sets}/minimal_evaluation_3.composition.json
    # Commit Compo   11    1    ${compo data sets}/minimal_evaluation_4.composition.json

    # Commit Compo   12    1    ${compo data sets}/minimal_instruction_2.composition.json
    # Commit Compo   13    1    ${compo data sets}/minimal_instruction_3.composition.json
    # Commit Compo   14    1    ${compo data sets}/minimal_instruction_4.composition.json

    # Commit Compo   15    1    ${compo data sets}/minimal_observation_2.composition.json
    # Commit Compo   16    1    ${compo data sets}/minimal_observation_3.composition.json
    # Commit Compo   17    1    ${compo data sets}/minimal_observation_4.composition.json

    # Commit Compo   18    1    ${compo data sets}/minimal_action2_2.composition.json
    # Commit Compo   19    1    ${compo data sets}/minimal_action2_3.composition.json



    # TODO: @WLAD MAKE SMOKE SUITE WORKS PROPERLY WITH THIS PART MOVE TO TCs, then DELETE THIS!!
    # # COMMENT: QUERY EXECUTION
    # #////////////////////////////////////////////////////////////////
    # #//                                                           ///
    # #//  UNCOMMENT ONE LINE AT A TIME TO TEST RELATED QUERY ONLY  ///
    # #//                                                           ///
    # #////////////////////////////////////////////////////////////////

    # execute ad-hoc query and check result (loaded DB)  A/100_get_ehrs.json    A/100.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/101_get_ehrs.json    A/101.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/102_get_ehrs.json    A/102.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/103_get_ehrs.json    A/103.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/104_get_ehrs.json    A/104.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/105_get_ehrs.json    A/105.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/106_get_ehrs.json    A/106.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/107_get_ehrs_top_5.json    A/107.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/108_get_ehrs_orderby_time-created.json    A/108.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/109_get_ehrs_within_timewindow.json       A/109.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/200_query.tmp.json    A/200.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/201_query.tmp.json    A/201.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/202_query.tmp.json    A/202.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/203_query.tmp.json    A/203.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/300_get_ehrs_by_contains_any_composition.json               A/300.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/400_get_ehrs_by_contains_composition_with_archetype.json    A/400.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/401_get_ehrs_by_contains_composition_with_archetype.json    A/401.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/402_get_ehrs_by_contains_composition_with_archetype.json    A/402.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/500_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/500.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/501_get_ehrs_by_contains_composition_contains_entry_of_type.json      A/501.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/502_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/502.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/503_get_ehrs_by_contains_composition_contains_entry_of_type.json    A/503.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/600_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/600.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/601_get_ehrs_by_contains_composition_contains_entry_with_archetype.json    A/601.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/602_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/602.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  A/603_get_ehrs_by_contains_composition_contains_entry_with_archetype.json  A/603.tmp.json

    # # execute ad-hoc query and check result (loaded DB)  B/100_get_compositions_from_all_ehrs.json    B/100.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/102_get_compositions_orderby_name.json    B/102.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/103_get_compositions_within_timewindow.json    B/103.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/200_get_compositions_from_ehr_by_id.json    B/200.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/300_get_compositions_with_archetype_from_all_ehrs.json    B/300.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/400_get_compositions_contains_section_with_archetype_from_all_ehrs.json    B/400.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/500_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/500.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/501_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/501.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/502_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/502.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/503_get_compositions_by_contains_entry_of_type_from_all_ehrs.json    B/503.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/600_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/600.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/601_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/601.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/602_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/602.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/603_get_compositions_by_contains_entry_with_archetype_from_all_ehrs.json    B/603.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/700_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/700.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/701_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/701.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/702_get_compositions_by_contains_entry_with_archetype_and_condition_from_all_ehrs.json    B/702.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/802_query.tmp.json    B/802.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  B/803_query.tmp.json    B/803.tmp.json

    # # execute ad-hoc query and check result (loaded DB)  D/200_select_data_values_from_all_ehrs_contains_composition.json    D/200.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/201_select_data_values_from_all_ehrs_contains_composition.json    D/201.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/300_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/300.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/301_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/301.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/302_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/302.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/303_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/303.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/304_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/304.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/306_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/306.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/307_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/307.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/308_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/308.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/309_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/309.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/310_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/310.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/312_select_data_values_from_all_ehrs_contains_composition_with_archetype_top_5.json    D/312.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/400_query.tmp.json    D/400.tmp.json   
    # # execute ad-hoc query and check result (loaded DB)  D/401_query.tmp.json    D/401.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/402_query.tmp.json    D/402.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/403_query.tmp.json    D/403.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/404_query.tmp.json    D/404.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/405_query.tmp.json    D/405.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/500_query.tmp.json    D/500.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/501_query.tmp.json    D/501.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/502_query.tmp.json    D/502.tmp.json
    # # execute ad-hoc query and check result (loaded DB)  D/503_query.tmp.json    D/503.tmp.json

    # ## FUTURE FEATURE: DON'T USE YET!
    #     # execute ad-hoc query and check result (loaded DB)  B/800_query.tmp.json    B/800.tmp.json    # GITHUB ISSUE #109
    #     # execute ad-hoc query and check result (loaded DB)  B/801_query.tmp.json    B/801.tmp.json    # GITHUB ISSUE #109
    #     # execute ad-hoc query and check result (loaded DB)  C/100_query.tmp.json    C/100.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/101_query.tmp.json    C/101.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/102_query.tmp.json    C/102.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/103_query.tmp.json    C/103.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/200_query.tmp.json    C/200.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/300_query.tmp.json    C/300.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/301_query.tmp.json    C/301.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/302_query.tmp.json    C/302.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/303_query.tmp.json    C/303.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/400_query.tmp.json    C/400.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  C/500_query.tmp.json    C/500.tmp.json
    #     # execute ad-hoc query and check result (loaded DB)  D/311_select_data_values_from_all_ehrs_contains_composition_with_archetype.json    D/311.tmp.json

