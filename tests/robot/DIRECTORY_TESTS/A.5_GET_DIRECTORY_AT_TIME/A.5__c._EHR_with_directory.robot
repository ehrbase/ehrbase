*** Settings ***
Documentation    Alternative flow 2: get directory at time on EHR with directory
...
...     Preconditions:
...         An EHR with ehr_id exists and has directory with one version.
...
...     Flow:
...         1. Invoke the get directory at time service for the ehr_id and current time
...         2. The service should return the current directory
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
Alternative flow 2: get directory at time on EHR with directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    get DIRECTORY at current time (JSON)

    validate GET-version@time response - 200 retrieved
