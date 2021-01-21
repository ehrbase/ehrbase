*** Settings ***
Documentation    Alternative flow 4: get directory at time on EHR with directory with multiple versions
...
...     Preconditions:
...         An EHR with ehr_id exists and has directory with two versions.
...
...     Flow:
...         1. Invoke the get directory at time service for the ehr_id and current time
...         2. The service should return the current latest directory
...
...     Postconditions:
...         None
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags    353    not-ready



*** Test Cases ***
Alternative flow 4: get directory at time on EHR with directory with multiple versions

    create EHR
    create DIRECTORY (JSON)    empty_directory.json
    update DIRECTORY (JSON)    subfolders_in_directory_with_details_items.json
    get DIRECTORY at current time (JSON)

        TRACE GITHUB ISSUE  353  bug
    
    validate GET-version@time response - 200 retrieved
