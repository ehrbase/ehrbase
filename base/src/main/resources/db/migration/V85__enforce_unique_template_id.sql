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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

DO
$$
    DECLARE
        count_id INTEGER; count_template_id INTEGER;
    BEGIN
        ALTER TABLE ehr.template_store
            DISABLE ROW LEVEL SECURITY;
        SELECT count(*) INTO count_id FROM ehr.template_store GROUP BY id HAVING COUNT(*) > 1;
        SELECT count(*) INTO count_template_id FROM ehr.template_store GROUP BY template_id HAVING COUNT(*) > 1;
        ALTER TABLE ehr.template_store
            ENABLE ROW LEVEL SECURITY;
        IF count_id > 0 THEN
            RAISE EXCEPTION 'Systems with duplicated internal IDs cannot be migrated automatically; See UPDATING.md';
        END IF;
        IF count_template_id > 0 THEN
            RAISE EXCEPTION 'Systems with duplicated template IDs cannot be migrated automatically; See UPDATING.md';
        END IF;
    END
$$;

ALTER TABLE ehr.template_store
    DROP CONSTRAINT template_store_pkey,
    ADD PRIMARY KEY (template_id, sys_tenant);

create unique index template_store_template_id
    on ehr.template_store (template_id, sys_tenant);