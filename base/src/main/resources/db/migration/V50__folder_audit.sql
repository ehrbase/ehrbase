/*
 *  Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

-- Adds commit_audit to each folder version

ALTER TABLE ehr.folder
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance

ALTER TABLE ehr.folder_history
    ADD COLUMN has_audit UUID NOT NULL references ehr.audit_details(id) ON DELETE CASCADE; -- has this audit_details instance