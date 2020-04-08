*** Settings ***
Documentation   Main flow: update directory from EHR with directory
...             
...             Preconditions:
...                 An EHR with ehr_id exists and has a directory.
...             
...             Flow:
...                 1. Invoke the update directory service for the ehr_id
...                 2. The service should return a positive result related w/
...                    the updated directory
...                 3. The directory version ID should reflect it is
...                    a new version from the previous directory version
...             
...             Postconditions:
...                 The EHR ehr_id has the updated directory structure.


Resource                ${CURDIR}${/}../../_resources/suite_settings.robot

Test Setup              Preconditions
Test Teardown           Postconditions

Force Tags              refactor



*** Test Cases ***
Main flow: update directory from EHR with directory
    [Documentation]     Steps of this test:
    ...                     1. Create empty folder
    ...                     2. Add subfolders
    ...                     3. Add items to folder/subfolder
    ...                     4. validate result
    [Tags]              

    update DIRECTORY (JSON)    update/2_add_subfolders.json
    validate PUT response - 200 updated

    update DIRECTORY (JSON)    update/3_add_items.json
    validate PUT response - 200 updated



*** Keywords ***
Preconditions
    create EHR
    create DIRECTORY (JSON)    update/1_create_empty_directory.json


Postconditions
    get DIRECTORY (JSON)
    validate GET response - 200 retrieved
