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
Documentation    OPT1.4 Specific Keywords
Resource         ../suite_settings.robot

# Resource    generic_keywords.robot



*** Variables ***
${VALID DATA SETS}     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}valid_templates
${INVALID DATA SETS}   ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}invalid_templates



*** Keywords ***
get valid OPT file
    [Arguments]         ${opt file}
    [Documentation]     Gets an OPT file from test_data_sets/valid_templates folder

    ${file}=            Get File             ${VALID DATA SETS}/${opt file}
    ${xml}=             Parse Xml            ${file}
                        Set Suite Variable    ${file}    ${file}
                        Set Suite Variable    ${expected}    ${xml}
                        Log Element          ${expected}


get invalid OPT file
    [Arguments]         ${opt file}
    [Documentation]     Gets an OPT file from test_data_sets/invalid_templates folder

    ${file}=            Get File             ${INVALID DATA SETS}/${opt file}

                        # handle empty file and empty XML
                        Run Keyword And Return If    """${file}"""=='${EMPTY}'
                        ...                          Set Suite Variable  ${file}  ${file}

                        Run Keyword And Return If    """${file}"""=="""<?xml version="1.0" encoding="utf-8"?>\n"""
                        ...                          Set Suite Variable  ${file}  ${file}

    ${xml}=             Parse Xml            ${file}
                        Set Suite Variable    ${file}    ${file}
                        Set Suite Variable    ${expected}    ${xml}
                        Log Element          ${expected}


Extract Template Id From OPT File
    [Documentation]     Extracts template_id from OPT (XML) file which was obtained
    ...                 with `get valid/invalid OPT file` keywords

    ${template_id}=     Get Element Text     ${expected}   xpath=template_id
    ...                 normalize_whitespace=True
                        Set Suite Variable    ${template_id}    ${template_id}
                        # Log To Console      ${template_id}


upload valid OPT
    [Arguments]           ${opt file}

    prepare new request session    XML
    get valid OPT file    ${opt file}
    upload OPT file
    server accepted OPT
    [Teardown]            Clean Up Suite Variables


upload invalid OPT
    [Arguments]           ${opt file}

    prepare new request session    XML
    ...                            Prefer=return=representation
    get invalid OPT file  ${opt file}
    upload OPT file
    server rejected OPT with status code 400
    # server response contains proper error message    # TODO: reactivate asap


upload OPT file
    [Documentation]     Uploads OPT file which was obtained with one of the Keywords
    ...                 `get valid OPT file` or `get invalid OPT file`
    ${resp}=            POST On Session      ${SUT}    /definition/template/adl1.4   expected_status=anything
                        ...                  data=${file}    headers=${headers}
                        Set Suite Variable    ${response}    ${resp}
                        # Log To Console      ${resp.content}

upload OPT file ECIS
    [Documentation]     Uploads OPT file which was obtained with one of the Keywords
    ...                 `get valid OPT file` or `get invalid OPT file`

    ${resp}=            POST On Session      ${SUT}    ${ECISURL}/template   expected_status=anything
                        ...                  data=${file}    headers=${headers}
                        Set Suite Variable    ${response}    ${resp}
                        # Log To Console      ${resp.content}

upload OPT file with version parameter
    # to be implemented
    Fail    msg=Break it till you make it!


upload same OPT with version parameter
    # to be implemented
    Fail    msg=Break it till you make it!


retrieve versioned OPT
    [Arguments]                         ${opt file}

    Log               NOT APPLICABLE FOR ADL 1.4    level=WARN
    Pass Execution    NOT APPLICABLE FOR ADL 1.4    not-ready

    prepare new request session    XML
    get valid OPT file                  ${opt file}
    Extract Template Id From OPT File
    retrieve OPT by template_id         ${template_id}
    verify server response
    [Teardown]                          Clean Up Suite Variables


verify server response
    # to be implemented
    Fail    msg=Brake it till you make it!


server accepted OPT
                        @{expectedStatusCodesList}      Create List     200     201
                        ${string_status_code}    Convert To String    ${response.status_code}
                        List Should Contain Value   ${expectedStatusCodesList}      ${string_status_code}


server rejected OPT with status code ${status code}
    [Documentation]     400: Bad Request - is returned when unable to upload a template,
    ...                      because of invalid content.
    ...                 409: Conflict - is returned when a template with given id
    ...                      already exists. This response is optional.

                        Should Be Equal As Strings    ${response.status_code}
                        ...                           ${status code}


server response contains proper error message
                        Should Contain Any  ${response.text}
                        ...                   Invalid template input content
                        ...                   Required request body is missing
                        ...                   Unexpected end of file after null
                        ...                   Supplied template has nil or empty template id value
                        ...                   Supplied template has nil or empty concept
                        ...                   Supplied template has nil or empty definition
                        ...                   Supplied template has nil or empty description
                        ...                   baz    # TODO: @WLAD add more

server returned specified version of OPT
    Fail    msg=Brake it till you make it!


retrieve OPT by template_id
    [Arguments]         ${template_id}
    [Documentation]     Gets OPT from server with provided template_id

    ${resp}=            GET On Session          ${SUT}    /definition/template/adl1.4/${template_id}   expected_status=anything
                        ...                  headers=${headers}
                        Log    ${resp.text}
                        # Log    ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   200
    ${xml}=             Parse Xml            ${resp.text}
                        Set Suite Variable    ${actual}    ${xml}
    [Return]    ${xml}


