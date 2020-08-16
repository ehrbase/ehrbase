*** Settings ***
Documentation    Alternative flow 2: get versioned directory from non existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the GET directory service for a random ehr_id
...         2. The service should return an error related with the non existence of the EHR
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
Alternative flow 2: get versioned directory from non-existent EHR

    create fake EHR

    get DIRECTORY at version - fake ehr_id (JSON)

    validate GET-@version response - 404 unknown ehr_id
