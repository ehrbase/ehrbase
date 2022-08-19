# Copyright (c) 2022 Vladislav Ploaia (Vitagroup - CDR Core Team)
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
...             ${\n}Based on:
...             https://github.com/ehrbase/ehrbase/blob/develop/doc/conformance_testing/COMPOSITION_VALIDATION_DATATYPES.md#631-test-ccase-dv_multimedia-open-constraint
...             ${\n}*6.3.1. Test ccase DV_MULTIMEDIA open constraint*
Metadata        TOP_TEST_SUITE    COMPOSITION

Resource        ../../_resources/keywords/composition_keywords.robot
Resource        ../../_resources/suite_settings.robot

Suite Setup         Precondition
Suite Teardown      Delete Template Using API


*** Variables ***
${composition_file}     Test_dv_multimedia_open_constraint.v0__.json
${optFile}              all_types/Test_dv_multimedia_open_constraint.v0.opt


*** Test Cases ***
Composition With DV_MULTIMEDIA.code_string NULL, DV_MULTIMEDIA.size NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_MULTIMEDIA open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_MULTIMEDIA.code_string using ${dvMultimediaCodeString} argument value NULL
    ...     - update DV_MULTIMEDIA.size using ${dvMultimediaSize} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_MULTIMEDIA Code String And Size
    ...     ${NULL}    ${NULL}      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_MULTIMEDIA.code_string application/dicom, DV_MULTIMEDIA.size NULL
    [Tags]      Negative
    [Documentation]     *Test case DV_MULTIMEDIA open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_MULTIMEDIA.code_string using ${dvMultimediaCodeString} argument value application/dicom
    ...     - update DV_MULTIMEDIA.size using ${dvMultimediaSize} argument value NULL
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_MULTIMEDIA Code String And Size
    ...     application/dicom    ${NULL}      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

Composition With DV_MULTIMEDIA.code_string NULL, DV_MULTIMEDIA.size 123
    [Tags]      Negative
    [Documentation]     *Test case DV_MULTIMEDIA open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_MULTIMEDIA.code_string using ${dvMultimediaCodeString} argument value NULL
    ...     - update DV_MULTIMEDIA.size using ${dvMultimediaSize} argument value 123
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 422.
    ${expectedStatusCode}   Set Variable    422
    ${statusCodeBoolean}    Commit Composition With Modified DV_MULTIMEDIA Code String And Size
    ...     ${NULL}    ${123}      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API


Composition With DV_MULTIMEDIA.code_string application/dicom, DV_MULTIMEDIA.size 123
    [Tags]      Positive
    [Documentation]     *Test case DV_MULTIMEDIA open constraint:*
    ...     - load json file from CANONICAL_JSON folder
    ...     - update DV_MULTIMEDIA.code_string using ${dvMultimediaCodeString} argument value application/dicom
    ...     - update DV_MULTIMEDIA.size using ${dvMultimediaSize} argument value 123
    ...     - commit composition
    ...     - check status code of the commited composition.
    ...     - Expected status code on commit composition = 201.
    ${expectedStatusCode}   Set Variable    201
    ${statusCodeBoolean}    Commit Composition With Modified DV_MULTIMEDIA Code String And Size
    ...     application/dicom    ${123}      ${expectedStatusCode}
    IF      ${statusCodeBoolean} == ${FALSE}
        Fail    Commit composition expected status code ${expectedStatusCode} is different.
    END
    [Teardown]  Delete Composition Using API

*** Keywords ***
Precondition
    Upload OPT    ${optFile}
    create EHR

Commit Composition With Modified DV_MULTIMEDIA Code String And Size
    [Arguments]     ${dvMultimediaCodeString}=audio/G726-24         ${dvMultimediaSize}=123       ${expectedCode}=201
    Load Json File With Composition
    ${initalJson}           Load Json From File     ${compositionFilePath}
    ${returnedJsonFile}     Change Json KeyValue and Save Back To File
    ...     ${initalJson}   ${dvMultimediaCodeString}      ${dvMultimediaSize}
    commit composition      format=CANONICAL_JSON
    ...                     composition=${composition_file}
    ${isStatusCodeEqual}    Run Keyword And Return Status
    ...     Should Be Equal As Strings      ${response.status_code}     ${expectedCode}
    ${isUidPresent}     Run Keyword And Return Status
    ...     Set Test Variable   ${version_uid}    ${response.json()['uid']['value']}
    IF      ${isUidPresent} == ${TRUE}
        ${short_uid}        Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                            Set Suite Variable   ${versioned_object_uid}    ${short_uid}
    ELSE
        Set Suite Variable   ${versioned_object_uid}    ${None}
    END
    [Return]    ${isStatusCodeEqual}

