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

ALTER TABLE template_store
    ADD COLUMN IF NOT EXISTS concept text DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS root_archetype text DEFAULT NULL;

UPDATE template_store
SET concept = (xpath('/*/*[local-name()="concept"]/text()', content::xml))[1]::text,
    root_archetype = (xpath('/*/*[local-name()="definition"]/*[local-name()="archetype_id"]/*[local-name()="value"]/text()', content::xml))[1]::text
WHERE concept IS NULL;
