/*
 * Copyright (c) 2025 vitasystems GmbH.
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

CREATE INDEX IF NOT EXISTS comp_version_history_contribution_idx
    ON comp_version_history USING hash (contribution_id);

CREATE INDEX IF NOT EXISTS ehr_folder_version_history_contribution_idx
    ON ehr_folder_version_history USING hash (contribution_id);

CREATE INDEX IF NOT EXISTS ehr_status_version_history_contribution_idx
    ON ehr_status_version_history USING hash (contribution_id);
