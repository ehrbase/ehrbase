*** Settings ***
Documentation    Alternative flow 6: get directory at time on EHR with directory with multiple versions first version
...
...     Preconditions:
...         An EHR with ehr_id and has directory with two versions.
...
...     Flow:
...         1. Invoke the get directory at time service for the ehr_id and a time
...            AFTER the first version of the directory was created, but
...            BEFORE the second version was created (update)
...         2. The service should return the first version of the directory
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
Alternative flow 6: get directory at time on EHR with directory with multiple versions first version

    create EHR

    create DIRECTORY (JSON)    empty_directory.json

    update DIRECTORY (JSON)    subfolders_in_directory_with_details_items.json

    get DIRECTORY at time (JSON)    ${time_of_first_version}

    validate GET-version@time response - 200 retrieved
