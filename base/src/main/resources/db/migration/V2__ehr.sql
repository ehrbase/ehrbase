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

-- Generate EtherCIS tables for PostgreSQL 9.3
-- Author: Christian Chevalley
--
--
--
--    alter table com.ethercis.ehr.consult_req_attachement
--        drop constraint FKC199A3AAB95913AB;
--
--    alter table com.ethercis.ehr.consult_req_attachement
--        drop constraint FKC199A3AA4204581F;
--

-- 20170605 RVE:
-- this file is a copy of jooq-pg/src/main/resources/ddls/pgsql_ehr.ddl with the following
-- modififactions:
--   - places extensions in the ext schema due to flyway restrictions
--   - replaced all VARCHAR with TEXT (because our tzid is longer than what fits)


-- storeComposition schema common;


-- storeComposition common_im entities
-- CREATE TABLE "system" ---------------------------------------
CREATE TABLE ehr.system (
	id UUid PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
	description TEXT NOT NULL,
	settings TEXT NOT NULL
 );

COMMENT ON TABLE  ehr.system IS 'system table for reference';

CREATE TABLE ehr.territory (
	code int unique primary key, -- numeric code
	twoLetter char(2),
	threeLetter char(3),
	text TEXT NOT NULL
 );

COMMENT ON TABLE  ehr.territory IS 'ISO 3166-1 countries codeset';

CREATE TABLE ehr.language (
	code varchar(5) unique primary key,
	description TEXT NOT NULL
 );

COMMENT ON TABLE  ehr.language IS 'ISO 639-1 language codeset';

CREATE TABLE ehr.terminology_provider (
	code TEXT unique primary key,
	source TEXT NOT NULL,
	authority TEXT
 );

COMMENT ON TABLE  ehr.terminology_provider IS 'openEHR identified terminology provider';

CREATE TABLE ehr.concept (
  id UUID unique primary key DEFAULT ext.uuid_generate_v4(),
	conceptID int,
	language varchar(5) references ehr.language(code),
	description TEXT
 );

COMMENT ON TABLE  ehr.concept IS 'openEHR common concepts (e.g. terminology) used in the system';

create table ehr.party_identified (
	id UUID primary key DEFAULT ext.uuid_generate_v4(),
	name TEXT,
  -- optional party ref attributes
  party_ref_value TEXT,
  party_ref_scheme TEXT,
  party_ref_namespace TEXT,
  party_ref_type TEXT
);

-- list of identifiers for a party identified
create table ehr.identifier (
	id_value TEXT, -- identifier value
	issuer TEXT, -- authority responsible for the identification (ex. France ASIP, LDAP server etc.)
  assigner TEXT, -- assigner of the identifier
	type_name TEXT, -- coding origin f.ex. INS-C, INS-A, NHS etc.
	party UUID not null references ehr.party_identified(id) -- entity identified with this identifier (normally a person, patient etc.)
);

COMMENT ON TABLE ehr.identifier IS 'specifies an identifier for a party identified, more than one identifier is possible';

-- defines the modality for accessing an com.ethercisrcis.ehr
create table ehr.access (
	id UUID PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
	settings TEXT,
	scheme TEXT -- name of access control scheme
 );

COMMENT ON TABLE ehr.access IS 'defines the modality for accessing an com.ethercis.ehr (security strategy implementation)';
--

-- storeComposition ehr_im entities
-- EHR Class emr_im 4.7.1
create table ehr.ehr (
    id UUID NOT NULL PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
    date_created timestamp default CURRENT_DATE,
    date_created_tzid TEXT, -- timezone id: GMT+/-hh:mm
    access UUID references ehr.access(id), -- access decision support (f.e. consent)
--    status UUID references ehr.status(id),
    system_id UUID references ehr.system(id),
    directory UUID null
);
COMMENT ON TABLE ehr.ehr IS 'EHR itself';

create table ehr.status (
  id UUID NOT NULL PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
  ehr_id UUID references ehr.ehr(id) ON DELETE CASCADE,
  is_queryable boolean default true,
  is_modifiable boolean default true,
  party UUID not null references ehr.party_identified(id),  -- subject (e.g. patient)
  other_details JSONB,
  sys_transaction TIMESTAMP NOT NULL,
  sys_period tstzrange NOT NULL -- temporal table
);

-- change history table
create table ehr.status_history (like ehr.status);
CREATE INDEX ehr_status_history ON ehr.status_history USING BTREE (id);

CREATE TRIGGER versioning_trigger BEFORE INSERT OR UPDATE OR DELETE ON ehr.status
FOR EACH ROW EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.status_history', true);

