*** Settings ***
Documentation    Alternative flow 1: has directory on existing EHR with directory
...
...     Preconditions:
...         An EHR with known ehr_id exists and has directory.
...
...     Flow:
...         1. Invoke the has DIRECTORY service for the ehr_id
...         2. The result must be true
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
Alternative flow 1: has directory on existing EHR with directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get DIRECTORY (JSON)

    validate GET response - 200 retrieved
