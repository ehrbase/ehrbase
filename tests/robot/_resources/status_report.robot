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

Documentation    TODO

Library         SeleniumLibrary
Library         Process
Library         OperatingSystem
Library         String
Library         Collections
Library         JSONLibrary
Library         REST
# Library         REST    SLACK_WEBHOOK_URL   e.g. https://hooks.slack.com/services/xxx/yyy/
Library         RequestsLibrary    WITH NAME  HTTP

Force Tags      chill

Suite Setup     Generate & Serve RF Metrics Report
Suite Teardown  Stop HTTP Server



*** Variables ***
${BROWSER}      Chrome
${URL}          http://127.0.0.1:8000/metrics.html
${SLACK}        SLACK_WEBHOOK_TOKEN
${EMOJI}        heavy_check_mark
${BUTTONSTYLE}  primary

${CIRCLE_PROJECT_REPONAME}  fooo
${CIRCLE_BRANCH}            bar
${CIRCLE_BUILD_NUM}         123
${CIRCLE_WORKFLOW_ID}       baz



*** Tasks ***
Get CircleCI Environment Variables
    ${SLACK_WEBHOOK_URL}=           Get Environment Variable    SLACK_WEBHOOK_URL
    ${SLACK_WEBHOOK_TOKEN}=         Get Environment Variable    SLACK_WEBHOOK_TOKEN
    ${SLACK_OAUTH_ACCESS_TOKEN}=    Get Environment Variable    SLACK_OAUTH_ACCESS_TOKEN

                                    Set Suite Variable    ${SLACK_WEBHOOK_URL}    ${SLACK_WEBHOOK_URL}
                                    Set Suite Variable    ${SLACK_WEBHOOK_TOKEN}    ${SLACK_WEBHOOK_TOKEN}
                                    Set Suite Variable    ${SLACK_OAUTH_ACCESS_TOKEN}    ${SLACK_OAUTH_ACCESS_TOKEN}


Generate Screenshot Of Test Status Report

    Open Headless Chrome And Go To URL
    Sleep    1
    Capture Element Screenshot    id:stats_screenshot_area    filename=../test-status-report.png

    [Teardown]    Close All Browsers


Send Notification To Slack Channel

    Upload Image To Slack via HTTP
    Compose Slack Message
    Send Slack Message

    [Teardown]      Fallback



*** Keywords ***
Open Headless Chrome And Go To URL

    ${options}=     Evaluate       sys.modules['selenium.webdriver'].ChromeOptions()    sys
                    Call Method    ${options}    add_argument    test-type
                    Call Method    ${options}    add_argument    --disable-extensions
                    Call Method    ${options}    add_argument    --headless
                    Call Method    ${options}    add_argument    --hide-scrollbars
                    Call Method    ${options}    add_argument    --disable-gpu
                    Call Method    ${options}    add_argument    --no-sandbox
                    Create Webdriver    Chrome    options=${options}
                    Set Window Size    1440    800
                    Go To    ${URL}
                    Wait Until Page Contains    Dashboard
                    Wait Until Page Contains Element    id=keywordsBarID
                    Wait Until Keyword Succeeds    3x   1x    Wait Until Page contains    robotframework-metrics
                    Wait Until Keyword Succeeds    3x   200ms
                    ...    Wait Until Element Is Visible    id:stats_screenshot_area    60s


Upload Image To Slack via HTTP

                    Create Session    SLACK    https://slack.com/api    debug=2    verify=True
    ${file_tuple}   Evaluate    ('TEST STATUS REPORT', open('test-status-report.png', 'rb'))

    # ${channels}     Evaluate    (None, 'playground')
    # # NOTE: If channels is provided then file will be displayed in given channel. We don't won't this!
    # #       We just want to grab public link of uploaded file!

    ${files}=       Create Dictionary    file=${file_tuple}    # channels=${channels}

    &{headers}=     Create Dictionary   Authorization=Bearer ${SLACK_OAUTH_ACCESS_TOKEN}
    ${resp}=        HTTP.Post Request    SLACK    /files.upload   files=${files}   headers=${headers}
                    # Log To Console    ${resp.status_code}
                    # Log To Console    ${resp.request.headers}
                    # Log To Console    ${resp.request.body}
                    # Log To Console    ${resp.request}
                    # Log To Console    ${resp.text}
                    # Log To Console    ${resp.content}
                    Wait Until Keyword Succeeds    10x    1s    Should Be Equal As Strings    ${resp.status_code}    200
    ${file_id}      Set Variable    ${resp.json()['file']['id']}
                    # Log To Console    FILE-ID: ${file_id}

    # make file public to get permalink_public
    ${resp}=        HTTP.Post Request    SLACK    /files.sharedPublicURL?file=${file_id}    headers=${headers}
                    # Log To Console    ${resp.content}
                    Wait Until Keyword Succeeds    10x    1s    Should Be Equal As Strings    ${resp.status_code}    200
                    Set Suite Variable    ${pub_link}    ${resp.json()['file']['permalink_public']}

    # construct direct image link from permalink_public
    Construct Direct Link


