*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_minimal_presistent_composition_TDD\TDS_example_not_passed
   upload OPT   minimal_persistent/persistent_minimal.opt
   create EHR
   commit composition (TDD\TDS)    composition=invalid/empty.composition
   ...                             template_id=persistent_minimal.en.v1
   ...                             prefer=minimal
   check status_code of commit composition    400

   [Teardown]    restart SUT

