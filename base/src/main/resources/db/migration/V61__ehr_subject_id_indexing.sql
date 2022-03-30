/*
 *  Copyright (c) 2021 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

-- Optimize query in the form:
--
-- select distinct on ("/ehr_id/value") "alias_27994528"."/ehr_id/value"
-- from (
--          select "ehr_join"."id" as "/ehr_id/value"
--          from "ehr"."entry"
--                   right outer join "ehr"."composition" as "composition_join"
--                                    on "composition_join"."id" = "ehr"."entry"."composition_id"
--                   right outer join "ehr"."ehr" as "ehr_join"
--                                    on "ehr_join"."id" = "composition_join"."ehr_id"
--                   join "ehr"."status" as "status_join"
--                        on "status_join"."ehr_id" = "ehr_join"."id"
--                   join "ehr"."party_identified" as "subject_ref"
--                        on "subject_ref"."id" = "status_join"."party"
--          where (jsonb_extract_path_text(cast("ehr"."js_party_ref"(
--                  "subject_ref"."party_ref_value",
--                  "subject_ref"."party_ref_scheme",
--                  "subject_ref"."party_ref_namespace",
--                  "subject_ref"."party_ref_type"
--              ) as jsonb),'id','value') = '30123')
--      ) as "alias_27994528"
-- In the lack of proper indexing, the WHERE condition evaluation requires in a nested loop, with an index, it is
-- done with a simple Bitmap index scan. This results in a > 10x performance optimization.
-- NB. index can be applied only on IMMUTABLE function!

---
-- FUNCTION: ehr.js_party_ref(text, text, text, text)

-- DROP FUNCTION ehr.js_party_ref(text, text, text, text);

CREATE OR REPLACE FUNCTION ehr.js_party_ref(
    text,
    text,
    text,
    text)
    RETURNS json
    LANGUAGE 'plpgsql'

    COST 100
    IMMUTABLE
AS $BODY$
DECLARE
    id_value ALIAS FOR $1;
    id_scheme ALIAS FOR $2;
    namespace ALIAS FOR $3;
    party_type ALIAS FOR $4;
BEGIN

    IF (id_value IS NULL AND id_scheme IS NULL AND namespace IS NULL AND party_type IS NULL) THEN
        RETURN NULL;
    ELSE
        RETURN
            json_build_object(
                    '_type', 'PARTY_REF',
                    'id',
                    json_build_object(
                            '_type', 'GENERIC_ID',
                            'value', id_value,
                            'scheme', id_scheme
                        ),
                    'namespace', namespace,
                    'type', party_type
                );
    END IF;
END
$BODY$;

-- ALTER FUNCTION ehr.js_party_ref(text, text, text, text)
--     OWNER TO ehrbase;

-- create index
create index if not exists ehr_subject_id_index on ehr.party_identified(jsonb_extract_path_text(cast("ehr"."js_party_ref"(
        ehr.party_identified.party_ref_value,
        ehr.party_identified.party_ref_scheme,
        ehr.party_identified.party_ref_namespace,
        ehr.party_identified.party_ref_type
    ) as jsonb),'id','value'))