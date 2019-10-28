*** Settings ***
Documentation    Alternative flow 1: create directory on EHR with directory
...
...     Preconditions:
...         An EHR with ehd_id exists, and has directory.
...
...     Flow:
...         1. Invoke the create directory service for the ehr_id
...         2. The service should return an error, related to the EHR directory
...            already existin
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
Alternative flow 1: create directory on EHR with directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    validate POST response - 201 created

    create DIRECTORY (JSON)    subfolders_in_directory.json

    validate POST response - 409 folder already exists
