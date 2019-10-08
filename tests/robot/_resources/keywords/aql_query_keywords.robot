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
Library    Collections
Library    String
Library    Process
Library    OperatingSystem


    ########################################################
    #                                                      #
    #  @WLAD/PABLO remove ${url} variable when AQL starts  #
    #  to use EHRBASE endpoints!!!!!!!!!                   #
    #                                                      #
    ########################################################

*** Variables ***
${url}     http://localhost:8080/ehrbase/rest/ecis/v1



*** Keywords ***
startup AQL SUT
    [Documentation]  used in Test Suite Setup
    ...              this keyword overrides another one with same name
    ...              from "generic_keywords.robot" file

    get application version
    unzip file_repo_content.zip
    empty operational_templates folder
    start ehrdb
    start openehr server


execute AQL query
    [Arguments]         ${aql_query}
    [Documentation]     Sends given AQL query via POST request.

    REST.POST           ${url}/query    ${aql_query}
    Integer             response status    200
