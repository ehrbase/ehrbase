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
Metadata    Author    *Wladislaw Wagner*
Metadata    Created    2020.12.28

Metadata        TOP_TEST_SUITE    ADMIN_TEMPLATE
Resource        ${EXECDIR}/robot/_resources/suite_settings.robot

Suite Setup     startup SUT
Suite Teardown  shutdown SUT

Force Tags     template



*** Variables ***
# comment: overriding defaults in suite_settings.robot
${SUT}                  ADMIN-TEST
${CACHE-ENABLED}        ${FALSE}



#////////////////////////////////////////////////////////////
#//                                                        //
#//   NOTE: Tests can't be executed in random order!       //
#//         Some have impact on each other                 //
#//         cause clean up steps don't work properly!      //
#//         Until this is fixed preserve given order!!!    //
#//                                                        //
#///////////////////////////////////////////////////////////

*** Test Cases ***
001 ADMIN - Delete All Templates (when none were uploaded before)
    (admin) delete all OPTs
    validate DELETE ALL response - 204 deleted ${0}


002 ADMIN - Delete All Templates (when only one was uploaded before)
    upload valid OPT    minimal/minimal_admin.opt
    (admin) delete all OPTs
    validate DELETE ALL response - 204 deleted ${1}


003 ADMIN - Delete Multiple Templates
    upload valid OPT    minimal/minimal_admin.opt
    upload valid OPT    minimal/minimal_evaluation.opt
    (admin) delete all OPTs
    validate DELETE ALL response - 204 deleted ${2}


004 ADMIN - Delete Existing Template
    [Tags]    382  not-ready  bug
    upload valid OPT    minimal/minimal_admin.opt
    (admin) delete OPT

        TRACE GITHUB ISSUE    382    message=see https://github.com/ehrbase/project_management/issues/382#issuecomment-777255800 for details

    validate DELETE response - 204 deleted
    [Teardown]    (admin) delete all OPTs


005 ADMIN - Delete Non-Existing Template
                        prepare new request session    XML
    ${resp}=            REST.DELETE    /admin/template/foo
                        Integer    response status    404
                        String     response body    pattern= .*Operational template with id foo not found
                        Output     response body


006 ADMIN - Invalid Usage of Delete Endpoint
                        prepare new request session    XML
    ${resp}=            REST.DELETE    /admin/template/
                        Integer    response status    404
                        Output     response body
    

007 ADMIN - Update Non-Existing Template
    generate random templade_id
    (admin) update OPT    minimal/minimal_admin_updated.opt
    validate PUT response - 404 unknown templade_id


008 ADMIN - Update Existing Template
    [Tags]    382  not-ready  bug
    upload valid OPT    minimal/minimal_admin.opt
    (admin) update OPT    minimal/minimal_admin_updated.opt

        TRACE GITHUB ISSUE    382    message=see https://github.com/ehrbase/project_management/issues/382#issuecomment-777049676 for details

    validate PUT response - 200 updated
    [Teardown]    (admin) delete all OPTs


009 ADMIN - Delete Multiple Templates Where Some Are In Use
    upload valid OPT    minimal/minimal_admin.opt
    upload valid OPT    minimal/minimal_evaluation.opt
    create new EHR (XML)
    commit composition (XML)    minimal/minimal_admin.composition.extdatetimes.xml
    (admin) delete all OPTs
    validate DELETE ALL response - 422 unprocessable entity

    [Teardown]    Run Keywords    (admin) delete composition    AND
                  ...             (admin) delete all OPTs


010a ADMIN - Delete Template That Is In Use
    upload valid OPT    minimal/minimal_admin.opt
    create new EHR (XML)
    commit composition (XML)    minimal/minimal_admin.composition.extdatetimes.xml
    (admin) delete OPT
    validate DELETE response - 422 unprocessable entity

    [Teardown]    Run Keywords    (admin) delete composition    AND
                  ...             (admin) delete all OPTs


010c ADMIN - Delete Template That Was In Use - (Admin)Deleted Composition
    [Documentation]    Composition is deleted with the admin endpoint and thus has been removed 
    ...                "physically" from database. The admin endpoint will respond with a 204
    ...                response and the template is removed.
    [Tags]    382  not-ready  bug
    upload valid OPT    minimal/minimal_admin.opt
    create new EHR (XML)
    commit composition (XML)    minimal/minimal_admin.composition.extdatetimes.xml
    (admin) delete composition
    (admin) delete OPT

        TRACE GITHUB ISSUE    382    message=see https://github.com/ehrbase/project_management/issues/382#issuecomment-777255800 for details

    validate DELETE response - 204 deleted
    # comment: check that template does not exist any more
    ${resp}=    Get Request    ${SUT}    /definition/template/adl1.4/${template_id}
                Should Be Equal As Strings    ${resp.status_code}    404

    [Teardown]    Run Keywords    (admin) delete all OPTs


010b ADMIN - Delete Template That Is In Use - Deleted Composition
    [Tags]    
    upload valid OPT    minimal/minimal_admin.opt
    create new EHR (XML)
    commit composition (XML)    minimal/minimal_admin.composition.extdatetimes.xml
    delete composition    ${version_uid}
    (admin) delete OPT
    validate DELETE response - 422 unprocessable entity

    [Teardown]    Run Keywords    (admin) delete all OPTs


