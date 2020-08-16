*** Settings ***
Documentation    Alternative flow 2: create directory on non-existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the create directory service for a random ehr_id
...         2. The service should return an error, related to the EHR that doesn't exist
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
Alternative flow 2: create directory on non-existent EHR

    create fake EHR

    create DIRECTORY - fake ehr_id (JSON)    subfolders_in_directory.json

    validate POST response - 404 unknown ehr_id
