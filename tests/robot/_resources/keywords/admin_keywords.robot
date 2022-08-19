# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Jake Smolka (Hannover Medical School).
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
Documentation   ADMIN Keywords
Resource        ../suite_settings.robot
Resource        template_opt1.4_keywords.robot



*** Keywords ***
(admin) delete ehr
    [Documentation]     Admin delete of EHR record with a given ehr_id.
    ...                 DEPENDENCY: `prepare new request session`

    &{resp}=            REST.DELETE    ${admin_baseurl}/ehr/${ehr_id}
                        # Should Be Equal As Strings   ${resp.status}   204
                        Integer    response status    204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


(admin) update OPT
    [Arguments]         ${opt_file}    ${prefer_return}=representation
    [Documentation]     Updates OPT via admin endpoint admin_baseurl/template/${template_id} \n\n

                        get valid OPT file    ${opt_file}
    
    &{headers}=         Create Dictionary    &{EMPTY}
                        Set To Dictionary    ${headers}
                        ...                  Content-Type=application/xml
                        ...                  Accept=application/xml
                        ...                  Prefer=return=${prefer_return}

                        Create Session       ${SUT}    ${ADMIN_BASEURL}    debug=2
                        ...                  auth=${CREDENTIALS}    verify=True

    ${resp}=            Put On Session    ${SUT}    /template/${template_id}   expected_status=anything
                        ...    data=${file}    headers=${headers}
                        Set Test Variable    ${response}    ${resp}
                        Set Test Variable    ${prefer_return}    ${prefer_return}


(admin) delete OPT
    [Arguments]         ${prefer_return}=representation
    [Documentation]     Admin delete OPT on server.
    ...                 Depends on any KW that exposes an variable named 'template_id'
    ...                 to test or suite level scope. \n\n
    ...                 valid values for 'prefer_return': \n\n\
    ...                 - representation (default) \n\n
    ...                 - minimal
                        prepare new request session
                        ...    Prefer=return=${prefer_return}
                        Set Test Variable    ${prefer_return}    ${prefer_return}
    &{resp}=            REST.DELETE    ${admin_baseurl}/template/${template_id}
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console

(admin) delete all OPTs
    [Documentation]     Admin delete OPT on server.
    ...                 Depends on any KW that exposes an variable named 'template_id'
    ...                 to test or suite level scope.
                        prepare new request session
    &{resp}=            REST.DELETE    ${admin_baseurl}/template/all
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


(admin) delete composition
    [Documentation]     Admin delete of Composition.
    ...                 Needs `${versioned_object_uid}` var from e.g. `commit composition (JSON)` KW.

    &{resp}=            REST.DELETE    ${admin_baseurl}/ehr/${ehr_id}/composition/${versioned_object_uid}
                        # Should Be Equal As Strings   ${resp.status}   204
                        Integer    response status    204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


Delete Composition Using API
    IF      '${versioned_object_uid}' != '${None}'
        &{resp}         REST.DELETE    ${admin_baseurl}/ehr/${ehr_id}/composition/${versioned_object_uid}
                        Run Keyword And Return Status   Integer    response status    204
                        Set Suite Variable    ${deleteCompositionResponse}    ${resp}
                        Output Debug Info To Console
    END

Delete Template Using API
    &{resp}             REST.DELETE   ${admin_baseurl}/template/${template_id}
                        Set Suite Variable    ${deleteTemplateResponse}    ${resp}
                        Output Debug Info To Console
                        Should Be Equal As Strings      ${deleteTemplateResponse.status}      200
                        #Delete All Sessions

check composition admin delete table counts
    Connect With DB

    ${contr_records}=   Count Rows In DB Table    ehr.contribution
                        # Should Be Equal As Integers    ${contr_records}     ${1}    # from creation of the EHR, which will not be deleted
    ${audit_records}=   Count Rows In DB Table    ehr.audit_details
                        # Should Be Equal As Integers    ${audit_records}     ${2}    # from creation of the EHR (1 for status, 1 for the wrapping contribution)
    ${compo_records}=   Count Rows In DB Table    ehr.composition
                        Should Be Equal As Integers    ${compo_records}     ${0}
    ${compo_h_records}=  Count Rows In DB Table    ehr.composition_history
                        Should Be Equal As Integers    ${compo_h_records}     ${0}
    ${entry_records}=   Count Rows In DB Table    ehr.entry
                        Should Be Equal As Integers    ${entry_records}     ${0}
    ${entry_h_records}=  Count Rows In DB Table    ehr.entry_history
                        Should Be Equal As Integers    ${entry_h_records}     ${0}
    ${event_context_records}=   Count Rows In DB Table    ehr.event_context
                        Should Be Equal As Integers    ${event_context_records}     ${0}
    ${entry_participation_records}=   Count Rows In DB Table    ehr.participation
                        Should Be Equal As Integers    ${entry_participation_records}     ${0}


(admin) delete contribution
    [Documentation]     Admin delete of Contribution.
    ...                 Needs `${contribution_uid}` var from e.g. `commit CONTRIBUTION (JSON)` KW.

    &{resp}=            REST.DELETE    ${admin_baseurl}/ehr/${ehr_id}/contribution/${contribution_uid}
                        # Should Be Equal As Strings   ${resp.status}   204
                        Integer    response status    204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


(admin) delete directory
    [Documentation]     Admin delete of Directory.
    ...                 Needs manualle created `${folder_versioned_uid}`.


    &{resp}=            REST.DELETE    ${admin_baseurl}/ehr/${ehr_id}/directory/${folder_versioned_uid}
                        # Should Be Equal As Strings   ${resp.status}   204
                        Integer    response status   204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console
