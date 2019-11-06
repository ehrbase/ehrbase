*** Settings ***
Documentation    Alternative flow 1: get versioned directory from existent EHR that has two versions of directory
...
...     Preconditions:
...         An EHR with known ehr_id exists in the server, has two versions of directory.
...
...     Flow:
...
...         1. Invoke the get versioned directory service for the ehr_id
...         2. The service should return the versioned folder and should have two versions
...
...     Postconditions:
...         None


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
Alternative flow 1: get versioned directory from existent EHR that has two versions of directory

    create EHR

    create DIRECTORY (JSON)    empty_directory.json

    update DIRECTORY (JSON)    subfolders_in_directory.json

    get DIRECTORY at version (JSON)

    validate GET-@version response - 200 retrieved
