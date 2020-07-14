*** Settings ***
Documentation    Alternative flow 2: has directory from non existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the HAS directory service for a random ehr_id and version uid
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

Force Tags    refactor



*** Test Cases ***
Alternative flow 2: has directory from non existent EHR

    create fake EHR

    get DIRECTORY at version - fake ehr_id (JSON)

    validate GET-@version response - 404 unknown ehr_id
