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
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags   353    not-ready



*** Test Cases ***
Alternative flow 2: get directory on EHR with complex directory structure and items

    create EHR
    create DIRECTORY (JSON)    subfolders_in_directory_with_details_items.json
    get DIRECTORY at time (JSON)    ${time_of_first_version}

        TRACE GITHUB ISSUE  353  bug

    validate GET-version@time response - 200 retrieved
