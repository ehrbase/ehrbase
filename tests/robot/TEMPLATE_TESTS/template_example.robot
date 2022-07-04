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
Test Example Generator For Templates (ECIS) - FLAT
    [Setup]    Upload Template Using ECIS Endpoint
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    FLAT
    Validate Response Body Has Format    FLAT

Test Example Generator For Templates (ECIS) - FLAT - Test Category And Coded Text Code And Value
    [Tags]      cdr-432
    Upload OPT ECIS    all_types/test_event.opt
    Extract Template Id From OPT File
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    FLAT
    Log     https://github.com/ehrbase/ehrbase/issues/897   console=yes
    Should Be Equal As Strings     ${response.json()["test_event/coded_text|code"]}   433
    Should Be Equal As Strings     ${response.json()["test_event/category|code"]}   433
    Should Be Equal As Strings     ${response.json()["test_event/coded_text|value"]}   event
    Should Be Equal As Strings     ${response.json()["test_event/category|value"]}   event
    [Teardown]      TRACE JIRA ISSUE      CDR-432

Test Example Generator For Templates (ECIS) - JSON And Commit Composition
    [Tags]      cdr-433
    Upload OPT ECIS    all_types/test_quantity_without_text.opt
    Extract Template Id From OPT File
    Get Example Of Web Template By Template Id (ECIS)       ${template_id}      JSON
    #Get Example Of Web Template By Template Id (OPENEHR)    ${template_id}    JSON
    ${json_str}    Convert JSON To String    ${response.json()}
    ${tempFilePath}    Set Variable
    ...     ${EXECDIR}/robot/_resources/test_data_sets/compositions/CANONICAL_JSON/test_quantity_without_text__.json
    Create File    ${tempFilePath}    ${json_str}
    create EHR
    Set Test Variable   ${externalTemplate}     ${template_id}
    ## Create action with payload from template example, stored in test_quantity_without_text__.json file
    commit composition  format=CANONICAL_JSON
    ...    composition=test_quantity_without_text__.json
    ...    extTemplateId=true
    ${compositionUid}   Set Variable    ${response.json()}[uid][value]
    ${template_id}      Set Variable    ${response.json()}[archetype_details][template_id][value]
    Set Test Variable     ${compositionUid}  ${composition_uid}
    #Log     ${response.json()}[content][0][data][events][0]
    Remove File    ${tempFilePath}
    ${eventsLength}     Get Length      ${response.json()}[content][0][data][events]
    Should Be True      ${eventsLength}==27     #27=3x9quantities
    #https://github.com/ehrbase/ehrbase/issues/900
    [Teardown]      TRACE JIRA ISSUE      CDR-433

Test Example Generator For Templates (ECIS) - JSON And Save It
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    JSON
    Validate Response Body Has Format    JSON
    Save Response (JSON) To File And Compare Template Ids    ${template_id}    composition_ecis_temp.json

Test Example Generator For Templates (ECIS) - XML And Save It
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    XML
    Validate Response Body Has Format    XML
    Save Response (XML) To File And Compare Template Ids    ${template_id}    composition_ecis_temp.xml

###########################################

Test Example Generator For Templates (OPENEHR) - FLAT
    [Setup]    Upload Template Using OPENEHR Endpoint
    Get Example Of Web Template By Template Id (OPENEHR)    ${template_id}    FLAT

Test Example Generator For Templates (OPENEHR) - JSON And Save It
    Get Example Of Web Template By Template Id (OPENEHR)    ${template_id}    JSON
    Validate Response Body Has Format    JSON
    Save Response (JSON) To File And Compare Template Ids    ${template_id}

Test Example Generator For Templates (OPENEHR) - XML And Save It
    Get Example Of Web Template By Template Id (OPENEHR)    ${template_id}    XML
    Validate Response Body Has Format    XML
    Save Response (XML) To File And Compare Template Ids    ${template_id}

Test Example Generator For Template (ECIS) - Specific Template
    [Documentation]     Create template, get example and check if template is returned.
    Upload OPT ECIS     all_types/tobacco_smoking_summary.v0.opt
    Extract Template Id From OPT File
    Get Example Of Web Template By Template Id (ECIS)    ${template_id}    JSON
    Validate Response Body Has Format    JSON
    #${returnValue0}  Get Value From Json     ${response}
    #...     $.content[0].data.items.[0][*].value
    #${returnValue1}  Get Value From Json     ${response}
    #...     $.content[0].data.items.[1][*].value
    #${returnValue2}  Get Value From Json     ${response}
    #...     $.content[0].data.items.[2][*].value

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

ApplyJSONLocatorAndReturnResult
    [Documentation]     Apply JSON path on result and return JSON Path evaluation
    [Arguments]     ${children_id_with_annotation}      ${annotation_key}=comment
    ${returnValue}  Get Value From Json     ${response}
    ...     $.webTemplate.tree.children[?(@.id='tobacco_smoking_summary')].children[?(@.id='${children_id_with_annotation}')].annotations.${annotation_key}
    ${lenReturnValue}   Get Length  ${returnValue}
    Should Be True      ${lenReturnValue} > 0
    [Return]    ${returnValue}

PerformChecksOnAnnotation
    [Documentation]     Store JSON Path evaluation to variables.
    ...     Dependency: keyword -> Get Web Template By Template Id (ECIS)
    ${startDateAnnotationComment}       ApplyJSONLocatorAndReturnResult         start_date
    ${quitDateAnnotationComment}        ApplyJSONLocatorAndReturnResult         quit_date
    ${overallUseAnnotationComment}      ApplyJSONLocatorAndReturnResult         overall_use
    ${packDefinitionAnnotationComment}      ApplyJSONLocatorAndReturnResult     pack_definition
    ${startDateAnnotationHelpText}          ApplyJSONLocatorAndReturnResult     start_date          helpText
    ${quitDateAnnotationHelpText}           ApplyJSONLocatorAndReturnResult     quit_date           helpText
    ${overallUseAnnotationHelpText}         ApplyJSONLocatorAndReturnResult     overall_use         helpText
    ${packDefinitionAnnotationHelpText}     ApplyJSONLocatorAndReturnResult     pack_definition     helpText
    ${startDateAnnotationValidation}        ApplyJSONLocatorAndReturnResult     start_date          validation
    ${quitDateAnnotationValidation}         ApplyJSONLocatorAndReturnResult     quit_date           validation
    ${overallUseAnnotationValidation}       ApplyJSONLocatorAndReturnResult     overall_use         validation
    ${packDefinitionAnnotationValidation}   ApplyJSONLocatorAndReturnResult     pack_definition     validation
    Log     ${startDateAnnotationComment}           console=yes
    Log     ${quitDateAnnotationComment}            console=yes
    Log     ${overallUseAnnotationComment}          console=yes
    Log     ${packDefinitionAnnotationComment}      console=yes
    Log     ${startDateAnnotationHelpText}          console=yes
    Log     ${quitDateAnnotationHelpText}           console=yes
    Log     ${overallUseAnnotationHelpText}         console=yes
    Log     ${packDefinitionAnnotationHelpText}     console=yes
    Log     ${startDateAnnotationValidation}        console=yes
    Log     ${quitDateAnnotationValidation}         console=yes
    Log     ${overallUseAnnotationValidation}       console=yes
    Log     ${packDefinitionAnnotationValidation}   console=yes