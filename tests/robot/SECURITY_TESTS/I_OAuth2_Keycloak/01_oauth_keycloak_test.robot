# Copyright (c) 2020 Wladislaw Wagner (www.trustincode.de) / (Vitasystems GmbH).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Metadata    Version    0.1.0
Metadata    Author    *Wladislaw Wagner* www.trustincode.de
Metadata    Created    2020.07.10
Metadata    Updated    2021.11.12

Documentation  Testing authentication with Keycloak OAuth Server

Resource        ../../_resources/suite_settings.robot



*** Test Cases ***
01 Keycloak OAuth server is online
    [Documentation]     Checks that Keycloak server is up and ready.

                    Create Session    keycloak    ${KEYCLOAK_URL}
    ${response}     Get Request    keycloak    /
                    Should Be Equal As Strings 	  ${response.status_code}    200


02 Master realm exists
        REST.GET   ${KEYCLOAK_URL}/realms/master
        Integer    response status   200
        Object     response body
        String     response body public_key


03 Ehrbase realm exists
        REST.GET   ${KEYCLOAK_URL}/realms/ehrbase
        Integer    response status   200
        Object     response body
        String     response body public_key
    

04 Access token with role in realm_access field is retrievable
        # NOTE: ${OAUTH_ACCESS_GRANT} comes from variables file: sut_config.py
        Request Access Token    ${OAUTH_ACCESS_GRANT}
        Status Code    200
        Set Suite Variable    ${password_access_token}    ${response.json()['access_token']}
        Access Token length is greater than    100    ${password_access_token}
        Decode JWT Password Access Token


05 Access token with role in scope field is retrievable
        Request Access Token    ${client_credentials_grant}
        Status Code    200
        Set Suite Variable    ${client_credentials_access_token}    ${response.json()['access_token']}
        Access Token length is greater than    100    ${client_credentials_access_token}
        Decode JWT Client Credentials Access Token


06 Base URL is secured
    [Documentation]     Checks private resource is NOT accessible without auth.
                        Create Session    secured    ${BASEURL}
    ${response}         Get Request    secured    /
                        Should Be Equal As Strings 	  ${response.status_code}    401


07 API endpoints are secured
    [Documentation]     Checks private resources are NOT accessible without auth.
    # EHR /EHR_STATUS
        REST.GET        ${BASEURL}/ehr
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123
        Integer         response status    401

        REST.POST       ${BASEURL}/ehr
        Integer         response status    401

        REST.PUT        ${BASEURL}/ehr
        Integer         response status    401

        REST.PUT        ${BASEURL}/ehr/123
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/ehr_status
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/ehr_status/123
        Integer         response status    401

        REST.PUT        ${BASEURL}/ehr/123/ehr_status
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_ehr_status
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_ehr_status/revision_history
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_ehr_status/version
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_ehr_status/version/123
        Integer         response status    401

    # COMPOSITION
        REST.POST       ${BASEURL}/ehr/123/composition
        Integer         response status    401

        REST.PUT        ${BASEURL}/ehr/123/composition/123
        Integer         response status    401

        REST.DELETE     ${BASEURL}/ehr/123/composition/123
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/composition/123
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/composition/123?version_at_time=111
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_composition/123
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_composition/123/revision_history
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_composition/123/version/123
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/versioned_composition/123/version?version_at_time=222
        Integer         response status    401

    # DIRECTORY
        REST.POST       ${BASEURL}/ehr/123/directory
        Integer         response status    401

        REST.PUT        ${BASEURL}/ehr/123/directory
        Integer         response status    401

        REST.DELETE    ${BASEURL}/ehr/123/directory
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/directory/123
        Integer         response status    401

        REST.GET        ${BASEURL}/ehr/123/directory
        Integer         response status    401

    # CONTRIBUTION
        REST.GET        ${BASEURL}/ehr/123/contribution/123
        Integer         response status    401

        REST.POST       ${BASEURL}/ehr/123/contribution
        Integer         response status    401


08 Private resources are NOT available with invalid/expired token
        Set Headers     { "Authorization": "Bearer ${expired_token}" }
        REST.GET        ${BASEURL}/ehr
                        Output
        Integer         response status    401


09 Private resources are available with valid token when using password grant
        Set Headers     { "Authorization": "Bearer ${password_access_token}" }
        REST.GET        ${BASEURL}/ehr/cd05e77d-63f8-4074-9937-80c4d4406bff
                        Output
        Integer         response status    404


