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
Metadata    Version    0.1.0
Metadata    Author    *Wladislaw Wagner*
Metadata    Created    2020.01.30

Documentation   B.1.c) Alternative flow 2: Create two EHRs for the same patient
...             
...             source: https://docs.google.com/document/d/1r_z_E8MhlNdeVZS4xecl-8KbG0JPqCzKtKMfhuL81jY/edit#heading=h.j8svfp6lz278

Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

# Suite Setup  startup SUT
# Suite Teardown  shutdown SUT

Force Tags    create_ehr    refactor



*** Test Cases ***
Create Same EHR Twice For The Same Patient (JSON)
    [Tags]    272    not-ready   bug

    prepare new request session    JSON

    generate random subject_id
    create new EHR for subject_id (JSON)    ${subject_id}

    create new EHR for subject_id (JSON)    ${subject_id}

    verify response



*** Keywords ***
verify response
    Integer    response status    409

    # TODO: response should indicate a conflict with an already existing EHR with the same subject id, namespace pair.
