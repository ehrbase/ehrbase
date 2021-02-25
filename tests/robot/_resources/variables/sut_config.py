# Copyright (c) 2020 Wladislaw Wagner (www.trustincode.de), (Vitasystems GmbH).
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


from requests import request


# KEYCLOAK SETTINGS
HEADER = {"Content-Type": "application/x-www-form-urlencoded"}
KEYCLOAK_URL = "http://localhost:8081/auth"
KC_AUTH_URL = KEYCLOAK_URL + "/realms/ehrbase/protocol/openid-connect/auth"
KC_ACCESS_TOKEN_URL = KEYCLOAK_URL + "/realms/ehrbase/protocol/openid-connect/token"
KC_JWT_ISSUERURI = KEYCLOAK_URL + "/realms/ehrbase"


# SUT CONFIGURATIONS
"""
CONFIG              SUT STARTUP AUTOMATED?      COMMENT
------              ----------------------      -------

DEV                 no                          manually start ehrbase, db
DEV-OAUTH           no                          manually start ehrbase, db, keycloak
TEST                yes
TEST-OAUTH          partly                      manually start keycloak
ADMIN-DEV           no                          manually start ehrbase, db
ADMIN-DEV-OAUTH     manual                      manually start ehrbase, db, keycloak
ADMIN-TEST          yes
ADMIN-TEST-OAUTH    partly                      manually start keycloak
"""

# dev environment: for local development
# requires manual startup of EHRbase and DB
DEV_CONFIG = {
    "SUT": "DEV",
    "BASEURL": "http://localhost:8080/ehrbase/rest/openehr/v1",
    "HEARTBEAT_URL": "http://localhost:8080/ehrbase/",
    "CREDENTIALS": ["ehrbase-user", "SuperSecretPassword"],
    "SECURITY_AUTHTYPE": "BASIC",
    "AUTHORIZATION": {
        "Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="
    },
    # NOTE: nodename is actually "CREATING_SYSTEM_ID"
    #       and can be set from cli when starting server .jar, i.e.:
    #       `java -jar application.jar --server.nodename=some.foobar.baz`
    #       EHRbase's default is local.ehrbase.org
    "NODENAME": "manual.execution.org",  # CREATING_SYSTEM_ID
    "CONTROL_MODE": "manual",
    "OAUTH_ACCESS_GRANT": {
        "client_id": "ehrbase-client",
        "scope": "openid",
        "username": "ehrbase",
        "password": "ehrbase",
        "grant_type": "password",
    },
    "JWT_ISSUERURI": KC_JWT_ISSUERURI,
    "OAUTH_NAME": "Ehr Base",
    "OAUTH_EMAIL": "ehrbase@ehrbase.org",
    "ACCESS_TOKEN": None,
    "KEYCLOAK_URL": KEYCLOAK_URL,
    "KC_AUTH_URL": KC_AUTH_URL,
    "KC_ACCESS_TOKEN_URL": KC_ACCESS_TOKEN_URL,
}

# admin-dev environment: for local test of admin interface
# requires manual startup of EHRbase and DB
ADMIN_DEV_CONFIG = {
    "SUT": "ADMIN-DEV",
    "BASEURL": "http://localhost:8080/ehrbase/rest/openehr/v1",
    "HEARTBEAT_URL": "http://localhost:8080/ehrbase/",
    "CREDENTIALS": ["ehrbase-admin", "EvenMoreSecretPassword"],
    "SECURITY_AUTHTYPE": "BASIC",
    "AUTHORIZATION": {
        "Authorization": "Basic ZWhyYmFzZS1hZG1pbjpFdmVuTW9yZVNlY3JldFBhc3N3b3Jk"
    },
    "NODENAME": "local.ehrbase.org",  # CREATING_SYSTEM_ID
    "CONTROL_MODE": "manual",
    "OAUTH_ACCESS_GRANT": {
        "client_id": "ehrbase-client",
        "scope": "openid",
        "username": "admin-robot",  # TODO: recreate exported-keycloak-config to have this user!
        "password": "admin-robot",  #       check README.md in SECURITY_TESTS folder for how to
        "grant_type": "password",
    },
    "JWT_ISSUERURI": KC_JWT_ISSUERURI,
    "OAUTH_NAME": "Admin Ehr Base",
    "OAUTH_EMAIL": "admin-ehrbase@ehrbase.org",
    "ACCESS_TOKEN": None,
    "KEYCLOAK_URL": KEYCLOAK_URL,
    "KC_AUTH_URL": KC_AUTH_URL,
    "KC_ACCESS_TOKEN_URL": KC_ACCESS_TOKEN_URL,
}

