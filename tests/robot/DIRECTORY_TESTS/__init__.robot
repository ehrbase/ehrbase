*** Settings ***
Metadata    Version    0.2.0
Metadata    Author    *Pablo Pazos*
Metadata    Author    *Wladislaw Wagner*

Documentation    DIRECTORY SERVICE TEST SUITE
...
...              test documentation: https://docs.google.com/document/d/1rR9KZ-hz_LUSyp0qdADtYydnDIgUqOHs532NvNipyzg

Resource    ${CURDIR}${/}../_resources/suite_settings.robot

Suite Setup  startup SUT
Suite Teardown  shutdown SUT

Force Tags    DIRECTORY
