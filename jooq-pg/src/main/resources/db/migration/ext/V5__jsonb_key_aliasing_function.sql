/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific LANGUAGE governing permissions and
 * limitations under the License.
 */

CREATE OR REPLACE FUNCTION rm_type_alias(t text)
    RETURNS text AS $$
BEGIN
    CASE t
        WHEN 'ACTION' THEN RETURN 'AN';
        WHEN 'ACTIVITY' THEN RETURN 'AY';
        WHEN 'ADMIN_ENTRY' THEN RETURN 'AE';
        WHEN 'ARCHETYPED' THEN RETURN 'AR';
        WHEN 'ARCHETYPE_ID' THEN RETURN 'AX';
        WHEN 'ATTESTATION' THEN RETURN 'AT';
        WHEN 'AUDIT_DETAILS' THEN RETURN 'AD';
        WHEN 'CLUSTER' THEN RETURN 'CL';
        WHEN 'CODE_PHRASE' THEN RETURN 'C';
        WHEN 'COMPOSITION' THEN RETURN 'CO';
        WHEN 'DV_BOOLEAN' THEN RETURN 'b';
        WHEN 'DV_CODED_TEXT' THEN RETURN 'c';
        WHEN 'DV_COUNT' THEN RETURN 'co';
        WHEN 'DV_DATE' THEN RETURN 'd';
        WHEN 'DV_DATE_TIME' THEN RETURN 'dt';
        WHEN 'DV_DURATION' THEN RETURN 'du';
        WHEN 'DV_EHR_URI' THEN RETURN 'eu';
        WHEN 'DV_IDENTIFIER' THEN RETURN 'id';
        WHEN 'DV_INTERVAL' THEN RETURN 'iv';
        WHEN 'DV_MULTIMEDIA' THEN RETURN 'mu';
        WHEN 'DV_ORDINAL' THEN RETURN 'o';
        WHEN 'DV_PARAGRAPH' THEN RETURN 'p';
        WHEN 'DV_PARSABLE' THEN RETURN 'pa';
        WHEN 'DV_PROPORTION' THEN RETURN 'pr';
        WHEN 'DV_QUANTITY' THEN RETURN 'q';
        WHEN 'DV_SCALE' THEN RETURN 'sc';
        WHEN 'DV_STATE' THEN RETURN 'st';
        WHEN 'DV_TEXT' THEN RETURN 'x';
        WHEN 'DV_TIME' THEN RETURN 't';
        WHEN 'DV_URI' THEN RETURN 'u';
        WHEN 'EHR_STATUS' THEN RETURN 'ES';
        WHEN 'ELEMENT' THEN RETURN 'E';
        WHEN 'EVALUATION' THEN RETURN 'EV';
        WHEN 'EVENT_CONTEXT' THEN RETURN 'EC';
        WHEN 'FEEDER_AUDIT' THEN RETURN 'FA';
        WHEN 'FEEDER_AUDIT_DETAILS' THEN RETURN 'FD';
        WHEN 'FOLDER' THEN RETURN 'F';
        WHEN 'GENERIC_ENTRY' THEN RETURN 'GE';
        WHEN 'GENERIC_ID' THEN RETURN 'GX';
        WHEN 'HIER_OBJECT_ID' THEN RETURN 'HX';
        WHEN 'HISTORY' THEN RETURN 'HI';
        WHEN 'INSTRUCTION' THEN RETURN 'IN';
        WHEN 'INSTRUCTION_DETAILS' THEN RETURN 'ID';
        WHEN 'INTERNET_ID' THEN RETURN 'IX';
        WHEN 'INTERVAL' THEN RETURN 'IV';
        WHEN 'INTERVAL_EVENT' THEN RETURN 'IE';
        WHEN 'ISM_TRANSITION' THEN RETURN 'IT';
        WHEN 'ITEM_LIST' THEN RETURN 'IL';
        WHEN 'ITEM_SINGLE' THEN RETURN 'IS';
        WHEN 'ITEM_TABLE' THEN RETURN 'TA';
        WHEN 'ITEM_TREE' THEN RETURN 'TR';
        WHEN 'LINK' THEN RETURN 'LK';
        WHEN 'LOCATABLE_REF' THEN RETURN 'LR';
        WHEN 'OBJECT_REF' THEN RETURN 'OR';
        WHEN 'OBJECT_VERSION_ID' THEN RETURN 'OV';
        WHEN 'OBSERVATION' THEN RETURN 'OB';
        WHEN 'PARTICIPATION' THEN RETURN 'PA';
        WHEN 'PARTY_IDENTIFIED' THEN RETURN 'PI';
        WHEN 'PARTY_REF' THEN RETURN 'PF';
        WHEN 'PARTY_RELATED' THEN RETURN 'PR';
        WHEN 'PARTY_SELF' THEN RETURN 'PS';
        WHEN 'POINT_EVENT' THEN RETURN 'PE';
        WHEN 'REFERENCE_RANGE' THEN RETURN 'RR';
        WHEN 'SECTION' THEN RETURN 'SE';
        WHEN 'TEMPLATE_ID' THEN RETURN 'TP';
        WHEN 'TERMINOLOGY_ID' THEN RETURN 'T';
        WHEN 'TERM_MAPPING' THEN RETURN 'TM';
        WHEN 'UUID' THEN RETURN 'U';
        ELSE RAISE EXCEPTION 'Missing type alias for %', t;
        END CASE;

