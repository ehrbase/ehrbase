# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH).
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
Metadata    Authors    *Wladislaw Wagner, Stefan Spiska*
Metadata    Created    2021.07.13
Metadata    Updated    2021.08.09
Metadata    Command    robot -d results -L TRACE -i cache robot/ADMIN_TESTS
Metadata    TOP_TEST_SUITE    CACHE

Documentation    Tests related to caching \n\n
...              How to run these tests locally \n\n
...              ============================== \n\n
...
...              robot -d results/ -L TRACE -i CACHE robot/ADMIN_TESTS/
...              OR
...              robot -v SUT:ADMIN-TEST -d results/ -L TRACE -i cache robot/ADMIN_TESTS/
...              OR (when you have started EHRbase + DB manually)
...              robot -v SUT:ADMIN-DEV -d results/ -L TRACE -i cache robot/ADMIN_TESTS/
...
...              Thanks to Stefan Spiska for providing the documentation for this test in \n\n
...              https://github.com/ehrbase/project_management/issues/451

Resource        ../_resources/keywords/admin_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/template_opt1.4_keywords.robot
Resource        ../_resources/keywords/aql_query_keywords.robot

#Suite Setup     startup SUT
#Suite Teardown  shutdown SUT

Force Tags     cache    cache_template_update    TODO

# NOTE: because of serious concerns about the template update feature
#       this test is deactiated and excluded from CI execution (via TODO tag)
#       (see https://wiki.vitagroup.ag/pages/viewpage.action?pageId=59973991 for details)
#       Consider reusing it to test template update feature in the future.
#       
#       Originally the sense of this test was not to test the template update feature, though.
#       Instead it was meant to test proper working of EHRbase's cache.
#       Thus a proper replacement for this test should be considered which
#       requires providing a new test design/documentation for Cache testing scenario.



*** Variables ***
# comment: overriding defaults in suite_settings.robot
${SUT}                         ADMIN-TEST
${CACHE-ENABLED}               ${TRUE}
${ALLOW-TEMPLATE-OVERWRITE}    ${FALSE}

${VALID DATA SETS}     ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}valid_templates



*** Test Cases ***
Commit Composition After Template Update (Cache Enabled)
    [Tags]    not-ready
    
    01) Run ehrbase with cache enabled true
    02) Upload Template Containing Node With Specific Archetype_ID
    03) Upload Composition Containing Entity Corresponding To Specific Archetype_ID
    04) Conduct Checks via 'GET Opt' and 'AQL Queries'
    05) Update Template Changing The Specific Archetype_ID To NEW Archetype_ID
    06) Upload Composition (To Updated Template) Containing Entity Corresponding To NEW Specific Archetype_ID
    07) Conduct Checks via 'GET Opt' and 'AQL Queries'
    08) Restart ehrbase
    09) Conduct Checks To Validate Cache Works Correctly
    # 10. Check That: Get Opt / Get Web-Template Contains The NEW Specific Archetype_ID    # TODO: clarify w/ @Stefan

    # [Teardown]    CLEAN UP










*** Keywords ***

startup SUT
    [Documentation]     Overrides `generic_keywords.startup SUT` keyword
    ...                 to add some ENVs required by this test suite.

    Set Environment Variable    ADMINAPI_ACTIVE    true
    Set Environment Variable    ADMINAPI_ALLOWDELETEALL    true
    #generic_keywords.startup SUT


01) Run ehrbase with cache enabled true
    Log    Robot takes care of this as long as you start the test with
    Log    robot -d results -L TRACE -i cache robot/ADMIN_TESTS


02) Upload Template Containing Node With Specific Archetype_ID

    upload valid OPT    minimal/cache.opt
    retrieve OPT by template_id    cache_test.v1


03) Upload Composition Containing Entity Corresponding To Specific Archetype_ID

    prepare new request session    XML    Prefer=return=representation
    create new EHR (XML)
    commit composition (XML)    minimal/cache_composition.xml


04) Conduct Checks via 'GET Opt' and 'AQL Queries'

    # comment: Check That - Get Opt / Get Web-Template Contains Specific Archetype_ID
                            prepare new request session    XML    Prefer=return=representation
        ${xml}=             retrieve OPT by template_id    cache_test.v1
        ${archetype_id1}=   Get Element Text    ${xml}    xpath=definition/archetype_id/value
                            Should Be Equal As Strings    ${archetype_id1}    openEHR-EHR-COMPOSITION.minimal.v1

        ${archetype_id2}=   Get Element Text    ${xml}    xpath=definition/attributes[2]/children/archetype_id/value
                            Should Be Equal As Strings    ${archetype_id2}    openEHR-EHR-ADMIN_ENTRY.minimal.v1


    # comment: Check That - Aql Query With Specific Archetype_ID Retunrs The Composition
        # comment: create AQL string and execute AQL query
        ${query1}=    Catenate
        ...           SELECT
        ...             c/uid/value, c/name/value, c/archetype_node_id, c/composer/name
        ...           FROM
        ...             EHR e
        ...           CONTAINS
        ...             COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1]
        Set Test Variable    ${payload}    {"q": "${query1}"}
        POST /query/aql (REST)     JSON
        # comment: valiate AQL result
        String    $.rows[0][0]    ${version_uid}
        String    $.rows[0][1]    Cache Test
        String    $.rows[0][2]    openEHR-EHR-COMPOSITION.minimal.v1
        String    $.rows[0][3]    Dr. Robot


05) Update Template Changing The Specific Archetype_ID To New Archetype_ID
    # comment: without restarting ehrbase!
    #          via the opt and the admin endpoints

    (admin) update OPT    minimal/cache_updated.opt
    Should Be Equal As Strings   ${response.status_code}   200


