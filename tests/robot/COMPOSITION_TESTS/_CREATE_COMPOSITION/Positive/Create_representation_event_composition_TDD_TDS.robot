*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
create_representation_event_composition_TDD\TDS
   upload OPT   nested/nested.opt
   create EHR
   commit composition (TDD\TDS)    nested.composition.TDD_TDS.xml   nested.en.v1
   check the successfull result of commit compostion (TDD\TDS)

   [Teardown]    restart SUT

