/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
ALTER TABLE ehr.participation DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.participation_history DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context DISABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context_history DISABLE ROW LEVEL SECURITY;
-- delete all participations that do not match any currently active event context -> trigger will move them to history
DELETE FROM ehr.participation p1
       WHERE NOT EXISTS (SELECT ec.id
                         FROM ehr.event_context ec
                         WHERE p1.event_context=ec.id AND p1.sys_transaction = ec.sys_transaction);

-- remove all participation's from history that do not have an associated event_context_history row
/* This is the case if i.e. a plugin was used to rollback a composition to a previous version,
 leaving orphans in the participation_history table after moving them there from the participation table */
DELETE FROM ehr.participation_history ph
       WHERE NOT EXISTS(SELECT ech.id
                        FROM ehr.event_context_history ech
                        WHERE ech.id=ph.event_context AND ech.sys_transaction=ph.sys_transaction);

-- set the correct sys_period for all participation_history rows
UPDATE ehr.participation_history ph
SET sys_period = (SELECT ech.sys_period
                  FROM ehr.event_context_history ech
                  WHERE ech.id=ph.event_context AND ech.sys_transaction=ph.sys_transaction);

ALTER TABLE ehr.participation ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.participation_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr.event_context_history ENABLE ROW LEVEL SECURITY;