06) Upload Composition (To Updated Template) Containing Entity Corresponding To NEW Specific Archetype_ID

    prepare new request session    XML    Prefer=return=representation
    create new EHR (XML)
    commit composition (XML)   minimal/cache_composition_updated.xml


07) Conduct Checks via 'GET Opt' and 'AQL Queries'

    # comment: Check That - Get Opt / Get Web-Template Contains The NEW Specific Archetype_ID
                            prepare new request session    XML    Prefer=return=representation
        ${xml}=             retrieve OPT by template_id    cache_test.v1
        ${archetype_id1}=   Get Element Text    ${xml}    xpath=definition/archetype_id/value
                            Should Be Equal As Strings    ${archetype_id1}    openEHR-EHR-COMPOSITION.minimal.v1
        ${archetype_id2}=   Get Element Text    ${xml}    xpath=definition/attributes[2]/children/archetype_id/value
                            Should Be Equal As Strings    ${archetype_id2}    openEHR-EHR-EVALUATION.minimal.v1

    # comment: Check That - Aql Query With Specific Archetype_id Returns No Entry
        # TODO: clarify w/ @stefan.spiska which query to use for this

    # comment: Check That - Aql Query With New Specific Archetype_id Returns The Composition
        ${query1}=    Catenate
        ...           SELECT
        ...             c/uid/value, c/name/value, c/archetype_node_id, c/composer/name
        ...           FROM
        ...             EHR e
        ...           CONTAINS
        ...             COMPOSITION c [openEHR-EHR-EVALUATION.minimal.v1]
        Set Test Variable    ${payload}    {"q": "${query1}"}
        POST /query/aql (REST)     JSON
        # comment: valiate AQL result
        String    $.rows[1][0]    ${version_uid}
        String    $.rows[1][1]    Cache Test Updated
        String    $.rows[1][2]    openEHR-EHR-COMPOSITION.minimal.v1
        String    $.rows[1][3]    Dr. Robot Updated


08) Restart Ehrbase
    stop openehr server
    log    ${CACHE-ENABLED}
    start openehr server
    log    ${CACHE-ENABLED}


08) Restart Ehrbase (With Cache Disabled)
    stop openehr server
    Set Suite Variable    ${CACHE-ENABLED}    ${FALSE}
    log    ${CACHE-ENABLED}
    start openehr server
    log    ${CACHE-ENABLED}


09) Conduct Checks To Validate Cache Works Correctly
    # comment: Check That: Get Opt Contains The NEW Specific Archetype_ID
        prepare new request session    XML
        ${xml}=     retrieve OPT by template_id    cache_test.v1
        ${archetype_id1}=    Get Element Text    ${xml}    xpath=definition/archetype_id/value
        Should Be Equal As Strings    ${archetype_id1}    openEHR-EHR-COMPOSITION.minimal.v1

        ${archetype_id2}=    Get Element Text    ${xml}    xpath=definition/attributes[2]/children/archetype_id/value
        Should Be Equal As Strings    ${archetype_id2}    openEHR-EHR-EVALUATION.minimal.v1
    
    # comment: Check That: AQL query with specific archetype_ID returns no entry
    
        # TODO: clarify w/ @stefan.spiska which query to use for this
        # comment: execute AQL query
        ${query1}=    Catenate
        ...           SELECT
        ...             c/uid/value, c/name/value, c/archetype_node_id, c/composer/name
        ...           FROM
        ...             EHR e
        ...           CONTAINS
        ...             COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1]
        Set Test Variable    ${payload}    {"q": "${query1}"}
        POST /query/aql (REST)     JSON
        # comment: valiate AQL result
        String    $.rows[0][1]    Cache Test
        String    $.rows[0][2]    openEHR-EHR-COMPOSITION.minimal.v1
        String    $.rows[0][3]    Dr. Robot


    # comment: Check That: AQL query with NEW specific archetype_ID returns the composition
        # comment: execute query
        ${query1}=    Catenate
        ...           SELECT
        ...             c/uid/value, c/name/value, c/archetype_node_id, c/composer/name
        ...           FROM
        ...             EHR e
        ...           CONTAINS
        ...             COMPOSITION c [openEHR-EHR-EVALUATION.minimal.v1]
        Set Test Variable    ${payload}    {"q": "${query1}"}
        POST /query/aql (REST)     JSON
        # comment: valiate AQL result
        String    $.rows[1][0]    ${version_uid}
        String    $.rows[1][1]    Cache Test Updated
        String    $.rows[1][2]    openEHR-EHR-COMPOSITION.minimal.v1
        String    $.rows[1][3]    Dr. Robot Updated


10) Check That: Get Opt / Get Web-Template Contains The NEW Specific Archetype_ID
    # TODO: clarify with @Stefan - seems to be just a duplication of step 9


CLEAN UP
    admin_keywords.(admin) delete composition
    admin_keywords.check composition admin delete table counts
    # sleep    0.5
    admin_keywords.(admin) delete all OPTs
    validate DELETE ALL response - 204 deleted ${1}


upload valid OPT
    [Arguments]           ${opt file}

    prepare new request session    XML
    
    get valid OPT file    ${opt file}
    Extract Template Id From OPT File

    ${resp}=            REST.POST    /definition/template/adl1.4    data=${VALID DATA SETS}/${opt file}
                        ...          headers=${headers}
                        Integer    response status    204


validate DELETE ALL response - 204 deleted ${amount}
                        Integer    response status   200
                        Integer    response body deleted    ${amount}











# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]
