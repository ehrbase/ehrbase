*** Settings ***
Documentation    Main flow: get directory at version from existent and empty EHR
...
...     Preconditions:
...         An empty EHR with known ehr_id exists in the server.
...
...     Flow:
...         1. Invoke the get directory at version service for the ehr_id and a random version uid
...         2. The service should return an error related to the non existence of the EHR directory
...
...     Postconditions:
...         None
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Main flow: get directory at version from existent and empty EHR

    create EHR

    get DIRECTORY at version - fake version_uid (JSON)

    validate GET-@version response - 404 unknown version_uid
