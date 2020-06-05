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
Library    Collections
Library    String
Library    Process
Library    OperatingSystem



*** Variables ***
${README_LINK}    https://github.com/ehrbase/ehrbase/blob/develop/tests/README.md
${MANUAL_TEST_ENV}    \#manually-controlled-sut



*** Keywords ***
# oooo    oooo oooooooooooo oooooo   oooo oooooo   oooooo     oooo   .oooooo.   ooooooooo.   oooooooooo.    .oooooo..o
# `888   .8P'  `888'     `8  `888.   .8'   `888.    `888.     .8'   d8P'  `Y8b  `888   `Y88. `888'   `Y8b  d8P'    `Y8
#  888  d8'     888           `888. .8'     `888.   .8888.   .8'   888      888  888   .d88'  888      888 Y88bo.
#  88888[       888oooo8       `888.8'       `888  .8'`888. .8'    888      888  888ooo88P'   888      888  `"Y8888o.
#  888`88b.     888    "        `888'         `888.8'  `888.8'     888      888  888`88b.     888      888      `"Y88b
#  888  `88b.   888       o      888           `888'    `888'      `88b    d88'  888  `88b.   888     d88' oo     .d8P
# o888o  o888o o888ooooood8     o888o           `8'      `8'        `Y8bood8P'  o888o  o888o o888bood8P'   8""88888P'
#
# [ HIGH LEVEL KEYWORDS ]



#     o8o                                           .o8   o8o   .o88o.  .o88o.
#     `"'                                          "888   `"'   888 `"  888 `"
#    oooo  .oooo.o  .ooooo.  ooo. .oo.         .oooo888  oooo  o888oo  o888oo
#    `888 d88(  "8 d88' `88b `888P"Y88b       d88' `888  `888   888     888
#     888 `"Y88b.  888   888  888   888       888   888   888   888     888
#     888 o.  )88b 888   888  888   888       888   888   888   888     888
#     888 8""888P' `Y8bod8P' o888o o888o      `Y8bod88P" o888o o888o   o888o
#     888
# .o. 88P
# `Y888P
#
# [ JSHON DIFF / COMPARE]

compare json-strings
    [Arguments]         ${actual_json}  ${expected_json}  &{options}
    [Documentation]     Compares two JSON strings.\\n
    ...
    ...                 :actual_json: valid JSON string\\n
    ...                 :expected_json: valid JSON string
    ...
    ...                 :options: with defaults
    ...                     - ignore_order=True,
    ...                     - report_repetition=False,
    ...                     - exclude_paths=None,
    ...                     - exclude_regex_paths=None,
    ...                     - ignore_string_type_changes=False,
    ...                     - ignore_numeric_type_changes=False,
    ...                     - ignore_type_subclasses=False,
    ...                     - ignore_string_case=False,
    ...                     - verbose_level=2
    ...
    ...                 Check DeedDiff reference for more details: https://deepdiff.readthedocs.io/en/latest/diff.html

    &{diff}=            compare jsons    ${actual_json}    ${expected_json}    &{options}

    [Return]            ${diff}


compare json-files
    [Arguments]         ${filepath_1}    ${filepath_2}    &{options}
    [Documentation]     Compares two JSON files given by filepath.
    ...
    ...                 :filepath_: valid path to a JSON test-data-set\n
    ...                 :options: same as in `compare json-strings`

    ${actual}=          Get File    ${filepath_1}
    ${expected}=        Get File    ${filepath_2}

    &{diff}=            compare jsons    ${actual}    ${expected}    &{options}

    [Return]            ${diff}


compare json-string with json-file
    [Arguments]         ${json_string}    ${json_file_by_filepath}    &{options}
    [Documentation]     Compares a JSON string with a JSON file given by filepath
    ...
    ...                 :json_string: valid JSON string
    ...                 :json_file_by_filepath: valid path to JSON test-data-set
    ...                 :options: same as in `compare json-strings`

    ${actual}=          Set Variable    ${json_string}
    ${expected}=        Get File    ${json_file_by_filepath}

    &{diff}=            compare jsons    ${actual}    ${expected}    &{options}

    [Return]            ${diff}


