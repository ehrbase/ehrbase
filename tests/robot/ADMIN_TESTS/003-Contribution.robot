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
Metadata    Version    0.1.0
Metadata    Author    *Wladislaw Wagner*
Metadata    Author    *Jake Smolka*
Metadata    Created    2020.09.01

Metadata        TOP_TEST_SUITE    ADMIN_EHR
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     startup SUT
Suite Teardown  shutdown SUT

Force Tags     ADMIN_contribution



*** Test Cases ***

ADMIN - Delete Contribution
    # pre check
    Connect With DB
    check contribution admin delete table counts initially
    # preparing and provisioning
    upload OPT    minimal/minimal_evaluation.opt
    prepare new request session    JSON    Prefer=return=representation
    create supernew ehr
    Set Test Variable  ${ehr_id}  ${response.body.ehr_id.value}
    ehr_keywords.validate POST response - 201 created ehr
    commit CONTRIBUTION (JSON)  minimal/minimal_evaluation.contribution.json
    # Execute admin delete EHR
    admin delete contribution
    Log To Console  ${response}
    # Test with count rows again - post check
    check contribution admin delete table counts



*** Keywords ***

startup SUT
    [Documentation]     Overrides `generic_keywords.startup SUT` keyword
    ...                 to add some ENVs required by this test suite.

    Set Environment Variable    ADMINAPI_ACTIVE    true
    Set Environment Variable    SYSTEM_ALLOWTEMPLATEOVERWRITE    true
    generic_keywords.startup SUT


admin delete contribution
    [Documentation]     Admin delete of Contribution.
    ...                 Needs `${contribution_uid}` var from e.g. `commit CONTRIBUTION (JSON)` KW.

    &{resp}=            REST.DELETE    ${baseurl}/admin/ehr/${ehr_id}/contribution/${contribution_uid}
                        Should Be Equal As Strings   ${resp.status}   204
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


check contribution admin delete table counts initially

    ${contr_records}=   Count Rows In DB Table    ehr.contribution
                        Should Be Equal As Integers    ${contr_records}     ${0}
    ${audit_records}=   Count Rows In DB Table    ehr.audit_details
                        Should Be Equal As Integers    ${audit_records}     ${0}
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


check contribution admin delete table counts

    ${contr_records}=   Count Rows In DB Table    ehr.contribution
                        Should Be Equal As Integers    ${contr_records}     ${1}    # from creation of the EHR, which will not be deleted
    ${audit_records}=   Count Rows In DB Table    ehr.audit_details
                        Should Be Equal As Integers    ${audit_records}     ${2}    # from creation of the EHR (1 for status, 1 for the wrapping contribution)
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