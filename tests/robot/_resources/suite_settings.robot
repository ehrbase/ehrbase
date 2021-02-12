# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
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

Documentation   General setting for OpenEHR test suites.
# Metadata    Version        1.0

Library     REST    ${BASE_URL}    #ssl_verify=false
Library     RequestsLibrary  WITH NAME  R
Library     String
Library     Collections
Library     OperatingSystem
Library     Process
Library     XML
Library     JSONLibrary
Library     DateTime

Library     ${EXECDIR}/robot/_resources/libraries/dockerlib.py
Library     ${EXECDIR}/robot/_resources/libraries/jsonlib.py
Library     ${EXECDIR}/robot/_resources/libraries/token_decoder.py

Resource    ${EXECDIR}/robot/_resources/keywords/generic_keywords.robot
Resource    ${EXECDIR}/robot/_resources/keywords/aql_query_keywords.robot
Resource    ${EXECDIR}/robot/_resources/keywords/composition_keywords.robot
Resource    ${EXECDIR}/robot/_resources/keywords/contribution_keywords.robot
Resource    ${EXECDIR}/robot/_resources/keywords/db_keywords.robot
Resource    ${EXECDIR}/robot/_resources/keywords/directory_keywords.robot
Resource    ${EXECDIR}/robot/_resources/keywords/ehr_keywords.robot
Resource    ${EXECDIR}/robot/_resources/keywords/template_opt1.4_keywords.robot
Variables   ${EXECDIR}/robot/_resources/variables/sut_config.py
            ...    ${SUT}    ${AUTH_TYPE}    ${NODOCKER}



*** Variables ***
# ${hip_baseurl_v1}     http://localhost:8080/ehrbase/rest/ecis/v1
# ${template_id}    IDCR%20-%20Immunisation%20summary.v0        # TODO: @wlad rm if nothing breaks
# ${invalid_ehr_id}    123
${BASE_URL}              http://localhost:8080/ehrbase/rest/openehr/v1
${PROJECT_ROOT}          ${EXECDIR}${/}..
${POM_FILE}              ${PROJECT_ROOT}${/}pom.xml
${CREATING_SYSTEM_ID}    ${NODENAME}
${SMOKE_TEST_PASSED}     ${TRUE}

${SUT}                   TEST    # Switch System Under Test (SUT). Check tests/README.md for details.
${CODE_COVERAGE}         False
${NODOCKER}              False
${AUTH_TYPE}             BASIC
${REDUMP_REQUIRED}       ${FALSE}
${ALLOW-TEMPLATE-OVERWRITE}    ${TRUE}
${CACHE-ENABLED}         ${TRUE}


# # local test environment: for development
# &{DEV}                  URL=http://localhost:8080/ehrbase/rest/openehr/v1
# ...                     HEARTBEAT=http://localhost:8080/ehrbase/
# ...                     CREDENTIALS=@{devcreds}
# ...                     BASIC={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
# ...                     OAUTH2=${sec_auth_type}
#                         # comment: nodename is actually "CREATING_SYSTEM_ID" and can be set from cli
#                         #          when starting server .jar
#                         #          i.e. java -jar application.jar --server.nodename=some.foobar.baz
#                         #          EHRbase's default is local.ehrbase.org
# ...                     NODENAME=local.ehrbase.org
# ...                     CONTROL=manual
# @{devcreds}             ehrbase-user    SuperSecretPassword

# # testing environment: used on CI pipeline
# &{TEST}                 URL=http://localhost:8080/ehrbase/rest/openehr/v1
# ...                     HEARTBEAT=http://localhost:8080/ehrbase/
# ...                     CREDENTIALS=@{testcreds}
# ...                     BASIC={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
# ...                     OAUTH=FUCK YOU TOO
# ...                     NODENAME=local.ehrbase.org
# ...                     CONTROL=docker
# @{testcreds}            ehrbase-user    SuperSecretPassword

# # staging environment
# &{STAGE}                URL=http://localhost:8080/ehrbase/rest/openehr/v1
# ...                     HEARTBEAT=http://localhost:8080/ehrbase/
# ...                     CREDENTIALS=@{stagecreds}
# ...                     BASIC={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
# ...                     OAUTH2={"Authorization": "Bearer 1234"}
# ...                     NODENAME=stage.ehrbase.org
# ...                     CONTROL=docker
# @{stagecreds}           username    password

# # pre production environment
# &{PREPROD}              URL=http://localhost:8080/ehrbase/rest/openehr/v1
# ...                     HEARTBEAT=http://localhost:8080/ehrbase/
# ...                     CREDENTIALS=@{preprodcreds}
# ...                     BASIC={"Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="}
# ...                     OAUTH2={"Authorization": "Bearer 1234"}
# ...                     NODENAME=preprod.ehrbase.org
# ...                     CONTROL=docker
# @{preprodcreds}         username    password

# # NOTE: for this configuration to work the following environment variables
# #       have to be available:
# #       BASIC_AUTH (basic auth string for EHRSCAPE, i.e.: export BASIC_AUTH="Basic abc...")
# #       EHRSCAPE_USER
# #       EHRSCAPE_PASSWORD
# &{EHRSCAPE}             URL=https://rest.ehrscape.com/rest/openehr/v1
# ...                     HEARTBEAT=https://rest.ehrscape.com/
# ...                     CREDENTIALS=@{scapecreds}
# ...                     BASIC_AUTH={"Authorization": "%{BASIC_AUTH}"}
# ...                     NODENAME=piri.ehrscape.com
# ...                     CONTROL=NONE
# @{scapecreds}           %{EHRSCAPE_USER}    %{EHRSCAPE_PASSWORD}



# ${BASEURL}              ${${SUT}.URL}
# ${HEARTBEAT_URL}        ${${SUT}.HEARTBEAT}
# ${AUTHORIZATION}        ${${SUT}.${SECURITY_AUTHTYPE}}
# ${CREATING_SYSTEM_ID}   ${${SUT}.NODENAME}
# ${CONTROL_MODE}         ${${SUT}.CONTROL}

# ${HEARTBEAT_URL}        ${HEARTBEAT}
# ${AUTHORIZATION}        ${SECURITY_AUTHTYPE}
# ${CREATING_SYSTEM_ID}   ${NODENAME}
# ${CONTROL_MODE}         ${CONTROL}



*** Keywords ***
