*** Settings ***
Documentation    Main flow: has directory on empty existing EHR
...
...     Preconditions:
...         An EHR with known ehr_id exists and doesn't have directory.
...
...     Flow:
...         1. Invoke the has DIRECTORY service for the ehr_id
...         2. The result must be false
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
Main flow: has directory on empty existing EHR

    create EHR

    get DIRECTORY (JSON)

    validate GET-version@time response - 404 unknown folder-version@time
