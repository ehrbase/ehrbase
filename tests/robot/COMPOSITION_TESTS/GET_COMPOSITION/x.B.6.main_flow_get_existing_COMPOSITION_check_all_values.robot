# Copyright (c) 2022 Vladislav Ploaia.
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
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION
Resource        ../../_resources/keywords/composition_keywords.robot


*** Test Cases ***
Get Existing COMPOSITION And Check All Values
    [Documentation]     Create, Get and validate that all values were returned,
    ...     for newly created composition, after Get operation.
    ...     This compares Get composition result with expected json.
    ...     Test case for bug: https://jira.vitagroup.ag/browse/CDR-373
    [Tags]  Positive
    Upload OPT    all_types/SSIAD PRIeSM.opt
    create EHR
    Set Test Variable    ${flat_composition_file_name}   SSIAD PRIeSM__.json
    commit composition   format=FLAT
    ...                  composition=${flat_composition_file_name}
    check the successful result of commit composition   nesting
    (FLAT) get composition by composition_uid    ${composition_uid}
    check composition exists
    Set Test Variable   ${template_id}      ssiad_priesm
    Compare content of compositions with the Original (FLAT)  ${COMPO DATA SETS}/FLAT/ssiad_priesm_expected_values.json