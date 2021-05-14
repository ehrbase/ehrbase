*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_representation_event_composition_TDD\TDS_ehr_id_can_not_be_modified
   upload OPT   nested/nested.opt
   create new EHR can't be modified
   commit composition (TDD\TDS)    valid/nested.composition.TDD_TDS.xml
   ...                             nested.en.v1
   ...                             representation
   ...                             incomplete
   check status_code of commit composition    400

   [Teardown]    restart SUT