compare json-file with json-string
    [Arguments]         ${json_file_by_filepath}    ${json_string}    &{options}
    [Documentation]     Compares a JSON file given by filepath with a JSON string.
    ...
    ...                 :json_file_by_filepath: valid path to JSON test-data-set
    ...                 :json_string: valid JSON string
    ...                 :options: same as in `compare json-strings`

    ${actual}=          Get File    ${json_file_by_filepath}
    ${expected}=        Set Variable    ${json_string}

    &{diff}=            compare jsons    ${actual}    ${expected}    &{options}

    [Return]            ${diff}






restart SUT
    # stop openehr server
    # stop and remove ehrdb
    # empty operational_templates folder
    # start ehrdb
    # start openehr server
    Delete All Templates


get application version
    ${root}=  Parse Xml    ${POM_FILE}
    ${version}=  Get Element Text   ${root}  version
    Set Global Variable    ${VERSION}    ${version}


unzip file_repo_content.zip
    Start Process  unzip  -o  ${PROJECT_ROOT}${/}.circleci/file_repo_content.zip
    ...                       alias=unzip  cwd=${PROJECT_ROOT}
    ...                       stderr=stderr_unzip.txt  stdout=stdout_unzip.txt

    Wait For Process  unzip  timeout=5
    Wait Until Created    ${PROJECT_ROOT}${/}file_repo  timeout=5
    List Directory    ${PROJECT_ROOT}${/}file_repo${/}knowledge/operational_templates


operational_templates folder exists
    ${result}=  Run Keyword And Return Status  Directory Should Exist  ${PROJECT_ROOT}${/}file_repo${/}knowledge/operational_templates
    [Return]  ${result}


operational_templates folder is empty
    List Files In Directory    ${PROJECT_ROOT}${/}file_repo${/}knowledge/operational_templates
    Directory Should Be Empty    ${PROJECT_ROOT}${/}file_repo${/}knowledge/operational_templates


