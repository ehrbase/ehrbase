*** Settings ***
Documentation    Main flow: create directory on empty EHR
...
...     Preconditions:
...         An EHR with ehr_id exists and doesn't have directory
...
...     Flow:
...         1. Invoke the create directory service for a random ehr_id
...         2. The service should return a a positive result related with the
...            directory just created for the EHR
...
...     Postconditions:
...         The EHR ehr_id has directory


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
Main flow: create directory on empty EHR

    create EHR

    create DIRECTORY (JSON)    empty_directory.json

    validate POST response - 201 created
