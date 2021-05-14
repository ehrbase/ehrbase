*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_representation_event_composition_FLAT
   upload OPT   nested/nested.opt
   create EHR
   commit composition (FLAT)    valid/nested.composition.FLAT.json
   ...                          nested.en.v1
   ...                          representation
   ...                          complete
   check the successfull result of commit compostion (FLAT)   nesting/_uid

   [Teardown]    restart SUT

