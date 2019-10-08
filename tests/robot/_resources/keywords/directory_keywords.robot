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

                        generate random version_uid

                        DELETE /ehr/ehr_id/directory    JSON


delete DIRECTORY - fake version_uid (JSON)
    [Documentation]     EHR exists but preceding_version_uid does not match
                        Set Test Variable  ${KEYWORD NAME}  DELETE DIRECTORY (JSON)

                        generate random version_uid

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
    # TODO: CLARIFY - would this be the same as GET VERSIONED DIRECTORY ???
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY (JSON)

                        GET /ehr/ehr_id/directory    JSON


get DIRECTORY at time (JSON)
    [Arguments]         ${time}
    [Documentation]     :time: valid time in the extended ISO8601 format
    ...                 e.g. 2015-01-20T19:30:22.765+01:00
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

                        Should Be Equal As Strings   ${response.status_code}   404


get DIRECTORY at time - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY (JSON)

                        GET /ehr/ehr_id/directory?version_at_time    JSON


get FOLDER in DIRECTORY at time - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT TIME (JSON)

                        GET /ehr/ehr_id/directory?version_at_time&path    JSON


get DIRECTORY at version - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY AT VERSION (JSON)

                        generate random version_uid

                        GET /ehr/ehr_id/directory/version_uid    JSON


get DIRECTORY at version - fake version_uid (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET DIRECTORY AT VERSION (JSON)

                        generate random version_uid

                        GET /ehr/ehr_id/directory/version_uid    JSON


get FOLDER in DIRECTORY at version - fake ehr_id (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT VERSION (JSON)

                        GET /ehr/ehr_id/directory/version_uid?path    JSON


get FOLDER in DIRECTORY at version - fake path (JSON)
                        Set Test Variable  ${KEYWORD NAME}  GET FOLDER AT VERSION (JSON)

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
    ...                 `${ehr_id}`, `${test_data}`

                        prepare directory request session    ${format}
                        ...                 Prefer=return=representation

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

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
    ...                 `${ehr_id}`, `${test_data}`

                        prepare directory request session    ${format}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

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
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method PUT on /ehr/ehr_id/directory endpoint
    ...                 DEPENDENCY: the following variables in test level scope:
    ...                 `${ehr_id}`, ${preceding_version_uid}, `${test_data}`

                        prepare directory request session    ${format}
                        ...                 Prefer=return=representation
                        ...                 If-Match=${preceding_version_uid}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

    ${resp}=            Post Request        ${SUT}   /ehr/${ehr_id}/directory
                        ...                 data=${test_data}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  PUT /ehr/ehr_id/directory


PUT /ehr/ehr_id/directory (w/o)
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method PUT on /ehr/ehr_id/directory endpoint
    ...                 WITHOUT (w/o) Prefer=return=representation header
    ...                 DEPENDENCY: the following variables in test level scope:
    ...                 `${ehr_id}`, ${preceding_version_uid}, `${test_data}`

                        prepare directory request session    ${format}
                        ...                 If-Match=${preceding_version_uid}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

    ${resp}=            Post Request        ${SUT}   /ehr/${ehr_id}/directory
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
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method DELETE on /ehr/ehr_id/directory endpoint
    ...                 DEPENDENCY: the following variables in test level scope
    ...                 `${ehr_id}`, `${preceding_version_uid}`

                        prepare directory request session    ${format}
                        ...             If-Match=${preceding_version_uid}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

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
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `${ehr_id}`, ${version_uid}

                        prepare directory request session    ${format}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

    ${resp}=            Get Request         ${SUT}   /ehr/${ehr_id}/directory/${version_uid}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory/version_uid


GET /ehr/ehr_id/directory/version_uid?path
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `${ehr_id}`, `${version_uid}`, `${path}`

                        prepare directory request session    ${format}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

    ${resp}=            Get Request         ${SUT}   /ehr/${ehr_id}/directory/${version_uid}?path=${path}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory/version_uid?path


GET /ehr/ehr_id/directory
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variable in test level scope:
    ...                 `${ehr_id}`

                        prepare directory request session    ${format}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

    ${resp}=            Get Request         ${SUT}   /ehr/ehr_id/directory
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory


GET /ehr/ehr_id/directory?version_at_time
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `${ehr_id}`, `${version_at_time}`

                        prepare directory request session    ${format}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

    ${resp}=            Get Request         ${SUT}   /ehr/ehr_id/directory?version_at_time=${version_at_time}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory?version_at_time


GET /ehr/ehr_id/directory?path
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `${ehr_id}`, `${path}`

                        prepare directory request session    ${format}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

    ${resp}=            Get Request         ${SUT}   /ehr/ehr_id/directory?paht=${path}
                        ...                 headers=${headers}

                        Set Test Variable   ${response}    ${resp}
                        Output Debug Info:  GET /ehr/ehr_id/directory?path


GET /ehr/ehr_id/directory?version_at_time&path
    [Arguments]         ${format}
    [Documentation]     Executes HTTP method GET on given endpoint
    ...                 DEPENDENCY - variables in test level scope:
    ...                 `${ehr_id}`, `${version_at_time}`, ${path}

                        prepare directory request session    ${format}

        TRACE JIRA BUG    NO-JIRA-ID    not-ready    message=endpoint not implemented

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
# [ POSITIVE RESPONSE CHECKS ]

check response: is positive
    Fail    msg=brake it till you make it!


check response: is positive - returns true
    Fail    msg=brake it till you make it!


check response: is positve - ehr_id has directory
    Fail    msg=brake it till you make it!


check response: is positive - returns structure of directory
    # TODO: may need more variants of this kw (e.g. first/latest version etc.)

    Fail    msg=brake it till you make it!


check response: is positive - returns versioned folder with two versions
    Fail    msg=brake it till you make it!



# [ NEGATIVE RESPONSE CHECKS ]

check response: is negative
    Fail    msg=brake it till you make it!


check response: is negative - EHR does not exist
    Fail    msg=brake it till you make it!


check response: is negative - DIRECTORY already exists
    Fail    msg=brake it till you make it!


check response: is negative - DIRECTORY does not exist
    Fail    msg=brake it till you make it!


check response: is negative - VERSIONED DIRECTORY does not exist
    Fail    msg=brake it till you make it!


check response: is positive - confirms deletion
    Fail    msg=brake it till you make it!







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





# [ HELPERS ]
extract version_uid from response (JSON)
        Fail    msg=brake it till you make it!


load valid dir test-data-set
    [Arguments]        ${valid_test_data_set}

    ${file}=            Get File            ${VALID DIR DATA SETS}/${valid_test_data_set}

                        Set Test Variable   ${test_data}    ${file}


load invalid dir test-data-set
    [Arguments]        ${invalid_test_data_set}

    ${file}=            Get File            ${INVALID DIR DATA SETS}/${invalid_test_data_set}

                        Set Test Variable   ${test_data}    ${file}







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


# *** keywords ***
# load dir test-data-set
#     [Arguments]        ${path}
#
#     Run Keyword If    '${VALID DIR DATA SETS}' in '${path}'  load valid dir test-data-set    ${path}
#     Run Keyword If    '${INVALID DIR DATA SETS}' in '${path}'  load invalid dir test-data-set    ${path}
