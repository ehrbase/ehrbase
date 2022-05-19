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
Documentation       Examples generator for Templates
...                 Documentation: To be defined

Resource            ../_resources/keywords/composition_keywords.robot


*** Variables ***
${COMPOSITIONS_PATH_JSON}       ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_JSON
${COMPOSITIONS_PATH_XML}        ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_XML


*** Test Cases ***
Test Example Generator for Templates (ECIS) - FLAT
    [Setup]    Upload Template Using ECIS Endpoint
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    FLAT
    Validate Response Body Has Format    FLAT

Test Example Generator for Templates (ECIS) - JSON and Save it
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    JSON
    Validate Response Body Has Format    JSON
    Save Response (JSON) To File And Compare Template Ids    ${template_id}    composition_ecis_temp.json

Test Example Generator for Templates (ECIS) - XML and Save it
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    XML
    Validate Response Body Has Format    XML
    Save Response (XML) To File And Compare Template Ids    ${template_id}    composition_ecis_temp.xml

###########################################

Test Example Generator for Templates (OPENEHR) - FLAT
    [Setup]    Upload Template Using OPENEHR Endpoint
    Get Example Of Web Template By Template Id (OPENEHR)    ${template_id}    FLAT

Test Example Generator for Templates (OPENEHR) - JSON and Save it
    Get Example Of Web Template By Template Id (OPENEHR)    ${template_id}    JSON
    Validate Response Body Has Format    JSON
    Save Response (JSON) To File And Compare Template Ids    ${template_id}

Test Example Generator for Templates (OPENEHR) - XML and Save it
    Get Example Of Web Template By Template Id (OPENEHR)    ${template_id}    XML
    Validate Response Body Has Format    XML
    Save Response (XML) To File And Compare Template Ids    ${template_id}

Test Example Generator For Templates (ECIS) - Get Annotations from Example
    [Documentation]     Create template, get it and check if additional annotations are present.
    [Tags]      not-ready
    Upload OPT ECIS    all_types/tobacco_smoking_summary.v0.opt
    Extract Template Id From OPT File
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    JSON
    Validate Response Body Has Format    JSON
    #Should Contain
    #webTemplate.tree.children[?(@.id='tobacco_smoking_summary')].children[?(@.id='start_date')].annotations.comment
    #webTemplate.tree.children[?(@.id='tobacco_smoking_summary')].children[?(@.id='start_date')].annotations.helpText
    #webTemplate.tree.children[?(@.id='tobacco_smoking_summary')].children[?(@.id='start_date')].annotations.validation
    #Save Response (JSON) To File And Compare Template Ids    ${template_id}
    [Teardown]    TRACE JIRA ISSUE    CDR-410

*** Keywords ***
Upload Template Using ECIS Endpoint
    [Documentation]    Keyword used to upload Template using ECIS endpoint
    Upload OPT ECIS    all_types/ehrn_family_history.opt
    Extract Template Id From OPT File

Upload Template Using OPENEHR Endpoint
    [Documentation]    Keyword used to Upload Template Using OPENEHR Endpoint
    Upload OPT    nested/nested.opt
    Extract Template Id From OPT File

Save Response (JSON) To File And Compare Template Ids
    [Documentation]    1. Save response (JSON) to temp file,
    ...    2. compare template ids:
    ...    - the template example response
    ...    with
    ...    - the one from file
    ...    3. delete temp file.
    ...    *Dependency:* composition_keywords.get example of web template by template id (ECIS/OPENEHR)
    [Arguments]    ${templateId}    ${jsonFile}=composition_openehr_temp.json
    #${json_str}    Convert JSON To String    ${response.json()}
    ${json_str}    Convert JSON To String    ${response}
    ${tempFilePath}    Set Variable    ${COMPOSITIONS_PATH_JSON}/${jsonFile}
    Create File    ${tempFilePath}    ${json_str}
    ${composition_json}    Load JSON From File    ${tempFilePath}
    ${template_id_saved_file}    Get Value From Json
    ...    ${composition_json}    $.archetype_details.template_id.value
    ${template_id_saved_file_str}    Evaluate    "".join(${template_id_saved_file})
    Should Be Equal As Strings    ${template_id_saved_file_str}
    ...    ${templateId}
    [Teardown]    Run Keyword And Return Status    Remove File    ${tempFilePath}

Save Response (XML) To File And Compare Template Ids
    [Documentation]    1. Save response (XML) to temp file,
    ...    2. Get data from file and check if string contains template id
    ...    3. delete temp file.
    ...    *Dependency:* composition_keywords.validate that response body is in format
    [Arguments]    ${templateId}    ${xmlFile}=composition_openehr_temp.xml
    ${tempFilePath}    Set Variable    ${COMPOSITIONS_PATH_XML}/${xmlFile}
    Save Xml    ${responseXML}    ${tempFilePath}
    ${xml_parsed}    Parse Xml    ${tempFilePath}
    ${xml_string}    Get Element Text    ${xml_parsed}
    Should Contain    ${xml_string}    ${templateId}    String does not contain template id: ${templateId}
    [Teardown]    Run Keyword And Return Status    Remove File    ${tempFilePath}
