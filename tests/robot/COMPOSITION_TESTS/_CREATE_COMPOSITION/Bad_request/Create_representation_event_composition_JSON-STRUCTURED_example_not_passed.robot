*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_representation_event_composition_JSON-STRUCTURED_example_not_passed
   upload OPT   nested/nested.opt
   create EHR
   commit composition (JSON-STRUCTURED)        composition=invalid/empty.composition
   ...                                         template_id=nested.en.v1
   check status_code of commit composition     400

   [Teardown]    restart SUT

