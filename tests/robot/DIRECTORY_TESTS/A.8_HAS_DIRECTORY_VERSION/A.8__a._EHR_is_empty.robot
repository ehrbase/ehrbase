*** Settings ***
Documentation    Main flow: has directory from existent and empty EHR
...
...     Preconditions:
...         An empty EHR with known ehr_id exists in the server.
...
...     Flow:
...         1. Invoke the HAS directory service for the ehr_id and a random version uid
...         2. The service should return false, because the EHR is empty
...
...     Postconditions:
...         None
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags    refactor



*** Test Cases ***
Main flow: has directory from existent and empty EHR

    create EHR

    get DIRECTORY at version - fake version_uid (JSON)

    validate GET-@version response - 404 unknown version_uid
