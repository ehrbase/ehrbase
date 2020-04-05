*** Settings ***
Documentation   Alternative flow 2: update directory on non-existing EHR
...             Preconditions:
...                 An EHR with ehr_id does not exist.
...             
...             Flow:
...                 1. Invoke the update directory service for random ehr_id
...                 2. The service should return an error
...                    related to the non-existent ehr_id
...             
...             Postconditions:
...                 None.



Resource    ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags    refactor



*** Test Cases ***
Alternative flow 2: update directory on non-existing EHR
    [Tags]              

    create fake EHR
    update DIRECTORY - fake ehr_id (JSON)    update/2_add_subfolders.json
    validate PUT response - 404 unknown ehr_id
