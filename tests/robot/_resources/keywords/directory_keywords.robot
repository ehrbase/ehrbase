# Robot Framework

*** Settings ***
Documentation    DIRECTORY Specific Keywords
Library          XML
Library          String

Resource    ${CURDIR}${/}../suite_settings.robot
Resource    generic_keywords.robot
Resource    template_opt1.4_keywords.robot
Resource    ehr_keywords.robot
Resource    composition_keywords.robot
Resource    contribution_keywords.robot



*** Variables ***
${VALID DIR DATA SETS}     ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/directory
${INVALID DIR DATA SETS}   ${PROJECT_ROOT}/tests/robot/_resources/test_data_sets/directory



*** Keywords ***
# oooo    oooo oooooooooooo oooooo   oooo oooooo   oooooo     oooo   .oooooo.   ooooooooo.   oooooooooo.    .oooooo..o
# `888   .8P'  `888'     `8  `888.   .8'   `888.    `888.     .8'   d8P'  `Y8b  `888   `Y88. `888'   `Y8b  d8P'    `Y8
#  888  d8'     888           `888. .8'     `888.   .8888.   .8'   888      888  888   .d88'  888      888 Y88bo.
#  88888[       888oooo8       `888.8'       `888  .8'`888. .8'    888      888  888ooo88P'   888      888  `"Y8888o.
#  888`88b.     888    "        `888'         `888.8'  `888.8'     888      888  888`88b.     888      888      `"Y88b
#  888  `88b.   888       o      888           `888'    `888'      `88b    d88'  888  `88b.   888     d88' oo     .d8P
# o888o  o888o o888ooooood8     o888o           `8'      `8'        `Y8bood8P'  o888o  o888o o888bood8P'   8""88888P'
#
# [ HIGH LEVEL KEYWORDS ]



#                                            .
#                                          .o8
#  .ooooo.  oooo d8b  .ooooo.   .oooo.   .o888oo  .ooooo.
# d88' `"Y8 `888""8P d88' `88b `P  )88b    888   d88' `88b
# 888        888     888ooo888  .oP"888    888   888ooo888
# 888   .o8  888     888    .o d8(  888    888 . 888    .o
# `Y8bod8P' d888b    `Y8bod8P' `Y888""8o   "888" `Y8bod8P'
#
# [ SUCEED CREATING ]

create DIRECTORY (JSON)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  CREATE DIRECTORY (JSON)

                        load valid dir test-data-set    ${valid_test_data_set}

                        POST /ehr/ehr_id/directory    JSON

                        Log  TO CLARIFY: @AXEL - version_uid format???  level=WARN
                            # API spec: 8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1
                            # version_uid has also to be part of `Location` and `ETag` in response headers

                        Set Test Variable  ${folder_uid}  ${response.json()['uid']['value']}
                        Set Test Variable  ${version_uid}  ${response.json()['uid']['value']}  #TODO: + ::openEHRSys.example.com::1
                        Set Test Variable  ${preceding_version_uid}  ${version_uid}

                        capture point in time    of_first_version


create DIRECTORY -w/o- (JSON)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  CREATE DIRECTORY NO BODY (JSON)

                        load valid dir test-data-set    ${valid_test_data_set}

                        POST /ehr/ehr_id/directory (w/o)    JSON


create DIRECTORY (XML)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  CREATE DIRECTORY (XML)

                        load valid dir test-data-set    ${valid_test_data_set}

                        POST /ehr/ehr_id/directory    XML



# [ FAIL CREATING ]

create DIRECTORY - fake ehr_id (JSON)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  FAIL CREATING DIR 1 (JSON)

                        load valid dir test-data-set    ${valid_test_data_set}

                        POST /ehr/ehr_id/directory    JSON

                        Should Be Equal As Strings   ${response.status_code}   404