END;
$$
    LANGUAGE plpgsql
    IMMUTABLE
    STRICT
    PARALLEL SAFE;

CREATE OR REPLACE FUNCTION rm_attribute_alias(a text)
    RETURNS text AS $$
BEGIN
    CASE a
        WHEN '_index' THEN RETURN 'I';
        WHEN '_magnitude' THEN RETURN 'M';
        WHEN '_type' THEN RETURN 'T';
        WHEN 'accuracy' THEN RETURN 'ay';
        WHEN 'accuracy_is_percent' THEN RETURN 'ayp';
        WHEN 'action_archetype_id' THEN RETURN 'aa';
        WHEN 'activities' THEN RETURN 'a';
        WHEN 'activity_id' THEN RETURN 'ac';
        WHEN 'alternate_text' THEN RETURN 'at';
        WHEN 'archetype_details' THEN RETURN 'ad';
        WHEN 'archetype_id' THEN RETURN 'aX';
        WHEN 'archetype_node_id' THEN RETURN 'A';
        WHEN 'assigner' THEN RETURN 'as';
        WHEN 'attestations' THEN RETURN 'att';
        WHEN 'attested_view' THEN RETURN 'atv';
        WHEN 'careflow_step' THEN RETURN 'cf';
        WHEN 'category' THEN RETURN 'ca';
        WHEN 'change_type' THEN RETURN 'ct';
        WHEN 'charset' THEN RETURN 'ch';
        WHEN 'code_string' THEN RETURN 'cd';
        WHEN 'commit_audit' THEN RETURN 'cau';
        WHEN 'committer' THEN RETURN 'co';
        WHEN 'composer' THEN RETURN 'cp';
        WHEN 'compression_algorithm' THEN RETURN 'calg';
        WHEN 'content' THEN RETURN 'c';
        WHEN 'context' THEN RETURN 'x';
        WHEN 'contribution' THEN RETURN 'con';
        WHEN 'current_state' THEN RETURN 'cu';
        WHEN 'data' THEN RETURN 'd';
        WHEN 'defining_code' THEN RETURN 'df';
        WHEN 'denominator' THEN RETURN 'de';
        WHEN 'description' THEN RETURN 't';
        WHEN 'details' THEN RETURN 'dt';
        WHEN 'domain_concept' THEN RETURN 'dc';
        WHEN 'duration' THEN RETURN 'du';
        WHEN 'encoding' THEN RETURN 'ec';
        WHEN 'end_time' THEN RETURN 'et';
        WHEN 'events' THEN RETURN 'e';
        WHEN 'expiry_time' THEN RETURN 'ex';
        WHEN 'external_ref' THEN RETURN 'er';
        WHEN 'feeder_audit' THEN RETURN 'f';
        WHEN 'feeder_system_audit' THEN RETURN 'fs';
        WHEN 'feeder_system_item_ids' THEN RETURN 'fX';
        WHEN 'folders' THEN RETURN 'fo';
        WHEN 'formalism' THEN RETURN 'fm';
        WHEN 'formatting' THEN RETURN 'fr';
        WHEN 'function' THEN RETURN 'fu';
        WHEN 'guideline_id' THEN RETURN 'gX';
        WHEN 'health_care_facility' THEN RETURN 'hc';
        WHEN 'hyperlink' THEN RETURN 'hy';
        WHEN 'id' THEN RETURN 'X';
        WHEN 'identifiers' THEN RETURN 'Xs';
        WHEN 'instruction_details' THEN RETURN 'n';
        WHEN 'instruction_id' THEN RETURN 'iX';
        WHEN 'integrity_check' THEN RETURN 'ic';
        WHEN 'integrity_check_algorithm' THEN RETURN 'ica';
        WHEN 'interval' THEN RETURN 'in';
        WHEN 'is_modifiable' THEN RETURN 'im';
        WHEN 'is_pending' THEN RETURN 'ip';
        WHEN 'is_queryable' THEN RETURN 'iq';
        WHEN 'is_terminal' THEN RETURN 'il';
        WHEN 'ism_transition' THEN RETURN 'it';
        WHEN 'issuer' THEN RETURN 'is';
        WHEN 'item' THEN RETURN 'j';
        WHEN 'items' THEN RETURN 'i';
        WHEN 'language' THEN RETURN 'la';
        WHEN 'lifecycle_state' THEN RETURN 'ls';
        WHEN 'links' THEN RETURN 'lk';
        WHEN 'location' THEN RETURN 'lc';
        WHEN 'lower' THEN RETURN 'l';
        WHEN 'lower_included' THEN RETURN 'li';
        WHEN 'lower_unbounded' THEN RETURN 'lu';
        WHEN 'magnitude' THEN RETURN 'm';
        WHEN 'magnitude_status' THEN RETURN 'ms';
        WHEN 'mappings' THEN RETURN 'mp';
        WHEN 'match' THEN RETURN 'ma';
        WHEN 'math_function' THEN RETURN 'mf';
        WHEN 'meaning' THEN RETURN 'me';
        WHEN 'media_type' THEN RETURN 'mt';
        WHEN 'mode' THEN RETURN 'mo';
        WHEN 'name' THEN RETURN 'N';
        WHEN 'namespace' THEN RETURN 'ns';
        WHEN 'narrative' THEN RETURN 'nv';
        WHEN 'normal_range' THEN RETURN 'nr';
        WHEN 'normal_status' THEN RETURN 'nt';
        WHEN 'null_flavour' THEN RETURN 'nf';
        WHEN 'null_reason' THEN RETURN 'nl';
        WHEN 'numerator' THEN RETURN 'nu';
        WHEN 'origin' THEN RETURN 'og';
        WHEN 'original_content' THEN RETURN 'oc';
        WHEN 'originating_system_audit' THEN RETURN 'oa';
        WHEN 'originating_system_item_ids' THEN RETURN 'os';
        WHEN 'other_context' THEN RETURN 'o';
        WHEN 'other_details' THEN RETURN 'od';
        WHEN 'other_input_version_uids' THEN RETURN 'oX';
        WHEN 'other_participations' THEN RETURN 'op';
        WHEN 'other_reference_ranges' THEN RETURN 'or';
        WHEN 'participations' THEN RETURN 'pp';
        WHEN 'path' THEN RETURN 'pa';
        WHEN 'performer' THEN RETURN 'pf';
        WHEN 'period' THEN RETURN 'pe';
        WHEN 'preceding_version_uid' THEN RETURN 'pX';
        WHEN 'precision' THEN RETURN 'pc';
        WHEN 'preferred_term' THEN RETURN 'pt';
        WHEN 'proof' THEN RETURN 'prf';
        WHEN 'property' THEN RETURN 'pr';
        WHEN 'protocol' THEN RETURN 'p';
        WHEN 'provider' THEN RETURN 'pv';
        WHEN 'purpose' THEN RETURN 'pu';
        WHEN 'qualified_rm_entity' THEN RETURN 'qr';
        WHEN 'range' THEN RETURN 'ra';
        WHEN 'reason' THEN RETURN 're';
        WHEN 'relationship' THEN RETURN 'rs';
        WHEN 'rm_entity' THEN RETURN 'rm';
        WHEN 'rm_name' THEN RETURN 'rn';
        WHEN 'rm_originator' THEN RETURN 'ro';
        WHEN 'rm_version' THEN RETURN 'rv';
        WHEN 'rows' THEN RETURN 'r';
        WHEN 'sample_count' THEN RETURN 'sn';
        WHEN 'scheme' THEN RETURN 'sc';
        WHEN 'setting' THEN RETURN 'se';
        WHEN 'signature' THEN RETURN 'sig';
        WHEN 'size' THEN RETURN 'si';
        WHEN 'specialisation' THEN RETURN 'sp';
        WHEN 'start_time' THEN RETURN 'st';
        WHEN 'state' THEN RETURN 's';
        WHEN 'subject' THEN RETURN 'su';
        WHEN 'summary' THEN RETURN 'y';
        WHEN 'symbol' THEN RETURN 'sy';
        WHEN 'system_id' THEN RETURN 'sX';
        WHEN 'target' THEN RETURN 'ta';
        WHEN 'template_id' THEN RETURN 'tm';
        WHEN 'terminology_id' THEN RETURN 'te';
        WHEN 'territory' THEN RETURN 'ty';
        WHEN 'thumbnail' THEN RETURN 'th';
        WHEN 'time' THEN RETURN 'ti';
        WHEN 'time_committed' THEN RETURN 'tc';
        WHEN 'timing' THEN RETURN 'tg';
        WHEN 'transition' THEN RETURN 'tr';
        WHEN 'type' THEN RETURN 'tp';
        WHEN 'uid' THEN RETURN 'U';
        WHEN 'units' THEN RETURN 'un';
        WHEN 'units_display_name' THEN RETURN 'ud';
        WHEN 'units_system' THEN RETURN 'us';
        WHEN 'upper' THEN RETURN 'u';
        WHEN 'upper_included' THEN RETURN 'ui';
        WHEN 'upper_unbounded' THEN RETURN 'uu';
        WHEN 'uri' THEN RETURN 'ur';
        WHEN 'value' THEN RETURN 'V';
        WHEN 'version_id' THEN RETURN 'vX';
        WHEN 'wf_definition' THEN RETURN 'wd';
        WHEN 'wf_details' THEN RETURN 'w';
        WHEN 'width' THEN RETURN 'wi';
        WHEN 'workflow_id' THEN RETURN 'wX';
        ELSE RAISE EXCEPTION 'Missing attribute alias for %', a;
        END CASE;

