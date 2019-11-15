*** Settings ***
Documentation    Main flow: get directory at time on empty EHR
...
...     Preconditions:
...         An EHR with ehr_id exists and doesn't have directory.
...
...     Flow:
...         1. Invoke the get directory at time service for the ehr_id and current time
...         2. The service should return feedback related to the non existent directory
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
Main flow: get directory at time on empty EHR

    create EHR

    get DIRECTORY at current time (JSON)

    validate GET-version@time response - 404 unknown folder-version@time
