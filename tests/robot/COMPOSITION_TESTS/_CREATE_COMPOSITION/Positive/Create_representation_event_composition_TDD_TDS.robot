*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_representation_event_composition_TDD\TDS
   upload OPT   nested/nested.opt
   create EHR
   commit composition (TDD\TDS)    composition=valid/nested.composition.TDD_TDS.xml
   ...                             template_id=nested.en.v1
   ...                             lifecycle=incomplete
   check the successfull result of commit compostion (TDD\TDS)

   [Teardown]    restart SUT