verify content of OPT
                        Log Element    ${actual}
                        Log Element    ${expected}

                        Elements Should Be Equal    ${actual}    ${expected}
                        ...                      normalize_whitespace=True


generate random templade_id
    [Documentation]     Generates a random template_id variable and puts it
    ...                 into Test Case Scope.

    ${template_id}=     Generate Random String    16    [NUMBERS]abcdef
                        Set Suite Variable    ${template_id}    ${template_id}


Clean Up Test Variables
    [Documentation]     Cleans up test variables to avoid impacts between tests.

                        Set Test Variable    ${file}           None
                        Set Test Variable    ${expected}       None
                        Set Test Variable    ${template_id}    None
    &{vars in memory}=  Get Variables
                        Log Many             &{vars in memory}


Clean Up Suite Variables
    [Documentation]     Cleans up test variables to avoid impacts between tests.

                        Set Suite Variable    ${file}           None
                        Set Suite Variable    ${expected}       None
                        Set Suite Variable    ${template_id}    None
    &{vars in memory}=  Get Variables
                        Log Many             &{vars in memory}


retrieve OPT list
    [Documentation]     List all available operational templates on the system.

    &{headers}          Set Headers    {"Content-Type": "application/json"}
    &{headers}          Set Headers    {"Accept": "application/json"}
    &{headers}          Set Headers    ${authorization}

    &{resp}=            REST.GET    ${baseurl}/definition/template/adl1.4
    ...                 headers=${headers}
                        # Output   response body
                        # Log To Console    ${resp}


verify OPT list has ${X} items
    Run Keyword And Return If  ${X}==0  OPT list is empty
    Array    response body    minItems=${X}    maxItems=${X}


OPT list is empty
    Array     response body   []


upload valid template (XML)
    [Tags]
    [Arguments]    ${template}
    [Documentation]    ${VALID DATA SETS} is path to data-sets

    Create Session  ethlocal  ${baseurl}
    ${file}=  Get File  ${VALID DATA SETS}${template}
    &{headers}=  Create Dictionary  Content-Type=application/xml
    ${resp}=  POST On Session  ethlocal  /template  data=${file}  headers=${headers}   expected_status=anything
    Log  ${resp.json()}
    Run Keyword If   ${resp.status_code} != 201  Log  Wrong Status Code  WARN
    Run Keyword If   ${resp.status_code} != 201  Set Tags  not-ready
    Should Be Equal As Strings   ${resp.status_code}   200
    [Teardown]







# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]

# server response contains proper error message
#     ${resp_bstring}     Set Variable    ${response.content}
#     ${resp_string}      Convert To String    ${resp_bstring}
#                         Should Contain Any    ${resp_string}
#                         ...                   foo
#                         ...                   bar

# start request session
#     Create Session      ${SUT}    ${${SUT}.URL}
#     ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True
#     &{headers}=         Create Dictionary    Content-Type=application/xml
#                         ...                  Prefer=return=representation
#                         Set Suite Variable    ${headers}    ${headers}


# start request session (XML)
#     Create Session      ${SUT}    ${${SUT}.URL}
#     ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True
#     &{headers}=         Create Dictionary    Content-Type=application/xml
#                         ...                  Prefer=return=representation
#                         ...                  Accept=application/xml
#                         Set Suite Variable    ${headers}    ${headers}

# retrieve list of uploaded OPTs (request lib example)
#     [Documentation]    List all available operational templates on the system.
#
#     ${resp}=           GET On Session          ${SUT}    /definition/template/adl1.4
#                        ...                  headers=${headers}
#                        Should Be Equal As Strings   ${resp.status_code}   200
#                        Log    ${resp.content}
#                        Log    ${resp.text}

# Clean Up Template Folder
#     Empty Directory    ${EXECDIR}/../../../../file_repo/knowledge/operational_templates
#     # NOTE: this can't work from inside Docker!
#     # TODO: either clean up with a shell script or get the whole folder structure
#     #       inside the container

# Validate Template List Has Minimum ${X} Items
#     Log  DEPRECATION WARNING - @WLAD replace/update this keyword!
#     ...  level=WARN
#
#     Array     $.templates    minItems=${X}

# Validate Template List Has Maximum ${X} Items
#     Log  DEPRECATION WARNING - @WLAD replace/update this keyword!
#     ...  level=WARN
#
#     Array     $.templates    maxItems=${X}

# Validate Template List Has ${X} Items
#     Log  DEPRECATION WARNING - @WLAD replace/update this keyword!
#     ...  level=WARN
#
#     Run Keyword And Return If  ${X}==0  Template List Is Empty
#     # ${count}    Get Length    ${template_list}
#     # Should Be Equal As Strings   ${count}    ${X}
#     Array    $.templates    minItems=${X}    maxItems=${X}

# Template List Is Empty
#     Log  DEPRECATION WARNING - @WLAD replace/update this keyword!
#     ...  level=WARN
#
#     Array     $.templates   []

# server rejected OPT
#                         Log  DEPRECATION WARNING - @WLAD replace this keyword!
#                         ...  level=WARN
#                         Should Be Equal As Strings   ${response.status_code}   400




################################################################################
#####################[ DEPRECATED ENDPOINTS]####################################
# NOTE: this keywords can be used later to check that deprecated endpoint were
#       removed from the system

# Get Template List
#     &{resp}=    REST.GET    /template
#     Integer  response status  200
#     ${templates}=   Array   response body templates
#     Set Test Variable    ${template_list}    ${templates}
