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

-- this script enhance an existing EtherCIS DB to support the cache summary extension it
-- is called by prepare_db shell script
-- PASS 1: prepare the cache summary configuration tables
-- C.Chevalley May 2017
-- See LICENSE.txt for licensing details
-------------------------------------------------------------------------------------------------
-- originally called: prepare_cache_summary_db_1.sql

CREATE TABLE ehr.heading (
  code VARCHAR(16) PRIMARY KEY ,
  name TEXT,
  description TEXT
);

CREATE TABLE ehr.template (
  uid UUID PRIMARY KEY,
  template_id TEXT UNIQUE,
  concept TEXT
);

CREATE TABLE ehr.template_heading_xref (
  heading_code VARCHAR(16) REFERENCES ehr.heading(code),
  template_id UUID REFERENCES ehr.template(uid)
);
-- fills in the headings
INSERT INTO ehr.heading
VALUES
  ('ORDERS', 'Orders', 'Orders'),
  ('RESULTS', 'Results', 'Results'),
  ('VITALS', 'Vitals', 'Vitals'),  ('DIAGNOSES', 'Diagnoses', 'Diagnoses');
