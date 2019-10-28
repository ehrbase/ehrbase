*** Settings ***
Documentation    Alternative flow 1: get directory on EHR with just a root directory
...
...     Preconditions:
...         An EHR with ehr_id exists and has an empty directory.
...
...     Flow:
...         1. Invoke the get directory service for the ehr_id
...         2. The service should return the structure of the empty directory for the EHR
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
Alternative flow 1: get directory on EHR with just a root directory

    create EHR

    create DIRECTORY (JSON)    empty_directory.json

    get DIRECTORY at version (JSON)
    
    validate GET-@version response - 200 retrieved
