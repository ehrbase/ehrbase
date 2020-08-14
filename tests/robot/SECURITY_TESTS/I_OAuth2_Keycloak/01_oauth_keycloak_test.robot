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
Metadata    Updated    2020.07.14

Documentation  Testing authentication with Keycloak OAuth Server

Resource        ${EXECDIR}/robot/_resources/suite_settings.robot



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
    

04 Access Token is retrievable
        Request Access Token
        Status Code    200
        Access Token length is greater than    100
        Decode JWT Access Token

05 Base URL is secured
    [Documentation]     Checks private resource is NOT accessible without auth.
                        Create Session    secured    ${BASEURL}
    ${response}         Get Request    secured    /
                        Should Be Equal As Strings 	  ${response.status_code}    401

06 API endpoints are secured
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


07 Private resources are NOT available with invalid/expired token
        Set Headers     { "Authorization": "Bearer ${expired_token}" }
        REST.GET        ${BASEURL}/ehr
                        Output
        Integer         response status    401


# 7) Private resources are available after auth
#    This is tested by reusing existing tests with changed settings on the CI 
#    For details check .circleci/config.yml --> search "SECURITY-test"





*** Keywords ***
Request Access Token
                        Create Session    keycloak   ${KEYCLOAK_URL}   verify=${False}    debug=3
    &{headers}=         Create Dictionary    Content-Type=application/x-www-form-urlencoded
    ${resp}=            Post Request    keycloak   /realms/ehrbase/protocol/openid-connect/token
                        ...             data=${OAUTH_ACCESS_GRANT}   headers=${headers}
                        # NOTE: ${OAUTH_ACCESS_GRANT} comes from variables file: sut_config.py
                        Set Test Variable    ${response}    ${resp}


Status Code
    [Arguments]         ${expected}
                        Should Be Equal As Strings 	${response.status_code}    ${expected}


Access Token length is greater than
    [Arguments]         ${expected}
    ${length} = 	    Get Length   ${response.json()['access_token']}
                        Should be true     ${length} > ${expected}
                        Set Suite Variable    ${access_token}    ${response.json()['access_token']}
                        # Log To Console    ${access_token}


Decode JWT Access Token
    &{decoded_token}=   decode token      ${ACCESS_TOKEN}
                        Log To Console    \nNAME: ${decoded_token.name}
                        Log To Console    EMAIL: ${decoded_token.email}
                        Log To Console    USERNAME: ${decoded_token.preferred_username}
                        Log To Console    CLIENT: ${decoded_token.azp}
                        Log To Console    ROLES: ${decoded_token.realm_access.roles}
                        Log To Console    \nDECODED TOKEN: ${decoded_token}
                        Dictionary Should Contain Item    ${decoded_token}    typ   Bearer





***Variables***
${expired_token}        eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJSSXc4WUN0MGdVZGtJU2VOSVA3SUhLSVlsMzU5cVBJcWNxb004azBFOU9ZIn0.eyJleHAiOjE1OTQ4NDEyODcsImlhdCI6MTU5NDg0MDk4NywianRpIjoiMTM0NGJlZmItZDMwZi00ZTQ3LWI1MWQtYjRmMThlOWY2NjU1IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgxL2F1dGgvcmVhbG1zL2VocmJhc2UiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiYjU4M2RhNjgtOTg5Zi00Mjk3LWE4OWMtYjQzN2M1MjhkZDEzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZWhyYmFzZS1yb2JvdCIsInNlc3Npb25fc3RhdGUiOiJiNzI3ZDI5Ny1jNmE5LTQ4MGItYjYxMi1jMzQ4ZmYwMjE5MjAiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iLCJBZG1pbiIsInVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiUm9ib3QgRnJhbWV3b3JrIiwicHJlZmVycmVkX3VzZXJuYW1lIjoicm9ib3QiLCJnaXZlbl9uYW1lIjoiUm9ib3QiLCJmYW1pbHlfbmFtZSI6IkZyYW1ld29yayIsImVtYWlsIjoicm9ib3RAZWhyYmFzZS5vcmcifQ.AqnaJrFVGOZjuDPPGaNpY1bwSC5i0gLrRicwy2gc6w_FjoGGTyWcVWhd_Krt5WZKxpRarycy4RYfzCniSjo5UgLCZzmkwo_RiBOTUuTVV1uGj3EHKIRXaSRCECRi7RMlQsIGIKXF61BnDAQtleB0RQIhMFbGnQUclVqXDFj8F7fp-FloucV2lOBpK92_x1NRh46shZAvSjoGgjwyLlZI7EJgpPT4HNIQElE5Gc4j8MmzRZpAoYXcj7uqlSRqhvWN1XunGulo9YWmCrEJNSf066aUxF1q7329YSpTL_PlNSg85ceZ16r5vd1uWQqUouzAhUy7LXcRvZS8HkJurZ26ng




# &{AUTH_DEV}             client_id=ehrbase-client  username=ehrbase  password=ehrbase  grant_type=password  scope=openid
# &{AUTH_TEST}            client_id=ehrbase-robot  username=robot  password=robot  grant_type=password  scope=openid
# &{HEADER}               Content-Type=application/x-www-form-urlencoded
# ${KEYCLOAK_URL}         http://localhost:8081/auth
# ${KC_AUTH_URL}          http://localhost:8081/auth/realms/ehrbase/protocol/openid-connect/auth
# ${KC_ACCESS_TOKEN_URL}  http://localhost:8081/auth/realms/ehrbase/protocol/openid-connect/token

# ${BASE_URL}             http://127.0.0.1:8080/ehrbase/rest/openehr/v1
# ${SECURITY_AUTHTYPE}    OAUTH2

# ${SUT}                  TEST    # DEFAULT

# # comment: local test environment: for development
# &{DEV}                  URL=http://localhost:8080/ehrbase/rest/openehr/v1
# ...                     HEARTBEAT=http://localhost:8080/ehrbase/
# ...                     CREDENTIALS=@{devcreds}
# ...                     BASIC={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
# ...                     OAUTH2={"Authorization": "Bearer ${{ (requests.request("POST", "http://localhost:8081/auth/realms/ehrbase/protocol/openid-connect/token", headers=$HEADER, data=$AUTH_DEV)).json()["access_token"] }}"}
# ...                     NODENAME=local.ehrbase.org
# ...                     CONTROL=manual
# @{devcreds}             ehrbase-user    SuperSecretPassword

# # comment: testing environment: used on CI pipeline
# &{TEST}                 URL=http://localhost:8080/ehrbase/rest/openehr/v1
# ...                     HEARTBEAT=http://localhost:8080/ehrbase/
# ...                     CREDENTIALS=@{testcreds}
# ...                     BASIC={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
# ...                     OAUTH2={"Authorization": "Bearer ${{ (requests.request("POST", "http://localhost:8081/auth/realms/ehrbase/protocol/openid-connect/token", headers=$HEADER, data=$AUTH_TEST)).json()["access_token"] }}"}
# ...                     NODENAME=local.ehrbase.org
# ...                     CONTROL=docker
# @{testcreds}            ehrbase-user    SuperSecretPassword

# ${BASEURL}              ${${SUT}.URL}
# ${HEARTBEAT_URL}        ${${SUT}.HEARTBEAT}
# ${AUTHORIZATION}        ${${SUT}.${SECURITY_AUTHTYPE}}
# ${CREATING_SYSTEM_ID}   ${${SUT}.NODENAME}
# ${CONTROL_MODE}         ${${SUT}.CONTROL}