10 Private resources are available with valid token when using client credentials grant
        Set Headers     { "Authorization": "Bearer ${client_credentials_access_token}" }
        REST.GET        ${BASEURL}/ehr/cd05e77d-63f8-4074-9937-80c4d4406bff
                        Output
        Integer         response status    404


# 7) Private resources are available after auth
#    This is tested by reusing existing tests with changed settings on the CI 
#    For details check .circleci/config.yml --> search "SECURITY-test"





*** Keywords ***
Request Access Token
    [Arguments]         ${grant}
                        Create Session    keycloak   ${KEYCLOAK_URL}   verify=${False}    debug=3
    &{headers}=         Create Dictionary    Content-Type=application/x-www-form-urlencoded
    ${resp}=            POST On Session    keycloak   /realms/ehrbase/protocol/openid-connect/token   expected_status=anything
                        ...             data=${grant}   headers=${headers}
                        Set Test Variable    ${response}    ${resp}

Status Code
    [Arguments]         ${expected}
                        Should Be Equal As Strings 	${response.status_code}    ${expected}


Access Token length is greater than
    [Arguments]         ${expected}    ${token}
    ${length} = 	    Get Length   ${token}
                        Should be true     ${length} > ${expected}


Decode JWT Password Access Token
    &{decoded_token}=   decode token      ${password_access_token}
                        Log To Console    \nNAME: ${decoded_token.name}
                        Log To Console    EMAIL: ${decoded_token.email}
                        Log To Console    USERNAME: ${decoded_token.preferred_username}
                        Log To Console    CLIENT: ${decoded_token.azp}
                        Log To Console    ROLES: ${decoded_token.realm_access.roles}
                        Log To Console    \nDECODED TOKEN: ${decoded_token}
                        Dictionary Should Contain Item    ${decoded_token}    typ   Bearer


Decode JWT Client Credentials Access Token
    &{decoded_token}=   decode token      ${client_credentials_access_token}
                        Log To Console    \nCLIENT: ${decoded_token.azp}
                        Log To Console    SCOPE: ${decoded_token.scope}
                        Log To Console    \nDECODED TOKEN: ${decoded_token}
                        Dictionary Should Contain Item    ${decoded_token}    typ   Bearer



*** Variables ***
${expired_token}               eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJSSXc4WUN0MGdVZGtJU2VOSVA3SUhLSVlsMzU5cVBJcWNxb004azBFOU9ZIn0.eyJleHAiOjE2MzY3MzYwMTUsImlhdCI6MTYzNjczNTcxNSwianRpIjoiNTNjNDQ3NTktNTYwNC00ZWNjLWI4NjktNDZlYjg4MjdiZjY3IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgyL2F1dGgvcmVhbG1zL2VocmJhc2UiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiYjU4M2RhNjgtOTg5Zi00Mjk3LWE4OWMtYjQzN2M1MjhkZDEzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZWhyYmFzZS1yb2JvdCIsInNlc3Npb25fc3RhdGUiOiIwZjI1NGE5Zi0zMDNiLTQ1MDgtYjE3Ny04NjJmOTlhNjc4NzgiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImVocmJhc2Uub3JnL3VzZXIiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwiZWhyYmFzZS5vcmcvYWRtaW5pc3RyYXRvciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJSb2JvdCBGcmFtZXdvcmsiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJyb2JvdCIsImdpdmVuX25hbWUiOiJSb2JvdCIsImZhbWlseV9uYW1lIjoiRnJhbWV3b3JrIiwiZW1haWwiOiJyb2JvdEBlaHJiYXNlLm9yZyJ9.D1bfUldXErEZEuooyhUNo9jpQAJ9VmZJnejCfFOshcMx573NBUYFHILsU-R-twprct-XuO8TdPW6PyBDeF1SFpFIyq8RhUvNLxjCBUPGas2FxovQ2d_P5pWL86vu7zk0IIm5nSrawqq4UzZ0rwTEP116YsJIHkdG89MVBzolmHkVnFN8ervisLmGy-xxhB_OLeRl-SRdn6oRxH1msVReeKmBv42OKdRLhLkaKefO8Hs_kl8VgE8UdBDwlg40m3S78p-hSmd9vdQ1XPwg2l5bklk0dBJsD_2mBM6Wfq5qnbG-u28oKQ-JmAj0Px_OWa2lLMGut-_Rnv95NAjnmaOV3g

# Client ID and secret come from the 'ehrbase-custom-user' client defined in Keycloak
${client_credentials_grant}    grant_type=client_credentials&client_id=ehrbase-custom-user&client_secret=5d49493b-8bfb-47f9-aa0f-43653370bf6f