*** Settings ***
Documentation    Alternative flow 1: has path on EHR with just root directory
...
...     Preconditions:
...         An EHR with known ehr_id exists and has an empty directory (no subfolders or items).
...
...     Flow:
...         1. Invoke the has path service for the ehr_id and the path $path from the data set
...         2. The result must be $result from the data set
...
...     Postconditions:
...         None
...
...     Data set
...         DS   | $path                   | $result |
...         -----+-------------------------+---------+
...         DS 1 | /                       | true    |
...         DS 2 | _any_other_random_path_ | false   |


Resource    ${CURDIR}${/}../../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/contribution_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/directory_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/template_opt1.4_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/ehr_keywords.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 1: has path on EHR with just root directory (DS 1)

    create EHR

    create DIRECTORY (JSON)    empty_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /

    validate GET-@version response - 200 retrieved



Alternative flow 1: has path on EHR with just root directory (DS 2)

    create EHR

    create DIRECTORY (JSON)    empty_directory.json

    generate random path

    get FOLDER in DIRECTORY at version (JSON)    ${path}

        TRACE GITHUB ISSUE  36  not-ready  DISCOVERED ISSUE: `path` URI parameter is ignored(?)

    validate GET-@version response - 404 unknown path