create DIRECTORY - invalid content (JSON)
    [Arguments]         ${invalid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  FAIL CREATING DIR 2 (JSON)

                        load invalid test-data-set    ${invalid_test_data_set}

                        POST /ehr/ehr_id/directory    JSON

                        Should Be Equal As Strings   ${response.status_code}   400





#                              .o8                .
#                             "888              .o8
# oooo  oooo  oo.ooooo.   .oooo888   .oooo.   .o888oo  .ooooo.
# `888  `888   888' `88b d88' `888  `P  )88b    888   d88' `88b
#  888   888   888   888 888   888   .oP"888    888   888ooo888
#  888   888   888   888 888   888  d8(  888    888 . 888    .o
#  `V88V"V8P'  888bod8P' `Y8bod88P" `Y888""8o   "888" `Y8bod8P'
#              888
#             o888o
#
# [ SUCCEED UPDATING ]

update DIRECTORY (JSON)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  UPDATE DIRECTORY (JSON)

                        load valid dir test-data-set    ${valid_test_data_set}

                        PUT /ehr/ehr_id/directory    JSON

                        Set Test Variable  ${folder_uid}  ${response.json()['uid']}
                        Set Test Variable  ${version_uid}  ${response.json()['uid']}  #TODO: + ::openEHRSys.example.com::1
                        Set Test Variable  ${preceding_version_uid}  ${version_uid}

                        capture point in time    of_updated_version


update DIRECTORY (XML)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  UPDATE DIRECTORY (XML)

                        load valid dir test-data-set    ${valid_test_data_set}

                        PUT /ehr/ehr_id/directory    XML


# [ FAIL UPDATING ]

update DIRECTORY - fake ehr_id (JSON)
    [Arguments]         ${valid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  FAIL UPDATING DIR 1 (JSON)

                        load valid dir test-data-set    ${valid_test_data_set}

                        PUT /ehr/ehr_id/directory    JSON

                        Should Be Equal As Strings   ${response.status_code}   404


update DIRECTORY - invalid content (JSON)
    [Arguments]         ${invalid_test_data_set}
                        Set Test Variable  ${KEYWORD NAME}  FAIL UPDATING DIR 2 (JSON)

                        load invalid test-data-set    ${invalid_test_data_set}

                        PUT /ehr/ehr_id/directory    JSON

                        Should Be Equal As Strings   ${response.status_code}   400





#       .o8            oooo                .
#      "888            `888              .o8
#  .oooo888   .ooooo.   888   .ooooo.  .o888oo  .ooooo.
# d88' `888  d88' `88b  888  d88' `88b   888   d88' `88b
# 888   888  888ooo888  888  888ooo888   888   888ooo888
# 888   888  888    .o  888  888    .o   888 . 888    .o
# `Y8bod88P" `Y8bod8P' o888o `Y8bod8P'   "888" `Y8bod8P'
#
# [ SUCCEED DELETING ]

delete DIRECTORY (JSON)
    [Documentation]
                        Set Test Variable  ${KEYWORD NAME}  DELETE DIRECTORY (JSON)

                        DELETE /ehr/ehr_id/directory    JSON


# [ FAIL DELETING ]

delete DIRECTORY - fake ehr_id (JSON)
    [Documentation]     EHR does not exist
                        Set Test Variable  ${KEYWORD NAME}  DELETE DIRECTORY (JSON)

                        generate fake version_uid

                        DELETE /ehr/ehr_id/directory    JSON


delete DIRECTORY - fake version_uid (JSON)
    [Documentation]     EHR exists but preceding_version_uid does not match
                        Set Test Variable  ${KEYWORD NAME}  DELETE DIRECTORY (JSON)

                        generate fake version_uid

                        DELETE /ehr/ehr_id/directory    JSON





#                        .             o8o
#                      .o8             `"'
# oooo d8b  .ooooo.  .o888oo oooo d8b oooo   .ooooo.  oooo    ooo  .ooooo.
# `888""8P d88' `88b   888   `888""8P `888  d88' `88b  `88.  .8'  d88' `88b
#  888     888ooo888   888    888      888  888ooo888   `88..8'   888ooo888
#  888     888    .o   888 .  888      888  888    .o    `888'    888    .o
# d888b    `Y8bod8P'   "888" d888b    o888o `Y8bod8P'     `8'     `Y8bod8P'
#
# [ SUCCEED RETRIEVING ]

  # NOTE: also used for HAS DIRECTORY/FOLDER/PATH cases

get DIRECTORY (JSON)
    [Documentation]     Retrieves the version of the directory FOLDER associated
    ...                 with the EHR identified by `ehr_id`.
    ...                 NOTE: this is "Get folder in directory version at time"
    ...                 **without** URI parameter (which are optional)!
    ...                 check the API here:
    ...     https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-get-1

                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY (JSON)

                        GET /ehr/ehr_id/directory    JSON
            

get DIRECTORY at time (JSON)
    [Documentation]     :time: valid time in the extended ISO8601 format
    ...                 e.g. 2015-01-20T19:30:22.765+01:00

    [Arguments]         ${time}

                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY AT TIME (JSON)

                        Set Test Variable    ${version_at_time}    ${time}

                        GET /ehr/ehr_id/directory?version_at_time    JSON


get DIRECTORY at current time (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY AT NOW (JSON)

                        capture point in time    current

                        Set Test Variable    ${version_at_time}    ${time_current}

                        GET /ehr/ehr_id/directory?version_at_time    JSON


get FOLDER in DIRECTORY (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER (JSON)

                        GET /ehr/ehr_id/directory?path    JSON


get FOLDER in DIRECTORY at time (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT TIME (JSON)

                        GET /ehr/ehr_id/directory?version_at_time&path    JSON


get DIRECTORY at version (JSON)
    [Documentation]     Retrieves a particular version of the directory FOLDER 
    ...                 identified by `version_uid` and associated with the EHR
    ...                 identified by `ehr_id`.

                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY AT VERSION (JSON)

                        GET /ehr/ehr_id/directory/version_uid    JSON


get FOLDER in DIRECTORY at version (JSON)
    [Arguments]         ${path}
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT VERSION (JSON)

                        Set Test Variable    ${path}    ${path}

                        GET /ehr/ehr_id/directory/version_uid?path    JSON


# get SUBFOLDER in DIRECTORY at version (JSON)
#     Fail    msg=brake it till you make it!
#
#
# get SUBFOLDER in DIRECTORY at time (JSON)
#     Fail    msg=brake it till you make it!



# [ FAIL RETRIEVING ]

get DIRECTORY - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY (JSON)

                        GET /ehr/ehr_id/directory    JSON


get DIRECTORY at time - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY (JSON)

                        GET /ehr/ehr_id/directory?version_at_time    JSON


get FOLDER in DIRECTORY at time - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT TIME (JSON)

                        GET /ehr/ehr_id/directory?version_at_time&path    JSON


get DIRECTORY at version - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY AT VERSION (JSON)

                        generate fake version_uid

                        GET /ehr/ehr_id/directory/version_uid    JSON


get DIRECTORY at version - fake version_uid (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY AT VERSION (JSON)

                        generate fake version_uid

                        GET /ehr/ehr_id/directory/version_uid    JSON


get FOLDER in DIRECTORY at version - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT VERSION (JSON)

                        GET /ehr/ehr_id/directory/version_uid?path    JSON



get FOLDER in DIRECTORY at version - fake path (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT VERSION (JSON)

                        generate random path

                        GET /ehr/ehr_id/directory/version_uid?path    JSON



get FOLDER in DIRECTORY at version - fake version_uid/path (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT VERSION (JSON)

                        generate fake version_uid
                        
                        generate random path

                        GET /ehr/ehr_id/directory/version_uid?path    JSON



# get SUBFOLDER in DIRECTORY at version - fake ehr_id (JSON)
#     Fail    msg=brake it till you make it!
#
#
# get SUBFOLDER in DIRECTORY at time - fake ehr_id (JSON)
#     Fail    msg=brake it till you make it!







# oooooooooooo ooooo      ooo oooooooooo.   ooooooooo.     .oooooo.   ooooo ooooo      ooo ooooooooooooo  .oooooo..o
# `888'     `8 `888b.     `8' `888'   `Y8b  `888   `Y88.  d8P'  `Y8b  `888' `888b.     `8' 8'   888   `8 d8P'    `Y8
#  888          8 `88b.    8   888      888  888   .d88' 888      888  888   8 `88b.    8       888      Y88bo.
#  888oooo8     8   `88b.  8   888      888  888ooo88P'  888      888  888   8   `88b.  8       888       `"Y8888o.
#  888    "     8     `88b.8   888      888  888         888      888  888   8     `88b.8       888           `"Y88b
#  888       o  8       `888   888     d88'  888         `88b    d88'  888   8       `888       888      oo     .d8P
# o888ooooood8 o8o        `8  o888bood8P'   o888o         `Y8bood8P'  o888o o8o        `8      o888o     8""88888P'
#
# [ HTTP METHODS / ENDPOINTS ]



# oooo            .       .                                                     .
# `888          .o8     .o8                                                   .o8
#  888 .oo.   .o888oo .o888oo oo.ooooo.       oo.ooooo.   .ooooo.   .oooo.o .o888oo
#  888P"Y88b    888     888    888' `88b       888' `88b d88' `88b d88(  "8   888
#  888   888    888     888    888   888       888   888 888   888 `"Y88b.    888
#  888   888    888 .   888 .  888   888       888   888 888   888 o.  )88b   888 .
# o888o o888o   "888"   "888"  888bod8P'       888bod8P' `Y8bod8P' 8""888P'   "888"
#                              888             888
#                             o888o           o888o
#
# [ HTTP POST ]

POST /ehr/ehr_id/directory
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method POST on /ehr/ehr_id/directory endpoint
    ...                 DEPENDENCY: the following variables in test level scope:
    ...                 `\${ehr_id}`, `\${test_data}`

                        prepare directory request session    ${format}
                        ...                 Prefer=return=representation

    ${resp}=            Post Request        ${SUT}   /ehr/${ehr_id}/directory
                        ...                 data=${test_data}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  POST /ehr/ehr_id/directory


POST /ehr/ehr_id/directory (w/o)
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method POST on /ehr/ehr_id/directory endpoint
    ...                 WITHOUT (w/o) Prefer=return=representation header
    ...                 DEPENDENCY: the following variables in test level scope:
    ...                 `\${ehr_id}`, `\${test_data}`

                        prepare directory request session    ${format}

    ${resp}=            Post Request        ${SUT}   /ehr/${ehr_id}/directory
                        ...                 data=${test_data}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  POST /ehr/ehr_id/directory (w/o r)





# oooo            .       .                                              .
# `888          .o8     .o8                                            .o8
#  888 .oo.   .o888oo .o888oo oo.ooooo.       oo.ooooo.  oooo  oooo  .o888oo
#  888P"Y88b    888     888    888' `88b       888' `88b `888  `888    888
#  888   888    888     888    888   888       888   888  888   888    888
#  888   888    888 .   888 .  888   888       888   888  888   888    888 .
# o888o o888o   "888"   "888"  888bod8P'       888bod8P'  `V88V"V8P'   "888"
#                              888             888
#                             o888o           o888o
#
# [ HTTP PUT ]

PUT /ehr/ehr_id/directory
    [Documentation]     Executes HTTP method PUT on /ehr/ehr_id/directory endpoint
    ...                 DEPENDENCY: the following variables in test level scope:
    ...                 `\${ehr_id}`, \${preceding_version_uid}, `\${test_data}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}
                        ...                 Prefer=return=representation
                        ...                 If-Match=${preceding_version_uid}

        TRACE GITHUB ISSUE  NO-ISSUE-ID  not-ready  message=endpoint not implemented  loglevel=WARN

    ${resp}=            Put Request        ${SUT}   /ehr/${ehr_id}/directory
                        ...                 data=${test_data}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  PUT /ehr/ehr_id/directory


PUT /ehr/ehr_id/directory (w/o prefer)
    [Documentation]     Executes HTTP method PUT on /ehr/ehr_id/directory endpoint
    ...                 WITHOUT (w/o) Prefer=return=representation header
    ...                 DEPENDENCY: the following variables in test level scope:
    ...                 `\${ehr_id}`, ${preceding_version_uid}, `\${test_data}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}
                        ...                 If-Match=${preceding_version_uid}

        TRACE GITHUB ISSUE  NO-ISSUE-ID  not-ready  message=endpoint not implemented  loglevel=WARN

    ${resp}=            Put Request        ${SUT}   /ehr/${ehr_id}/directory
                        ...                 data=${test_data}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  PUT /ehr/ehr_id/directory





# oooo            .       .                         .o8            oooo                .
# `888          .o8     .o8                        "888            `888              .o8
#  888 .oo.   .o888oo .o888oo oo.ooooo.        .oooo888   .ooooo.   888   .ooooo.  .o888oo  .ooooo.
#  888P"Y88b    888     888    888' `88b      d88' `888  d88' `88b  888  d88' `88b   888   d88' `88b
#  888   888    888     888    888   888      888   888  888ooo888  888  888ooo888   888   888ooo888
#  888   888    888 .   888 .  888   888      888   888  888    .o  888  888    .o   888 . 888    .o
# o888o o888o   "888"   "888"  888bod8P'      `Y8bod88P" `Y8bod8P' o888o `Y8bod8P'   "888" `Y8bod8P'
#                              888
#                             o888o
#
# [ HTTP DELETE ]

DELETE /ehr/ehr_id/directory
    [Documentation]     Executes HTTP method DELETE on /ehr/ehr_id/directory endpoint
    ...                 DEPENDENCY: the following variables in test level scope
    ...                 `\${ehr_id}`, `\${preceding_version_uid}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}
                        ...             If-Match=${preceding_version_uid}

        TRACE GITHUB ISSUE  NO-ISSUE-ID  not-ready  message=endpoint not implemented  loglevel=WARN

    ${resp}=            Delete Request      ${SUT}   /ehr/${ehr_id}/directory
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  DELETE /ehr/ehr_id/directory





# oooo            .       .                                            .
# `888          .o8     .o8                                          .o8
#  888 .oo.   .o888oo .o888oo oo.ooooo.        .oooooooo  .ooooo.  .o888oo
#  888P"Y88b    888     888    888' `88b      888' `88b  d88' `88b   888
#  888   888    888     888    888   888      888   888  888ooo888   888
#  888   888    888 .   888 .  888   888      `88bod8P'  888    .o   888 .
# o888o o888o   "888"   "888"  888bod8P'      `8oooooo.  `Y8bod8P'   "888"
#                              888            d"     YD
#                             o888o           "Y88888P'
#
# [ HTTP GET ]

GET /ehr/ehr_id/directory/version_uid
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `\${ehr_id}`, `\${version_uid}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}

        TRACE GITHUB ISSUE  81  not-ready

    ${resp}=            Get Request         ${SUT}   /ehr/${ehr_id}/directory/${version_uid}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory/version_uid


GET /ehr/ehr_id/directory/version_uid?path
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `\${ehr_id}`, `\${version_uid}`, `\${path}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}

    ${resp}=            Get Request         ${SUT}   /ehr/${ehr_id}/directory/${version_uid}?path=${path}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory/version_uid?path


GET /ehr/ehr_id/directory
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variable in test level scope:
    ...                 `\${ehr_id}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}

    ${resp}=            Get Request         ${SUT}   /ehr/ehr_id/directory
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory

        TRACE GITHUB ISSUE  41  not-ready
        ...               message=DISCOVERED ERROR: Get folder in directory version at time fails


GET /ehr/ehr_id/directory?version_at_time
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `\${ehr_id}`, `\${version_at_time}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}

        TRACE GITHUB ISSUE  41  not-ready
        ...               message=DISCOVERED ERROR: Get folder in directory version at time fails

    ${resp}=            Get Request         ${SUT}   /ehr/ehr_id/directory?version_at_time=${version_at_time}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory?version_at_time


GET /ehr/ehr_id/directory?path
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `\${ehr_id}`, `\${path}`

    [Arguments]         ${format}

                        prepare directory request session    ${format}

        TRACE GITHUB ISSUE  41  not-ready
        ...               message=DISCOVERED ERROR: Get folder in directory version at time fails

    ${resp}=            Get Request         ${SUT}   /ehr/ehr_id/directory?paht=${path}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory?path


GET /ehr/ehr_id/directory?version_at_time&path
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `\${ehr_id}`, `\${version_at_time}`, \${path}

    [Arguments]         ${format}

                        prepare directory request session    ${format}

        TRACE GITHUB ISSUE  41  not-ready
        ...               message=DISCOVERED ERROR: Get folder in directory version at time fails

    ${resp}=            Get Request         ${SUT}   /ehr/ehr_id/directory?version_at_time=${version_at_time}&paht=${path}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory?version_at_time&path







# ooooooooo.   oooooooooooo  .oooooo..o ooooooooo.     .oooooo.   ooooo      ooo  .oooooo..o oooooooooooo  .oooooo..o
# `888   `Y88. `888'     `8 d8P'    `Y8 `888   `Y88.  d8P'  `Y8b  `888b.     `8' d8P'    `Y8 `888'     `8 d8P'    `Y8
#  888   .d88'  888         Y88bo.       888   .d88' 888      888  8 `88b.    8  Y88bo.       888         Y88bo.
#  888ooo88P'   888oooo8     `"Y8888o.   888ooo88P'  888      888  8   `88b.  8   `"Y8888o.   888oooo8     `"Y8888o.
#  888`88b.     888    "         `"Y88b  888         888      888  8     `88b.8       `"Y88b  888    "         `"Y88b
#  888  `88b.   888       o oo     .d8P  888         `88b    d88'  8       `888  oo     .d8P  888       o oo     .d8P
# o888o  o888o o888ooooood8 8""88888P'  o888o         `Y8bood8P'  o8o        `8  8""88888P'  o888ooooood8 8""88888P'
#
# [ RESPONSE VALIDATION ]

# POST POST POST POST
#/////////////////////

validate POST response - 201 created
    [Documentation]     CASE: new directory was created.
    ...                 Request was send with `Prefer=return=representation`.

                        Should Be Equal As Strings    ${response.status_code}    201
                        #TODO:  Should Be Equal       ${response.json()['status']}    OK / Created

                        Dictionary Should Contain Key    ${response.json()}    uid
                        Dictionary Should Contain Key    ${response.json()}    folders
                        # Dictionary Should Contain Item    ${response.json()['folders']}    _type  FOLDER 

                        Dictionary Should Contain Key    ${response.headers}    Location
                        #TODO: value of Location as per API spec:
                        #      Location: {baseUrl}/ehr/$ehr_id/directory/version_uid
                        #      where the last part is version_uid
                        #      version_uid format: 8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1
                        Dictionary Should Contain Key    ${response.headers}    ETag

        TRACE GITHUB ISSUE  37  not-ready  message=Response header ETag does not match expected value

                        Dictionary Should Contain Item    ${response.headers}    ETag  ${version_uid}


validate POST response (w/o) - 201 created
    [Documentation]     CASE: new directory was created.
    ...                 NO `Prefer` header was send thus no content in body!

                        Should Be Equal As Strings    ${response.status_code}    201

                        Log    ${response.json()['status']}
                        #TODO:  Should Be Equal          ${response.json()['status']}    OK / Created
                        Dictionary Should Contain Key    ${response.headers}    Location
                        Dictionary Should Contain Key    ${response.headers}    ETag
                        # TODO: Dictionary Should Contain Item    ${response.headers}    ETag  ${version_uid}


validate POST response - 400 invalid ehr_id
    [Documentation]     CASE: invalid `ehr_id`

                        Should Be Equal As Strings    ${response.status_code}    400

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        # NOTE: high risk of false positive!!!
                        Log  VERIFY 400 is correct here!  level=WARN    #TODO: remove when code/tests matures


validate POST response - 400 invalid content
    [Documentation]     CASE: invalid content - e.g. could not be converted to a valid directory FOLDER.

                        Should Be Equal As Strings    ${response.status_code}    400

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        Log  VERIFY 400 is correct here!  level=WARN    #TODO: remove when code/tests matures


validate POST response - 404 unknown ehr_id
    [Documentation]     CASE: EHR with `ehr_id` does not exist.

                        Should Be Equal As Strings    ${response.status_code}    404

                        Should Be Equal    ${response.json()['status']}    Not Found
                        Should Be Equal    ${response.json()['error']}    EHR with id ${ehr_id} not found.


validate POST response - 409 folder already exists
    [Documentation]     CASE: EHR with `ehr_id` already has a directory FOLDER.
    ...                 NOTE: @PABLO this is not (yet) in the SPEC

        TRACE GITHUB ISSUE  NO-ISSUI-ID  not-ready

                        Should Be Equal As Strings    ${response.status_code}    409

                        #TODO:  Should Be Equal    ${response.json()['status']}    Not Found
                        #TODO:  Should Be Equal    ${response.json()['error']}    EHR with id ${ehr_id} already has a FOLDER.



# PUT PUT PUT PUT PUT
#////////////////////

validate PUT response - 200 updated
    [Documentation]     CASE: directory successfully updated.
    ...                 Updated resource is returned if `Prefer` header is set to `return=representation`.

                        Should Be Equal As Strings    ${response.status_code}    200

                        #TODO:  Should Be Equal    ${response.json()['status']}    OK / Updated
                        #TODO:  Should Be Equal    ${response.json()['message']} ...


validate PUT response - 204 updated
    [Documentation]     CASE: directory was updated AND `Prefer` header is missing
    ...                 or `Prefer` header is set to `return=minimal`.

                        Should Be Equal As Strings    ${response.status_code}    204

                        #TODO:  Should Be Equal    ${response.json()['status']}    OK / Updated
                        #TODO:  Should Be Equal    ${response.json()['message']} ...


validate PUT response - 400 invalid ehr_id
    [Documentation]     CASE: invalid `ehr_id`.

                        Should Be Equal As Strings    ${response.status_code}    400

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        # NOTE: high risk of false positive!!!
                        Log  VERIFY 400 is correct here!  level=WARN    #TODO: remove when code/tests matures


validate PUT response - 400 invalid content
    [Documentation]     CASE:  invalid content - e.g. could not be converted to a valid directory FOLDER.
                        Should Be Equal As Strings    ${response.status_code}    400

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        # NOTE: high risk of false positive!!!
                        Log  VERIFY 400 is correct here!  level=WARN    #TODO: remove when code/tests matures


validate PUT response - 404 unknown ehr_id
    [Documentation]     CASE: EHR with `ehr_id` does not exist.

                        Should Be Equal As Strings    ${response.status_code}    404

                        #TODO:  Should Be Equal    ${response.json()['status']}    Not Found
                        #TODO:  Should Be Equal    ${response.json()['error']} ...


validate PUT response - 412 precondition failed
    [Documentation]     CASE: `If-Match` request header doesn’t match the latest version.
    ...                 Returns also latest `version_uid` in the `Location` and `ETag` headers.

                        Should Be Equal As Strings    ${response.status_code}    412

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        Dictionary Should Contain Key    ${response.headers}    Location
                        Dictionary Should Contain Key    ${response.headers}    ETag
                        #TODO: Dictionary Should Contain Item    ${response.headers}    ETag  ${version_uid}



# DELETE DELETE DELETE
#/////////////////////

validate DELETE response - 204 deleted
    [Documentation]     CASE: directory was deleted.

                        Should Be Equal As Strings    ${response.status_code}    204

                        #TODO:  Should Be Equal    ${response.json()['status']}    Deleted
                        #TODO:  Should Be Equal    ${response.json()['message']} ...


validate DELETE response - 400 invalid ehr_id
    [Documentation]     CASE: invalid `ehr_id`, headers, etc.

                        Should Be Equal As Strings    ${response.status_code}    400

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        # NOTE: high risk of false positive!!!
                        Log  VERIFY 400 is correct here!  level=WARN    #TODO: remove when code/tests matures


validate DELETE response - 404 unknown ehr_id
    [Documentation]     CASE: EHR with `ehr_id` does not exist.

                        Should Be Equal As Strings    ${response.status_code}    404

                        #TODO:  Should Be Equal    ${response.json()['status']}    Not Found
                        #TODO:  Should Be Equal    ${response.json()['error']} ...


validate DELETE response - 412 precondition failed
    [Documentation]     CASE: `If-Match` request header doesn’t match the latest version.
    ...                 Returns also latest `version_uid` in the `Location` and `ETag` headers.

                        Should Be Equal As Strings    ${response.status_code}    412

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...



# GET GET GET GET GET
#////////////////////

validate GET-@version response - 200 retrieved
    [Documentation]     CASE: requested directory FOLDER is successfully retrieved.

                        Should Be Equal As Strings    ${response.status_code}    200

                        #TODO:  Should Be Equal    ${response.json()['status']}    OK / Retrieved

                        Dictionary Should Contain Key    ${response.json()}    uid
                        Dictionary Should Contain Key    ${response.json()}    folders
                        # Dictionary Should Contain Item    ${response.json()['folder']}    _type  FOLDER


validate GET-@version response - 400 bad request
    [Documentation]     CASE: invalid `ehr_id`, `version_uid`, `path`, headers, etc.

                        Should Be Equal As Strings    ${response.status_code}    400

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        # NOTE: high risk of false positive!!!
                        Log  VERIFY 400 is correct here!  level=WARN    #TODO: remove when code/tests matures


validate GET-@version response - 404 unknown ehr_id
    [Documentation]     CASE: EHR with `ehr_id` does not exist.

                        Should Be Equal As Strings    ${response.status_code}    404

                        Should Be Equal    ${response.json()['status']}    Not Found
                        Should Be Equal    ${response.json()['error']}    EHR with id ${ehr_id} not found.


validate GET-@version response - 404 unknown version_uid
    [Documentation]     CASE: directory with `version_uid` does not exist.

                        Should Be Equal As Strings    ${response.status_code}    404

                        Should Be Equal    ${response.json()['status']}    Not Found
                        Should Be Equal    ${response.json()['error']}    Folder with id ${folder_uid} could not be found


validate GET-@version response - 404 unknown path
    [Documentation]     CASE: `path` does not exist within the directory.

                        Should Be Equal As Strings    ${response.status_code}    404

                        Should Be Equal               ${response.json()['status']}    Not Found
                        #TODO: Should Be Equal    ${response.json()['error']}    Path '${path}' could not be found.



# GET2 GET2 GET2 GET2
#////////////////////

validate GET response - 200 retrieved
    [Documentation]     CASE: requested directory FOLDER is successfully retrieved.
    ...                 NOTE: this check is for `GET /ehr/ehr_id/directory` endpoint
    ...                 without the optional URI parameters `version_at_time` and `path`.

                        Should Be Equal As Strings    ${response.status_code}    200

                        #TODO:  Should Be Equal    ${response.json()['status']}    OK / Retrieved

                        Dictionary Should Contain Key    ${response.json()}    uid
                        Dictionary Should Contain Key    ${response.json()}    folders
                        # Dictionary Should Contain Item    ${response.json()['folder']}    _type  FOLDER


validate GET-version@time response - 200 retrieved
    [Documentation]     CASE: requested directory FOLDER is successfully retrieved.

                        Should Be Equal As Strings    ${response.status_code}    200

                        #TODO:  Should Be Equal    ${response.json()['status']}    OK / Retrieved

                        Dictionary Should Contain Key    ${response.json()}    uid
                        Dictionary Should Contain Key    ${response.json()}    folders
                        # Dictionary Should Contain Item    ${response.json()['folder']}    _type  FOLDER


validate GET-version@time response - 204 folder has been deleted
    [Documentation]     CASE: directory at version_at_time time has been deleted.

                        Should Be Equal As Strings    ${response.status_code}    204

                        #TODO:  Should Be Equal    ${response.json()['status']}    Detled


validate GET-version@time response - 400 bad request
    [Documentation]     CASE: invalid `ehr_id`, `version_at_time`, `path`, headers, etc.

                        Should Be Equal As Strings    ${response.status_code}    400

                        #TODO:  Should Be Equal    ${response.json()['status']}    Bad Request
                        #TODO:  Should Be Equal    ${response.json()['error']} ...

                        # NOTE: high risk of false positive!!!
                        Log  VERIFY 400 is correct here!  level=WARN    #TODO: remove when code/tests matures


validate GET-version@time response - 404 unknown ehr_id
    [Documentation]     CASE: EHR with `ehr_id` does not exist.

                        Should Be Equal As Strings    ${response.status_code}    404

                        Should Be Equal    ${response.json()['status']}    Not Found
                        Should Be Equal    ${response.json()['error']}    EHR with id ${ehr_id} not found.


validate GET-version@time response - 404 unknown folder-version@time
    [Documentation]     CASE: directory does not exists at `version_at_time` time.

                        Should Be Equal As Strings    ${response.status_code}    404

                        #TODO:  Should Be Equal    ${response.json()['status']}    Not Found
                        #TODO:  Should Be Equal    ${response.json()['error']} ...


validate GET-version@time response - 404 unknown path
    [Documentation]     CASE: `path` does not exist within the directory.

                        Should Be Equal As Strings    ${response.status_code}    404

                        #TODO:  Should Be Equal    ${response.json()['status']}    Not Found
                        #TODO:  Should Be Equal    ${response.json()['error']} ...







# ooooo   ooooo oooooooooooo       .o.       oooooooooo.   oooooooooooo ooooooooo.    .oooooo..o
# `888'   `888' `888'     `8      .888.      `888'   `Y8b  `888'     `8 `888   `Y88. d8P'    `Y8
#  888     888   888             .8"888.      888      888  888          888   .d88' Y88bo.
#  888ooooo888   888oooo8       .8' `888.     888      888  888oooo8     888ooo88P'   `"Y8888o.
#  888     888   888    "      .88ooo8888.    888      888  888    "     888`88b.         `"Y88b
#  888     888   888       o  .8'     `888.   888     d88'  888       o  888  `88b.  oo     .d8P
# o888o   o888o o888ooooood8 o88o     o8888o o888bood8P'   o888ooooood8 o888o  o888o 8""88888P'
#
# [ HTTP HEADERS ]

prepare directory request session
    [Arguments]     ${format}=JSON    &{extra_headers}
    [Documentation]     Prepares request settings for usage with RequestLibrary
    ...                 :format: JSON (default) / XML
    ...                 :extra_headers: optional - e.g. Prefer=return=representation
    ...                                            e.g. If-Match={ehrstatus_uid}

                        # case: JSON
                        Run Keyword If      $format=='JSON'    set request headers
                        ...                 content=application/json
                        ...                 accept=application/json
                        ...                 &{extra_headers}

                        # case: XML
                        Run Keyword If      $format=='XML'    set request headers
                        ...                 content=application/xml
                        ...                 accept=application/xml
                        ...                 &{extra_headers}


set request headers
    [Arguments]         ${content}=application/json  ${accept}=application/json  &{extra_headers}
    [Documentation]     Sets the headers of a request
    ...                 :content: application/json (default) / application/xml
    ...                 :accept: application/json (default) / application/xml
    ...                 :extra_headers: optional

                        Log Many            ${content}  ${accept}  ${extra_headers}

    &{headers}=         Create Dictionary   Content-Type=${content}
                        ...                 Accept=${accept}

                        Run Keyword If      ${extra_headers}    Set To Dictionary    ${headers}    &{extra_headers}

                        Create Session      ${SUT}    ${${SUT}.URL}
                        ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True

                        Set Test Variable   ${headers}    ${headers}







# oooooooooooo       .o.       oooo    oooo oooooooooooo      oooooooooo.         .o.       ooooooooooooo       .o.
# `888'     `8      .888.      `888   .8P'  `888'     `8      `888'   `Y8b       .888.      8'   888   `8      .888.
#  888             .8"888.      888  d8'     888               888      888     .8"888.          888          .8"888.
#  888oooo8       .8' `888.     88888[       888oooo8          888      888    .8' `888.         888         .8' `888.
#  888    "      .88ooo8888.    888`88b.     888    "          888      888   .88ooo8888.        888        .88ooo8888.
#  888          .8'     `888.   888  `88b.   888       o       888     d88'  .8'     `888.       888       .8'     `888.
# o888o        o88o     o8888o o888o  o888o o888ooooood8      o888bood8P'   o88o     o8888o     o888o     o88o     o8888o
#
# [ FAKE DATA ]

generate random path
    ${rstring}=         Generate Random String  12  [LETTERS]
    ${path}=            Set Variable  /${rstring}
                        Set Test Variable    ${path}    ${path}


generate fake version_uid
    [Documentation]     Generates a random `folder_uid`, `version_uid` and exposes it
    ...                 (also as `preceding_version_uid`) to test level scope

    ${uid}=             Evaluate    str(uuid.uuid4())    uuid
                        Set Test Variable    ${folder_uid}    ${uid}
                        Set Test Variable    ${version_uid}    ${uid}
                        Set Test Variable    ${preceding_version_uid}    ${version_uid}



# [ HELPERS ]
extract version_uid from response (JSON)
                        Set Test Variable    ${version_uid}    ${response.json()['uid']}


load valid dir test-data-set
    [Arguments]        ${valid_test_data_set}

    ${file}=            Get File    ${VALID DIR DATA SETS}/${valid_test_data_set}

                        Set Test Variable    ${test_data}    ${file}


load invalid dir test-data-set
    [Arguments]        ${invalid_test_data_set}

    ${file}=            Get File    ${INVALID DIR DATA SETS}/${invalid_test_data_set}

                        Set Test Variable    ${test_data}    ${file}







# oooooooooo.        .o.         .oooooo.   oooo    oooo ooooo     ooo ooooooooo.
# `888'   `Y8b      .888.       d8P'  `Y8b  `888   .8P'  `888'     `8' `888   `Y88.
#  888     888     .8"888.     888           888  d8'     888       8   888   .d88'
#  888oooo888'    .8' `888.    888           88888[       888       8   888ooo88P'
#  888    `88b   .88ooo8888.   888           888`88b.     888       8   888
#  888    .88P  .8'     `888.  `88b    ooo   888  `88b.   `88.    .8'   888
# o888bood8P'  o88o     o8888o  `Y8bood8P'  o888o  o888o    `YbodP'    o888o
#
# [ BACKUP ]

*** Comments ***

VARIANTS x2 (JSON / XML)
  # CREATE
  # with Prefer=return=representation header
  create DIRECTORY - EHR is empty
  create DIRECTORY - EHR with directory
  create DIRECTORY - EHR does not exist
  create DIRECTORY - Content is invalid

  # without Prefer=return=representation header
  create DIRECTORY (w/o) - EHR is empty
  create DIRECTORY (w/o) - EHR with directory
  create DIRECTORY (w/o) - EHR does not exist


  # UPDATE
  # with Prefer=return=representation header
  update DIRECTORY - EHR with directory
  update DIRECTORY - EHR does not exist
  update DIRECTORY - Content is invalid

  # without Prefer=return=representation header
  update DIRECTORY (w/o) - EHR with directory
  update DIRECTORY (w/o) - EHR does not exist
  update DIRECTORY (w/o) - Content is invalid


  # DELETE
  delete DIRECTORY
  delete DIRECTORY - fake ehr_id
  delete DIRECTORY - fake version_uid


  # GET
  get DIRECTORY

  get DIRECTORY at time

  get DIRECTORY at version

  get FOLDER in DIRECTORY at version

  get FOLDER in DIRECTORY at time

  get SUBFOLDER in DIRECTORY at version

  get SUBFOLDER in DIRECTORY at time


  # HAS
  has DIRECTORY

  has DIRECTORY at time

  has DIRECTORY at version

  has FOLDER in DIRECTORY at version

  has FOLDER in DIRECTORY at time

  has SUBFOLDER in DIRECTORY at version

  has SUBFOLDER in DIRECTORY at time


  # RESPONSES
  validate POST response - 201 created
  validate POST response - 400 invalid ehr_id
  validate POST response - 400 invalid content
  validate POST response - 404 unknown ehr_id
  validate POST response - 409 folder already exists

  validate PUT response - 200 updated
  validate PUT response - 204 updated
  validate PUT response - 400 invalid ehr_id
  validate PUT response - 400 invalid content
  validate PUT response - 404 unknown ehr_id
  validate PUT response - 412 precondition failed
  
  validate DELETE response - 204 deleted
  validate DELETE response - 400 invalid ehr_id
  validate DELETE response - 404 unknown ehr_id
  validate DELETE response - 412 precondition failed

  validate GET response - 200 retrieved
  validate GET response - 404 unknown ehr_id
  validate GET response - 404 unknown version_uid
  validate GET response - 404 unknown path

  validate GET @version response - 200 retrieved
  validate GET @version response - 404 unknown ehr_id
  validate GET @version response - 404 unknown version_uid
  validate GET @version response - 404 unknown path

  validate GET version@time response - 200 retrieved
  validate GET version@time response - 204 folder has been deleted
  validate GET version@time response - 404 unknown ehr_id
  validate GET version@time response - 404 unknown folder-version@time
  validate GET version@time response - 404 unknown path


# *** keywords ***
# load dir test-data-set
#     [Arguments]        ${path}
#
#     Run Keyword If    '${VALID DIR DATA SETS}' in '${path}'  load valid dir test-data-set    ${path}
#     Run Keyword If    '${INVALID DIR DATA SETS}' in '${path}'  load invalid dir test-data-set    ${path}
