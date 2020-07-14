*** Settings ***
Documentation    Alternative flow 1: get versioned directory from existent EHR that has two versions of directory
...
...     Preconditions:
...         An EHR with known ehr_id exists in the server, has two versions of directory.
...
...     Flow:
...
...         1. Invoke the get versioned directory service for the ehr_id
...         2. The service should return the versioned folder and should have two versions
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
Alternative flow 1: get versioned directory from existent EHR that has two versions of directory

    create EHR
    create DIRECTORY (JSON)    empty_directory.json
    update DIRECTORY (JSON)    subfolders_in_directory.json
    get DIRECTORY at version (JSON)
    validate GET-@version response - 200 retrieved    root

    # TODO: @WLAD implement check for the second step in flow:
    #       " ... and should have two versions"
