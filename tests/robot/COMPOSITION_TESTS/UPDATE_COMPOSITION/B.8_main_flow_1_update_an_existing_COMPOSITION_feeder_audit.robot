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
# Author: Vladislav Ploaia



*** Settings ***
Documentation   Composition Integration Tests
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot

Force Tags



*** Test Cases ***
Update an existing event COMPOSITION Feeder Audit and Links values
    Upload OPT    all_types/genericFHIRTemplate.opt
    create EHR
    Get Web Template By Template Id (ECIS)     genericFHIRTemplate
    commit composition      format=CANONICAL_JSON
    ...                     composition=composition_feeder_audit.json
    ${template}     Set Variable    genericFHIRTemplate
    Set Test Variable   ${template}
    check the successful result of commit composition
    check content of updated composition generic (JSON)
    ...     ['feeder_audit']['original_content']['value']
    ...     Feeder Audit 1
    check content of updated composition generic (JSON)
    ...     ['links'][0]['meaning']['value']
    ...     initial val1
    check content of updated composition generic (JSON)
    ...     ['links'][0]['type']['value']
    ...     initial type1
    check content of updated composition generic (JSON)
    ...     ['links'][0]['target']['value']
    ...     ehr:/target1
    ##
    check content of updated composition generic (JSON)
    ...     ['links'][1]['meaning']['value']
    ...     initial val2
    check content of updated composition generic (JSON)
    ...     ['links'][1]['type']['value']
    ...     initial type2
    check content of updated composition generic (JSON)
    ...     ['links'][1]['target']['value']
    ...     ehr:/target2
    get composition by composition_uid      ${composition_uid}
    ${template_id}     Set Variable    genericFHIRTemplate
    Set Test Variable   ${template_id}
    update composition (JSON)   minimal_FEEDER_AUDIT.feeder_audit.original_content.value_links.json  file_type=json
    ##Check feeder_audit and links after composition update
    check content of updated composition generic (JSON)
    ...     ['feeder_audit']['original_content']['value']
    ...     Lorem Ipsum
    check content of updated composition generic (JSON)
    ...     ['links'][0]['meaning']['value']
    ...     modified val1
    check content of updated composition generic (JSON)
    ...     ['links'][0]['type']['value']
    ...     modified type1
    check content of updated composition generic (JSON)
    ...     ['links'][0]['target']['value']
    ...     ehr:/target1_a
    ##
    check content of updated composition generic (JSON)
    ...     ['links'][1]['meaning']['value']
    ...     modified val2
    check content of updated composition generic (JSON)
    ...     ['links'][1]['type']['value']
    ...     modified type2
    check content of updated composition generic (JSON)
    ...    ['links'][1]['target']['value']
    ...     ehr:/target2_b
    [Teardown]    restart SUT