# test environment: used on CI pipeline, can be used locally, too
# handles startup/shutdown of EHRbase and DB automatically
TEST_CONFIG = {
    "SUT": "TEST",
    "BASEURL": "http://localhost:8080/ehrbase/rest/openehr/v1",
    "HEARTBEAT_URL": "http://localhost:8080/ehrbase/",
    "CREDENTIALS": ["ehrbase-user", "SuperSecretPassword"],
    "SECURITY_AUTHTYPE": "BASIC",
    "AUTHORIZATION": {
        "Authorization": "Basic ZWhyYmFzZS11c2VyOlN1cGVyU2VjcmV0UGFzc3dvcmQ="
    },
    "NODENAME": "local.ehrbase.org",  # alias CREATING_SYSTEM_ID
    "CONTROL_MODE": "docker",
    "OAUTH_ACCESS_GRANT": {
        "client_id": "ehrbase-robot",
        "scope": "openid",
        "username": "robot",
        "password": "robot",
        "grant_type": "password",
    },
    "JWT_ISSUERURI": KC_JWT_ISSUERURI,
    "OAUTH_NAME": "Robot Framework",
    "OAUTH_EMAIL": "robot@ehrbase.org",
    "ACCESS_TOKEN": None,
    "KEYCLOAK_URL": KEYCLOAK_URL,
    "KC_AUTH_URL": KC_AUTH_URL,
    "KC_ACCESS_TOKEN_URL": KC_ACCESS_TOKEN_URL,
}

# admin-test environment: used on CI to test admin interface, can be used locally, too
# handles startup/shutdown of EHRbase and DB automatically
ADMIN_TEST_CONFIG = {
    "SUT": "ADMIN-TEST",
    "BASEURL": "http://localhost:8080/ehrbase/rest/openehr/v1",
    "HEARTBEAT_URL": "http://localhost:8080/ehrbase/",
    "CREDENTIALS": ["ehrbase-admin", "EvenMoreSecretPassword"],
    "SECURITY_AUTHTYPE": "BASIC",
    "AUTHORIZATION": {
        "Authorization": "Basic ZWhyYmFzZS1hZG1pbjpFdmVuTW9yZVNlY3JldFBhc3N3b3Jk"
    },
    "NODENAME": "local.ehrbase.org",  # alias CREATING_SYSTEM_ID
    "CONTROL_MODE": "docker",
    "OAUTH_ACCESS_GRANT": {
        "client_id": "ehrbase-robot",
        "scope": "openid",
        "username": "admin-robot",  # TODO: recreate exported-keycloak-config to have this user!
        "password": "admin-robot",  #       check README.md in SECURITY_TESTS folder for how to
        "grant_type": "password",
    },
    "JWT_ISSUERURI": KC_JWT_ISSUERURI,
    "OAUTH_NAME": "Admin Robot Framework",
    "OAUTH_EMAIL": "admin-robot@ehrbase.org",
    "ACCESS_TOKEN": None,
    "KEYCLOAK_URL": KEYCLOAK_URL,
    "KC_AUTH_URL": KC_AUTH_URL,
    "KC_ACCESS_TOKEN_URL": KC_ACCESS_TOKEN_URL,
}


# # NOTE: for this configuration to work the following environment variables
# #       have to be available:
# #       BASIC_AUTH (basic auth string for EHRSCAPE, i.e.:
# #                   export BASIC_AUTH="Basic abc...")
# #       EHRSCAPE_USER
# #       EHRSCAPE_PASSWORD
# &{EHRSCAPE}             URL=https://rest.ehrscape.com/rest/openehr/v1
# ...                     HEARTBEAT=https://rest.ehrscape.com/
# ...                     CREDENTIALS=@{scapecreds}
# ...                     BASIC_AUTH={"Authorization": "%{BASIC_AUTH}"}
# ...                     NODENAME=piri.ehrscape.com
# ...                     CONTROL=NONE
# @{scapecreds}           %{EHRSCAPE_USER}    %{EHRSCAPE_PASSWORD}


