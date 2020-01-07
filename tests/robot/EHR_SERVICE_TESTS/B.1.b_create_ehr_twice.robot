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
Metadata    Created    2019.03.03

Documentation   B.1.b) Alternative flow 1: Create same EHR twice
...
...             https://vitasystemsgmbh.atlassian.net/wiki/spaces/ETHERCIS/pages/498532998/EHR+Test+Suite#EHRTestSuite-b.Alternativeflow1:CreatesameEHRtwice
...
...             Flow:
...
...                 1. Invoke the create EHR service (for each item in the Data set with given ehr_id, data sets 9 to 16)
...                 2. The server should answer with a positive response associated to "EHR created"
...                 3. Invoke the create EHR service (for the same item as in 1.)
...                 4. The server should answer with a negative response, and that should be related with the EHR existence,
...                    like "EHR with ehr_id already exists"
...
...             Postconditions:
...                 A new EHR will exists in the system, the first one created, and be consistent with the data sets used.

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

# Setup/Teardown from __init.robot is used
# Suite Setup    startup SUT
# Suite Teardown    shutdown SUT

Force Tags    create_ehr



*** Test Cases ***
Create Same EHR Twice (JSON)

    [Documentation]     Uses PUT method on /ehr/{{ehr_id}} endpoint to create new EHR.

    prepare new request session    JSON
    generate random ehr_id
    create new EHR by ID        ${ehr_id}
    create new EHR by ID        ${ehr_id}

    verify server response


Create Same EHR Twice (XML)

    [Documentation]     Uses PUT method on /ehr/{{ehr_id}} endpoint to create new EHR.

    prepare new request session    XML
    generate random ehr_id
    create new EHR by ID        ${ehr_id}
    create new EHR by ID        ${ehr_id}

    verify server response



*** Keywords ***
verify server response
    server complains about already existing ehr_id


server complains about already existing ehr_id
    # Log To Console      ${response}
    # Log To Console      ${response.status}
    Should Be Equal As Strings    ${response.status}    409
    
    # String    response body error    EHR with this ID already exists
    # TODO: create separate checks for JSON/XML responses
