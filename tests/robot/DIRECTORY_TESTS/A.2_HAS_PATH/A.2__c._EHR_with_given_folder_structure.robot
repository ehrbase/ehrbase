*** Settings ***
Documentation    Alternative flow 2: has path on EHR with given folder structure
...
...     Preconditions:
...         An EHR with known ehr_id exists and has directory with an internal structure.
...
...     Flow:
...         1. Invoke the has path service for the ehr_id and the path $path from the data set
...         2. The result must be $result from the data set
...
...     Postconditions:
...         None
...
...     Data set
...         DS   | $path                                | $result |
...         -----+--------------------------------------+---------+
...         DS 1 | /                                    | true    |
...         DS 2 | /emergency                           | true    |
...         DS 3 | /emergency/episode-x                 | true    |
...         DS 4 | /emergency/episode-x/summary-compo-x | true    |
...         DS 5 | ... see test documentation ...
...              | ...
...         DS 9 | ...


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

Force Tags    refactor    todo-data-driven



*** Test Cases ***
Alternative flow 2: has path on EHR with given folder structure (DS 1)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 2)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /emergency

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 3)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /emergency/episode-x

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 4)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /emergency/episode-x/summary-compo-x

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 5)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /emergency/episode-y

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 6)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /emergency/episode-y/summary-compo-y

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 7)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /hospitalization

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 8)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get FOLDER in DIRECTORY at version (JSON)    /hospitalization/summary-compo-z

    # check response: is positive

    validate GET-@version response - 200 retrieved



Alternative flow 2: has path on EHR with given folder structure (DS 9)

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    generate random path

    get FOLDER in DIRECTORY at version (JSON)    ${path}

        TRACE GITHUB ISSUE  36  not-ready  DISCOVERED ISSUE: `path` URI parameter is ignored(?)

    validate GET-@version response - 404 unknown path
