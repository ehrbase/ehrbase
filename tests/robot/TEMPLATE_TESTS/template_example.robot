# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School),
# Nataliya Flusman (Solit Clouds), Nikita Danilin (Solit Clouds)
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
# Created date: 11 April 2022



*** Settings ***
Documentation   Examples generator for Templates
...             Documentation: To be defined

Resource        ../_resources/keywords/composition_keywords.robot


*** Variables ***
${COMPOSITIONS_PATH_JSON}   ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_JSON
${COMPOSITIONS_PATH_XML}    ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_XML

*** Test Cases ***
Test Example Generator for Templates (ECIS) - FLAT
    [Tags]
    [Setup]       Upload Template using ECIS endpoint
    get example of web template by template id (ECIS)      ${template_id}      FLAT
    validate that response body is in format    FLAT

Test Example Generator for Templates (ECIS) - JSON and Save it
    [Tags]
    get example of web template by template id (ECIS)      ${template_id}      JSON
    validate that response body is in format    JSON
    Save Response (JSON) To File And Compare Template Ids    ${template_id}     composition_ecis_temp.json

Test Example Generator for Templates (ECIS) - XML and Save it
    [Tags]
    get example of web template by template id (ECIS)      ${template_id}      XML
    validate that response body is in format    XML
    Save Response (XML) To File And Compare Template Ids    ${template_id}     composition_ecis_temp.xml

###########################################

Test Example Generator for Templates (OPENEHR) - FLAT
    [Tags]
    [Setup]     Upload Template using OPENEHR endpoint
    get example of web template by template id (OPENEHR)      ${template_id}      FLAT

Test Example Generator for Templates (OPENEHR) - JSON and Save it
    [Tags]
    get example of web template by template id (OPENEHR)      ${template_id}      JSON
    validate that response body is in format    JSON
    Save Response (JSON) To File And Compare Template Ids       ${template_id}

Test Example Generator for Templates (OPENEHR) - XML and Save it
    [Tags]
    get example of web template by template id (OPENEHR)      ${template_id}      XML
    validate that response body is in format    XML
    Save Response (XML) To File And Compare Template Ids    ${template_id}

*** Keywords ***
Upload Template using ECIS endpoint
    [Documentation]     Keyword used to upload Template using ECIS endpoint
    upload OPT ECIS     all_types/ehrn_family_history.opt
    Extract Template_id From OPT File

Upload Template using OPENEHR endpoint
    [Documentation]     Keyword used to upload Template using OPENEHR endpoint
    upload OPT          nested/nested.opt
    Extract Template_id From OPT File

Save Response (JSON) To File And Compare Template Ids
    [Documentation]     1. Save response (JSON) to temp file,
    ...                 2. compare template ids:
    ...                     - the template example response
    ...                     with
    ...                     - the one from file
    ...                 3. delete temp file.
    ...                 *Dependency:* composition_keywords.get example of web template by template id (ECIS/OPENEHR)
    [Arguments]     ${templateId}       ${jsonFile}=composition_openehr_temp.json
    ${json_str}         Convert JSON To String      ${response.json()}
    ${tempFilePath}     Set Variable    ${COMPOSITIONS_PATH_JSON}/${jsonFile}
    Create File         ${tempFilePath}     ${json_str}
    ${composition_json}     Load JSON From File    ${tempFilePath}
    ${template_id_saved_file}       Get Value From Json
    ...         ${composition_json}     $.archetype_details.template_id.value
    ${template_id_saved_file_str}       Evaluate        "".join(${template_id_saved_file})
    Should Be Equal As Strings      ${template_id_saved_file_str}
    ...                             ${templateId}
    [Teardown]      Run Keyword And Return Status       Remove File     ${tempFilePath}

Save Response (XML) To File And Compare Template Ids
    [Documentation]     1. Save response (XML) to temp file,
    ...                 2. Get data from file and check if string contains template id
    ...                 3. delete temp file.
    ...                 *Dependency:* composition_keywords.validate that response body is in format
    [Arguments]     ${templateId}       ${xmlFile}=composition_openehr_temp.xml
    ${tempFilePath}     Set Variable    ${COMPOSITIONS_PATH_XML}/${xmlFile}
    Save Xml            ${responseXML}     ${tempFilePath}
    ${xml_parsed}       Parse Xml       ${tempFilePath}
    ${xml_string}       Get Element Text     ${xml_parsed}
    Should Contain    ${xml_string}    ${templateId}    String does not contain template id: ${templateId}
    [Teardown]      Run Keyword And Return Status       Remove File     ${tempFilePath}