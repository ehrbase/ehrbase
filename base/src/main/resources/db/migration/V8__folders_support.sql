/*
 * Modifications copyright (C) 2019 Vitasystems GmbH,  Hannover Medical School, , and Luis Marco-Ruiz (Hannover Medical School).

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

-- Table: ehr.folder

-- DROP TABLE ehr.folder;

CREATE TABLE ehr.folder
(
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    in_contribution uuid NOT NULL,
    name text COLLATE pg_catalog."default" NOT NULL,
    archetype_node_id text COLLATE pg_catalog."default" NOT NULL,
    active boolean DEFAULT true,
    details jsonb,
    sys_transaction timestamp without time zone NOT NULL,
	sys_period tstzrange NOT NULL,
    CONSTRAINT folder_pk PRIMARY KEY (id),
	CONSTRAINT folder_in_contribution_fkey FOREIGN KEY (in_contribution)
        REFERENCES ehr.contribution (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;



-- Index: folder_in_contribution_idx

-- DROP INDEX ehr.folder_in_contribution_idx;

CREATE INDEX folder_in_contribution_idx
    ON ehr.folder USING btree
    (in_contribution)
    TABLESPACE pg_default;

-- Table: ehr.folder_hierarchy

-- DROP TABLE ehr.folder_hierarchy;

CREATE TABLE ehr.folder_hierarchy
(
    parent_folder uuid NOT NULL,
    child_folder uuid NOT NULL,
    in_contribution uuid,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period tstzrange NOT NULL,
    CONSTRAINT folder_hierarchy_pkey PRIMARY KEY (parent_folder, child_folder),
    CONSTRAINT folder_hierarchy_in_contribution_fk FOREIGN KEY (in_contribution)
        REFERENCES ehr.contribution (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT folder_hierarchy_parent_fk FOREIGN KEY (parent_folder)
        REFERENCES ehr.folder (id) MATCH SIMPLE
        ON UPDATE CASCADE
        ON DELETE CASCADE
        DEFERRABLE
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

-- Index: folder_hierarchy_in_contribution_idx

-- DROP INDEX ehr.folder_hierarchy_in_contribution_idx;

CREATE INDEX folder_hierarchy_in_contribution_idx
    ON ehr.folder_hierarchy USING btree
    (in_contribution)
    TABLESPACE pg_default;

-- DROP INDEX ehr.fki_folder_hierarchy_parent_fk;

CREATE INDEX fki_folder_hierarchy_parent_fk
    ON ehr.folder_hierarchy USING btree
    (parent_folder)
    TABLESPACE pg_default;

-- Table: ehr.folder_hierarchy_history

-- DROP TABLE ehr.folder_hierarchy_history;

CREATE TABLE ehr.folder_hierarchy_history
(
    parent_folder uuid NOT NULL,
    child_folder uuid NOT NULL,
    in_contribution uuid NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period tstzrange NOT NULL,
    CONSTRAINT folder_hierarchy_history_pkey PRIMARY KEY (parent_folder, child_folder, in_contribution)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

-- Table: ehr.folder_history

-- DROP TABLE ehr.folder_history;

CREATE TABLE ehr.folder_history
(
    id uuid NOT NULL,
    in_contribution uuid NOT NULL,
    name text COLLATE pg_catalog."default" NOT NULL,
    archetype_node_id text COLLATE pg_catalog."default" NOT NULL,
    active boolean NOT NULL,
    details jsonb,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period tstzrange NOT NULL,
    CONSTRAINT folder_history_pkey PRIMARY KEY (id, in_contribution)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;


-- Trigger: versioning_trigger

-- DROP TRIGGER versioning_trigger ON ehr.folder;

CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.folder
    FOR EACH ROW
    EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.folder_history', 'true');


-- Trigger: versioning_trigger

-- DROP TRIGGER versioning_trigger ON ehr.folder_hierarchy;

CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.folder_hierarchy
    FOR EACH ROW
    EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.folder_hierarchy_history', 'true');



-- Table: ehr.object_ref_history

-- DROP TABLE ehr.object_ref_history;

CREATE TABLE ehr.object_ref_history
(
    id_namespace text COLLATE pg_catalog."default" NOT NULL,
    type text COLLATE pg_catalog."default" NOT NULL,
    id uuid NOT NULL,
    in_contribution uuid NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period tstzrange NOT NULL,
    CONSTRAINT object_ref_hist_pkey PRIMARY KEY (id, in_contribution)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

--ALTER TABLE ehr.object_ref_history
  --  OWNER to postgres;
COMMENT ON TABLE ehr.object_ref_history
    IS '*implements https://specifications.openehr.org/releases/RM/Release-1.0.3/support.html#_object_ref_history_class

*id implemented as native UID from postgres instead of a separate table.
';

-- Table: ehr.object_ref

-- DROP TABLE ehr.object_ref;

CREATE TABLE ehr.object_ref
(
    id_namespace text COLLATE pg_catalog."default" NOT NULL,
    type text COLLATE pg_catalog."default" NOT NULL,
    id uuid NOT NULL,
    in_contribution uuid NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period tstzrange NOT NULL,
    CONSTRAINT object_ref_pkey PRIMARY KEY (id, in_contribution),
    CONSTRAINT object_ref_in_contribution_fkey FOREIGN KEY (in_contribution)
        REFERENCES ehr.contribution (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

--ALTER TABLE ehr.object_ref
  --  OWNER to postgres;
COMMENT ON TABLE ehr.object_ref
    IS '*implements https://specifications.openehr.org/releases/RM/Release-1.0.3/support.html#_object_ref_class

*id implemented as native UID from postgres instead of a separate table.
';
-- Index: obj_ref_in_contribution_idx

-- DROP INDEX ehr.obj_ref_in_contribution_idx;

CREATE INDEX obj_ref_in_contribution_idx
    ON ehr.object_ref USING btree
    (in_contribution)
    TABLESPACE pg_default;

-- Trigger: versioning_trigger

-- DROP TRIGGER versioning_trigger ON ehr.object_ref;

CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.object_ref
    FOR EACH ROW
    EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.object_ref_history', 'true');

-- Table: ehr.folder_items

-- DROP TABLE ehr.folder_items;

CREATE TABLE ehr.folder_items
(
    folder_id uuid NOT NULL,
    object_ref_id uuid NOT NULL,
    in_contribution uuid NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
	sys_period tstzrange NOT NULL,
    CONSTRAINT folder_items_pkey PRIMARY KEY (folder_id, object_ref_id, in_contribution),
    CONSTRAINT folder_items_folder_fkey FOREIGN KEY (folder_id)
        REFERENCES ehr.folder (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE,
    CONSTRAINT folder_items_in_contribution_fkey FOREIGN KEY (in_contribution)
        REFERENCES ehr.contribution (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT folder_items_obj_ref_fkey FOREIGN KEY (in_contribution, object_ref_id)
        REFERENCES ehr.object_ref (in_contribution, id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;


-- Table: ehr.folder_items_history

-- DROP TABLE ehr.folder_items_history;

CREATE TABLE ehr.folder_items_history
(
 folder_id uuid NOT NULL,
    object_ref_id uuid NOT NULL,
    in_contribution uuid NOT NULL,
    sys_transaction timestamp without time zone NOT NULL,
    sys_period tstzrange NOT NULL,
    CONSTRAINT folder_items_hist_pkey PRIMARY KEY (folder_id, object_ref_id, in_contribution)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

-- Index: folder_hist_idx


-- DROP INDEX ehr.folder_hist_idx;

CREATE INDEX folder_hist_idx
    ON ehr.folder_items_history USING btree
    (folder_id, object_ref_id, in_contribution)
    TABLESPACE pg_default;


-- Trigger: versioning_trigger

-- DROP TRIGGER versioning_trigger ON ehr.folder_items;

CREATE TRIGGER versioning_trigger
    BEFORE INSERT OR DELETE OR UPDATE
    ON ehr.folder_items
    FOR EACH ROW
    EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.folder_items_history', 'true');



-- Trigger and function to maintain consistent the delete of FOLDER.items, deletes OBJECT_REF rows after a deletion on FOLDER_ITEMS occurs.
CREATE OR REPLACE FUNCTION ehr.tr_function_delete_folder_item()
    RETURNS trigger
    AS $$BEGIN
DELETE FROM ehr.object_ref
WHERE ehr.object_ref.id=OLD.object_ref_id AND
		ehr.object_ref.in_contribution= OLD.in_contribution;
	RETURN OLD;
END;
$$ LANGUAGE plpgsql;
--ALTER FUNCTION ehr.tr_function_delete_folder_item()
 --   OWNER TO postgres;

COMMENT ON FUNCTION ehr.tr_function_delete_folder_item()
    IS 'fires after deletion of folder_items when the corresponding Object_ref  needs to be deleted.';


CREATE TRIGGER tr_folder_item_delete
AFTER DELETE ON ehr.folder_items
FOR EACH ROW
EXECUTE PROCEDURE ehr.tr_function_delete_folder_item();