Change Json KeyValue and Save Back To File
    [Documentation]     Updates DV_MULTIMEDIA.code_string and DV_MULTIMEDIA.size values
    ...     in Composition json, using arguments values.
    ...     Takes 3 arguments:
    ...     1 - jsonContent = Loaded Json content
    ...     2 - value to be on DV_MULTIMEDIA.code_string
    ...     3 - value to be on DV_MULTIMEDIA.size
    [Arguments]     ${jsonContent}      ${dvMultimediaCodeStringToUpdate}      ${dvMultimediaSizeToUpdate}
    ${dvMultimediaCodeStringJsonPath1}     Set Variable
    ...     content[0].data.events[0].data.items[0].value.media_type.code_string
    ${dvMultimediaCodeStringJsonPath2}     Set Variable
    ...     content[0].data.events[1].data.items[0].value.media_type.code_string
    ${dvMultimediaCodeStringJsonPath3}     Set Variable
    ...     content[0].data.events[2].data.items[0].value.media_type.code_string
    ${dvMultimediaSizeJsonPath1}     Set Variable       content[0].data.events[0].data.items[0].value.size
    ${dvMultimediaSizeJsonPath2}     Set Variable       content[0].data.events[1].data.items[0].value.size
    ${dvMultimediaSizeJsonPath3}     Set Variable       content[0].data.events[2].data.items[0].value.size
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvMultimediaCodeStringJsonPath1}
    ...             new_value=${dvMultimediaCodeStringToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvMultimediaCodeStringJsonPath2}
    ...             new_value=${dvMultimediaCodeStringToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvMultimediaCodeStringJsonPath3}
    ...             new_value=${dvMultimediaCodeStringToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvMultimediaSizeJsonPath1}
    ...             new_value=${dvMultimediaSizeToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvMultimediaSizeJsonPath2}
    ...             new_value=${dvMultimediaSizeToUpdate}
    ${json_object}          Update Value To Json	json_object=${jsonContent}
    ...             json_path=${dvMultimediaSizeJsonPath3}
    ...             new_value=${dvMultimediaSizeToUpdate}
    ${changedDvMultimediaCodeString1}   Get Value From Json     ${jsonContent}      ${dvMultimediaCodeStringJsonPath1}
    ${changedDvMultimediaCodeString2}   Get Value From Json     ${jsonContent}      ${dvMultimediaCodeStringJsonPath2}
    ${changedDvMultimediaCodeString3}   Get Value From Json     ${jsonContent}      ${dvMultimediaCodeStringJsonPath3}
    ${changedDvMultimediaSize1}   Get Value From Json     ${jsonContent}      ${dvMultimediaSizeJsonPath1}
    ${changedDvMultimediaSize2}   Get Value From Json     ${jsonContent}      ${dvMultimediaSizeJsonPath2}
    ${changedDvMultimediaSize3}   Get Value From Json     ${jsonContent}      ${dvMultimediaSizeJsonPath3}
    ${json_str}     Convert JSON To String    ${json_object}
    Create File     ${compositionFilePath}    ${json_str}
    [return]    ${compositionFilePath}

Load Json File With Composition
    [Documentation]     Loads Json content from composition file.
    ...     Stores file content in test variable, as well as full file path.
    ${COMPO DATA SETS}     Set Variable
    ...     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}compositions
    ${file}                 Get File   ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    ${compositionFilePath}  Set Variable    ${COMPO DATA SETS}/CANONICAL_JSON/${composition_file}
    Set Test Variable       ${file}
    Set Test Variable       ${compositionFilePath}

Delete Template Using API
    &{resp}=            REST.DELETE   ${admin_baseurl}/template/${template_id}
                        Set Suite Variable    ${deleteTemplateResponse}    ${resp}
                        Output Debug Info To Console
                        ${isDeleteTemplateFailed}     Run Keyword And Return Status
                        ...     Should Be Equal As Strings      ${resp.status}      200
                        IF      ${isDeleteTemplateFailed} == ${FALSE} and '${resp.status}' == '404'
                            Log     Delete Template returned ${resp.status} code.   console=yes
                        END
                        #Delete All Sessions

Delete Composition Using API
    IF      '${versioned_object_uid}' != '${None}'
        &{resp}         REST.DELETE    ${admin_baseurl}/ehr/${ehr_id}/composition/${versioned_object_uid}
                        ${isDeleteCompositionFailed}     Run Keyword And Return Status
                        ...     Integer    response status    204
                        Set Suite Variable    ${deleteCompositionResponse}    ${resp}
                        IF      ${isDeleteCompositionFailed} == ${FALSE} and '${deleteCompositionResponse.status}' == '404'
                            Log     Delete Composition returned ${deleteCompositionResponse.status} code.   console=yes
                        END
                        Output Debug Info To Console
    END