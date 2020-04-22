*** Settings ***
Documentation   Alternative flow 1: update directory on empty EHR
...             Preconditions:
...                 An EHR with ehr_id exists and doesn't have a directory.
...             
...             Flow:
...                 1. Invoke the update directory service for the ehr_id
...                 2. The service should return an error
...                    related to the non existent directory to update
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
Alternative flow 1: update directory on empty EHR
    [Tags]            

    create EHR
    update DIRECTORY - ehr w/o directory (JSON)    update/2_add_subfolders.json
    validate PUT response - 404 unknown directory
