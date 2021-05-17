*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_minimal_presistent_composition_JSON-STRUCTURED_ehr_id_does not match the pattern
   [Tags]   not-ready   bug   2024
   upload OPT   minimal_persistent/persistent_minimal.opt
   Set Test Variable    ${ehr_id}              11111111-1111-1111-1111-1111111111
   commit composition (JSON-STRUCTURED)        valid/persistent_minimal.composition.JSON-STRUCTURED.json
   ...                                         persistent_minimal.en.v1
   ...                                         representation
   ...                                         complete
   check status_code of commit composition     400

   [Teardown]    restart SUT