empty operational_templates folder
    ${folder exists}=  operational_templates folder exists
    Run Keyword If  ${folder exists}  Run Keywords
    ...    Empty Directory    ${PROJECT_ROOT}${/}file_repo${/}knowledge/operational_templates
    ...    AND  Wait Until Removed    ${PROJECT_ROOT}${/}file_repo${/}knowledge/operational_templates/*
    ...    AND  operational_templates folder is empty


start openehr server
    get application version
    run keyword if  '${CODE_COVERAGE}' == 'True'   start server process with coverage
    run keyword if  '${CODE_COVERAGE}' == 'False'  start server process without coverage
    Wait For Process  ehrserver  timeout=25  on_timeout=continue
    Is Process Running  ehrserver
    ${status}=      Run Keyword And Return Status    Process Should Be Running    ehrserver
                    Run Keyword If    ${status}==${FALSE}    Fatal Error    Server failed to start!
    wait until openehr server is ready
    wait until openehr server is online


start server process without coverage
    ${result}=          Start Process  java  -jar  ${PROJECT_ROOT}${/}application/target/application-${VERSION}.jar
                        ...                  --cache.enabled\=false
                        ...                  --server.nodename\=${CREATING_SYSTEM_ID}    alias=ehrserver
                        ...                    cwd=${PROJECT_ROOT}    stdout=stdout.txt    stderr=stderr.txt


start server process with coverage
    ${result}=          Start Process  java  -javaagent:${JACOCO_LIB_PATH}/jacocoagent.jar\=output\=tcpserver,address\=127.0.0.1
                        ...                  -jar    ${PROJECT_ROOT}${/}application/target/application-${VERSION}.jar
                        ...                  --cache.enabled\=false
                        ...                  --server.nodename\=${CREATING_SYSTEM_ID}    alias=ehrserver
                        ...                    cwd=${PROJECT_ROOT}    stdout=stdout.txt    stderr=stderr.txt


wait until openehr server is ready
    Wait Until Keyword Succeeds  120 sec  3 sec  text "Started EhrBase ..." is in log
    [Teardown]  Run keyword if  "${KEYWORD STATUS}"=="FAIL"  abort test execution    Server is NOT running!


text "Started EhrBase ..." is in log
    ${stdout}=  Get File  ${PROJECT_ROOT}${/}stdout.txt
    Log  ${stdout}
    # Should Contain    ${stdout}    Started EhrBase
    Should Match Regexp    ${stdout}    Started EhrBase in \\d+.\\d+ seconds


wait until openehr server is online
    Wait Until Keyword Succeeds  33 sec  3 sec  openehr server is online
    [Teardown]  Run keyword if  "${KEYWORD STATUS}"=="FAIL"  abort test execution    Server is NOT running!


openehr server is online
    prepare new request session  JSON
    REST.GET    ${HEARTBEAT_URL}
    Integer  response status  404


abort test execution
    [Arguments]         @{TEST_ENVIRONMENT_STATUS}

    ${overall_status}    ${server_status}    ${db_status}=    Set Variable    ${TEST_ENVIRONMENT_STATUS}
                        Log    ABORTING EXECUTION DUE TO TEST ENVIRONMENT ISSUES:    level=ERROR
                        Run Keyword if    ${server_status}==${FALSE}    Log
                        ...               Could not connect to server!    level=ERROR
                        Run Keyword if    ${db_status}==${FALSE}    Log
                        ...               Could not connect to database!    level=ERROR

                        Fatal Error  ABORTED TEST EXECUTION!


abort test execution if this test fails
    [Documentation]     Aborts test execution if some given preconditions
    ...                 could not be met.
    Log Variables
    ${status}=      Set Variable    ${TESTSTATUS}
                    Run Keyword If  "${status}"=="FAIL"
                    ...             Fatal Error  Aborted Execution - Preconditions not met!


prepare new request session
    [Arguments]         ${headers}=JSON    &{extra_headers}
    [Documentation]     Prepares request settings for RESTistance AND RequestsLibrary
    ...                 :headers: valid argument values:
    ...                     - JSON (default)
    ...                     - XML
    ...                     - no accept header
    ...                     - no content header
    ...                     - no accept header xml
    ...                     - no accept/content headers
    ...                     - no headers
    ...
    ...                 :extra_headers: optional, valid value examples: 
    ...                     - Prefer=return=representation
    ...                     - If-Match={ehrstatus_uid}
    
                        Log Many            ${headers}  ${extra_headers}

                        # case: JSON (DEFAULT)
                        Run Keyword If      $headers=='JSON'    set request headers
                        ...                 content=application/json
                        ...                 accept=application/json
                        ...                 &{extra_headers}

                        # case: XML
                        Run Keyword If      $headers=='XML'    set request headers
                        ...                 content=application/xml
                        ...                 accept=application/xml
                        ...                 &{extra_headers}

                        # case: no Accept header, Content-Type=JSON
                        Run Keyword If      $headers=='no accept header'    set request headers
                        ...                 content=application/json
                        ...                 &{extra_headers}

                        # case: no Accept header, Content-Type=XML
                        Run Keyword If      $headers=='no accept header xml'    set request headers
                        ...                 content=application/xml
                        ...                 &{extra_headers}

                        # case: no Content-Type header
                        Run Keyword If      $headers=='no content header'    set request headers
                        ...                 accept=application/json
                        ...                 &{extra_headers}

                        # case: no Accept & no Content-Type header
                        Run Keyword If      $headers=='no accept/content headers'    set request headers
                        ...                 &{extra_headers}

                        # case: no headers
                        Run Keyword If      $headers=='no headers'    set request headers  

                        # case: mixed cases like JSON/XML or XML/JSON can be added here!

set request headers
    [Arguments]         ${content}=${NONE}  ${accept}=${NONE}  &{extra_headers}
    [Documentation]     Sets the headers of a request
    ...                 :content: None (default) / application/json / application/xml
    ...                 :accept: None (default) / application/json / application/xml
    ...                 :extra_headers: optional - e.g. Prefer=return=representation
    ...                                            e.g. If-Match={ehrstatus_uid}
    ...
    ...                 ATTENTIION: RESTInstance lib sets {"Content-Type": "applicable/json"}
    ...                             and {"Accept": "application/json, */*"} by default!
    ...                             As a workaround set them to None, null or empty - i.e.:
    ...                             - "Content-Type=    "
    ...                             - "Accept=    "

                        Log Many            ${content}  ${accept}  ${extra_headers}
                        Run Keyword If    "${content}"=="${NONE}" and "${accept}"=="${NONE}"
                        ...    Log To Console   \nWARNING: NO REQUEST HEADERS SET!

    &{headers}=         Create Dictionary     &{EMPTY}
    
                        Set To Dictionary    ${headers}
                        ...                  Content-Type=${content}
                        ...                  Accept=${accept}
                        ...                  &{extra_headers}

    # comment: headers for RESTinstance Library
    &{headers}=         Set Headers         ${headers}
                        Set Headers         ${authorization}

    # comment: headers for RequestLibrary
                        Create Session      ${SUT}    ${${SUT}.URL}    debug=2
                        ...                 auth=${${SUT}.CREDENTIALS}    verify=True

                        Set Suite Variable   ${headers}    ${headers}