def get_variables(sut="TEST", auth_type="BASIC", nodocker="NEIN!"):
    # DEV CONFIG W/ OAUTH
    if (
        sut == "DEV"
        and auth_type == "OAUTH"
        or (auth_type == "OAUTH" and (nodocker.upper() in ["TRUE", ""]))
    ):
        DEV_CONFIG["SECURITY_AUTHTYPE"] = "OAUTH"
        DEV_CONFIG["ACCESS_TOKEN"] = request(
            "POST",
            KC_ACCESS_TOKEN_URL,
            headers=HEADER,
            data=DEV_CONFIG["OAUTH_ACCESS_GRANT"],
        ).json()["access_token"]
        DEV_CONFIG["AUTHORIZATION"] = {
            "Authorization": "Bearer " + DEV_CONFIG["ACCESS_TOKEN"]
        }
        return DEV_CONFIG

    # ADMIN-DEV CONFIG W/ OAUTH
    if (
        sut == "ADMIN-DEV"
        and auth_type == "OAUTH"
        or (auth_type == "OAUTH" and (nodocker.upper() in ["TRUE", ""]))
    ):
        ADMIN_DEV_CONFIG["SECURITY_AUTHTYPE"] = "OAUTH"
        ADMIN_DEV_CONFIG["ACCESS_TOKEN"] = request(
            "POST",
            KC_ACCESS_TOKEN_URL,
            headers=HEADER,
            data=ADMIN_DEV_CONFIG["OAUTH_ACCESS_GRANT"],
        ).json()["access_token"]
        ADMIN_DEV_CONFIG["AUTHORIZATION"] = {
            "Authorization": "Bearer " + ADMIN_DEV_CONFIG["ACCESS_TOKEN"]
        }
        return ADMIN_DEV_CONFIG

    # TEST CONFIG W/ OAUTH
    if sut == "TEST" and auth_type == "OAUTH":
        TEST_CONFIG["SECURITY_AUTHTYPE"] = "OAUTH"
        TEST_CONFIG["ACCESS_TOKEN"] = request(
            "POST",
            KC_ACCESS_TOKEN_URL,
            headers=HEADER,
            data=TEST_CONFIG["OAUTH_ACCESS_GRANT"],
        ).json()["access_token"]
        TEST_CONFIG["AUTHORIZATION"] = {
            "Authorization": "Bearer " + TEST_CONFIG["ACCESS_TOKEN"]
        }
        return TEST_CONFIG

    # ADMIN-TEST CONFIG W/ OAUTH
    if sut == "ADMIN-TEST" and auth_type == "OAUTH":
        ADMIN_TEST_CONFIG["SECURITY_AUTHTYPE"] = "OAUTH"
        ADMIN_TEST_CONFIG["ACCESS_TOKEN"] = request(
            "POST",
            KC_ACCESS_TOKEN_URL,
            headers=HEADER,
            data=TEST_CONFIG["OAUTH_ACCESS_GRANT"],
        ).json()["access_token"]
        ADMIN_TEST_CONFIG["AUTHORIZATION"] = {
            "Authorization": "Bearer " + ADMIN_TEST_CONFIG["ACCESS_TOKEN"]
        }
        return ADMIN_TEST_CONFIG

    # ADMIN-TEST CONFIG W/ BASIC AUTH
    if sut == "ADMIN-TEST":
        return ADMIN_TEST_CONFIG

    # DEV CONFIG W/ BASIC AUTH
    if sut == "DEV" or (nodocker.upper() in ["TRUE", ""]):
        return DEV_CONFIG

    # if nodocker.upper() in ["TRUE", ""]:
    #     return DEV_CONFIG

    # ADMIN-DEV CONFIG W/ BASIC AUTH
    if sut == "ADMIN-DEV" or (
        sut == "ADMIN-DEV" and (nodocker.upper() in ["TRUE", ""])
    ):
        return ADMIN_DEV_CONFIG

    # TEST CONFIG W/ BASIC AUTH
    else:
        return TEST_CONFIG
