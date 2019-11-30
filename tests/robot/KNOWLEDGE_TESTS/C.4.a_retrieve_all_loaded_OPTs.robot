# Copyright (c) 2019 Wladislaw Wagner (Vitasystems GmbH), Pablo Pazos (Hannover Medical School).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Documentation   OPT1.4 integration tests
...             retrieve all loaded OPTs
...
...             Precondtions for exectuion:
...                 1. operational_templates folder is empty
...                 2. DB container started
...                 3. openehr-server started
...
...             Preconditions:
...                 All valid OPTs should be loaded.
...
...             Postconditions:
...                 None
...
...             Flow:
...                 1. Invoke the retrieve OPTs service
...                 2. All the loaded OPTs should be returned, if there are versions of any OPTs,
...                    only the last version is retrieved
...                    (NOTE: versioning is not applicable for ADL 1.4)

Resource    ${CURDIR}${/}../_resources/suite_settings.robot
Resource    ${CURDIR}${/}../_resources/keywords/template_opt1.4_keywords.robot

# Suite Setup  startup OPT SUT
Suite Teardown  Delete All Templates

Force Tags   OPT14    refactor



*** Test Cases ***

Establish Preconditions: load valid OPTs into SUT
    [Template]         upload valid OPT
    [Documentation]    SUT == System Under Test

    all_types/Test_all_types.opt
    minimal/minimal_action.opt
    minimal/minimal_admin.opt
    minimal/minimal_evaluation.opt
    minimal/minimal_instruction.opt
    minimal/minimal_observation.opt
    minimal_entry_combination/obs_act.opt
    minimal_entry_combination/obs_admin.opt
    minimal_entry_combination/obs_eva.opt
    minimal_entry_combination/obs_inst.opt
    minimal_persistent/persistent_minimal_all_entries.opt
    minimal_persistent/persistent_minimal_2.opt
    minimal_persistent/persistent_minimal.opt
    nested/nested.opt
    versioned/Test versioned v1.opt
    versioned/Test versioned v2.opt


Retrieve OPT List From Server
    [Documentation]    ...

    retrieve list of uploaded OPTs
    verify server response
    clean up test variables



*** Keywords ***
upload valid OPT
    [Arguments]           ${opt file}

    start request session
    get valid OPT file    ${opt file}
    upload OPT file
    server accepted OPT
    [Teardown]            clean up test variables


retrieve list of uploaded OPTs
    retrieve OPT list


# TODO: @WLAD tidy up this one
verify server response
    [Documentation]     Multiple verifications of the response are conducted:
    ...                 - response status code is 200
    ...                 - response body is a list
    ...                 - the list contains 16 entries (16 OPTs uploaded)
    ...                 - all entries in the list are unique (no duplicates)
    ...                 - each entry in the list is a JSON object
    ...                 - each entry in the list has 4 required properties
    ...                   1. templateId
    ...                   2. concept
    ...                   3. uid
    ...                   4. createdOn
    ...                 - each property's value type is 'string'
    ...                 - additional properties are allowed

    Integer  response status  200
    Array    response body
    Array    $                  # same as above
    Array    response body      uniqueItems=true
    Object   $[*]               # each entry in list is a JSON object

    # this checks entries in the list @ index 0
    Object  response body 0
    ...     required=["concept", "template_id","archetype_id", "created_timestamp"]
    #...     additionalProperties=false	# must not have other properties
    Object  $[0]               # same as line above

    # this checks the type of each property's value in the list
    String  response body 0 concept
    String  $[*].concept      # same as "String  response body * concept" ???
    String  $[0].concept      # same as "String  response body 0 concept"
   # String  $[0].createdOn
  #  String  $[*].templateId
  #  String  $[*].uid
  #  String  $[*].createdOn          # FAILS: cause not OPTs have this property
    String  $[*].archetype_id
    String  $[*].created_timestamp
    String  $[*].template_id

    # log to console for quick debugging
    Output  response body 0
    Output  $[0]
    Output  $[0].template_id
  #  Output  $[0].uid
    Output  $[0].concept
  #  Output  $[0].createdOn
  #  Output  $[0].lastAccessTime
  #  Output  $[0].lastModifiedTime
  #  Output  $[0].errorList
  #  Output  $[0].path
    # Output  $[*].templateId
    # Output  $[*].concept
    # Output  $[*]

    # logs response time to console
    Output  response seconds

    # this checks all entries in the list
    Object  $[*]
    ...     required=["concept", "template_id","archetype_id", "created_timestamp"]
    # ...     additionalProperties=false	# must not have other properties

    # # this checks all entries in the list   # awaiting clarification about syntax
    # Object  response body ???
    # ...     required=["concept", "archetype_id", "created_timestamp"]
    # ...     additionalProperties=false	# must not have other properties

    # # this checks the type of each property's value in the list
    # # awaiting clarification about syntax
    # String  response body * template_id     # FAILS ON EHRSCAPE
    # String  response body * concept
    # String  response body * archetype_id
    # String  response body * created_timestamp

    verify OPT list has 16 items