COMMENT ON TABLE ehr.status IS 'specifies an ehr modality and ownership (patient)';

--storeComposition table ehr.event_participation (
--	context UUID references ehr.event_context(id),
--	participation UUID references ehr.participation(id)
--);

-- COMMENT ON TABLE ehr.event_participation IS 'specifies parties participating in an event context';

-- TODO make it compliant with openEHR common IM section 6
-- storeComposition table ehr.versioned (
-- id UUID PRIMARY KEY DEFAULT ext.uuid_generate_v4(),-- this is used by the object which this version def belongs to (composition etc.)
-- object UUID not null, -- a versioning strategy identifier, can be generated by the RDBMS (PG)
-- created timestamp default NOW()
-- );

-- COMMENT ON TABLE ehr.versioned IS 'used to reference a versioning system';
create type ehr.contribution_data_type as enum('composition', 'folder', 'ehr', 'system', 'other');
create type ehr.contribution_state as enum('complete', 'incomplete', 'deleted');
create type ehr.contribution_change_type as enum('creation', 'amendment', 'modification', 'synthesis', 'Unknown', 'deleted');

-- COMMON IM
-- change control

create table ehr.contribution (
	id UUID primary key DEFAULT ext.uuid_generate_v4(),
  ehr_id UUID references ehr.ehr(id) ON DELETE CASCADE ,
  contribution_type ehr.contribution_data_type, -- specifies the type of data it contains
  state ehr.contribution_state, -- current state in lifeCycleState
  signature TEXT,
	system_id UUID references ehr.system(id),
	committer UUID references ehr.party_identified(id),
	time_committed timestamp default NOW(),
  time_committed_tzid TEXT, -- timezone id
	change_type ehr.contribution_change_type,
	description TEXT, -- is a DvCodedText
  sys_transaction TIMESTAMP NOT NULL,
  sys_period tstzrange NOT NULL -- temporal table
);

create table ehr.attestation (
  id UUID PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
  contribution_id UUID REFERENCES ehr.contribution(id) ON DELETE CASCADE ,
  proof TEXT,
  reason TEXT,
  is_pending BOOLEAN
);

CREATE TABLE ehr.attested_view (
  id UUID PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
  attestation_id UUID REFERENCES ehr.attestation(id) ON DELETE CASCADE,
  --  DvMultimedia
  alternate_text TEXT,
  compression_algorithm TEXT,
  media_type TEXT,
  data BYTEA,
  integrity_check BYTEA,
  integrity_check_algorithm TEXT,
  thumbnail UUID, -- another multimedia holding the thumbnail
  uri TEXT
);

-- change history table
CREATE TABLE ehr.contribution_history (like ehr.contribution);
CREATE INDEX ehr_contribution_history ON ehr.contribution_history USING BTREE (id);

COMMENT ON TABLE ehr.contribution IS 'Contribution table, compositions reference this table';

CREATE TRIGGER versioning_trigger BEFORE INSERT OR UPDATE OR DELETE ON ehr.contribution
FOR EACH ROW EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.contribution_history', true);

create table ehr.composition (
    id UUID PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
    ehr_id UUID references ehr.ehr(id) ON DELETE CASCADE,
--    version UUID references ehr.versioned(id),
    in_contribution UUID references ehr.contribution(id) ON DELETE CASCADE , -- in contribution version
    active boolean default true, -- true if this composition is still valid (e.g. not replaced yet)
    is_persistent boolean default true,
    language varchar(5) references ehr.language(code), -- pointer to the language codeset. Indicates what broad category this Composition is belogs to, e.g. �persistent� - of longitudinal validity, �event�, �process� etc.
    territory int references ehr.territory(code), -- Name of territory in which this Composition was written. Coded fromBinder openEHR �countries� code set, which is an expression of the ISO 3166 standard.
    composer UUID not null references ehr.party_identified(id), -- points to the PARTY_PROXY who has created the composition
    sys_transaction TIMESTAMP NOT NULL,
    sys_period tstzrange NOT NULL -- temporal table
    -- item UUID not null, -- point to the first section in composition
);

-- change history table
CREATE TABLE ehr.composition_history (like ehr.composition);
CREATE INDEX ehr_composition_history ON ehr.composition_history USING BTREE (id);

COMMENT ON TABLE ehr.composition IS 'Composition table';

CREATE TRIGGER versioning_trigger BEFORE INSERT OR UPDATE OR DELETE ON ehr.composition
FOR EACH ROW EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.composition_history', true);

