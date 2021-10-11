-- Drop FK constraints on _history tables
ALTER TABLE ehr.composition_history
    DROP CONSTRAINT composition_history_attestation_ref_fkey;
ALTER TABLE ehr.composition_history
    DROP CONSTRAINT composition_history_has_audit_fkey;
ALTER TABLE ehr.folder_history
    DROP CONSTRAINT folder_history_has_audit_fkey;
ALTER TABLE ehr.status_history
    DROP CONSTRAINT status_history_attestation_ref_fkey;
ALTER TABLE ehr.status_history
    DROP CONSTRAINT status_history_in_contribution_fkey;
ALTER TABLE ehr.status_history
    DROP CONSTRAINT status_history_has_audit_fkey;

-- Create missing indexes
CREATE INDEX IF NOT EXISTS attestation_reference_idx ON ehr.attestation (reference);
CREATE INDEX IF NOT EXISTS attested_view_attestation_idx ON ehr.attested_view (attestation_id);
CREATE INDEX IF NOT EXISTS compo_xref_child_idx ON ehr.compo_xref (child_uuid);
CREATE INDEX IF NOT EXISTS composition_history_ehr_idx ON ehr.composition_history (ehr_id);
CREATE INDEX IF NOT EXISTS contribution_ehr_idx ON ehr.contribution (ehr_id);
CREATE INDEX IF NOT EXISTS entry_history_composition_idx ON ehr.entry_history (composition_id);
CREATE INDEX IF NOT EXISTS event_context_history_composition_idx ON ehr.event_context_history (composition_id);
CREATE INDEX IF NOT EXISTS folder_history_contribution_idx ON ehr.folder_history (in_contribution);
CREATE INDEX IF NOT EXISTS folder_items_contribution_idx ON ehr.folder_items (in_contribution);
CREATE INDEX IF NOT EXISTS folder_items_history_contribution_idx ON ehr.folder_items_history (in_contribution);
CREATE INDEX IF NOT EXISTS folder_hierarchy_history_contribution_idx ON ehr.folder_hierarchy_history (in_contribution);
CREATE INDEX IF NOT EXISTS object_ref_history_contribution_idx ON ehr.object_ref_history (in_contribution);
CREATE INDEX IF NOT EXISTS participation_history_event_context_idx ON ehr.participation_history (event_context);
CREATE INDEX IF NOT EXISTS status_history_ehr_idx ON ehr.status_history (ehr_id);

-- Delete complete EHR function
CREATE OR REPLACE FUNCTION ehr.admin_delete_ehr_full(ehr_id_param UUID)
    RETURNS TABLE
            (
                deleted boolean
            )
AS
$$
BEGIN
    -- Disable versioning triggers
    ALTER TABLE ehr.composition
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_hierarchy
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_items
        DISABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.object_ref
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
                     --   * ehr.folder_hierarchy
                     --   * ehr.folder_items
                     --   * ehr.object_ref
                     --   * ehr.participation

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
                     delete_folder_items
                         AS (DELETE FROM ehr.folder_items fi USING select_contribution_ids sci WHERE fi.in_contribution = sci.id),
                     delete_folder_hierarchy
                         AS (DELETE FROM ehr.folder_hierarchy fh USING select_contribution_ids sci WHERE fh.in_contribution = sci.id),
                     delete_folder
                         AS (DELETE FROM ehr.folder f USING select_contribution_ids sci WHERE f.in_contribution = sci.id RETURNING f.id, f.has_audit),
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
                     delete_folder_history
                         AS (DELETE FROM ehr.folder_history fh USING select_contribution_ids sc WHERE fh.in_contribution = sc.id RETURNING fh.id, fh.has_audit),
                     delete_folder_items_history
                         AS (DELETE FROM ehr.folder_items_history fih USING select_contribution_ids sc WHERE fih.in_contribution = sc.id),
                     delete_folder_hierarchy_history
                         AS (DELETE FROM ehr.folder_hierarchy_history fhh USING select_contribution_ids sc WHERE fhh.in_contribution = sc.id),
                     delete_participation_history
                         AS (DELETE FROM ehr.participation_history ph USING delete_event_context_hisotry dech WHERE ph.event_context = dech.id),
                     object_ref_history
                         AS (DELETE FROM ehr.object_ref_history orh USING select_contribution_ids sc WHERE orh.in_contribution = sc.id),
                     delete_status_history
                         AS (DELETE FROM ehr.status_history WHERE ehr_id = ehr_id_param RETURNING id, attestation_ref, has_audit),

                     -- Delete audit_details
                     delete_composition_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_composition dc WHERE ad.id = dc.has_audit),
                     delete_status_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_status ds WHERE ad.id = ds.has_audit),
                     delete_attestation_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_attestation da WHERE ad.id = da.has_audit),
                     delete_folder_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_folder df WHERE ad.id = df.has_audit),
                     delete_contribution_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_contribution dc WHERE ad.id = dc.has_audit),
                     delete_composition_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_composition_history dch WHERE ad.id = dch.has_audit),
                     delete_status_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_status_history dsh WHERE ad.id = dsh.has_audit),
                     delete_folder_history_audit
                         AS (DELETE FROM ehr.audit_details ad USING delete_folder_history dfh WHERE ad.id = dfh.has_audit)

                 SELECT true;

    -- Restore versioning triggers
    ALTER TABLE ehr.composition
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.entry
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.event_context
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_hierarchy
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.folder_items
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.object_ref
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.participation
        ENABLE TRIGGER versioning_trigger;
    ALTER TABLE ehr.status
        ENABLE TRIGGER versioning_trigger;
END
$$
    LANGUAGE plpgsql;
