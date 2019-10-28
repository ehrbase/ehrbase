*** Settings ***
Documentation    Alternative flow 1: get directory at version from existent EHR that has two versions of directory
...
...     Preconditions:
...         An EHR with known ehr_id exists in the server, has two versions of directory.
...
...     Flow:
...         1. Invoke the GET DIRECTORY AT VERSION service for the ehr_id and
...            the version_uid of the FIRST version of directory
...         2. The service should return the first version of the directory
...         3. Invoke the GET DIRECTORY AT VERSION service for the ehr_id and
...            the version_uid of the SECOND version of directory
...         4. The service should return the second version of the directory
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
Alternative flow 1: get directory at version from existent EHR that has two versions of directory

    create EHR

    create DIRECTORY (JSON)    subfolders_in_directory.json
    validate POST response - 201 created

    get DIRECTORY at version (JSON)
    validate GET-@version response - 200 retrieved
    

    update DIRECTORY (JSON)    subfolders_in_directory_with_details.json
    validate PUT response - 200 updated

    get DIRECTORY at version (JSON)
    # TODO: check that it is the SECOND version
    validate GET-@version response - 200 retrieved
