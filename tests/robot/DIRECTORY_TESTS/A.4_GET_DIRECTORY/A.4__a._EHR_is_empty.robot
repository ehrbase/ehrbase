*** Settings ***
Documentation    Main flow: get directory on empty EHR
...
...     Preconditions:
...         An EHR with ehr_id exists.
...
...     Flow:
...         1. Invoke the get directory service for the ehr_id
...         2. The service should return an error, related to the directory that doesn't exist
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
Main flow: get directory on empty EHR

    create EHR

    get DIRECTORY (JSON)

    validate GET-version@time response - 404 unknown folder-version@time
