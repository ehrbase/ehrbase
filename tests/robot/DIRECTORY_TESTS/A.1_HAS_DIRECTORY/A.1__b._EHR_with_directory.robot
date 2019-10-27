*** Settings ***
Documentation    Alternative flow 1: has directory on existing EHR with directory
...
...     Preconditions:
...         An EHR with known ehr_id exists and has directory.
...
...     Flow:
...         1. Invoke the has DIRECTORY service for the ehr_id
...         2. The result must be true
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
Alternative flow 1: has directory on existing EHR with directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json

    # TODO: TOCLARIFY WITH @PABLO
    #       WHICH ENDPOINT SHOULD BE USER IN THIS CASE?
    #       GET /ehr/{ehr_id}/directory/${version_uid}
    #   OR  GET /ehr/{ehr_id}/directory
    get DIRECTORY (JSON)

    # check response: is positive    # TODO: implement some data checks

    validate GET-@version response - 200 retrieved
