/*
 *  Copyright (c) 2021 Vitasystems GmbH.
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

DO
$$
    DECLARE
        count_folder         INTEGER;
        count_folder_history INTEGER;
    BEGIN
        ALTER TABLE ehr.folder
            DISABLE ROW LEVEL SECURITY;
        ALTER TABLE ehr.folder_history
            DISABLE ROW LEVEL SECURITY;
        SELECT count(*) FROM ehr.folder INTO count_folder;
        SELECT count(*) FROM ehr.folder_history INTO count_folder_history;
        ALTER TABLE ehr.folder
            ENABLE ROW LEVEL SECURITY;
        ALTER TABLE ehr.folder_history
            ENABLE ROW LEVEL SECURITY;

        IF count_folder != 0 or count_folder_history != 0
        THEN
            RAISE EXCEPTION 'Systems with existing ehr directories cannot be migrated automatically; See UPDATING.md';
        END IF;
    END
$$;

create table ehr.ehr_folder
(
    id                uuid             NOT NULL,
    ehr_id            uuid             NOT NULL,
    ehr_folders_idx   int              NOT NULL,
    row_num           int              NOT NULL,
    contribution_id   uuid             NOT NULL,
    audit_id          uuid             NOT NULL,
    archetype_node_id TEXT,
    path              TEXT[],
    hierarchy_idx     text collate "C" not null,
    hierarchy_idx_cap text collate "C" not null,
    hierarchy_idx_len int              not null,
    items             uuid[],
    fields            jsonb,
    namespace         TEXT default '1f332a66-0e57-11ed-861d-0242ac120002',
    sys_version       INT              NOT NULL,
    sys_period_lower  timestamptz      NOT NULL,
    PRIMARY KEY (ehr_id, id),
    FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id),
    FOREIGN KEY (contribution_id) REFERENCES ehr.contribution (id),
    FOREIGN KEY (audit_id) REFERENCES ehr.audit_details (id)
);

create index folder2_path_idx ON ehr.ehr_folder USING btree ((path[2]), ehr_id);
create index archetype_node_idx ON ehr.ehr_folder USING btree (archetype_node_id, (path[2]), ehr_id);

ALTER TABLE ehr.ehr_folder
    ENABLE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr.ehr_folder FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));

create table ehr.ehr_folder_history
(
    id                uuid,
    ehr_id            uuid        NOT NULL,
    ehr_folders_idx   int              NOT NULL,
    row_num           int              NOT NULL,
    contribution_id   uuid        NOT NULL,
    audit_id          uuid        NOT NULL,
    archetype_node_id TEXT,
    path              TEXT[],
    hierarchy_idx     text collate "C" not null,
    hierarchy_idx_cap text collate "C" not null,
    hierarchy_idx_len int              not null,
    items          uuid[],
    fields            jsonb,
    namespace         TEXT default '1f332a66-0e57-11ed-861d-0242ac120002',
    sys_version       INT         NOT NULL,
    sys_period_lower  timestamptz NOT NULL,
    sys_period_upper  timestamptz,
    sys_deleted       boolean     NOT NULL,
    PRIMARY KEY (ehr_id, id, sys_version),
    FOREIGN KEY (ehr_id) REFERENCES ehr.ehr (id),
    FOREIGN KEY (contribution_id) REFERENCES ehr.contribution (id),
    FOREIGN KEY (audit_id) REFERENCES ehr.audit_details (id)
);

ALTER TABLE ehr.ehr_folder_history
    ENABLE ROW LEVEL SECURITY;
CREATE POLICY ehr_policy_all ON ehr.ehr_folder_history FOR ALL USING (namespace = current_setting('ehrbase.current_tenant')) WITH CHECK (namespace = current_setting('ehrbase.current_tenant'));

-- Remove old folder structure

alter table ehr.ehr
    drop column if exists directory;
drop table ehr.folder, ehr.folder_hierarchy, ehr.folder_items,ehr.folder_history,ehr.folder_items_history,ehr.folder_hierarchy_history,ehr.object_ref, ehr.object_ref_history;

drop function ehr.admin_delete_folder(folder_id_input uuid);
drop function ehr.admin_delete_folder_history(folder_id_input uuid);
drop function ehr.admin_delete_folder_obj_ref_history(contribution_id_input uuid);

create or replace function ehr.admin_delete_ehr_full(ehr_id_param uuid)
    returns TABLE
            (
                deleted boolean
            )
    language plpgsql
    security definer
    SET search_path = ehr, pg_temp
as
$$
BEGIN
    -- Disable versioning triggers
    ALTER TABLE ehr.composition
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.status
        DISABLE TRIGGER versioning_trigger;

    RETURN QUERY WITH
                     -- Query IDs
                     select_composition_ids
                         AS (SELECT id FROM ehr.composition WHERE ehr_id = ehr_id_param),
                     select_contribution_ids
                         AS (SELECT id FROM ehr.contribution WHERE ehr_id = ehr_id_param),

                     -- Delete data

                     -- ON DELETE CASCADE:
                     --   * ehr.attested_view
                     --   * ehr.entry
                     --   * ehr.event_context
                     --   * ehr.object_ref
                     --   * ehr.participation
                     -- ehr_folder will be deleted by the ehrbase backend
                     delete_compo_xref
                         AS (DELETE FROM ehr.compo_xref cx USING select_composition_ids sci WHERE cx.master_uuid = sci.id OR cx.child_uuid = sci.id),
                     delete_composition
                         AS (DELETE FROM ehr.composition WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),
                     delete_status
                         AS (DELETE FROM ehr.status WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),
                     select_attestation_ids AS (SELECT id
                                                FROM ehr.attestation
                                                WHERE reference IN
                                                      (SELECT attestation_ref FROM delete_composition)
                                                   OR reference IN (SELECT attestation_ref FROM delete_status)),
                     delete_attestation
                         AS (DELETE FROM ehr.attestation a USING select_attestation_ids sa WHERE a.id = sa.id RETURNING a.reference, a.has_audit),
                     delete_attestation_ref
                         AS (DELETE FROM ehr.attestation_ref ar USING delete_attestation da WHERE ar.ref = da.reference),
                     delete_contribution
                         AS (DELETE FROM ehr.contribution c WHERE c.ehr_id = ehr_id_param RETURNING c.id, c.has_audit),
                     delete_ehr
                         AS (DELETE FROM ehr.ehr e WHERE e.id = ehr_id_param RETURNING e.access),
                     delete_access
                         AS (DELETE FROM ehr.access a USING delete_ehr de WHERE a.id = de.access),

                     -- Delete _history
                     delete_composition_history
                         AS (DELETE FROM ehr.composition_history WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),
                     delete_entry_history
                         AS (DELETE FROM ehr.entry_history eh USING delete_composition_history dch WHERE eh.composition_id = dch.id),
                     delete_event_context_hisotry
                         AS (DELETE FROM ehr.event_context_history ech USING delete_composition_history dch WHERE ech.composition_id = dch.id RETURNING ech.id),
                     delete_participation_history
                         AS (DELETE FROM ehr.participation_history ph USING delete_event_context_hisotry dech WHERE ph.event_context = dech.id),
                     delete_status_history
                         AS (DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),

                     -- Delete audit_details
                     delete_composition_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_composition dc WHERE ad.id = dc.has_audit),
                     delete_status_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_status ds WHERE ad.id = ds.has_audit),
                     delete_attestation_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_attestation da WHERE ad.id = da.has_audit),
                     delete_contribution_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_contribution dc WHERE ad.id = dc.has_audit),
                     delete_composition_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_composition_history dch WHERE ad.id = dch.has_audit),
                     delete_status_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_status_history dsh WHERE ad.id = dsh.has_audit)

                 SELECT true;

    -- Restore versioning triggers
    ALTER TABLE ehr.composition
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.status
        ENABLE TRIGGER versioning_trigger;
END
$$;


create or replace function ehr.js_ehr(uuid, text) returns json
    language plpgsql
as
$$
DECLARE
    ehr_uuid ALIAS FOR $1;
    server_id ALIAS FOR $2;
    contribution_json_array        JSONB[];
    contribution_details           JSONB;
    composition_version_json_array JSONB[];
    composition_in_ehr_id          RECORD;
BEGIN

    FOR contribution_details IN (SELECT ehr.js_contribution(contribution.id, server_id)
                                 FROM ehr.contribution
                                 WHERE contribution.ehr_id = ehr_uuid
                                   AND contribution.contribution_type != 'ehr')
        LOOP
            contribution_json_array := array_append(contribution_json_array, contribution_details);
        END LOOP;

    FOR composition_in_ehr_id IN (SELECT composition.id, composition.sys_transaction
                                  FROM ehr.composition
                                  WHERE composition.ehr_id = ehr_uuid)
        LOOP
            composition_version_json_array := array_append(
                    composition_version_json_array,
                    jsonb_build_object(
                            '_type', 'VERSIONED_COMPOSITION',
                            'id', ehr.js_object_version_id(ehr.composition_uid(composition_in_ehr_id.id, server_id)),
                            'time_created', ehr.js_dv_date_time(composition_in_ehr_id.sys_transaction, 'Z')
                        )
                );
        END LOOP;


    RETURN (WITH ehr_data AS (SELECT ehr.id                as ehr_id,
                                     ehr.date_created      as date_created,
                                     ehr.date_created_tzid as date_created_tz,
                                     ehr.access            as access,
                                     system.settings       as system_value
                              FROM ehr.ehr
                                       JOIN ehr.system ON system.id = ehr.system_id
                              WHERE ehr.id = ehr_uuid)
            SELECT jsonb_strip_nulls(
                           jsonb_build_object(
                                   '_type', 'EHR',
                                   'ehr_id', ehr.js_canonical_hier_object_id(ehr_data.ehr_id),
                                   'system_id', ehr.js_canonical_hier_object_id(ehr_data.system_value),
                                   'ehr_status', ehr.js_ehr_status(ehr_data.ehr_id, server_id),
                                   'time_created', ehr.js_dv_date_time(ehr_data.date_created, ehr_data.date_created_tz),
                                   'contributions', contribution_json_array,
                                   'compositions', composition_version_json_array
                               )
                       -- 'ehr_access'
                       -- 'tags'
                       )

            FROM ehr_data);
END
$$;


