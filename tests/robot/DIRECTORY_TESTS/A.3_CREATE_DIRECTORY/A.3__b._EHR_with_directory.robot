*** Settings ***
Documentation    Alternative flow 1: create directory on EHR with directory
...
...     Preconditions:
...         An EHR with ehd_id exists, and has directory.
...
...     Flow:
...         1. Invoke the create directory service for the ehr_id
...            w/ same directory name that already exists.
...         2. The service should return an error, 
...            related to already existing EHR directory
...
...     Postconditions:
...         None


Resource    ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Alternative flow 1: create directory on EHR with directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json
    validate POST response - 201 created directory

    create DIRECTORY (JSON)    subfolders_in_directory.json
    validate POST response - 409 folder already exists
