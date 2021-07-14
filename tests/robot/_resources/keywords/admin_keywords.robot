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

    &{resp}=            REST.DELETE    ${baseurl}/admin/ehr/${ehr_id}
                        Should Be Equal As Strings   ${resp.status}   204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


(admin) update OPT
    [Arguments]         ${opt_file}    ${prefer_return}=representation
    [Documentation]     Updates OPT via admin endpoint /admin/template/${template_id} \n\n
    ...                 valid values for 'prefer_return': \n\n\
    ...                 - representation (default) \n\n
    ...                 - minimal
                        prepare new request session    XML
                        ...    Prefer=return=${prefer_return}
                        Set Test Variable    ${prefer_return}    ${prefer_return}
                        get valid OPT file    ${opt_file}
                        # upload OPT file
    ${resp}=            Put Request    ${SUT}    /admin/template/${template_id}
                        ...    data=${file}    headers=${headers}
                        Set Test Variable    ${response}    ${resp}


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
    &{resp}=            REST.DELETE    ${baseurl}/admin/template/${template_id}
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


(admin) delete composition
    [Documentation]     Admin delete of Composition.
    ...                 Needs `${versioned_object_uid}` var from e.g. `commit composition (JSON)` KW.

    &{resp}=            REST.DELETE    ${baseurl}/admin/ehr/${ehr_id}/composition/${versioned_object_uid}
                        Should Be Equal As Strings   ${resp.status}   204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


(admin) delete contribution
    [Documentation]     Admin delete of Contribution.
    ...                 Needs `${contribution_uid}` var from e.g. `commit CONTRIBUTION (JSON)` KW.

    &{resp}=            REST.DELETE    ${baseurl}/admin/ehr/${ehr_id}/contribution/${contribution_uid}
                        Should Be Equal As Strings   ${resp.status}   204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


(admin) delete directory
    [Documentation]     Admin delete of Directory.
    ...                 Needs manualle created `${folder_versioned_uid}`.


    &{resp}=            REST.DELETE    ${baseurl}/admin/ehr/${ehr_id}/directory/${folder_versioned_uid}
                        # Should Be Equal As Strings   ${resp.status}   204
                        Integer    response status   204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console