Construct Direct Link
    # permalink_public
    # EXAMPLE:       https://slack-files.com/TLMG3B5DF-FLJTQERJ6-7ffa714628
    # GENERIC:  https://slack-files.com/{team_id}-{file_id}-{pub_secret}

    ${tmpstring}=   Split String From Right    ${pub_link}    -
                    # Log To Console    ${tmpstring}
    ${team_id}=     Split String From Right    ${tmpstring}[1]    /
    ${team_id}      Set Variable    ${team_id}[1]
                    # Log To Console    ${team_id}
    ${file_id}      Set Variable     ${tmpstring}[2]
    ${pub_secret}   Set Variable    ${tmpstring}[3]
                    # Log To Console    ${file_id}
                    # Log To Console    ${pub_secret}

    # direct_link
    # EXAMPLE:  https://files.slack.com/files-pri/T04AG7BVD-FLWHBHY86/filename.png?pub_secret=1ba8263c00
    # GENERIC:  https://files.slack.com/files-pri/{team_id}-{file_id}/{filename}?pub_secret={pub_secret}

    ${directlink}=  Set Variable  https://files.slack.com/files-pri/${team_id}-${file_id}/test_status_report?pub_secret=${pub_secret}
                    # Log To Console    ${directlink}
                    Set Suite Variable    ${direct_link}    ${directlink}


Compose Slack Message

    # capture CircleCI environment variables
    ${CIRCLE_PROJECT_REPONAME}=     Get Environment Variable    CIRCLE_PROJECT_REPONAME
    ${CIRCLE_BRANCH}=               Get Environment Variable    CIRCLE_BRANCH
    ${CIRCLE_BUILD_NUM}=            Get Environment Variable    CIRCLE_BUILD_NUM
    ${CIRCLE_BUILD_URL}=            Get Environment Variable    CIRCLE_BUILD_URL
    ${CIRCLE_WORKFLOW_ID}=          Get Environment Variable    CIRCLE_WORKFLOW_ID
    ${CIRCLE_STAGE}=                Get Environment Variable    CIRCLE_STAGE    # job name
    ${CIRCLE_REPOSITORY_URL}=       Get Environment Variable    CIRCLE_REPOSITORY_URL
    ${CIRCLE_COMPARE_URL}=          Get Environment Variable    CIRCLE_COMPARE_URL
    ${CIRCLE_NODE_INDEX}=           Get Environment Variable    CIRCLE_NODE_INDEX
    ${CIRCLE_PROJECT_USERNAME}=     Get Environment Variable    CIRCLE_PROJECT_USERNAME    # company name
    ${CIRCLE_USERNAME}=             Get Environment Variable    CIRCLE_USERNAME
    ${SLACK_BUILD_STATUS}=          Get Environment Variable    SLACK_BUILD_STATUS
    Set Test Variable               ${SLACK_BUILD_STATUS}       ${SLACK_BUILD_STATUS}
    ${VCS_ID}=                      Set Variable                04f903f0-ae5b-49ae-ba01-b25f817e1f11

    # load message template
    ${file}=            Load JSON From File    slack-message.json

    # visualize build status
                    Get Build Status

    # # put env vars into message
    # ${image_title}      Get Value From Json    ${file}  $.['blocks'][?(@.block_id == 'image')]['title']['text']
    # ${image_url}        Get Value From Json    ${file}  $.['blocks'][?(@.block_id == 'image')]['image_url']
    # ${dash_url}         Get Value From Json    ${file}  $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'dash')]['url']
    # ${report_url}       Get Value From Json    ${file}  $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'report')]['url']
    # ${log_url}          Get Value From Json    ${file}  $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'log')]['url']
    # ${workflow_url}     Get Value From Json    ${file}  $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'flow')]['url']
    # ${build_url}        Get Value From Json    ${file}  $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'build')]['url']

    # update image title
                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'image')]['title']['text']
                    ...    ${CIRCLE_PROJECT_REPONAME} | branch: ${CIRCLE_BRANCH} | :${EMOJI}:

    # update image_url with direct_link
                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'image')]['image_url']
                    ...    ${direct_link}

    # update dashboard, report and log urls
                    Update Value To Json    ${file}
                    ...     $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'dash')]['url']
                    ...     https://${CIRCLE_BUILD_NUM}-${VCS_ID}-bb.circle-artifacts.com/0/home/circleci/project/tests/results/metrics.html

                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'report')]['url']
                    ...    https://${CIRCLE_BUILD_NUM}-${VCS_ID}-bb.circle-artifacts.com/0/home/circleci/project/tests/results/report.html

                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'log')]['url']
                    ...    https://${CIRCLE_BUILD_NUM}-${VCS_ID}-bb.circle-artifacts.com/0/home/circleci/project/tests/results/log.html

                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'flow')]['url']
                    ...    https://circleci.com/workflow-run/${CIRCLE_WORKFLOW_ID}
                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'flow')]['style']
                    ...    ${BUTTONSTYLE}

                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'build')]['url']
                    ...    ${CIRCLE_BUILD_URL}
                    Update Value To Json    ${file}
                    ...    $.['blocks'][?(@.block_id == 'actions')]['elements'][?(@.action_id == 'build')]['style']
                    ...    ${BUTTONSTYLE}

    # convert stuff back to JSON
    ${message}=     Convert To Dictionary    ${file}
                    # Log To Console    ${message}
                    Set Suite Variable    ${message}    ${message}


