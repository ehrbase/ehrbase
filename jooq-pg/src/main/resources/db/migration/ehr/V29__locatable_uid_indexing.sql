/*
 * Copyright (c) 2026 vitasystems GmbH.
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

ALTER TABLE comp_data ADD COLUMN IF NOT EXISTS uid_root text;
UPDATE comp_data
SET uid_root = CASE
    WHEN num = 0 THEN vo_id::text
    ELSE split_part(data -> 'U' ->> 'V','::', 1)
    END
WHERE
    uid_root IS NULL
    AND
    (num = 0 OR
      (entity_concept IS NULL AND data ? 'U'));

--TODO index definition
CREATE INDEX IF NOT EXISTS comp_data_uid_root_idx ON comp_data USING btree (uid_root);
