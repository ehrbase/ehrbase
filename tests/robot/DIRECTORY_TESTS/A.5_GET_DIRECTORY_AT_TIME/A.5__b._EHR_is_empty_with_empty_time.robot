*** Settings ***
Documentation    Alternative flow 1: get directory at time on empty EHR with empty time
...
...     Preconditions:
...         An EHR with ehr_id exists and doesn't have directory.
...
...     Flow:
...         1. Invoke the get directory at time service for the ehr_id and empty time
...         2. The service should return feedback related to the non existent directory
...
...     Postconditions:
...         None


Resource    ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 1: get directory at time on empty EHR with empty time
    [Tags]              228

    create EHR

    get DIRECTORY at time (JSON)    ${EMPTY}

        TRACE GITHUB ISSUE  228  not-ready

    validate GET-version@time response - 404 unknown folder-version@time
