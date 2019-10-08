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
Documentation   EHR Integration Tests
...

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

Suite Setup    startup SUT
Suite Teardown    shutdown SUT

Force Tags      composition    obsolete



*** Test Cases ***
Create new composition (FLAT/JSON)
    [Tags]    not-ready
    create ehr  1234-555  namespace_555
    extract ehrId
    create composition  ${template_id}  FLAT  composition_001.json
    expect response status  201
    [Teardown]  TW @Dev Wrong Status Code - tag(s): not-ready

  # TODO @ Wlad - create test cases for all variations of `create new composition`
  # Create new composition (XML)
  # Create new composition (ECISFLAT)

Retrieve composition FLAT
    [Tags]  not-ready
    create ehr  1234-577  namespace_555
    extract ehrId
    create composition  ${template_id}  FLAT  composition_001.json
    extract composition_id
    retrieve composition json    ${composition_id}   FLAT
    # Output    response body
    # TODO @ Wlad - make keyword --> verify response body is FLAT/JSON
    expect response status  200

Retrieve composition FLAT with embeded XML
    [Tags]  not-ready
    create ehr  1234-587  namespace_555
    extract ehrId
    create composition  ${template_id}  FLAT  composition_001.json
    extract composition_id
    retrieve composition json    ${composition_id}   XML
    # Output    response body
    # TODO @ Wlad - make keyword --> verify response body JSON embeds XML
    expect response status  200

Retrieve composition ECISFLAT
    [Tags]  not-ready
    create ehr  1234-597  namespace_555
    extract ehrId
    create composition  ${template_id}  FLAT  composition_001.json
    extract composition_id
    retrieve composition json    ${composition_id}   ECISFLAT
    # Output    response body
    # TODO @ Wlad - make keyword --> verify response body is ECISFLAT/JSON
    expect response status  200

Retrieve composition RAW
    [Tags]  not-ready
    create ehr  1234-588  namespace_555
    extract ehrId
    create composition  ${template_id}  FLAT  composition_001.json
    extract composition_id
    retrieve composition json    ${composition_id}   RAW
    # Output    response body
    # TODO @ Wlad - make keyword --> verify response body is RAW/JSON
    expect response status  200

Update composition
    [Tags]  not-ready
    create ehr  1234-558  namespace_555
    extract ehrId
    create composition  ${template_id}  FLAT  composition_001.json
    extract composition_id
    REST.PUT   /composition/${composition_id}?templateId=${template_id}&format=FLAT
    # Output
    expect response status  200

Delete composition
    [Tags]  not-ready
    create ehr  1234-559  namespace_555
    extract ehrId
    create composition  ${template_id}  FLAT  composition_001.json
    extract composition_id
    REST.DELETE   /composition/${composition_id}
    expect response status  200



*** Keywords ***
