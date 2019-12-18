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
    Set Suite Variable    ${VERSION}    ${version}


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
    Wait For Process  ehrserver  timeout=10  on_timeout=continue
    Is Process Running  ehrserver
    Process Should Be Running  ehrserver
    wait until openehr server is ready
    wait until openehr server is online


start server process without coverage
    ${result}=          Start Process  java  -jar  ${PROJECT_ROOT}${/}application/target/application-${VERSION}.jar
                        ...                  --cache.enabled\=false    alias=ehrserver 
                        ...                    cwd=${PROJECT_ROOT}    stdout=stdout.txt    stderr=stderr.txt


start server process with coverage
    ${result}=          Start Process  java  -javaagent:${JACOCO_LIB_PATH}/jacocoagent.jar\=output\=tcpserver,address\=127.0.0.1
                        ...                  -jar    ${PROJECT_ROOT}${/}application/target/application-${VERSION}.jar
                        ...                  --cache.enabled\=false    alias=ehrserver
                        ...                    cwd=${PROJECT_ROOT}    stdout=stdout.txt    stderr=stderr.txt


wait until openehr server is ready
    Wait Until Keyword Succeeds  120 sec  3 sec  text "Started EhrBase ..." is in log
    [Teardown]  Run keyword if  "${KEYWORD STATUS}"=="FAIL"  abort test execution if server not ready


text "Started EhrBase ..." is in log
    ${stdout}=  Get File  ${PROJECT_ROOT}${/}stdout.txt
    Log  ${stdout}
    # Should Contain    ${stdout}    Started EhrBase
    Should Match Regexp    ${stdout}    Started EhrBase in \\d+.\\d+ seconds


wait until openehr server is online
    Wait Until Keyword Succeeds  33 sec  3 sec  openehr server is online
    [Teardown]  Run keyword if  "${KEYWORD STATUS}"=="FAIL"  abort test execution if server not ready


openehr server is online
    REST.GET    http://localhost:8080/ehrbase/swagger-ui.html
    Integer  response status  200


abort test execution if server not ready
    Log    THE SERVER IS NOT RUNNING    ERROR
    Fatal Error  Aborted Tests Execution - Server is NOT running!


abort test execution if this test fails
    [Documentation]     Aborts test execution if some given preconditions
    ...                 could not be met.
    Log Variables
    ${status}=      Set Variable    ${TESTSTATUS}
                    Run Keyword If  "${status}"=="FAIL"
                    ...             Fatal Error  Aborted Execution - Preconditions not met!


startup SUT
    get application version
    unzip file_repo_content.zip
    start ehrdb
    start openehr server


shutdown SUT
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
    run postgresql container
    wait until ehrdb is ready


stop ehrdb
    [Documentation]     Stops DB container by using a keyword `stop ehrdb container`
    ...                 from custom library: dockerlib.py

    ${logs}  ${status}  stop ehrdb container
    Log      ${logs}
    Log      ${status}
    wait until ehrdb is stopped
    Should Be Equal As Integers  ${status}[StatusCode]  0


stop and remove ehrdb

    Log     DEPRECATION WARNING - @WLAD replace/update this keyword!
    ...     level=WARN
            # NOTE: remove `stop ehrdb` from this keyword!
            #      `remove_ehrdb_container` cracefully stops and waits for
            #        container to be removed

    # stop ehrdb
    remove ehrdb container  # kw from dockerlib.py


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


TRACE JIRA BUG
    [Arguments]     ${JIRA_BUG_ID}
    ...             ${not-ready}=
    ...             ${message}=Next step fails due to a bug!
    ...             ${loglevel}=ERROR

    Log  DEPRECATION WARNING - @WLAD replace/update this keyword!
    ...  level=WARN

                    Log    ${message} | JIRA: ${JIRA_BUG_ID}   level=${loglevel}
                    Set Tags    bug    ${JIRA_BUG_ID}
                    Run Keyword If    '${not-ready}'=='not-ready'    Set Tags    not-ready


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
