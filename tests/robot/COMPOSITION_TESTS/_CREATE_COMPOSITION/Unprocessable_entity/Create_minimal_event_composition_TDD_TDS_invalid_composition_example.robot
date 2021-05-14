*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_minimal_event_composition_TDD\TDS_invalid_composition_example
   [Tags]   not-ready
   
   upload OPT   nested/nested.opt
   create EHR
   commit composition (TDD\TDS)    invalid/nested.composition.TDD_TDS.xml
   ...                             nested.en.v1
   ...                             minimal
   ...                             complete
   check status_code of commit composition    422

   [Teardown]    restart SUT