Send Slack Message

    ${JSON}=        Get File    slack-message.json
                    REST.POST    ${SLACK_WEBHOOK_URL}/${SLACK_WEBHOOK_TOKEN}   ${message}
                    Wait Until Keyword Succeeds    3x    1s    Integer    response status    200
                    # Output


Generate & Serve RF Metrics Report                                                       # uncomment for debugging
                    Start Process  robotmetrics  -M   metrics.html  --logo  ./logo.jpg   # stderr=merr.txt  stdout=mout.txt
                    Wait Until Created    ./metrics.html
                    Sleep  1
                    Start Simple HTTP Server


Start Simple HTTP Server
                    Start Process   python    -m    http.server    alias=HTTPSERVER      # stderr=stderr.txt    stdout=stdout.txt


Stop HTTP Server
                    Terminate Process    handle=HTTPSERVER
                    # Send Signal To Process    stop    handle=HTTPSERVER
                    Process Should Be Stopped    handle=HTTPSERVER
                    Remove File    ./test-status-report.png


Get Build Status
    Run Keyword If    '${SLACK_BUILD_STATUS}'=='FAIL'    Set Test Variable    ${EMOJI}    no_entry
    Run Keyword If    '${SLACK_BUILD_STATUS}'=='FAIL'    Set Test Variable    ${BUTTONSTYLE}    danger


Fallback
    Run keyword if    "${TEST STATUS}"=="FAIL"    Log   \n\nSorry guys! Somethin' went wrong. No report this time :-(\n    ERROR





#   ██████╗  █████╗  ██████╗██╗  ██╗██╗   ██╗██████╗
#   ██╔══██╗██╔══██╗██╔════╝██║ ██╔╝██║   ██║██╔══██╗
#   ██████╔╝███████║██║     █████╔╝ ██║   ██║██████╔╝
#   ██╔══██╗██╔══██║██║     ██╔═██╗ ██║   ██║██╔═══╝
#   ██████╔╝██║  ██║╚██████╗██║  ██╗╚██████╔╝██║
#   ╚═════╝ ╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝ ╚═════╝ ╚═╝
#
#   [ BACKUP ]

Upload Image To Slack via REST

    # NOTE: REST cant't deal with this yet --> TypeError: Object of type 'BufferedReader' is not JSON serializable

    ${file_tuple}   Evaluate    ('TEST STATUS REPORT', open('test-status-report.png', 'rb'))
    ${channels}     Evaluate    (None, 'playground')
    ${files}=       Create Dictionary    file=${file_tuple}  channels=${channels}

    # ${file}         Get Binary File    test-status-report.png
    Set Headers     {"Authorization": "Bearer xoxp-701547379457-696494594291-710681511959-9c9a861be3770efdd4f8637a076bf8c8"}
    Set Headers     {"Content-Type": "multipart/form-data"}
    REST.POST       https://slack.com/api/files.upload    ${files}    validate=False
    Output


Upload Image To Slack via CURL
    Start Process    curl  -H  Authorization: Bearer xoxp-701547379457-696494594291-710681511959-9c9a861be3770efdd4f8637a076bf8c8
    ...                    -F  file\=@test-status-report.png
    ...                    -F  initial_comment\=hello world
    # ...                    -F  channels\=playground    # uncomment if u want file to be displayed on given channel
    ...                    https://slack.com/api/files.upload
    ...                    stderr=curlerr.txt    stdout=curlout.txt
