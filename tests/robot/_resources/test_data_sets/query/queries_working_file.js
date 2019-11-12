// all EHRs
//****
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e"
}
//****
{
  "q": "SELECT e/uid/value as uid, e/time_created/value as date FROM EHR e"
}
//****
{
  "q": "SELECT e/uid/value, e/time_created/value, e/system_id/value FROM EHR e"
}
//****
{
  "q": "SELECT e/ehr_id/value as uid, e/system_id/value as system_id, e/time_created/value as time_created FROM EHR e"
}

// all EHRs with status
//****
{
  "q": "SELECT e/ehr_id/value as uid, e/ehr_status FROM EHR e"
}

// all EHRs containing COMPOSITIONs
// ***
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c"
}

// all EHRs containing COMPOSITIONs with archetype ID
// ***
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1]"
}
// ***
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c WHERE c/archetype_node_id='openEHR-EHR-COMPOSITION.minimal.v1'"
}
// ***
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c WHERE c/archetype_node_id matches {'openEHR-EHR-COMPOSITION.minimal.v1'}"
}

// Searching by an archetype that doesnt exists
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.xxxxxx.v1]"
}

// all EHRs containing COMPOSITIONs with archetype ID with parameters
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c [$archetype_id]",
  "query_parameters": {
    "archetype_id": "openEHR-EHR-COMPOSITION.minimal.v1"
  }
}
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c WHERE c/archetype_node_id=$archetype_id",
  "query_parameters": {
    "archetype_id": "openEHR-EHR-COMPOSITION.minimal.v1"
  }
}
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c WHERE c/archetype_node_id matches $archetype_id",
  "query_parameters": {
    "archetype_id": "{'openEHR-EHR-COMPOSITION.minimal.v1'}"
  }
}

// Searching by an archetype that doesnt exists
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e CONTAINS COMPOSITION c [$archetype_id]",
  "query_parameters": {
    "archetype_id": "openEHR-EHR-COMPOSITION.xxxxx.v1"
  }
}

// all COMPOSITIONs
{
  "q": "SELECT c/uid/value FROM COMPOSITION c"
}

// all COMPOSITIONs in EHRs
{
  "q": "SELECT e/ehr_id/value, c/uid/value FROM EHR e CONTAINS COMPOSITION c"
}
{
  "q": "SELECT c/uid/value as uid, c/context/start_time/value as date_created FROM EHR e CONTAINS COMPOSITION c"
}

// EHR by id
//**-**
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e [ehr_id/value='dd616472-9432-4004-ad85-fd47affb1cc8']"
}
//*****
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value='dd616472-9432-4004-ad85-fd47affb1cc8'"
}
//****
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e WHERE uid='dd616472-9432-4004-ad85-fd47affb1cc8'"
}
//****
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e WHERE uid matches {'dd616472-9432-4004-ad85-fd47affb1cc8'}"
}

// Using a variable instead of constant for the uid value
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e [ehr_id/value=$uid]",
  "query_parameters": {
    "uid": "dd616472-9432-4004-ad85-fd47affb1cc8"
  }
}
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e WHERE e/ehr_id/value=$uid",
  "query_parameters": {
    "uid": "dd616472-9432-4004-ad85-fd47affb1cc8"
  }
}
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e WHERE uid=$uid",
  "query_parameters": {
    "uid": "dd616472-9432-4004-ad85-fd47affb1cc8"
  }
}
{
  "q": "SELECT e/ehr_id/value as uid FROM EHR e WHERE uid matches $uid",
  "query_parameters": {
    "uid": "{'dd616472-9432-4004-ad85-fd47affb1cc8'}"
  }
}


// COMPOSITION by condition over data, different options
// Needs data loaded for minimal_observation.opt
//***
{
  "q": "SELECT c FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1] WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'first value'"
}
// ****
{
  "q": "SELECT c FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1] WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value matches {'first value'}"
}
//****
{
  "q": "SELECT c FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1] WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = $value",
  "query_parameters": {
    "value": "first value"
  }
}

// COMPOSITION by condition over data, different options, with limit
// Needs data loaded for minimal_observation.opt
{
  "q": "SELECT c FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1] WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'first value'",
  "offset": 0,
  "fetch": 10
}
{
  "q": "SELECT c FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1] WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value matches {'first value'}",
  "offset": 0,
  "fetch": 10
}
{
  "q": "SELECT c FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1] WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = $value",
  "query_parameters": {
    "value": "first value"
  },
  "offset": 0,
  "fetch": 10
}


// OBSERVATION data from openEHR-EHR-OBSERVATION.minimal.v1
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  c/name as compo_name_gives_error,
  o/name as obs_name,
  o/data[at0001]/name as history_name,
  o/data as data_is_null,
  o/data[at0001] as data_with_node_id,
  o/data[at0001]/origin as origin,
  o/data[at0001]/origin/value as origin_value,
  o/data[at0001]/events[at0002]/time/value,
  o/data[at0001]/events[at0002]/data[at0003] as event_data,
  o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1]"
}

