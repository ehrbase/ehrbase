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

--variant 1
ALTER TABLE comp_version ADD COLUMN start_time_magnitude numeric;
--variant 2
ALTER TABLE comp_version ADD COLUMN start_time jsonb;

UPDATE comp_version v
SET start_time_magnitude = (d.data -> 'st' -> 'M')::numeric, start_time = d.data -> 'st'
FROM comp_data d
WHERE v.vo_id = d.vo_id AND d.rm_entity = 'EC';

--variant 1
CREATE INDEX IF NOT EXISTS comp_version_start_time_magnitude_idx ON comp_version (start_time_magnitude ASC);
--variant 2
CREATE INDEX IF NOT EXISTS comp_version_start_time_magnitude_idx ON comp_version (((start_time -> 'M')::numeric) ASC);
