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

# Resource    ${CURDIR}${/}../_resources/suite_settings.robot
# Resource    ${CURDIR}${/}../_resources/keywords/composition_keywords.robot



Force Tags      circleci



*** Test Cases ***
Dummy Test For CircleCi Pipeline Testing/Debugging
    [Documentation]     CI Pipeline Testing/Debugging

    ${uuid}=            Evaluate    str(uuid.uuid4())    uuid
                        Set Test Variable    ${uuid}     ${uuid}
                        Log To Console    \n\n\tUUID: ${uuid}\n
