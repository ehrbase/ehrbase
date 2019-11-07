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
Documentation   Composition Integration Tests

Resource    ${CURDIR}${/}../../_resources/keywords/composition_keywords.robot

# Resource    ${CURDIR}${/}../_resources/suite_settings.robot
# Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
# Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot
# Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot



Force Tags    JSON



*** Test Cases ***
Main flow get existing COMPOSITION latest

    upload OPT      minimal/minimal_observation.opt

    create EHR

    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.xml

    update composition (JSON)   minimal/minimal_observation.composition.participations.extdatetimes.v2.xml
    check composition update succeeded

    get composition - latest version
    check content of compositions latest version (JSON)

    [Teardown]    restart SUT



    # TODO: check with PABLO what to do with this

    # FIXME: this is for testing version_at_time
    # # test try to use the uid of the first version but the current time for version_at_time
    # ${date}=      Get Current Date    result_format=%Y-%m-%dT%H:%M:%S
    #               Log To Console      ${date}
    # # this is the real versioned_object_uid not the version_uid !!!
    # ${versioned_object_uid}   Fetch From Left   ${compo_version_uid}    ::
    #               Log To Console    ${versioned_object_uid}
    # &{params}=    Create Dictionary   version_at_time=${date}
    # ${resp}=      Get Request   ${SUT}   /ehr/${ehr_id}/composition/${versioned_object_uid}   #params=${params}
    # # The response is JSON
    # Log To Console    ${resp.request.headers}
    # Log To Console    ${resp.request.path_url}
    # Log To Console    ${resp.content}
    #
    # #Log To Console    ${resp.content}
    # Should Be Equal As Strings   ${resp.status_code}   200
    #Log To Console    ${resp.json()['uid']['value']}
