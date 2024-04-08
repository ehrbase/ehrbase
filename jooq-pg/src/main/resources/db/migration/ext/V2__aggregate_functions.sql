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

-- max() for jsonb
CREATE OR REPLACE FUNCTION jsonb_larger(j1 jsonb, j2 jsonb)
    RETURNS jsonb AS $$
BEGIN
    IF j1 > j2 THEN
        RETURN j1;
    ELSE
        RETURN j2;
    END IF;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE STRICT PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE max(jsonb)(
    SFUNC = jsonb_larger,
    STYPE = jsonb ,
    FINALFUNC_MODIFY = READ_ONLY,
    COMBINEFUNC = jsonb_larger,
    MFINALFUNC_MODIFY = READ_ONLY,
    SORTOP = >,
    PARALLEL = SAFE
    );

-- min() for jsonb
CREATE OR REPLACE FUNCTION jsonb_smaller(j1 jsonb, j2 jsonb)
    RETURNS jsonb AS $$
BEGIN
    IF j1 < j2 THEN
        RETURN j1;
    ELSE
        RETURN j2;
    END IF;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE STRICT PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE min(jsonb)(
    SFUNC = jsonb_smaller,
    STYPE = jsonb ,
    FINALFUNC_MODIFY = READ_ONLY,
    COMBINEFUNC = jsonb_smaller,
    MFINALFUNC_MODIFY = READ_ONLY,
    SORTOP = <,
    PARALLEL = SAFE
    );

-- avg() for jsonb

CREATE OR REPLACE FUNCTION jsonb_avg_acc(s numeric[], j2 jsonb)
    RETURNS numeric[] AS $$
BEGIN
    IF jsonb_typeof(j2) = 'number'::text THEN
        RETURN s || j2::numeric;
    ELSE
        RETURN s;
    END IF;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE STRICT PARALLEL SAFE;

CREATE OR REPLACE FUNCTION jsonb_avg_combine(s1 numeric[], s2 numeric[])
    RETURNS numeric[] AS $$
BEGIN
    RETURN s1 || s2;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE STRICT PARALLEL SAFE;

CREATE OR REPLACE FUNCTION jsonb_avg(s numeric[])
    RETURNS jsonb AS $$
DECLARE
    len numeric;
    sum numeric := 0;
    x numeric;
BEGIN
    len := COALESCE(array_length(s,1),0)::numeric;
    IF len > 0 THEN
        FOREACH x IN ARRAY s LOOP
                sum := sum + x;
            END LOOP;
        RETURN to_jsonb(sum/len);
    ELSE
        RETURN NULL;
    END IF;
END;
$$
    LANGUAGE plpgsql
    IMMUTABLE STRICT PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE avg(jsonb)(
    SFUNC = jsonb_avg_acc,
    STYPE = numeric[] ,
    FINALFUNC = jsonb_avg,
    FINALFUNC_MODIFY = READ_ONLY,
    COMBINEFUNC = jsonb_avg_combine,
    INITCOND = '{}',
    MFINALFUNC_MODIFY = READ_ONLY,
    PARALLEL = SAFE
    );

-- sum() for jsonb

CREATE OR REPLACE FUNCTION jsonb_sum(s numeric[])
    RETURNS jsonb AS $$
DECLARE
    sum numeric := 0;
    x numeric;
BEGIN
    IF COALESCE(array_length(s,1),0) > 0 THEN
        FOREACH x IN ARRAY s LOOP
                sum := sum + x;
            END LOOP;
        RETURN to_jsonb(sum);
    ELSE
        RETURN NULL;
    END IF;
END;
$$
    language plpgsql
    IMMUTABLE STRICT PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE sum(jsonb)(
    SFUNC = jsonb_avg_acc,
    STYPE = numeric[],
    FINALFUNC = jsonb_sum,
    FINALFUNC_MODIFY = READ_ONLY,
    COMBINEFUNC = jsonb_avg_combine,
    INITCOND = '{}',
    MFINALFUNC_MODIFY = READ_ONLY,
    PARALLEL = SAFE
    );
