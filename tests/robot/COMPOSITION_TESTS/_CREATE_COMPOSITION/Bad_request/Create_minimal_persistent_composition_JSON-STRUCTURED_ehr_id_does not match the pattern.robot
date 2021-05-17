*** Settings ***
Resource    ${EXECDIR}/robot/_resources/suite_settings.robot

*** Test Cases ***
Create_minimal_persistent_composition_JSON-STRUCTURED_ehr_id_does not match the pattern
   [Tags]   not-ready   bug   2024
   upload OPT   minimal_persistent/persistent_minimal.opt
   create fake EHR not match pattern
   commit composition (JSON-STRUCTURED)        valid/persistent_minimal.composition.JSON-STRUCTURED.json
   ...                                         persistent_minimal.en.v1
   ...                                         representation
   ...                                         complete
   check status_code of commit composition     400

   [Teardown]    restart SUT

