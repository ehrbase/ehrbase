*** Settings ***
Documentation    Alternative flow 7: get directory at time on non existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the get directory at time service for a random ehr_id and current time
...         2. The service should return an error about the non existent EHR
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
Alternative flow 7: get directory at time on non existent EHR

    create fake EHR

    get DIRECTORY at current time (JSON)

    validate GET-version@time response - 404 unknown ehr_id
