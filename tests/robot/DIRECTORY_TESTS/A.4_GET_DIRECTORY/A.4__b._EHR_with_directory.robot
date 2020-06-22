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
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

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
    validate GET-@version response - 200 retrieved    root