// OBSERVATION data from openEHR-EHR-OBSERVATION.minimal.v1 with value matches regex
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  o/name as obs_name,
  o/data[at0001]/name as history_name,
  o/data[at0001]/origin as origin,
  o/data[at0001]/origin/value as origin_value,
  o/data[at0001]/events[at0002]/time/value,
  o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1]
  WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value matches {'^text.*'}"
}

// OBSERVATION data from openEHR-EHR-OBSERVATION.minimal.v1 with value equals constant
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  o/data[at0001] as data_with_node_id,
  o/data[at0001]/origin/value as origin_value,
  o/data[at0001]/events[at0002]/time/value,
  o/data[at0001]/events[at0002]/data[at0003] as event_data,
  o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1]
  WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'first value'"
}



// TODO: same as above but using a variable for the equals

// OBSERVATION data from openEHR-EHR-OBSERVATION.minimal.v1 with value NOT equals constant I
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  o/data[at0001] as data_with_node_id,
  o/data[at0001]/origin/value as origin_value,
  o/data[at0001]/events[at0002]/time/value,
  o/data[at0001]/events[at0002]/data[at0003] as event_data,
  o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1]
  WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value != 'xxx'"
}

// OBSERVATION data from openEHR-EHR-OBSERVATION.minimal.v1 with value NOT equals constant II
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  o/data[at0001] as data_with_node_id,
  o/data[at0001]/origin/value as origin_value,
  o/data[at0001]/events[at0002]/time/value,
  o/data[at0001]/events[at0002]/data[at0003] as event_data,
  o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1]
  WHERE NOT o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value = 'xxx'"
}


// OBSERVATION data from openEHR-EHR-OBSERVATION.minimal.v1 with value greater than constant
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  o/data[at0001] as data_with_node_id,
  o/data[at0001]/origin/value as origin_value,
  o/data[at0001]/events[at0002]/time/value,
  o/data[at0001]/events[at0002]/data[at0003] as event_data,
  o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS OBSERVATION o [openEHR-EHR-OBSERVATION.minimal.v1]
  WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/value > 'first value'"
}

// ACTION data from openEHR-EHR-ACTION.minimal.v1
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  c/name/value,
  a/description as description_struct,
  a/description[at0001]/items[at0002]/value/uri/value as description_value_dvmm_uri,
  a/description[at0001]/items[at0002]/value/size as description_value_dvmm_size,
  a/time as time1,
  a/time/value as time2,
  a/ism_transition/current_state/value,
  a/ism_transition/current_state/defining_code as code_phrase,
  a/ism_transition/current_state/defining_code/code_string as code,
  a/ism_transition/current_state/xxx as invalid_path
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ACTION a [openEHR-EHR-ACTION.minimal.v1]"
}

// ADMIN_ENTRY data from openEHR-EHR-ADMIN_ENTRY.minimal.v1
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  c/name/value,
  a/data as data,
  a/data[at0001] as data_with_node_id,
  a/data[at0001]/items[at0002] as data_items,
  a/data[at0001]/items[at0002]/value as ordinal,
  a/data[at0001]/items[at0002]/value/value as int,
  a/data[at0001]/items[at0002]/value/symbol as symbol_ctext,
  a/data[at0001]/items[at0002]/value/symbol/value as symbol_ctext_value,
  a/data[at0001]/items[at0002]/value/symbol/code as symbol_ctext_wrong_code_path,
  a/data[at0001]/items[at0002]/value/symbol/defining_code/code_string as symbol_ctext_code
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS ADMIN_ENTRY a [openEHR-EHR-ADMIN_ENTRY.minimal.v1]"
}

// EVALUATION data from openEHR-EHR-EVALUATION.minimal.v1
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  c/name/value,
  ev/data[at0001]/items[at0002] as element,
  ev/data[at0001]/items[at0002]/value as dv_quantity_not_serialized,
  ev/data[at0001]/items[at0002]/value/magnitude,
  ev/data[at0001]/items[at0002]/value/units
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS EVALUATION ev [openEHR-EHR-EVALUATION.minimal.v1]"
}

// INSTRUCTION data from openEHR-EHR-INSTRUCTION.minimal.v1
{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  c/name/value,
  i/narrative,
  i/activities
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS INSTRUCTION i [openEHR-EHR-INSTRUCTION.minimal.v1]"
}

{
  "q": "SELECT c/uid/value,
  c/archetype_node_id,
  c/archetype_details/template_id/value,
  c/name/value,
  i/narrative,
  i/activities[at0001] as activities
  FROM EHR e CONTAINS COMPOSITION c [openEHR-EHR-COMPOSITION.minimal.v1] CONTAINS INSTRUCTION i [openEHR-EHR-INSTRUCTION.minimal.v1]
  WHERE activities/items[at0003]/value < 'PT1H'"
}
