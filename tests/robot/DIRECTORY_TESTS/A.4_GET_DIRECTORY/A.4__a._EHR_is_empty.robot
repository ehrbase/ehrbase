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

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Main flow: get directory on empty EHR
    [Tags]              228

    create EHR

    get DIRECTORY (JSON)

    TRACE GITHUB ISSUE  228  not-ready

    validate GET-version@time response - 404 unknown folder-version@time
