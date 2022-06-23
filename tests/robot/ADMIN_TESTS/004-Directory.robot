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
Metadata    Authors    *Wladislaw Wagner*, *Jake Smolka*
Metadata    Created    2020.09.01

Metadata        TOP_TEST_SUITE    ADMIN_DIRECTORY

Resource        ../_resources/keywords/admin_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/directory_keywords.robot

#Suite Setup     startup SUT
#Suite Teardown  shutdown SUT

Force Tags     ADMIN_directory



*** Variables ***
${SUT}          ADMIN-TEST    # overriding defaults in suite_settings.robot



*** Test Cases ***

ADMIN - Delete Directory
    # pre check
    #Connect With DB
    # check directory admin delete table counts initially
    # preparing and provisioning
    Upload OPT    minimal/minimal_evaluation.opt
    prepare new request session    JSON    Prefer=return=representation
    create supernew ehr
    Set Test Variable  ${ehr_id}  ${response.body.ehr_id.value}
    ehr_keywords.validate POST response - 201 created ehr
    create DIRECTORY (JSON)    subfolders_in_directory.json
    ${versioned_uid}=   Fetch From Left  ${version_uid}  ::
    Set Test Variable  ${folder_versioned_uid}  ${versioned_uid}
    # Execute (admin) delete ehr
    (admin) delete directory
    Log To Console  ${response}
    # Test with count rows again - post check
    # check directory admin delete table counts



*** Keywords ***

startup SUT
    [Documentation]     Overrides `generic_keywords.startup SUT` keyword
    ...                 to add some ENVs required by this test suite.

    Set Environment Variable    ADMINAPI_ACTIVE    true
    Set Environment Variable    SYSTEM_ALLOWTEMPLATEOVERWRITE    true
    generic_keywords.startup SUT


check directory admin delete table counts initially

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
    ${folder_records}=   Count Rows In DB Table    ehr.folder
                        Should Be Equal As Integers    ${folder_records}     ${0}
    ${folder_hierarchy_records}=   Count Rows In DB Table    ehr.folder_hierarchy
                        Should Be Equal As Integers    ${folder_hierarchy_records}     ${0}
    ${folder_items_records}=   Count Rows In DB Table    ehr.folder_items
                        Should Be Equal As Integers    ${folder_items_records}     ${0}
    ${object_ref_records}=   Count Rows In DB Table    ehr.object_ref
                        Should Be Equal As Integers    ${object_ref_records}     ${0}


check directory admin delete table counts

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
    ${folder_records}=   Count Rows In DB Table    ehr.folder
                        Should Be Equal As Integers    ${folder_records}     ${0}
    ${folder_hierarchy_records}=   Count Rows In DB Table    ehr.folder_hierarchy
                        Should Be Equal As Integers    ${folder_hierarchy_records}     ${0}
    ${folder_items_records}=   Count Rows In DB Table    ehr.folder_items
                        Should Be Equal As Integers    ${folder_items_records}     ${0}
    ${object_ref_records}=   Count Rows In DB Table    ehr.object_ref
                        Should Be Equal As Integers    ${object_ref_records}     ${0}