END;
$$
    LANGUAGE plpgsql
    IMMUTABLE
    STRICT
    PARALLEL SAFE;

/* This function only performs aliasing. It does NOT do additional things like calculating _magnitude values! */
CREATE OR REPLACE FUNCTION jsonb_with_aliased_keys_and_types(j jsonb)
    RETURNS jsonb AS $$
DECLARE
    array_elem jsonb;
    ret jsonb;
    current_key text;
    current_value jsonb;
BEGIN
    IF j IS NULL THEN
        RETURN NULL;
    END IF;

    CASE jsonb_typeof(j)
        WHEN 'array' THEN
            ret = '[]'::jsonb;
            FOR array_elem IN SELECT jsonb_array_elements(j) LOOP
                    ret := ret || jsonb_with_aliased_keys_and_types(array_elem);
                END LOOP;
        WHEN 'object' THEN
            ret := '{}'::jsonb;
            FOR current_key IN SELECT jsonb_object_keys(j) LOOP
                    IF current_key = '_type' THEN
                        current_value := to_jsonb(rm_type_alias(j ->> current_key));
                    ELSE
                        current_value := jsonb_with_aliased_keys_and_types(j -> current_key);
                    END IF;
                    ret := jsonb_insert(ret, ARRAY[rm_attribute_alias(current_key)], current_value);
                END LOOP;
        ELSE
            ret := j;
        END CASE;

    RETURN ret;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE
    STRICT
    PARALLEL SAFE;
