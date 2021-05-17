*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_minimal_event_composition_TDD\TDS_ehr_id_not hexadecimal
   [Tags]   not-ready   bug   2024
   upload OPT   nested/nested.opt
   create fake EHR not hexadecimal
   commit composition (TDD\TDS)    valid/nested.composition.TDD_TDS.xml
   ...                             nested.en.v1
   ...                             minimal
   ...                             complete
   check status_code of commit composition    400

   [Teardown]    restart SUT

