*** Settings ***
Metadata    Version    0.1.0
Metadata    Authors    *Wladislaw Wagner / Pablo Pazos*

Documentation    (AQL) QUERY SERVICE TEST SUITE
...
...              Test Documentation: https://docs.google.com/document/d/13TuxEX1T0ZBlguLBfMkulP-3iFUFFyBejbM2aUTxb1M

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/generic_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/aql_query_keywords.robot
# Resource    ${CURDIR}${/}../_resources/keywords/composition_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot
Resource    ${CURDIR}${/}../_resources/keywords/ehr_keywords.robot

Suite Setup    startup SUT
Suite Teardown  shutdown SUT

Force Tags    AQL
