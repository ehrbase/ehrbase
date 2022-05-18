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
Documentation    COMPOSITION Specific Keywords

Resource   ../suite_settings.robot
Resource    ehr_keywords.robot
Resource    template_opt1.4_keywords.robot



*** Variables ***
${VALID DATA SETS}     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}valid_templates
${INVALID DATA SETS}   ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}invalid_templates
${COMPO DATA SETS}     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}compositions



*** Keywords ***
# 1) High Level Keywords
#
# 2) HTTP Methods
#    POST
#    PUT
#    DELETE
#    GET
#
# 3) HTTP Headers
#
# 4) FAKE Data

create fake composition
    generate random composition_uid


# TODO: rename to `generate random versioned_object_uid`
generate random composition_uid
    [Documentation]     Generates a random UUIDv4 spec conform `versioned_object_uid`,
    ...                 an OpenEHR spec conform `version_uid` (alias `preceding_version_uid`).

    ${uid}=             Evaluate    str(uuid.uuid4())    uuid
                        Set Test Variable   ${composition_uid}    ${uid}    # TODO: remove
                        Set Test Variable   ${versioned_object_uid}    ${uid}
                        Set Test Variable   ${version_uid}    ${uid}::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${preceding_version_uid}    ${version_uid}


generate random version_uid
    [Documentation]     Generates a random COMPOSITION `version_uid` and exposes it
    ...                 also as `preceding_version_uid` to test level scope

    ${uid}=             Evaluate    str(uuid.uuid4())    uuid
                        Set Test Variable   ${version_uid}    ${uid}::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${preceding_version_uid}    ${version_uid}


commit invalid composition (JSON)
    [Arguments]         ${composition_json}
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 DEPENDENCY: `upload OPT`, `create EHR`
    ...
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

                        # TODO: FIX ME! should be 'get invalid compo file'
                        get invalid OPT file    ${composition_json}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation

    ${resp}=            POST On Session     ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   400


commit invalid composition (XML)
    [Arguments]         ${composition_xml}
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 DEPENDENCY: `upload OPT`, `create EHR`
    ...
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

                        # TODO: FIX ME! Should be 'get invalid compo file'
                        get invalid OPT file    ${composition_xml}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation

    ${resp}=            POST On Session     ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   400


commit composition - no referenced OPT
    [Arguments]         ${composition}
    [Documentation]     Creates a new COMPOSITION with missing referenced OPT
    ...                 DEPENDENCY: `create EHR`, `prepare new request session` with proper args!!!
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

                        # TODO: FIX ME! Rename KW properly
                        get valid OPT file  ${composition}

    ${resp}=            POST On Session       ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        # log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   422


commit composition - no referenced EHR
    [Arguments]         ${composition}
    [Documentation]     Creates a new COMPOSITION with missing referenced EHR
    ...                 DEPENDENCY: `create EHR`, `prepare new request session` with proper args!!!
    ...                             e.g. content-type must be application/xml
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

                        get valid OPT file  ${composition}

                        prepare new request session    XML    Prefer=return=representation

    ${resp}=            POST On Session     ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   404


commit composition (JSON)
    [Arguments]         ${json_composition}
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 DEPENDENCY: `upload OPT`, `create EHR`
    ...
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

                        # TODO: FIX ME! rename/replace KW
                        get valid OPT file  ${json_composition}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation

    ${resp}=            POST On Session     ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   201

                        Set Test Variable   ${composition_uid}    ${resp.json()['uid']['value']}    # TODO: remove
                        Set Test Variable   ${version_uid}    ${resp.json()['uid']['value']}    # full/long compo uid
                        Set Test Variable   ${version_uid_v1}    ${version_uid}                  # different namesfor full uid
                        Set Test Variable   ${preceding_version_uid}    ${version_uid}          # for usage in other steps

    ${short_uid}=       Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${compo_uid_v1}    ${short_uid}                      # TODO: rmv
                        Set Test Variable   ${versioned_object_uid}    ${short_uid}

                        Set Test Variable   ${response}    ${resp}
                        capture point in time    1


commit composition without accept header
    [Arguments]         ${composition}
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 DEPENDENCY: `upload OPT`, `create EHR`
    ...
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

                        # TODO: FIX ME! replace KW
                        get valid OPT file  ${composition}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Prefer=return=representation

    ${resp}=            POST On Session     ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   201

                        Set Test Variable   ${composition_uid}    ${resp.json()['uid']['value']}    # TODO: remove
                        Set Test Variable   ${version_uid}    ${resp.json()['uid']['value']}    # full/long compo uid
                        Set Test Variable   ${version_uid_v1}    ${version_uid}                  # different namesfor full uid
                        Set Test Variable   ${preceding_version_uid}    ${version_uid}          # for usage in other steps

    ${short_uid}=       Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${compo_uid_v1}    ${short_uid}                      # TODO: rmv
                        Set Test Variable   ${versioned_object_uid}    ${short_uid}

                        Set Test Variable   ${response}    ${resp}
                        capture point in time    1


check content of composition (JSON)
                        # Should Be Equal As Strings    ${response.status_code}    200
    ${text}=            Set Variable        ${response.json()['content'][0]['data']['events'][0]['data']['items'][0]['value']['value']}
                        Should Be Equal     ${text}    original value


commit composition (XML)
    [Arguments]         ${xml_composition}
    [Documentation]     POST /ehr/${ehr_id}/composition

                        # TODO: FIX ME! replace KW
                        get valid OPT file  ${xml_composition}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/xml
                        ...                 Prefer=return=representation

    ${resp}=            POST On Session     ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   201

    ${xresp}=           Parse Xml           ${resp.text}
                        Log Element         ${xresp}
                        # Log Element        ${xresp}  xpath=composition

    ${long_uid}=        Get Element         ${xresp}      uid/value
                        Set Test Variable   ${composition_uid}    ${long_uid.text}          # TODO: rmv
                        Set Test Variable   ${version_uid}    ${long_uid.text}                  # full/long compo uid
                        Set Test Variable   ${version_uid_v1}    ${version_uid}                  # different namesfor full uid
                        Set Test Variable   ${preceding_version_uid}    ${version_uid}          # for usage in other steps

    ${short_uid}=       Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${compo_uid_v1}    ${short_uid}                 # TODO; rmv
                        Set Test Variable   ${versioned_object_uid}    ${short_uid}

                        Set Test Variable   ${response}    ${resp}
                        capture point in time    1


check content of composition (XML)
                        # Should Be Equal As Strings    ${response.status_code}    200
    ${xml}=             Parse Xml           ${response.text}
    # ${text}=            Get Element         ${xml}    composition/content/data/events/data/items/value/value     # This works, but is not spec conform!  TODO: remove
    ${text}=            Get Element         ${xml}    content/data/events/data/items/value/value
                        Element Text Should Be    ${text}    original value

