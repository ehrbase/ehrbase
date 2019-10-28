*** Settings ***
Documentation    Alternative flow 2: delete directory from non-existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the get directory service for a random ehr_id
...         2. The service should return an error related to the non existent EHR
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
Alternative flow 2: delete directory from non-existent EHR

    create fake EHR

    delete DIRECTORY - fake ehr_id (JSON)

    validate DELETE response - 404 unknown ehr_id
