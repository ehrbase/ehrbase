*** Settings ***
Documentation    Main flow: has path on empty EHR
...
...     Preconditions:
...         An EHR with known ehr_id exists and doesn't have directory.
...
...     Flow:
...       1. Invoke the has path service for the ehr_id with a random FOLDER path
...       2. The result must be false
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
Main flow: has path on empty EHR

    create EHR

    get FOLDER in DIRECTORY at version - fake version_uid/path (JSON)

    validate GET-@version response - 404 unknown version_uid
