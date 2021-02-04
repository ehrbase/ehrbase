/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
-- these are pg functions wrappers to be able to access them from within jOOQ
CREATE OR REPLACE FUNCTION ehr.jsonb_array_elements(jsonb_val JSONB)
    RETURNS SETOF JSONB AS
$$
BEGIN
    RETURN QUERY SELECT jsonb_array_elements(jsonb_val);
END
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.jsonb_array_elements(jsonb_val JSONB)
    RETURNS SETOF JSONB AS
$$
BEGIN
    RETURN QUERY SELECT jsonb_array_elements(jsonb_val);
END
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.jsonb_extract_path(from_json jsonb, VARIADIC path_elems text[])
    RETURNS JSONB AS
$$
BEGIN
    RETURN jsonb_extract_path(from_json, path_elems);
END
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ehr.jsonb_extract_path_text(from_json jsonb, VARIADIC path_elems text[])
    RETURNS TEXT AS
$$
BEGIN
    RETURN jsonb_extract_path_text(from_json, path_elems);
END
$$
    LANGUAGE plpgsql;