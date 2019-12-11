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
Library          XML

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
                        Set Test Variable    ${file}    ${file}
                        Set Test Variable    ${expected}    ${xml}
                        Log Element          ${expected}


get invalid OPT file
    [Arguments]         ${opt file}
    [Documentation]     Gets an OPT file from test_data_sets/invalid_templates folder

    ${file}=            Get File             ${INVALID DATA SETS}/${opt file}

                        # handle empty file and empty XML
                        Run Keyword And Return If    """${file}"""=='${EMPTY}'
                        ...                          Set Test Variable  ${file}  ${file}

                        Run Keyword And Return If    """${file}"""=="""<?xml version="1.0" encoding="utf-8"?>\n"""
                        ...                          Set Test Variable  ${file}  ${file}

    ${xml}=             Parse Xml            ${file}
                        Set Test Variable    ${file}    ${file}
                        Set Test Variable    ${expected}    ${xml}
                        Log Element          ${expected}


extract template_id from OPT file
    [Documentation]     Extracts template_id from OPT (XML) file which was obtained
    ...                 with `get valid/invalid OPT file` keywords

    ${template_id}=     Get Element Text     ${expected}   xpath=template_id
    ...                 normalize_whitespace=True
                        Set Test Variable    ${template_id}    ${template_id}
                        # Log To Console      ${template_id}


start request session
    Create Session      ${SUT}    ${${SUT}.URL}
    ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True
    &{headers}=         Create Dictionary    Content-Type=application/xml
                        ...                  Prefer=return=representation
                        Set Test Variable    ${headers}    ${headers}


start request session (XML)
    Create Session      ${SUT}    ${${SUT}.URL}
    ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True
    &{headers}=         Create Dictionary    Content-Type=application/xml
                        ...                  Prefer=return=representation
                        ...                  Accept=application/xml
                        Set Test Variable    ${headers}    ${headers}


upload valid OPT
    [Arguments]           ${opt file}

    start request session
    get valid OPT file    ${opt file}
    upload OPT file
    server accepted OPT
    [Teardown]            clean up test variables


upload OPT file
    [Documentation]     Uploads OPT file which was obtained with one of the Keywords
    ...                 `get valid OPT file` or `get invalid OPT file`

    ${resp}=            Post Request         ${SUT}    /definition/template/adl1.4
                        ...                  data=${file}    headers=${headers}
                        Set Test Variable    ${response}    ${resp}
                        # Log To Console      ${resp.content}


retrieve versioned OPT
    [Arguments]                         ${opt file}

    Log               NOT APPLICABLE FOR ADL 1.4    level=WARN
    Pass Execution    NOT APPLICABLE FOR ADL 1.4    not-ready

    start request session
    get valid OPT file                  ${opt file}
    extract template_id from OPT file
    retrieve OPT by template_id         ${template_id}
    verify server response
    [Teardown]                          clean up test variables


server accepted OPT
                        Should Be Equal As Strings   ${response.status_code}   201


server rejected OPT with status code ${status code}
    [Documentation]     400: Bad Request - is returned when unable to upload a template,
    ...                      because of invalid content.
    ...                 409: Conflict - is returned when a template with given id
    ...                      already exists. This response is optional.

                        Should Be Equal As Strings   ${response.status_code}
                        ...                          ${status code}


server returned specified version of OPT
    Fail    msg=Brake it till you make it!


retrieve OPT by template_id
    [Arguments]         ${template_id}
    [Documentation]     Gets OPT from server with provided template_id

    ${resp}=            Get Request          ${SUT}    /definition/template/adl1.4/${template_id}
                        ...                  headers=${headers}
                        Log    ${resp.text}
                        # Log    ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   200
    ${xml}=             Parse Xml            ${resp.text}
                        Set Test Variable    ${actual}    ${xml}


verify content of OPT
                        Log Element    ${actual}
                        Log Element    ${expected}

                        Elements Should Be Equal    ${actual}    ${expected}
                        ...                      normalize_whitespace=True


generate random templade_id
    [Documentation]     Generates a random template_id variable and puts it
    ...                 into Test Case Scope.

    ${template_id}=     Generate Random String    16    [NUMBERS]abcdef
                        Set Test Variable    ${template_id}    ${template_id}


clean up test variables
    [Documentation]     Cleans up test variables to avoid impacts between tests.

                        Set Test Variable    ${file}           None
                        Set Test Variable    ${expected}       None
                        Set Test Variable    ${template_id}    None
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
    ${resp}=  Post Request  ethlocal  /template  data=${file}  headers=${headers}
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

# retrieve list of uploaded OPTs (request lib example)
#     [Documentation]    List all available operational templates on the system.
#
#     ${resp}=           Get Request          ${SUT}    /definition/template/adl1.4
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