server sanity check
    [Documentation]     Sends a GET request to ${HEARTBEAT_URL} to check whether
    ...                 the server is up and running.
    
    ${server_status}    Run Keyword And Return Status    openehr server is online
    [RETURN]            ${server_status}


database sanity check
    [Documentation]     Connects to local PostgreSQL DB regardless whether it was 
    ...                 installed natively or dockerized. Disconnects immediately
    ...                 on success.
    ...                 Is skipped when CONTROL_MODE is not manual - e.g. when SUT
    ...                 is on a remote host.

    Return From Keyword If    "${CONTROL_MODE}"=="docker"    NO DB CHECK ON REMOTE SUT

    ${db_status}        Run Keyword And Return Status    Connect With DB
                        Run Keyword If    $db_status    Disconnect From Database
    [RETURN]            ${db_status}


do quick sanity check

    ${server_status}    server sanity check
    ${db_status}        database sanity check

    ${env_status}       Set Variable If   $server_status==False or $db_status==False
                        ...    ${FALSE}    ${TRUE}


                        Set Global Variable    @{TEST_ENVIRONMENT_STATUS}
                        ...                    ${env_status}    ${server_status}    ${db_status}

    [RETURN]            ${env_status}    ${server_status}    ${db_status}


warn about manual test environment start up
    [Tags]              robot:flatten
    Log    ${EMPTY}                                                                             level=WARN
    Log    /////////////////////////////////////////////////////////////////////                level=WARN
    Log    //${SPACE * 64}///                                                                   level=WARN
    Log    //${SPACE * 10} YOU HAVE CHOSEN TO START YOUR SUT MANUALLY! ${SPACE * 9}///          level=WARN
    Log    //${SPACE * 5} MAKE SURE IT MEETS PREREQUISITES FOR TEST EXECUTION! ${SPACE * 5}///  level=WARN
    Log    //${SPACE * 6} MAKE SURE TO RESET IT PROPERLY AFTER EACH TEST RUN! ${SPACE * 5}///   level=WARN
    Log    //${SPACE * 64}///                                                                   level=WARN
    Log    /////////////////////////////////////////////////////////////////////                level=WARN
    Log    ${EMPTY}                                                                             level=WARN
    Log    [ check "Manually Controlled SUT" in test README ]                                   level=WARN
    Log    [ ${README_LINK}${MANUAL_TEST_ENV} ]                                                 level=WARN
    Log    ${EMPTY}                                                                             level=WARN
    Set Global Variable    ${SKIP_SHUTDOWN_WARNING}    ${FALSE}


warn about manual test environment shut down
    Run Keyword And Return If    ${SKIP_SHUTDOWN_WARNING}==${TRUE}    Log
                          ...    skipping manual test env control warning due to test abortion
    Log    ${EMPTY}                                                                             level=WARN
    Log    /////////////////////////////////////////////////////////////////////                level=WARN
    Log    //${SPACE * 64}///                                                                   level=WARN
    Log    //${SPACE * 13}REMBER TO PROPERLY RESTART YOUR SUT! ${SPACE * 13} ///                level=WARN
    Log    //${SPACE * 64}///                                                                   level=WARN
    Log    /////////////////////////////////////////////////////////////////////                level=WARN
    Log    ${EMPTY}                                                                             level=WARN


