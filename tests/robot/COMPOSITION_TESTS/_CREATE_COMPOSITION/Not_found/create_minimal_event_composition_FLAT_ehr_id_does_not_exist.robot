*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
create_minimal_event_composition_FLAT_ehr_id_does_not_exist
   upload OPT   nested/nested.opt
   create fake EHR
   commit composition (FLAT)    nested.composition.FLAT.json   nested.en.v1
   check status_code of commit composition    404

   [Teardown]    restart SUT