011 ADMIN - Update Template That Is In Use
    [Tags]    382    not-ready    bug
    upload valid OPT    minimal/minimal_admin.opt
    create new EHR (XML)
    commit composition (XML)    minimal/minimal_admin.composition.extdatetimes.xml
    (admin) update OPT    minimal/minimal_admin_updated.opt

        TRACE GITHUB ISSUE    382    message=see https://github.com/ehrbase/project_management/issues/382#issuecomment-777248693 for details

    validate PUT response - 422 unprocessable entity

    # TODO: @WLAD make sure the template was NOT modified!
    #       use a GET request, s. example below:
    ${resp}=    Get Request    ${SUT}    /definition/template/adl1.4/${template_id}
                ...    headers=${headers}
                log    ${resp.content}
                XML.Element Text Should Be    ${resp.content}    Minimal admin
                ...                           xpath=concept

    [Teardown]    Run Keywords    (admin) delete composition    AND
                  ...             (admin) delete all OPTs


012 ADMIN - Invalid Usage of Update Endpoint
                        prepare new request session    XML
    ${resp}=            REST.PUT    /admin/template/
                        Integer    response status    404
                        Output     response body


013 ADMIN - Invalid Usage of Update Endpoint
                        prepare new request session    XML
    ${resp}=            REST.PUT    /admin/template/foo
                        Integer    response status    400
                        Output     response body


014 ADMIN - Invalid Usage of Update Endpoint
                        prepare new request session    XML
    ${resp}=            REST.PUT    /admin/template/foo    {"foo": "bar"}
                        Integer    response status    404
                        Output     response body
                        String     response body    pattern=.*Template with id foo does not exist


015 ADMIN - Invalid Usage of Update Endpoint
                        prepare new request session    XML
    ${resp}=            REST.PUT    /admin/template/${123}    {"foo": "bar"}
                        Integer    response status    404
                        Output     response body
                        String     response body    pattern=.*Template with id 123 does not exist





*** Keywords ***

startup SUT
    [Documentation]     Overrides `generic_keywords.startup SUT` keyword
    ...                 to add some ENVs required by this test suite.

    Set Environment Variable    ADMINAPI_ACTIVE    true
    Set Environment Variable    ADMINAPI_ALLOWDELETEALL    true
    Set Environment Variable    SYSTEM_ALLOWTEMPLATEOVERWRITE    true
    generic_keywords.startup SUT


upload valid OPT
    [Arguments]           ${opt file}

    prepare new request session    XML
    ...    Prefer=return=representation
    get valid OPT file    ${opt file}
    extract template_id from OPT file
    upload OPT file
    Set Test Variable    ${response}    ${response}
    server accepted OPT
    

(admin) update OPT
    [Arguments]         ${opt_file}
                        prepare new request session    XML
                        ...    Prefer=return=representation
                        get valid OPT file    ${opt_file}
                        # upload OPT file
    ${resp}=            Put Request    ${SUT}    /admin/template/${template_id}
                        ...    data=${file}    headers=${headers}
                        Set Test Variable    ${response}    ${resp}


validate PUT response - 200 updated
                        Should Be Equal As Strings    ${response.status_code}   200
                        log    ${response.content}
                        XML.Element Text Should Be    ${response.content}    Minimal Admin UPDATED BY ROBOT
                        ...                           xpath=concept


validate PUT response - 404 unknown templade_id
                        log   ${response.content}
                        Should Be Equal As Strings    ${response.status_code}    404
                        Should Match    ${response.text}    *Template with id ${template_id} does not exist*


validate PUT response - 422 unprocessable entity
                        log   ${response.content}
                        Should Be Equal As Strings    ${response.status_code}    422
                        Should Match    ${response.text}    *Template with id ${template_id} is used by X composition(s)*


(admin) delete OPT
    [Documentation]     Admin delete OPT on server.
    ...                 Depends on any KW that exposes an variable named 'template_id'
    ...                 to test or suite level scope.
                        prepare new request session
                        ...    Prefer=return=representation
    &{resp}=            REST.DELETE    ${baseurl}/admin/template/${template_id}
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


validate DELETE response - 204 deleted
                        Integer    response status   204
                        String     response body    ${EMPTY}


validate DELETE response - 422 unprocessable entity
                        Integer    response status   422
                        String     response body error
                        ...        pattern=Cannot delete template minimal_admin.en.v1 since the following compositions are still using it.*


(admin) delete all OPTs
    [Documentation]     Admin delete OPT on server.
    ...                 Depends on any KW that exposes an variable named 'template_id'
    ...                 to test or suite level scope.
                        prepare new request session
    &{resp}=            REST.DELETE    ${baseurl}/admin/template/all
                        Set Test Variable    ${response}    ${resp}
                        Output Debug Info To Console


validate DELETE ALL response - 204 deleted ${amount}
                        Integer    response status   200
                        Integer    response body deleted    ${amount}


validate DELETE ALL response - 422 unprocessable entity
                        Integer    response status   422
                        String     response body error
                        ...        pattern=Cannot delete template minimal_admin.en.v1 since the following compositions are still using it.*










# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]


# VARIANTS
# PUT /admin/template/{template_id}    200
# PUT /admin/template/{template_id}    404
# PUT /admin/template/{template_id}    422
# PUT /admin/template/                 404
# PUT /admin/template/123              404
# PUT /admin/template/foobar           404

# DELETE /admin/template/{template_id}    204
# DELETE /admin/template/{template_id}    422
# DELETE /admin/template/all              200
# DELETE /admin/template/all              422
# DELETE /admin/template/all              200 (ohne vorher opts hochzuladen)
# DELETE /admin/template/all              200 (nur 1 opt vorher hochgeladen)
