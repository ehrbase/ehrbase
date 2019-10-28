*** Settings ***
Documentation    Alternative flow 2: get directory on EHR with complex directory structure and items
...
...     Preconditions:
...         An EHR with ehr_id exists and has a directory with a complex structure
...         (contains subfolders and items).
...
...     Flow:
...         1. Invoke the get directory service for the ehr_id
...         2. The service should return the full structure of the complex
...            directory for the EHR
...
...     Postconditions:
...         None


Resource    ${CURDIR}${/}../../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/contribution_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/directory_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/template_opt1.4_keywords.robot
Resource    ${CURDIR}${/}../../_resources/keywords/ehr_keywords.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 2: get directory on EHR with complex directory structure and items

    create EHR
        
        TRACE GITHUB ISSUE  43  not-ready
        ...             message=DISCOVERED ERROR: PGobject cannot be cast to com.nedap.archie.rm

    create DIRECTORY (JSON)    subfolders_in_directory_with_details_items.json

    get DIRECTORY at time (JSON)  time

    validate GET version@time response - 200 retrieved
