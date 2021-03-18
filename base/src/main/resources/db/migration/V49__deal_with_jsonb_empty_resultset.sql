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
-- extend standard jsonb_array_elements to return an empty json object instead of an empty resultset
-- this is required to avoid empty results due to performing cartesian product with an empty set.
-- NB. this function is used when dealing with ITEM_STRUCTURE (composition entry f.e.)
CREATE OR REPLACE FUNCTION ehr.xjsonb_array_elements(entry JSONB)
    RETURNS SETOF JSONB AS
$$
BEGIN
    IF (entry IS NULL) THEN
        RETURN QUERY SELECT NULL::jsonb ;
    ELSE
        RETURN QUERY SELECT jsonb_array_elements(entry);
    END IF;

END
$$
    LANGUAGE plpgsql;