create table ehr.event_context (
  id UUID primary key DEFAULT ext.uuid_generate_v4(),
  composition_id UUID references ehr.composition(id) ON DELETE CASCADE , -- belong to composition
  start_time TIMESTAMP not null,
  start_time_tzid TEXT, -- time zone id: format GMT +/- hh:mm
  end_time TIMESTAMP null,
  end_time_tzid TEXT, -- time zone id: format GMT +/- hh:mm
  facility UUID references ehr.party_identified(id), -- points to a party identified
  location TEXT,
  other_context JSONB, -- supports a cluster for other context definition
  setting UUID references ehr.concept(id), -- codeset setting, see ehr_im section 5
--	program UUID references ehr.program(id), -- the program defined for this context (only in full ddl version)
  sys_transaction TIMESTAMP NOT NULL,
  sys_period tstzrange NOT NULL -- temporal table
);

-- change history table
create table ehr.event_context_history (like ehr.event_context);
CREATE INDEX ehr_event_context_history ON ehr.event_context_history USING BTREE (id);

CREATE TRIGGER versioning_trigger BEFORE INSERT OR UPDATE OR DELETE ON ehr.event_context
FOR EACH ROW EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.event_context_history', true);

COMMENT ON TABLE ehr.event_context IS 'defines the context of an event (time, who, where... see openEHR IM 5.2';

create table ehr.participation (
  id UUID primary key DEFAULT ext.uuid_generate_v4(),
  event_context UUID NOT NULL REFERENCES ehr.event_context(id) ON DELETE CASCADE,
  performer UUID references ehr.party_identified(id),
  function TEXT,
  mode TEXT,
  start_time timestamp,
  start_time_tzid TEXT, -- timezone id
  sys_transaction TIMESTAMP NOT NULL,
  sys_period tstzrange NOT NULL -- temporal table
);

-- change history table
create table ehr.participation_history (like ehr.participation);
CREATE INDEX ehr_participation_history ON ehr.participation_history USING BTREE (id);

CREATE TRIGGER versioning_trigger BEFORE INSERT OR UPDATE OR DELETE ON ehr.participation
FOR EACH ROW EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.participation_history', true);

COMMENT ON TABLE ehr.participation IS 'define a participating party for an event f.ex.';

create type ehr.entry_type as enum('section','care_entry', 'admin', 'proxy');

create table ehr.entry (
	id UUID primary key DEFAULT ext.uuid_generate_v4(),
	composition_id UUID references ehr.composition(id) ON DELETE CASCADE , -- belong to composition
	sequence int, -- ordering sequence number
	item_type ehr.entry_type,
  template_id TEXT, -- operational template to rebuild the structure entry
  template_uuid UUID, -- optional, used with operational template for consistency
  archetype_id TEXT, -- ROOT archetype id (not sure still in use...)
  category UUID null references ehr.concept(id), -- used to specify the type of content: Evaluation, Instruction, Observation, Action with different languages
  entry JSONB,            -- actual content version dependent (9.3: json, 9.4: jsonb). entry is either CARE_ENTRY or ADMIN_ENTRY
  sys_transaction TIMESTAMP NOT NULL,
  sys_period tstzrange NOT NULL -- temporal table
);

-- change history table
CREATE TABLE ehr.entry_history (like ehr.entry);
CREATE INDEX ehr_entry_history ON ehr.entry_history USING BTREE (id);

COMMENT ON TABLE ehr.entry IS 'this table hold the actual archetyped data values (fromBinder a template)';

CREATE TRIGGER versioning_trigger BEFORE INSERT OR UPDATE OR DELETE ON ehr.entry
FOR EACH ROW EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.entry_history', true);

-- CONTAINMENT "pseudo" index for CONTAINS clause resolution
create TABLE ehr.containment (
  comp_id UUID,
  label ltree,
  path text
);

-- CREATE INDEX label_idx ON ehr.containment USING BTREE(label);
-- CREATE INDEX comp_id_idx ON ehr.containment USING BTREE(comp_id);

-- meta data
CREATE TABLE ehr.template_meta (
  template_id TEXT,
  array_path TEXT[] -- list of paths containing an item list with list size > 1
);

CREATE INDEX template_meta_idx ON ehr.template_meta(template_id);

-- simple cross reference table to link INSTRUCTIONS with ACTIONS or other COMPOSITION
CREATE TABLE ehr.compo_xref (
  master_uuid UUID REFERENCES ehr.composition(id),
  child_uuid UUID REFERENCES ehr.composition(id),
  sys_transaction TIMESTAMP NOT NULL
);
CREATE INDEX ehr_compo_xref ON ehr.compo_xref USING BTREE (master_uuid);