abort tests due to issues with manually controlled test environment
    Log    ${EMPTY}                                                                             level=WARN
    Log    /////////////////////////////////////////////////////////////////////                level=WARN
    Log    //${SPACE * 64}///                                                                   level=WARN
    Log    //${SPACE * 10} YOU HAVE CHOSEN TO START YOUR SUT MANUALLY ${SPACE * 10}///          level=WARN
    Log    //${SPACE * 6} BUT IT IS NOT AVAILABLE OR IS NOT SET UP PROPERLY! ${SPACE * 6}///    level=WARN
    Log    //${SPACE * 64}///                                                                   level=WARN
    Log    /////////////////////////////////////////////////////////////////////                level=WARN
    Log    ${EMPTY}                                                                             level=WARN
    Log    [ check "Manually Controlled SUT" in test README ]                                   level=WARN
    Log    [ ${README_LINK}${MANUAL_TEST_ENV} ]                                                 level=WARN
    Log    ${EMPTY}                                                                             level=WARN
    Set Global Variable    ${SKIP_SHUTDOWN_WARNING}    ${TRUE}
    abort test execution    @{TEST_ENVIRONMENT_STATUS}

    abort test execution  TEST_ENVIRONMENT_STATUS
    abort test execution if this test fails

startup SUT
    get application version

    # comment: switch to manual test environment control when "-v nodocker" cli option is used
    Run Keyword If      $NODOCKER.upper() in ["TRUE", ""]    Run Keywords
               ...      Set Global Variable    ${NODOCKER}    TRUE    AND
               ...      Set Global Variable    ${BASEURL}    ${DEV.URL}    AND
               ...      Set Global Variable    ${HEARTBEAT_URL}    ${DEV.HEARTBEAT}    AND
               ...      Set Global Variable    ${AUTHORIZATION}    ${DEV.AUTH}    AND
               ...      Set Global Variable    ${CREATING_SYSTEM_ID}    ${DEV.NODENAME}    AND
               ...      Set Global Variable    ${CONTROL_MODE}    ${DEV.CONTROL}

                        Log    \n\t SUT CONFIG (EHRbase v${VERSION})\n    console=true
                        Log    \t BASEURL: ${BASEURL}    console=true
                        Log    \t HEARTBEAT: ${HEARTBEAT_URL}    console=true
                        Log    \t AUTH: ${AUTHORIZATION}    console=true
                        Log    \t CREATING SYSTEM ID: ${CREATING_SYSTEM_ID}    console=true
                        Log    \t CONTROL MODE: ${CONTROL_MODE}\n    console=true

    ${sanity_check_passed}  ${server_status}  ${db_status}=    do quick sanity check

    Run Keyword And Return If   "${CONTROL_MODE}"=="manual" and ${sanity_check_passed}
                          ...    warn about manual test environment start up

    Run Keyword And Return If   "${CONTROL_MODE}"=="manual" and not ${sanity_check_passed}
                          ...   abort tests due to issues with manually controlled test environment

    # comment: test environment controlled by Robot
    get application version
    start ehrdb
    start openehr server


shutdown SUT
    Run Keyword And Return If   "${CONTROL_MODE}"=="manual"
                          ...    warn about manual test environment shut down

    stop openehr server
    stop and remove ehrdb
    empty operational_templates folder


stop openehr server
    run keyword if  '${CODE_COVERAGE}' == 'True'   dump test coverage
    ${result}=  Terminate Process  ehrserver  # kill=true
    Process Should Be Stopped	ehrserver
    Log  ${result.stderr}
    Log  ${result.stdout}


dump test coverage
    run process  java  -jar  ${JACOCO_LIB_PATH}/jacococli.jar  dump  --destfile\=${COVERAGE_DIR}/jacoco-it_temp.exec   alias=coverage_dump
    @{coverage_files}=  list files in directory  ${COVERAGE_DIR}  *.exec  absolute
    run process  java  -jar  ${JACOCO_LIB_PATH}/jacococli.jar  merge  @{coverage_files}  --destfile\=${COVERAGE_DIR}/jacoco-it.exec


start ehrdb
    ${status}           Run Keyword And Return Status    run postgresql container
                        Run Keyword If    ${status}==${FALSE}    Fatal Error   Could not start DB!

                        wait until ehrdb is ready


stop and remove ehrdb
    [Documentation]     Stos DB container gracefully and waits for it to be removed
    ...                 Uses KW from custom library: dockerlib.py

                        Remove EhrDB Container


restart ehrdb
    [Documentation]    Restarts Docker Container of DB.

    remove ehrdb container
    start ehrdb


ehrdb is stopped
    [Documentation]    Checks that DB is stopped properly.
    ...                Uses keyword from custom library: dockerlib.py

    ${logs}=  get logs from ehrdb
    ${db_logs}=  Convert To String    ${logs}
    Should Contain    ${db_logs}  database system is shut down


