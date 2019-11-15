*** Settings ***
Documentation    Alternative flow 3: has path on non-existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the has path service for a random ehr_id and path
...         2. The service should return an error, related to the EHR that doesn't exist
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
Alternative flow 3: has path on non-existent EHR

    create fake EHR

    get FOLDER in DIRECTORY at version - fake version_uid/path (JSON)

    validate GET-@version response - 404 unknown ehr_id
