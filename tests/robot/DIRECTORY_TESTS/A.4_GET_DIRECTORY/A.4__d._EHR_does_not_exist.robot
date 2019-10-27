*** Settings ***
Documentation    Alternative flow 3: get directory on non-existent EHR
...
...     Preconditions:
...         None
...
...     Flow:
...         1. Invoke the get directory service for a random ehr_id
...         2. The service should return an error related with the non existent EHR
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

Force Tags    refactor



*** Test Cases ***
Alternative flow 3: get directory on non-existent EHR

    create fake EHR

        Log  TO CLARIFY WITH @PPAZOS: use `GET /ehr/ehr_id/directory` OR `GET /ehr/ehr_id/directory/\${version_uid}` here?
        ...  level=WARN

    get DIRECTORY - fake ehr_id (JSON)

    # check response: is negative - EHR does not exist

    validate GET-@version response - 404 unknown ehr_id