ehrdb is ready
    ${logs}=  get logs from ehrdb
    ${db_logs}=  Convert To String    ${logs}
    Should Contain    ${db_logs}    database system is ready to accept connections


wait until ehrdb is ready
    Wait Until Keyword Succeeds  33 sec  3 sec  ehrdb is ready


wait until ehrdb is stopped
    Wait Until Keyword Succeeds  10 sec  3 sec  ehrdb is stopped


TW ${TEST WARNING MESSAGE} - tag(s): ${TAG:.*}
    [Documentation]  Log Test WARNING (TW)
    @{TAG} =  Split String    ${TAG}
    Run keyword if  "${TEST STATUS}"=="FAIL"  log a WARNING and set tag(s)
    ...             ${TEST WARNING MESSAGE}  @{TAG}


KW ${KEYWORD WARNING MESSAGE} - tag(s): ${TAG:.*}
    [Documentation]  Log Keyword WARNING (KW)
    @{TAG} =  Split String    ${TAG}
    Run keyword if  "${KEYWORD STATUS}"=="FAIL"  log a WARNING and set tag(s)
    ...             ${KEYWORD WARNING MESSAGE}  @{TAG}


TE ${TEST ERROR MESSAGE} - tag(s): ${TAG:.*}
    [Documentation]  Log Test ERROR (TE)
    @{TAG} =  Split String    ${TAG}
    Run keyword if  "${TEST STATUS}"=="FAIL"  log an ERROR and set tag(s)
    ...             ${TEST ERROR MESSAGE}  @{TAG}


KE ${KEYWORD ERROR MESSAGE} - tag(s): ${TAG:.*}
    [Documentation]  Log Keyword ERROR (KE)
    @{TAG} =  Split String    ${TAG}
    Run keyword if  "${KEYWORD STATUS}"=="FAIL"  log an ERROR and set tag(s)
    ...             ${KEYWORD ERROR MESSAGE}  @{TAG}


log a WARNING and set tag(s)
    [Arguments]  ${WARNING MESSAGE}  @{TAG}
    Log  ${WARNING MESSAGE} - tags: @{TEST TAGS}   WARN
    Set Tags  @{TAG}


log an ERROR and set tag(s)
    [Arguments]  ${ERROR MESSAGE}  @{TAG}
    Log  ${ERROR MESSAGE} - tags: @{TEST TAGS}   ERROR
    Set Tags  @{TAG}


THIS IS JUST A PLACEHOLDER!
    Fail    Placeholder - no impact on CI!
    [Teardown]  Set Tags    not-ready    TODO


TRACE GITHUB ISSUE
    [Arguments]     ${GITHUB_ISSUE}
    ...             ${not-ready}=
    ...             ${message}=Next step fails due to a bug!
    ...             ${loglevel}=ERROR

                    Log    ${message} | <a href="https://github.com/ehrbase/project_management/issues/${GITHUB_ISSUE}">Github ISSUE #${GITHUB_ISSUE}</a>
                    ...    level=${loglevel}    html=True

                    Set Tags    bug    GITHUB ISSUE ${GITHUB_ISSUE}
                    Run Keyword If    '${not-ready}'=='not-ready'    Set Tags    not-ready







# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]

# start openehr server
#     ${result}=  Start Process  java  -jar  ${PROJECT_ROOT}${/}application/target/application-${VERSION}.jar
#     ...                              alias=ehrserver  cwd=${PROJECT_ROOT}  stdout=stdout.txt
#     Wait For Process  ehrserver  timeout=10  on_timeout=continue
#     Is Process Running  ehrserver
#     Process Should Be Running  ehrserver
#     wait until openehr server is ready
#     openehr server is online

# reset ehrdb
#     Log  DEPRECATION WARNING - @WLAD replace/update this keyword!
#     ...  level=WARN
#     stop ehrdb
#     remove ehrdb container
#     start ehrdb

# start docker container
#     [Arguments]   ${container_name}  ${expose_port}  ${image}
#     [Documentation]  expose_port format: -p 27017:27017
#     ...
#     ${RC}=  Run And Return Rc  docker run --name ${container_name} ${expose_port} -d ${image}
#     Should Be Equal As Integers  ${RC}  0
