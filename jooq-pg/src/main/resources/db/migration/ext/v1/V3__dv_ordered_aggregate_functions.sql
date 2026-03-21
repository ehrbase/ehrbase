/*
 * Copyright (c) 2024 vitasystems GmbH.
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

-- Function for getting the magnitude() of DV_ORDERED
CREATE OR REPLACE FUNCTION jsonb_dv_ordered_magnitude(dv jsonb)
    RETURNS jsonb AS $$
BEGIN
    CASE dv ->> 'T'
        WHEN 'q', 'co' THEN
            RETURN dv -> 'm';
        WHEN 'pr', 't', 'd', 'dt', 'du' THEN
            RETURN dv -> 'M';
        WHEN 'sc', 'o' THEN
            RETURN dv -> 'V';
        ELSE
            RETURN null;
        END CASE;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE
    STRICT
    PARALLEL SAFE;

--max() for DV_ORDERED jsonb
CREATE OR REPLACE FUNCTION dv_ordered_larger(j1 jsonb, j2 jsonb)
    RETURNS jsonb AS $$
DECLARE
    m1 jsonb:= jsonb_dv_ordered_magnitude(j1);
    m2 jsonb:= jsonb_dv_ordered_magnitude(j2);
    cond boolean := m1 > m2;
BEGIN
    IF cond THEN
        RETURN j1;
    ELSEIF cond IS NOT NULL THEN
        RETURN j2;
    ELSEIF m1 IS NOT NULL THEN
        RETURN j1;
    ELSEIF m2 IS NOT NULL THEN
        RETURN j2;
    ELSE
        RETURN NULL;
    END IF;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE
    PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE max_dv_ordered(jsonb)(
    SFUNC = dv_ordered_larger,
    STYPE = jsonb,
    FINALFUNC_MODIFY = READ_ONLY,
    COMBINEFUNC = dv_ordered_larger,
    MFINALFUNC_MODIFY = READ_ONLY,
    SORTOP = >,
    PARALLEL = SAFE
    );

-- min() for DV_ORDERED jsonb
CREATE OR REPLACE FUNCTION dv_ordered_smaller(j1 jsonb, j2 jsonb)
    RETURNS jsonb AS $$
DECLARE
    m1 jsonb:= jsonb_dv_ordered_magnitude(j1);
    m2 jsonb:= jsonb_dv_ordered_magnitude(j2);
    cond boolean := m1 < m2;
BEGIN
    IF cond THEN
        RETURN j1;
    ELSEIF cond IS NOT NULL THEN
        RETURN j2;
    ELSEIF m1 IS NOT NULL THEN
        RETURN j1;
    ELSEIF m2 IS NOT NULL THEN
        RETURN j2;
    ELSE
        RETURN NULL;
    END IF;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE
    PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE min_dv_ordered(jsonb)(
    SFUNC = dv_ordered_smaller,
    STYPE = jsonb,
    FINALFUNC_MODIFY = READ_ONLY,
    COMBINEFUNC = dv_ordered_smaller,
    MFINALFUNC_MODIFY = READ_ONLY,
    SORTOP = <,
    PARALLEL = SAFE
    );