-- log user sessions with logon id, session id and other parameters
CREATE TABLE ehr.session_log (
  id UUID primary key DEFAULT uuid_generate_v4(),
  subject_id TEXT NOT NULL,
  node_id TEXT,
  session_id TEXT,
  session_name TEXT,
  session_time TIMESTAMP,
  ip_address TEXT
);

-- views to abstract querying
-- EHR STATUS
CREATE VIEW ehr.ehr_status AS
  SELECT ehr.id, party.name AS name,
                 party.party_ref_value AS ref,
                 party.party_ref_scheme AS scheme,
                 party.party_ref_namespace AS namespace,
                 party.party_ref_type AS type,
                 identifier.*
      FROM ehr.ehr ehr
        INNER JOIN ehr.status status ON status.ehr_id = ehr.id
        INNER JOIN ehr.party_identified party ON status.party = party.id
        LEFT JOIN ehr.identifier identifier ON identifier.party = party.id;

-- Composition expanded view (include context and other meta_data
CREATE OR REPLACE VIEW ehr.comp_expand AS
  SELECT
    ehr.id                            AS ehr_id,
    party.party_ref_value             AS subject_externalref_id_value,
    party.party_ref_namespace         AS subject_externalref_id_namespace,
    entry.composition_id,
    entry.template_id,
    entry.archetype_id,
    entry.entry,
    trim(LEADING '''' FROM (trim(TRAILING ''']' FROM
                                 (regexp_split_to_array(json_object_keys(entry.entry :: JSON), 'and name/value=')) [2
                                 ]))) AS composition_name,
    compo.language,
    compo.territory,
    ctx.start_time,
    ctx.start_time_tzid,
    ctx.end_time,
    ctx.end_time_tzid,
    ctx.other_context,
    ctx.location                      AS ctx_location,
    fclty.name                        AS facility_name,
    fclty.party_ref_value             AS facility_ref,
    fclty.party_ref_scheme            AS facility_scheme,
    fclty.party_ref_namespace         AS facility_namespace,
    fclty.party_ref_type              AS facility_type,
    compr.name                        AS composer_name,
    compr.party_ref_value             AS composer_ref,
    compr.party_ref_scheme            AS composer_scheme,
    compr.party_ref_namespace         AS composer_namespace,
    compr.party_ref_type              AS composer_type
  FROM ehr.entry
    INNER JOIN ehr.composition compo ON compo.id = ehr.entry.composition_id
    INNER JOIN ehr.event_context ctx ON ctx.composition_id = ehr.entry.composition_id
    INNER JOIN ehr.party_identified compr ON compo.composer = compr.id
    INNER JOIN ehr.ehr ehr ON ehr.id = compo.ehr_id
    INNER JOIN ehr.status status ON status.ehr_id = ehr.id
    LEFT JOIN ehr.party_identified party ON status.party = party.id
    -- LEFT JOIN ehr.system sys ON ctx.setting = sys.id
    LEFT JOIN ehr.party_identified fclty ON ctx.facility = fclty.id;

--- CREATED INDEX
CREATE INDEX label_idx ON ehr.containment USING GIST (label);
CREATE INDEX comp_id_idx ON ehr.containment USING BTREE(comp_id);
CREATE INDEX gin_entry_path_idx ON ehr.entry USING gin(entry jsonb_path_ops);
CREATE INDEX template_entry_idx ON ehr.entry (template_id);

-- to optimize comp_expand, index FK's
CREATE INDEX entry_composition_id_idx ON ehr.entry (composition_id);
CREATE INDEX composition_composer_idx ON ehr.composition (composer);
CREATE INDEX composition_ehr_idx ON ehr.composition (ehr_id);
CREATE INDEX status_ehr_idx ON ehr.status (ehr_id);
CREATE INDEX status_party_idx ON ehr.status (party);
CREATE INDEX context_facility_idx ON ehr.event_context (facility);
CREATE INDEX context_composition_id_idx ON ehr.event_context (composition_id);
CREATE INDEX context_setting_idx ON ehr.event_context (setting);


-- AUDIT TRAIL has been replaced by CONTRIBUTION
-- create table ehr.audit_trail (
--     id UUID PRIMARY KEY DEFAULT ext.uuid_generate_v4(),
--     composition_id UUID references ehr.composition(id),
--     committer UUID not null references ehr.party_identified(id), -- contributor
--     date_created TIMESTAMP,
--     date_created_tzid VARCHAR(15), -- timezone id
--     party UUID not null references ehr.party_identified(id), -- patient
--     serial_version VARCHAR(50),
--     system_id UUID references ehr.system(id)
-- );
