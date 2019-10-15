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
${aql_queries}    ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/query/aql_queries/




*** Test Cases ***
Main flow: execute ad-hoc QUERY where data exists (ALTERNATIVE I)
    [Template]         execute ad-hoc query and check result

    get_EHR/00_all.json
    get_EHR/07_by_id.json

    get_COMPOSITION/00_from_all_ehrs.json
    get_COMPOSITION/01_from_ehr_by_id.json

    get_ENTRIE/00_from_ehr_with_uid_contains_compositions_from_all_ehrs.json
    get_ENTRIE/01_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json

    get_DATA/00_from_all_ehrs.json
    get_DATA/02_from_all_ehrs_contains_composition.json



Main flow: execute ad-hoc QUERY where data exists (ALTERNATIVE II)
    [Template]         execute ad-hoc query and check result

    A.1.a_get_ehrs.json
    A.2.a_get_ehr_by_id.json

    B.1.a_get_full_compositions_from_all_ehrs.json
    B.2.a_get_full_compositions_from_ehr_by_id.json

    C.1.a_get_all_entries_from_ehr_with_uid_contains_compositions_from_all_ehrs.json
    C.2.a_get_all_entries_from_ehr_with_uid_contains_compositions_with_archetype_from_all_ehrs.json

    D.1.a_select_data_values_from_all_ehrs.json
    D.2.a_select_data_values_from_all_ehrs_contains_composition.json
