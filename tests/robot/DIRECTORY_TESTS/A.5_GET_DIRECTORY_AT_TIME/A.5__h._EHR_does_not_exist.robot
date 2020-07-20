*** Settings ***
Documentation    Alternative flow 7: get directory at time on non existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the get directory at time service for a random ehr_id and current time
...         2. The service should return an error about the non existent EHR
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
Alternative flow 7: get directory at time on non existent EHR

    create fake EHR

    get DIRECTORY at current time (JSON)

    validate GET-version@time response - 404 unknown ehr_id
