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


*** Test Cases ***

ADMIN - Delete EHR
                        start ehrbase
                        # pre check
                        Connect With DB
                        check ehr admin delete table counts
                        # preparing and provisioning
                        prepare new request session    JSON    Prefer=return=representation
                        create supernew ehr
                        Set Test Variable  ${ehr_id}  ${response.body.ehr_id.value}
                        ehr_keywords.validate POST response - 201 created ehr
                        # Execute admin delete EHR
                        admin delete ehr
                        Log To Console  ${response}
                        # Test with count rows again - post check
                        check ehr admin delete table counts

*** Keywords ***

start ehrbase

                        Set Environment Variable  ADMINAPI_ACTIVE   true
                        generic_keywords.startup SUT


admin delete ehr
    [Documentation]     Admin delete of EHR record with a given ehr_id.
    ...                 DEPENDENCY: `prepare new request session`

    &{resp}=            REST.DELETE    ${baseurl}/admin/ehr/${ehr_id}
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console

check ehr admin delete table counts

    ${ehr_records}=     Count Rows In DB Table    ehr.ehr
                        Should Be Equal As Integers    ${ehr_records}       ${0}
    ${status_records}=  Count Rows In DB Table    ehr.status
                        Should Be Equal As Integers    ${status_records}    ${0}
    ${status_h_records}=  Count Rows In DB Table    ehr.status_history
                        Should Be Equal As Integers    ${status_h_records}  ${0}
    ${contr_records}=   Count Rows In DB Table    ehr.status
                        Should Be Equal As Integers    ${contr_records}     ${0}
    ${contr_records}=   Count Rows In DB Table    ehr.status
                        Should Be Equal As Integers    ${contr_records}     ${0}