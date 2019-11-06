*** Settings ***
Metadata    Version    0.1.0
Metadata    Author    *Pablo Pazos*
Metadata    Created    2019.09.09

Library    Collections
Library    OperatingSystem

Documentation    Data oriented tests to verify correct parsing of different AQL payloads

Resource         ${EXECDIR}${/}..${/}tests${/}robot${/}_resources${/}suite_settings.robot

Test Template    execute ad-hoc query

Force Tags       AQL    obsolete

*** Test Cases ***
A.1.a_get_ehrs   A.1.a_get_ehrs.json

*** Keywords ***
execute ad-hoc query
    [Arguments]     ${aql_payload}

                    Set Tags         not-ready
                    Set Log Level    TRACE

                    Set Test Variable   ${aql_file_path}   ${PROJECT_ROOT}${/}tests${/}robot${/}_resources${/}test_data_sets${/}query${/}${aql_payload}
    ${aql_file}=    Get File   ${aql_file_path}

    &{headers}=     Create Dictionary   Content-Type=application/json
                    ...                 Accept=application/json
                    ...                 Prefer=return=representation

                    Create Session      ${SUT}    ${${SUT}.URL}
                    ...                 auth=${${SUT}.CREDENTIALS}    debug=2    verify=True


    ${resp}=        Post Request        ${SUT}   /query/aql   data=${aql_file}   headers=${headers}
                    #Log To Console      ${resp.content}
                    Log                 ${resp.content}
                    Should Be Equal As Strings   ${resp.status_code}   200

                    Log    "This will only work if there are no EHRs in the database"
    ${expected}     Set Variable        {"q":"SELECT e/ehr_id/value FROM EHR e","name":null,"columns":[],"rows":[]}
    ${resp.content} should exactly match json ${expected}


*** Keywords ***
${response payload} should exactly match json ${expected json}
    ${json1}=    Evaluate    json.loads('''${response payload}''')    json
    ${json2}=    Evaluate    json.loads('''${expected json}''')    json
    Log Dictionary    ${json1}
    Log Dictionary    ${json2}
    # payloads are passed to custom lib jsohnlib.py
    payloads should match exactly  ${json1}  ${json2}