commit same composition again
    [Arguments]         ${opt_file}
    [Documentation]     Commits a COMPOSITION a second time
    ...                 DEPENDENCY: `commit composition (JSON/XML)`
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition

                        get valid OPT file  ${opt_file}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation

        TRACE GITHUB ISSUE  125  bug

    ${resp}=            POST On Session     ${SUT}   /ehr/${ehr_id}/composition   expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   400


commit composition
    [Arguments]         ${format}   ${composition}
    ...         ${need_template_id}=true   ${prefer}=representation
    ...         ${lifecycle}=complete    ${extTemplateId}=false
    [Documentation]     Creates the first version of a new COMPOSITION
    ...                 DEPENDENCY: `upload OPT`, `create EHR`
    ...
    ...                 ENDPOINT: POST /ehr/${ehr_id}/composition
    ...
    ...                 FORMAT VARIABLES: FLAT, TDD, STRUCTURED, CANONICAL_JSON, CANONICAL_XML

    @{template}=        Split String    ${composition}   __
    ${template}=        Get From List   ${template}      0

    Set Suite Variable    ${template_id}    ${template}

    ${file}=           Get File   ${COMPO DATA SETS}/${format}/${composition}

    &{headers}=        Create Dictionary   Prefer=return=${prefer}
    ...                openEHR-VERSION.lifecycle_state=${lifecycle}

    IF    '${need_template_id}' == 'true'
        Set To Dictionary   ${headers}   openEHR-TEMPLATE_ID=${template}
    END

    IF   '${format}'=='CANONICAL_JSON'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/json
        Set To Dictionary   ${headers}   Accept=application/json    
    ELSE IF   '${format}'=='CANONICAL_XML'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/xml
        Set To Dictionary   ${headers}   Accept=application/xml
    ELSE IF   '${format}'=='FLAT'
        Set To Dictionary   ${headers}   Content-Type=application/json
        Set To Dictionary   ${headers}   Accept=application/json
        Set To Dictionary   ${headers}   X-Forwarded-Host=example.com
        Set To Dictionary   ${headers}   X-Forwarded-Port=333
        Set To Dictionary   ${headers}   X-Forwarded-Proto=https
        IF  '${extTemplateId}' == 'true'
            ${template_id}      Set Variable   ${externalTemplate}
            ${template}      Set Variable   ${externalTemplate}
            Set Suite Variable    ${template_id}    ${template}
        END
        &{params}       Create Dictionary     format=FLAT   ehrId=${ehr_id}     templateId=${template_id}
        Create Session      ${SUT}    ${ECISURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
    ELSE IF   '${format}'=='TDD'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/openehr.tds2+xml
        Set To Dictionary   ${headers}   Accept=application/openehr.tds2+xml
    ELSE IF   '${format}'=='STRUCTURED'
        Create Session      ${SUT}    ${BASEURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
        Set To Dictionary   ${headers}   Content-Type=application/openehr.wt.structured+json
        Set To Dictionary   ${headers}   Accept=application/openehr.wt.structured+json
    END

    IF          '${format}'=='FLAT'
        ${resp}     POST On Session     ${SUT}   composition   params=${params}
        ...     expected_status=anything   data=${file}   headers=${headers}
    ELSE
        ${resp}     POST On Session     ${SUT}   /ehr/${ehr_id}/composition
        ...     expected_status=anything   data=${file}   headers=${headers}
    END

    Set Test Variable   ${response}     ${resp}
    Set Test Variable   ${format}       ${format}
    Set Test Variable   ${template}     ${template}


    capture point in time    1


check the successful result of commit composition
    [Arguments]         ${template_for_path}=null
    [Documentation]     Checks result of commit new composition if the result is successful
    ...                 DEPENDENCY: `commit composition`

    Should Be Equal As Strings   ${response.status_code}   201

    ${Location}   Set Variable    ${response.headers}[Location]

    IF   '${format}' != 'FLAT'
    ${ETag}       Get Substring   ${response.headers}[ETag]    1    -1
    END

    IF  '${format}' == 'CANONICAL_JSON'
        ${compositionUid}=   Set Variable   ${response.json()}[uid][value]
        ${template_id}=       Set Variable   ${response.json()}[archetype_details][template_id][value]
        ${composer}           Set Variable   ${response.json()}[composer][name]
        # @ndanilin: EhrBase don't return context for persistent composition.
        #            It seems to us that it's wrong so a setting check is disabled yet.
        # ${setting}            Set variable   ${response.json()}[context][setting][value]
        Set Test Variable     ${compositionUid}  ${composition_uid}
    ELSE IF   '${format}' == 'CANONICAL_XML'
        ${xresp}=             Parse Xml             ${response.text}
        ${compositionUid}=   Get Element Text      ${xresp}   uid/value
        ${template_id}=       Get Element Text      ${xresp}   archetype_details/template_id/value
        ${composer}=          Get Element Text      ${xresp}   composer/name
        # @ndanilin: EhrBase don't return context for persistent composition.
        #            It seems to us that it's wrong so a setting check is disabled yet.        
        # ${setting}=           Get Element Text      ${xresp}   context/setting/value
        Set Test Variable     ${compositionUid}  ${composition_uid}
    ELSE IF   '${format}' == 'FLAT'
        # ${composition_uid}    Set Variable   ${response.json()}[${template_for_path}/_uid]
        # @ndanilin: in FLAT response isn't template_id so make a following placeholder:
        ${template_id}=       Set Variable   ${template}
        #${composer}           Set Variable   ${response.json()}[${template_for_path}/composer|name]
        #${setting}            Set variable   ${response.json()}[${template_for_path}/context/setting|value]
        ${compositionUid}=    Collections.Get From Dictionary    ${response.json()}    compositionUid
        Set Test Variable     ${compositionUid}  ${composition_uid}
    ELSE IF   '${format}' == 'TDD'
        ${xresp}=             Parse Xml                 ${response.text}
        ${composition_uid}=   Get Element Text          ${xresp}   uid/value
        ${template_id}=       Get Element Attribute     ${xresp}   template_id
        ${composer}=          Get Element Text          ${xresp}   composer/name
        ${setting}=           Get Element Text      ${xresp}   context/setting/value 
    ELSE IF   '${format}' == 'STRUCTURED'
        ${composition_uid}    Set Variable   ${response.json()}[${template_for_path}][_uid][0]
        # @ndanilin: in STRUCTURED response isn't template_id so make a following placeholder:
        ${template_id}=       Set Variable   ${template}
        ${composer}           Set Variable   ${response.json()}[${template_for_path}][composer][0][|name]
        ${setting}            Set variable   ${response.json()}[${template_for_path}][context][0][setting][0][|value]
    END

    IF   '${format}' != 'FLAT'
    Should Be Equal    ${ETag}            ${composition_uid}
    # @ndanilin: EhrBase returns in header 'Location' wrong data so this check is disabled yet:
    #            - not baseUrl but ipv6
    #            - composition uid without system_id and version
    # Should Be Equal    ${Location}        ${BASEURL}/ehr/${ehr_id}/composition/${composition_uid}
    Should Be Equal    ${template_id}     ${template}
    Should Be Equal    ${composer}        composer test value
    # @ndanilin: EhrBase don't return context for persistent composition.
    #            It seems to us that it's wrong so a setting check is disabled yet.    
    # Should Be Equal    ${setting}         other care
    END
        
check status_code of commit composition
    [Arguments]    ${status_code}
    Should Be Equal As Strings   ${response.status_code}   ${status_code}
    

update composition (JSON)
    [Arguments]         ${new_version_of_composition}   ${file_type}=xml
    [Documentation]     Commit a new version for the COMPOSITION
    ...                 DEPENDENCY: `commit composition (JSON/XML)` keyword
    ...                 ENDPOINT: PUT /ehr/${ehr_id}/composition/${versioned_object_uid}

    IF      '${file_type}' == 'xml'
        get valid OPT file  ${new_version_of_composition}
        &{headers}          Create Dictionary   Content-Type=application/xml
                            ...                 Accept=application/json
                            ...                 Prefer=return=representation
                            ...                 If-Match=${preceding_version_uid}
        ${resp}             PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${compo_uid_v1}   data=${file}   expected_status=anything   headers=${headers}
                            log to console      ${resp.content}
                            Set Test Variable   ${composition_uid_v2}    ${resp.json()['uid']['value']}    # TODO: remove
                            Set Test Variable   ${version_uid_v2}    ${resp.json()['uid']['value']}

        ${short_uid}        Remove String       ${version_uid_v2}    ::${CREATING_SYSTEM_ID}::1
                            Set Test Variable   ${versioned_object_uid_v2}    ${short_uid}

                            Set Test Variable   ${response}    ${resp}
                            capture point in time    2
    END

    IF      '${file_type}' == 'json'
        ${file}=           Get File   ${COMPO DATA SETS}/${format}/${new_version_of_composition}
        &{headers}          Create Dictionary   Content-Type=application/json
                            ...                 Accept=application/json
                            ...                 Prefer=return=representation
                            ...                 If-Match=${composition_uid}
        ${composition_id}        Remove String       ${composition_uid}    ::${CREATING_SYSTEM_ID}::1
        &{params}          Create Dictionary     ehr_id=${ehr_id}   composition_id=${composition_id}
        ${resp}             PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${composition_id}
        ...                 data=${file}   headers=${headers}     params=${params}
                            log to console      ${resp.content}
                            Set Test Variable   ${composition_uid_v2}    ${resp.json()['uid']['value']}    # TODO: remove
                            Set Test Variable   ${version_uid_v2}    ${resp.json()['uid']['value']}

        ${short_uid}        Remove String       ${version_uid_v2}    ::${CREATING_SYSTEM_ID}::1
                            Set Test Variable   ${versioned_object_uid_v2}    ${short_uid}

                            Set Test Variable   ${response}    ${resp}
                            capture point in time    2
    END


update composition (FLAT)
    [Arguments]         ${new_version_of_composition}
    [Documentation]     Commit a new version for the COMPOSITION
    ...                 DEPENDENCY: `commit composition' keyword

    ${file}=           Get File   ${COMPO DATA SETS}/${format}/${new_version_of_composition}

    &{headers}=         Create Dictionary   Content-Type=application/json
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation

    &{params}=          Create Dictionary     format=FLAT   ehrId=${ehr_id}  templateId=${template_id}

    ${resp}=            PUT On Session      ${SUT}   composition/${composition_uid}    params=${params}  expected_status=anything   data=${file}   headers=${headers}
                        log to console      ${resp.content}
                        Set Test Variable   ${response}    ${resp}
                        capture point in time    2

    Should Be Equal As Strings    ${response.status_code}    200


update composition - is modifiable false (JSON)
    [Arguments]         ${new_version_of_composition}
    [Documentation]     Commit a new version for the COMPOSITION
    ...                 DEPENDENCY: `commit composition (JSON/XML)` keyword
    ...                 ENDPOINT: PUT /ehr/${ehr_id}/composition/${versioned_object_uid}

    get valid OPT file  ${new_version_of_composition}
    &{headers}          Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation
                        ...                 If-Match=${preceding_version_uid}
    ${resp}             PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${compo_uid_v1}   data=${file}   expected_status=anything   headers=${headers}
                        log to console      ${resp.content}
                        Set Test Variable   ${response}     ${resp.content}

update composition - invalid opt reference (JSON)
    [Arguments]         ${new_version_of_composition}
    [Documentation]     Commit a new version for the COMPOSITION but with wrong OPT reference.
    ...                 DEPENDENCY: `commit composition (JSON/XML)` keyword
    ...                 ENDPOINT: PUT /ehr/${ehr_id}/composition/${versioned_object_uid}

                        # TODO: @WLAD rename to "get invalid compo dataset    ${new_version_of_composition}"
                        #       when refactoring this resource file!
                        #       ALL compo dataset should be moved into proper test_data_sets/ subfolders.
                        #       At the moment they are all (valid/invalid) in  "valid_templates" !!!
                        get valid OPT file  ${new_version_of_composition}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation
                        ...                 If-Match=${preceding_version_uid}

    ${resp}=            PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${compo_uid_v1}   data=${file}   expected_status=anything   headers=${headers}
                        Log To Console      \nREQUEST HEADERS:\n${resp.request.headers}
                        Log To Console      \nRESPONSE:\n${resp.content}
                        Set Test Variable   ${response}    ${resp}


update composition - invalid opt reference (XML)
    [Arguments]         ${new_version_of_composition}
    [Documentation]     Commit a new version for the COMPOSITION but with wrong OPT reference.
    ...                 DEPENDENCY: `commit composition (JSON/XML)` keyword
    ...                 ENDPOINT: PUT /ehr/${ehr_id}/composition/${versioned_object_uid}

                        get valid OPT file  ${new_version_of_composition}
    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/xml
                        ...                 Prefer=return=representation
                        ...                 If-Match=${preceding_version_uid}

    ${resp}=            PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${compo_uid_v1}   data=${file}   expected_status=anything   headers=${headers}
                        Log To Console      \nREQUEST HEADERS:\n${resp.request.headers}
                        Log To Console      \nRESPONSE:\n${resp.content}
                        Set Test Variable   ${response}    ${resp}


check composition update succeeded
    [Documentation]     the uids without the version should be the same
    ...                 DEPENDENCY: `update composition (JSON/XML)` keyword

                        Should Be Equal As Strings    ${response.status_code}   200

    ${compo_uid_v1}=    Get Substring       ${composition_uid}    0    -1       # TODO: rmv
    ${compo_uid_v2}=    Get Substring       ${composition_uid_v2}    0    -1    # TODO: rmv
                        Should Be Equal     ${compo_uid_v1}    ${compo_uid_v2}  # TODO: rmv

    ${uid_v1}=          Get Substring       ${version_uid_v1}    0    -1
    ${uid_v2}=          Get Substring       ${version_uid_v2}    0    -1
                        Should Be Equal     ${uid_v1}    ${uid_v2}


check content of updated composition (JSON)
                        Should Be Equal As Strings    ${response.status_code}    200
    ${text}=            Set Variable    ${response.json()['content'][0]['data']['events'][0]['data']['items'][0]['value']['value']}
                        Should Be Equal     ${text}    modified value

check content of updated composition generic (JSON)
    [Documentation]     Get text from response, based on path provided as argument1.
    ...                 Argument2 is the expected value.
    ...                 Applicable for response in JSON format.
    [Arguments]         ${pathToLookInto}   ${expectedVal}
    @{expectedStatusCodesList}      Create List     200     201
                        ${string_status_code}    Convert To String    ${response.status_code}
                        List Should Contain Value   ${expectedStatusCodesList}      ${string_status_code}
                        #Should Be Equal As Strings    ${response.status_code}    200
    ${text}             Set Variable    ${response.json()${pathToLookInto}}
                        Should Be Equal     ${text}    ${expectedVal}

update composition (XML)
    [Arguments]         ${new_version_of_composition}
    [Documentation]     Commit a new version for the COMPOSITION
    ...                 DEPENDENCY: `commit composition (JSON/XML)` keyword
    ...                 PUT /ehr/${ehr_id}/composition/${versioned_object_uid}

                        get valid OPT file  ${new_version_of_composition}
    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/xml
                        ...                 Prefer=return=representation
                        ...                 If-Match=${preceding_version_uid}   # TODO: must be ${preceding_version_uid} - has same format as `version_uid`
    ${resp}=            PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${compo_uid_v1}   data=${file}   expected_status=anything   headers=${headers}
                        log to console      ${resp.content}

    # compo.uid.value has the version_uid
    ${xresp}=           Parse Xml           ${resp.text}

    ${long_uid}=        Get Element         ${xresp}      uid/value

                        Set Test Variable   ${composition_uid_v2}     ${long_uid.text}    # TODO: remove
                        Set Test Variable   ${version_uid_v2}     ${long_uid.text}

    ${short_uid}=       Remove String       ${version_uid_v2}    ::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${versioned_object_uid_v2}    ${short_uid}

                        Set Test Variable   ${response}    ${resp}
                        capture point in time    2


check content of updated composition (XML)
                        Should Be Equal As Strings    ${response.status_code}    200
    ${xml}=             Parse Xml           ${response.text}

    ${text}=            Get Element         ${xml}    content/data/events/data/items/value/value
    # ${text}=            Get Element         ${xml}    composition/content/data/events/data/items/value/value    # TODO: remove

                        Element Text Should Be    ${text}    modified value


update non-existent composition (JSON)
    [Arguments]         ${new_version_of_composition}
    [Documentation]     Commit a new version for a non-existent COMPOSITION
    ...                 DEPENDENCY: `generate random composition uid(s)` keyword
    ...                 ENDPOINT: PUT /ehr/${ehr_id}/composition/${versioned_object_uid}

                        get valid OPT file  ${new_version_of_composition}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/json
                        ...                 Prefer=return=representation

                        ...                 If-Match=${preceding_version_uid}
    ${resp}=            PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${versioned_object_uid}   data=${file}   expected_status=anything    headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   404


update non-existent composition (XML)
    [Arguments]         ${new_version_of_composition}
    [Documentation]     Commit a new version for a non-existent COMPOSITION
    ...                 DEPENDENCY: `generate random composition uid(s)` keyword
    ...                 ENDPOINT: PUT /ehr/${ehr_id}/composition/${versioned_object_uid}

                        get valid OPT file  ${new_version_of_composition}

    &{headers}=         Create Dictionary   Content-Type=application/xml
                        ...                 Accept=application/xml
                        ...                 Prefer=return=representation

                        ...                 If-Match=${preceding_version_uid}
    ${resp}=            PUT On Session         ${SUT}   /ehr/${ehr_id}/composition/${versioned_object_uid}   data=${file}   expected_status=anything   headers=${headers}
                        log to console      ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   404


# TODO: rename keyword properly e.g. by version_uid
get composition by composition_uid
    [Arguments]         ${uid}
    [Documentation]     :uid: version_uid
    ...                 DEPENDENCY: `prepare new request session` with proper Headers
    ...                     e.g. Content-Type=application/xml  Accept=application/xml  Prefer=return=representation
    ...                     and `commit composition (JSON/XML)` keywords

    # the uid param in the doc is verioned_object.uid but is really the version.uid,
    # because the response from the create compo has this endpoint in the Location header

    ${resp}=            GET On Session         ${SUT}    /ehr/${ehr_id}/composition/${uid}    expected_status=anything   headers=${headers}
                        log to console      ${resp.content}
                        Set Test Variable   ${response}    ${resp}

# TODO: rename keyword properly e.g. by version_uid
(FLAT) get composition by composition_uid
    [Arguments]         ${uid}
    [Documentation]     :uid: version_uid
    ...                 DEPENDENCY: `prepare new request session` with proper Headers
    ...                     e.g. Content-Type=application/xml  Accept=application/xml  Prefer=return=representation
    ...                     and `commit composition (JSON/XML)` keywords

    # the uid param in the doc is verioned_object.uid but is really the version.uid,
    # because the response from the create compo has this endpoint in the Location header
    &{params}=          Create Dictionary     format=FLAT
    Create Session      ${SUT}    ${ECISURL}    debug=2
        ...                 auth=${CREDENTIALS}    verify=True
    ${resp}=            GET On Session         ${SUT}  composition/${uid}  params=${params}  expected_status=anything   headers=${headers}
                        log to console      ${resp.content}
                        Set Test Variable   ${response}    ${resp}


Get Web Template By Template Id (ECIS)
    [Arguments]         ${template_id}

    Create Session      ${SUT}    ${ECISURL}    debug=2
    ...                 auth=${CREDENTIALS}    verify=True
    ${resp}=            GET On Session         ${SUT}  template/${template_id}  expected_status=anything   headers=${headers}
                        log to console      ${resp.content}
                        Set Test Variable   ${response}    ${resp}
                        Should Be Equal As Strings   ${resp.status_code}   200

Get Example Of Web Template By Template Id (ECIS)
    [Arguments]         ${template_id}      ${responseFormat}

    Create Session      ${SUT}    ${ECISURL}    debug=2
    ...                 auth=${CREDENTIALS}    verify=True
    &{params}          Create Dictionary      format=${responseFormat}
    ${headers}         Create Dictionary      Accept=application/json
    ...                                       Content-Type=application/xml
    ...                                       Prefer=return=representation
    IF      '${responseFormat}' != 'FLAT'
            ${resp}            GET On Session         ${SUT}
                        ...     template/${template_id}/example  expected_status=anything   headers=${headers}
                        ...     params=${params}
    ELSE
            ${resp}            GET On Session         ${SUT}
                        ...     template/${template_id}/example  expected_status=anything   headers=${headers}
    END
                        log to console      ${resp.content}
                        Set Test Variable   ${response}    ${resp}
                        Status Should Be    200

Get Example Of Web Template By Template Id (OPENEHR)
    [Arguments]         ${template_id}      ${responseFormat}

    Create Session      ${SUT}    ${baseurl}    debug=2
    ...                 auth=${CREDENTIALS}    verify=True
    &{params}          Create Dictionary     format=${responseFormat}
    ${headers}         Create Dictionary     Accept=application/json
    ...                                      Content-Type=application/xml
    ...                                      Prefer=return=representation
    IF      '${responseFormat}' == 'JSON'
            ${resp}            GET On Session         ${SUT}
                                ...     definition/template/adl1.4/${template_id}/example  expected_status=anything
                                ...     headers=${headers}      params=${params}
    ELSE IF      '${responseFormat}' == 'XML'
            ${headers}         Create Dictionary     Accept=application/xml
            ...                                      Content-Type=application/xml
            ...                                      Prefer=return=representation
            ${resp}            GET On Session         ${SUT}
                                ...     definition/template/adl1.4/${template_id}/example
                                ...     expected_status=anything        headers=${headers}
    ELSE
             ${resp}            GET On Session         ${SUT}
                                ...     definition/template/adl1.4/${template_id}/example  expected_status=anything   headers=${headers}
    END
                        log to console      ${resp.content}
                        Set Test Variable   ${response}    ${resp}
                        Status Should Be    200

Validate Response Body Has Format
    [Documentation]     Check if response body contains representation in format
    ...                 provided as argument.
    ...                 Expected format can be JSON or XML.
    ...                 If format is not provided, JSON is default expected format.
    ...                 Dependencies:
    ...                 - `get example of web template by template id (BASE)`
    ...                 or
    ...                 - `get example of web template by template id (ECIS)`
    [Arguments]         ${expectedFormat}=JSON
                        IF          '${expectedFormat}' == 'JSON'
                            ${templateName}     Get Value From Json     ${response.json()}
                            ...     name.value
                            log to console     ${templateName}
                        ELSE IF     '${expectedFormat}' == 'XML'
                            ${xml}     Parse Xml        ${response.text}
                            Set Test Variable       ${responseXML}      ${xml}
                        ELSE
                            #log to console      ${response.text}
                            Should Contain      ${response.text}    family_history/category|terminology
                        END

Get All Web Templates
    Create Session      ${SUT}    ${ECISURL}    debug=2
    ...                 auth=${CREDENTIALS}    verify=True
    ${resp}=            GET On Session          ${SUT}  template  expected_status=anything   headers=${headers}
                        log to console          ${resp.content}
                        Set Test Variable       ${response}    ${resp}
                        Status Should Be        200
                        ${xml}     Parse Xml        ${response.text}
                        Set Suite Variable      ${xml}


Check If Get Templates Response Has
    [Arguments]         @{templatesIDList}
    [Documentation]     Verify in Get Template response if templates are present.
    ...                 DEPENDENCY: `get all web templates`
    ...                 Argument to be provided as list of 1 or n elements.
    ...                 Example: check if get templates response has    ${template1}    ${template2}
    log to console      ${XML}
    @{responseTemplates}        Get Elements Texts      ${xml}      templates/templates/template_id
    FOR     ${template}     IN      @{templatesIDList}
        List Should Contain Value   ${responseTemplates}    ${template}
    END


get versioned composition by uid
    [Arguments]         ${format}    ${uid}
    [Documentation]     :uid: versioned_object_uid
    ...                 DEPENDENCY: `prepare new request session`
    ...                     and `commit composition (JSON/XML)` keywords
    ...                 format: JSON or XML for accept/content headers
    ...                 ENDPOINT: /ehr/${ehr_id}/versioned_composition/${versioned_object_uid}

                        prepare new request session    ${format}

    ${resp}=            GET On Session         ${SUT}    /ehr/${ehr_id}/versioned_composition/${uid}    expected_status=anything   headers=${headers}
                        log to console      ${resp.content}
                        Set Test Variable   ${response}    ${resp}


# Note: uses REST.GET
get versioned composition of EHR by UID
    [Arguments]         ${uid}
    [Documentation]     Gets versioned composition with given uid.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_id` e.g.
    ...                             - `create new EHR`
    ...                             - `generate random ehr_id`
    ...                             and creation of composition, giving its ID as argument

    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/versioned_composition/${uid}
                        ...         headers={"Accept": "application/json"}
                        Set Test Variable    ${response}    ${resp}


get revision history of versioned composition of EHR by UID
    [Arguments]         ${uid}
    [Documentation]     Gets revision history of versioned composition with given uid.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_id` e.g.
    ...                             - `create new EHR`
    ...                             - `generate random ehr_id`
    ...                             and creation of composition, giving its ID as argument

    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/versioned_composition/${uid}/revision_history
                        ...         headers={"Accept": "application/json"}
                        Set Test Variable    ${response}    ${resp}


get version of versioned composition of EHR by UID and time
    [Arguments]         ${uid}
    [Documentation]     Gets composition with given UID by time.
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_id` e.g.
    ...                             - `create new EHR`
    ...                             - `generate random ehr_id`
    ...                 Input: `query` variable containing query parameters as object or directory (e.g. _limit=2 for [$URL]?_limit=2)
    ...                 which can be empty too

    # Trick to see if ${query} was set. (if not, "Get Variale Value" will set the value to None)
    ${query} = 	Get Variable Value 	${query}
    # Only run the GET with query if $query was set
    Run Keyword Unless 	$query is None 	internal get version of versioned composition of EHR by UID and time with query    ${uid}
    Run Keyword If 	$query is None 	internal get version of versioned composition of EHR by UID and time without query    ${uid}


get version of versioned composition of EHR by UID
    [Arguments]         ${versioned_object_uid}    ${version_uid}
    [Documentation]     Gets composition with given version UID
    ...                 DEPENDENCY: `prepare new request session` and keywords that
    ...                             create and expose an `ehr_id` e.g.
    ...                             - `create new EHR`
    ...                             - `generate random ehr_id`
    ...                             and creation of composition, giving its ID as argument

    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/versioned_composition/${versioned_object_uid}/version/${version_uid}
                        ...         headers={"Accept": "application/json"}
                        Set Test Variable    ${response}    ${resp}


# internal only, do not call from outside. use "get version of versioned composition of EHR by UID and time" instead
internal get version of versioned composition of EHR by UID and time with query
    [Arguments]         ${uid}
    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/versioned_composition/${uid}/version    ${query}
                        ...         headers={"Accept": "application/json"}
                        Set Test Variable    ${response}    ${resp}


# internal only, do not call from outside. use "get version of versioned composition of EHR by UID and time" instead
internal get version of versioned composition of EHR by UID and time without query
    [Arguments]         ${uid}
    &{resp}=            REST.GET    ${baseurl}/ehr/${ehr_id}/versioned_composition/${uid}/version
                        ...         headers={"Accept": "application/json"}
                        Set Test Variable    ${response}    ${resp}

# get versioned composition by version_uid
#     [Documentation]     ENDPOINT: /ehr/{ehr_id}/versioned_composition/{versioned_object_uid}/version/{version_uid}
#                         Pass Execution    TODO    PLACEHOLDER


check content of versioned composition (JSON)
                        Should Be Equal As Strings    ${response.status_code}    200
                        Should Be Equal    ${response.json()['uid']['value']}    ${versioned_object_uid}
                        Should Be Equal    ${response.json()['owner_id']['id']['value']}    ${ehr_id}


check content of versioned composition (XML)
                        Should Be Equal As Strings    ${response.status_code}    200
    ${xml}=             Parse Xml           ${response.text}
    ${uid}=             Get Element         ${xml}    uid/value
                        Element Text Should Be    ${uid}    ${versioned_object_uid}
    ${owner}=           Get Element         ${xml}    owner_id/id/value
                        Element Text Should Be    ${owner}    ${ehr_id}


get composition - latest version
    [Arguments]         ${format}
    [Documentation]     The way to return the latest version is using the versioned_composition with
    ...                 the versioned_object_uid and without the version_at_time param.
    ...                 format: JSON or XML for accept/content headers

                        prepare new request session    ${format}    Prefer=return=representation
    ${resp}=            GET On Session           ${SUT}   /ehr/${ehr_id}/versioned_composition/${versioned_object_uid}/version    expected_status=anything   headers=${headers}
                        log to console        ${resp.text}
                        Set Test Variable     ${response}    ${resp}


check content of compositions latest version (JSON)
    [Documentation]     DEPENDENCY: `get composition - latest version` keyword
                        Should Be Equal As Strings   ${response.status_code}   200
                        Set Test Variable     ${version_uid_latest}    ${response.json()['uid']['value']}

                        # comment: Check the latest version uid is equal to the second committed compo uid
                        Should Be Equal       ${version_uid_latest}    ${composition_uid_v2}

                        # comment: check content of the latest version is equal to the content committed on the second compo
                        # should be the content in the 2nd committed compo "modified value"
                        Set Test Variable     ${text}    ${response.json()['data']['content'][0]['data']['events'][0]['data']['items'][0]['value']['value']}
                        Should Be Equal       ${text}    modified value

Compare content of compositions with the Original (FLAT)
    [Arguments]         ${expected_result_data_set}
                        ${file}=            Load JSON From File    ${expected_result_data_set}
                        Set Test Variable      ${expected_result}    ${file}
                        Log To Console  \n/////////// EXPECTED //////////////////////////////
                        Output    ${expected result}
                        Set Test Variable  ${actual_response}   ${response.json()}
                        Log To Console  \n/////////// ACTUAL  //////////////////////////////
                        Output    ${actual_response}
    &{diff}=            compare_jsons_ignoring_properties  ${actual_response["composition"]}  ${expected result}  ${template_id}/_uid
                        Should Be Empty  ${diff}  msg=DIFF DETECTED!


check content of compositions latest version (XML)
    [Documentation]     DEPENDENCY: `get compostion - latest version (XML)`
                        Should Be Equal As Strings   ${response.status_code}   200
                        ${xresp}=           Parse Xml             ${response.text}
                        ${xversion_uid_latest}=  Get Element      ${xresp}      uid/value

                        # Check the latest version uid is equal to the second committed compo uid
                        Element Text Should Be    ${xversion_uid_latest}    ${version_uid_v2}

                        # check content of the latest version is equal to the content committed on the second compo
                        # should be the content in the 2nd committed compo "modified value"
    ${xtext}=           Get Element      ${xresp}      data/content[1]/data/events[1]/data/items[1]/value/value
                        Element Text Should Be    ${xtext}    modified value


get versioned composition - version at time
    [Arguments]         ${time_x}
    [Documentation]     DEPENDENCY: `commit composition (JSON)`
    ...                 :time_x: variable w. DateTime-TimeZone (like returned from `capture point in time` kw)
    ...
    ...                 ENDPOINT: /ehr/{ehr_id}/versioned_composition/{versioned_object_uid}/version{?version_at_time}

    # # extract the versioned_object_uid
    # ${v_object_uid}=    Fetch From Left       ${composition_uid}    ::
    # #
    # #                     Sleep    5s
    # # # time after first commit
    # # ${time1}=           Get Current Date      UTC    result_format=%Y-%m-%dT%H:%M:%S
    # # ${time1_tz}=        Catenate              SEPARATOR=${EMPTY}    ${time1}   +00:00
    # #                     log to console        ${time1_tz}


    # Get version at time 1, should exist and be COMPO 1
    &{params}=          Create Dictionary     version_at_time=${time_x}
    ${resp}=            GET On Session           ${SUT}   /ehr/${ehr_id}/versioned_composition/${versioned_object_uid}/version   expected_status=anything
                        ...                   params=${params}

                        log to console        ${resp.content}
                        log to console        ${resp.request.path_url}
                        log to console        ${resp.request}

                        Set Test Variable     ${response}    ${resp}


get composition - version at time (XML)
    [Arguments]         ${time_x}
    [Documentation]     DEPENDENCY: `commit composition (XML)`
    ...                 :time_x: variable w. DateTime-TimeZone (like returned from `capture point in time` kw)
    ...                 ENDPOINT: /ehr/{ehr_id}/versioned_composition/{versioned_object_uid}/version{?version_at_time}

    &{params}=          Create Dictionary     version_at_time=${time_x}
    &{headers}=         Create Dictionary     Accept=application/xml
    ${resp}=            GET On Session           ${SUT}   /ehr/${ehr_id}/versioned_composition/${versioned_object_uid}/version   expected_status=anything
                        ...                   params=${params}   headers=${headers}

                        log to console        ${resp.content}
                        log to console        ${resp.request.path_url}
                        log to console        ${resp.request}

                        Set Test Variable     ${response}    ${resp}


check content of compositions version at time (JSON)
    [Arguments]         ${time_x_nr}    ${value}
    [Documentation]     DEPENDENCY: `get compostion - version at time`
    ...                 :time_x_nr:  a string like `time_1`

                        Should Be Equal As Strings   ${response.status_code}   200
    ${version_uid}=     Set Variable    ${response.json()['uid']['value']}

    Run Keyword If      '${time_x_nr}'=='time_1'   Should Be Equal       ${version_uid}    ${composition_uid}
    Run Keyword If      '${time_x_nr}'=='time_2'   Should Be Equal       ${version_uid}    ${composition_uid_v2}


                        # check content of the latest version is equal to the content committed on the first compo
                        Set Test Variable     ${text}    ${response.json()['data']['content'][0]['data']['events'][0]['data']['items'][0]['value']['value']}
                        Should Be Equal       ${text}    ${value}


check content of compositions version at time (XML)
    [Arguments]         ${time_x_nr}    ${value}
    [Documentation]     DEPENDENCY: `get compostion - version at time (XML)`
    ...                 :time_x_nr:  a string like `time_1`
                        Should Be Equal As Strings   ${response.status_code}   200

    # compo.uid.value has the version_uid
    ${xresp}=           Parse Xml             ${response.text}
    ${version_uid}=     Get Element           ${xresp}      uid/value

    Run Keyword If      '${time_x_nr}'=='time_1'    Element Text Should Be    ${version_uid}    ${composition_uid}
    Run Keyword if      '${time_x_nr}'=='time_2'    Element Text Should Be    ${version_uid}    ${composition_uid_v2}

    # check content of the latest version is equal to the content committed on the first compo
    ${xtext}=           Get Element           ${xresp}      data/content[1]/data/events[1]/data/items[1]/value/value
                        Element Text Should Be    ${xtext}    ${value}


check composition exists
    [Documentation]     DEPENDENCY: `get composition` keywords

                        Should Be Equal As Strings   ${response.status_code}   200


check composition does not exist
    [Documentation]     DEPENDENCY: `get composition` keywords

                        Should Be Equal As Strings   ${response.status_code}   404
                        Log To Console    ${response.text}
                        # Should Contain Any    ${response.text}
                        # ...                   foo
                        # ...                   bar
                        

check composition does not exist (version at time)
    [Documentation]     DEPENDENCY: `get composition - version at time` keywords

                        Should Be Equal As Strings   ${response.status_code}   404


check composition does not exist (latest version)
    [Documentation]     DEPENDENCY: `get composition - latest version`
                        Should Be Equal As Strings   ${response.status_code}   404
                        # Should Contain Any  ${response.text}
                        # ...                   foo   # TODO @WLAD update asap
                        # ...                   bar


check versioned composition does not exist
    [Documentation]     DEPENDENCY: `get versioned composition`
                        Should Be Equal As Strings   ${response.status_code}   404
                        # Should Contain Any  ${response.text}
                        # ...                   foo   # TODO @WLAD update asap
                        # ...                   bar


delete composition
    [Arguments]         ${uid}      ${ehrScape}=false
    [Documentation]     :uid: preceding_version_uid (format of version_uid)

    IF      '${ehrScape}' == 'false'
        ${resp}     Delete On Session   ${SUT}   /ehr/${ehr_id}/composition/${uid}   expected_status=anything
        Status Should Be    204
                            # the ETag comes with quotes, this removes them
        ${del_version_uid}      Get Substring           ${resp.headers['ETag']}    1    -1
        log to console          \ndeleted version uid:  ${del_version_uid}
        Set Test Variable       ${del_version_uid}      ${del_version_uid}
    ELSE
        ${resp}     Delete On Session   ${SUT}   /composition/${uid}   expected_status=anything
        Status Should Be    200

    END
        log to console      ${resp.headers}
        log to console      ${resp.content}

delete composition - invalid - is modifiable false
    [Arguments]         ${uid}
    [Documentation]     :uid: preceding_version_uid (format of version_uid)

    ${resp}     Delete On Session   ${SUT}   /ehr/${ehr_id}/composition/${uid}   expected_status=anything
    Set Test Variable   ${response}     ${resp}
    check response: is negative indicating does not allow modification
    log to console      ${resp.headers}
    log to console      ${resp.content}

get deleted composition
    [Documentation]     The deleted compo should not exist
    ...                 204 is the code for deleted - as per openEHR REST spec

    ${resp}=            GET On Session           ${SUT}   /ehr/${ehr_id}/composition/${del_version_uid}   expected_status=anything
                        log to console          ${resp.content}
                        Status Should Be        204

get deleted composition (EHRScape)
    [Documentation]     The deleted compo should not exist
    ...                 204 is the code for deleted - as per openEHR REST spec:
    #...                 https://www.ehrscape.com/reference.html#_composition

    ${resp}=            GET On Session          ${SUT}   /composition/${composition_uid}   expected_status=anything
                        log to console          ${resp.content}
                        Status Should Be        204

delete non-existent composition
    [Documentation]     DEPENDENCY `prepare new request session`, `generate random composition_uid`

                        prepare new request session
    ${resp}=            Delete On Session      ${SUT}   /ehr/${ehr_id}/composition/${preceding_version_uid}   expected_status=anything
                        log to console    ${resp.content}
                        Should Be Equal As Strings   ${resp.status_code}   404


Upload OPT
    [Arguments]     ${opt_file}

    # TODO: rm comments
    # setting proper Accept=application/xxx header
    # Run Keyword If    '${accept-header}'=='JSON'   template_opt1.4_keywords.start request session
    
                        prepare new request session    XML
                        ...                          Prefer=return=representation
    
    # Run Keyword If    '${accept-header}'=='XML'    start request session (XML)

                        get valid OPT file    ${opt_file}
                        upload OPT file
                        server accepted OPT

Upload OPT ECIS
    [Arguments]     ${opt_file}

    # TODO: rm comments
    # setting proper Accept=application/xxx header
    # Run Keyword If    '${accept-header}'=='JSON'   template_opt1.4_keywords.start request session

                        prepare new request session    XML
                        ...                          Prefer=return=representation

    # Run Keyword If    '${accept-header}'=='XML'    start request session (XML)

                        get valid OPT file    ${opt_file}
                        upload OPT file ECIS
                        server accepted OPT

create EHR
    [Arguments]         ${accept-header}=JSON

    Run Keyword If      '${accept-header}'=='JSON'
    ...                 Run Keywords    prepare new request session   JSON
    ...                                 Prefer=return=representation
    ...                 AND             create new EHR
    # ...                 AND             extract ehr_id from response (JSON)
    # ...                 AND             extract ehrstatus_uid (JSON)

    Run Keyword If      '${accept-header}'=='XML'
    ...                 Run Keywords    prepare new request session    XML
    ...                                 content=application/xml
    ...                                 accept=application/xml    Prefer=return=representation
    ...                 AND             create new EHR (XML)
    ...                 AND             extract ehr_id from response (XML)
    ...                 AND             extract ehrstatus_uid (XML)

Create ECIS EHR
    [Arguments]
    create new EHR      ehrScape=True
    # ...                 AND             extract ehr_id from response (JSON)
    # ...                 AND             extract ehrstatus_uid (JSON)

create EHR wih x forwarded headers
    [Arguments]         ${accept-header}=JSON

    Run Keyword If      '${accept-header}'=='JSON'
    ...                 Run Keywords    prepare new request session   JSON
    ...                                 Prefer=return=representation
    ...                                 X-Forwarded-Host=example.com
    ...                                 X-Forwarded-Port=333
    ...                                 X-Forwarded-Proto=https
    ...                 AND             create new EHR
    # ...                 AND             extract ehr_id from response (JSON)
    # ...                 AND             extract ehrstatus_uid (JSON)

    Run Keyword If      '${accept-header}'=='XML'
    ...                 Run Keywords    prepare new request session    XML
    ...                                 content=application/xml
    ...                                 accept=application/xml    Prefer=return=representation
    ...                                 X-Forwarded-Host=example.com
    ...                                 X-Forwarded-Port=333
    ...                                 X-Forwarded-Proto=https
    ...                 AND             create new EHR (XML)
    ...                 AND             extract ehr_id from response (XML)
    ...                 AND             extract ehrstatus_uid (XML)

capture time before first commit
    capture point in time   0

capture point in time
    [Arguments]         ${point_in_time}
    [Documentation]     :point_in_time: integer (0, 1, 2) or string (i.e. initial_version, first_version, etc.)
    ...                 Gets the current date/time when this keyword is called and
    ...                 exposes it as a variable to test level scope with whatever name was given
    ...                 to point_in_time parameter, i.e. ${time_0}, ${time_initial_version}, etc.
    ...
    ...                 The value of the exposed variable is a timestamp in extended ISO8601 format
    ...                 e.g. 2015-01-20T19:30:22.765+01:00
    ...                 s. http://robotframework.org/robotframework/latest/libraries/DateTime.html
    ...                 for DateTime Library docs
                        Sleep    1   # gives DB some time to finish it's operation
    ${zone}=            Set Suite Variable    ${time_zone}    ${{ tzlocal.get_localzone() }}
    ${time}=            Set Variable    ${{ datetime.datetime.now(tz=tzlocal.get_localzone()).isoformat() }}
    ${offset}=          Set Suite Variable    ${utc_offset}    ${time}[-6:]
                        Set Suite Variable   ${time_${point_in_time}}   ${time}



create EHR and commit a composition for versioned composition tests
    [Documentation]     Creates an EHR and commits a pre-set composition to kick off a test environment.
    ...                 Important returned vars are: `${ehr_id}` and `${version_uid}`

    prepare new request session    JSON    Prefer=return=representation
    create new EHR
    Should Be Equal As Strings    ${response.status}    201

    Upload OPT    minimal/minimal_observation.opt
    commit composition (JSON)    minimal/minimal_observation.composition.participations.extdatetimes.xml


update a composition for versioned composition tests
    [Documentation]     Updates a pre-set composition to alter a versioned test environment.
    ...                 Requires `${compo_uid_v1}` or to be run after the `create EHR and commit a composition 
    ...                 for versioned composition tests` keyword.

    update composition (JSON)   minimal/minimal_observation.composition.participations.extdatetimes.v2.xml
    check composition update succeeded



# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]

# commit composition (XML)
#     [Arguments]         ${xml_composition}
#     [Documentation]     Creates a composition by using POST method and XML file
#     ...                 from `/test_data_sets/xml_compositions/` folder
#     ...                 DEPENDENCY: use it right after `create EHR XML` which
#     ...                             provides the `ehr_id`.
#
#     ${file}=            Get File           ${CURDIR}/../test_data_sets/xml_compositions/${xml_composition}
#     &{headers}=         Create Dictionary  Content-Type=application/xml  Prefer=return=representation  Accept=application/xml
#     ${resp}=            POST On Session       ${SUT}   /ehr/${ehr_id}/composition   data=${file}   headers=${headers}
#                         Should Be Equal As Strings   ${resp.status_code}   201
#
#     ${xresp}=           Parse Xml          ${resp.text}
#                         Log Element        ${xresp}
#                         Log Element        ${xresp}  xpath=composition
#     ${xcompo_version_uid}=     Get Element        ${xresp}      composition/uid/value
#                         Set Test Variable  ${compo_version_uid}     ${xcompo_version_uid.text}
#                         # Log To Console     ${compo_version_uid}

# get composition by composition_uid (XML)
#     [Arguments]         ${uid}
#     [Documentation]     DEPENDENCY: `commit composition (JSON/XML)` keywords
#
#     # the uid param in the doc is verioned_object.uid but is really the version.uid,
#     # because the response from the create compo has this endpoint in the Location header
#
#     &{headers}=         Create Dictionary   Content-Type=application/xml   Accept=application/xml    Prefer=return=representation
#     ${resp}=            GET On Session         ${SUT}   /ehr/${ehr_id}/composition/${uid}    headers=${headers}
#                         log to console      ${resp.content}
#                         Set Test Variable   ${response}    ${resp}

# capture time after first commit
#     ${time}=            Get Current Date    UTC    result_format=%Y-%m-%dT%H:%M:%S
#     ${time_tz}=         Catenate            SEPARATOR=${EMPTY}    ${time}   +00:00
#                         Set Test Variable   ${time_1}    ${time_tz}

# capture time after second commit
#     ${time}=            Get Current Date    UTC    result_format=%Y-%m-%dT%H:%M:%S
#     ${time_tz}=         Catenate            SEPARATOR=${EMPTY}    ${time}   +00:00
#                         Set Test Variable   ${time_2}    ${time_tz}
