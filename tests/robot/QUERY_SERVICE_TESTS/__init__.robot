*** Settings ***
Metadata    Version    0.1.0
Metadata    Authors    *Wladislaw Wagner / Pablo Pazos*

Documentation    (AQL) QUERY SERVICE TEST SUITE
...
...              Test Documentation: https://docs.google.com/document/d/13TuxEX1T0ZBlguLBfMkulP-3iFUFFyBejbM2aUTxb1M

Resource    ${CURDIR}${/}../_resources/suite_settings.robot

Suite Setup    startup SUT
Suite Teardown  shutdown SUT

Force Tags    AQL
