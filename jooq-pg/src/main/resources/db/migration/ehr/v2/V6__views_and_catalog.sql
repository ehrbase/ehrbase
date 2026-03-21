-- =====================================================================
-- V6: SQL Views, View Catalog, and Materialized Views
-- Phase 5 of the EHRbase rewrite
-- =====================================================================

-- =====================================================================
-- 1. VIEW CATALOG TABLE
-- =====================================================================

CREATE TABLE ehr_system.view_catalog (
    id                UUID DEFAULT uuidv7() PRIMARY KEY,
    view_name         TEXT NOT NULL,
    view_schema       TEXT NOT NULL DEFAULT 'ehr_views',
    view_type         TEXT NOT NULL DEFAULT 'template',
    template_id       TEXT,
    source            TEXT NOT NULL DEFAULT 'auto',
    description       TEXT,
    column_metadata   JSONB,
    is_materialized   BOOLEAN NOT NULL DEFAULT false,
    refresh_schedule  TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    sys_tenant        SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),
    UNIQUE (view_name, view_schema, sys_tenant)
);

CREATE INDEX idx_view_catalog_type ON ehr_system.view_catalog (view_type);
CREATE INDEX idx_view_catalog_template ON ehr_system.view_catalog (template_id);

ALTER TABLE ehr_system.view_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.view_catalog FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.view_catalog
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- =====================================================================
-- 2. COMPLIANCE VIEWS
-- =====================================================================

-- Audit trail with human-readable labels
CREATE OR REPLACE VIEW ehr_views.v_audit_trail AS
SELECT
    a.id AS audit_id,
    a.created_at,
    a.event_type,
    CASE a.event_type
        WHEN 'data_access' THEN 'Data Access'
        WHEN 'data_modify' THEN 'Data Modification'
        WHEN 'auth_success' THEN 'Authentication Success'
        WHEN 'auth_failure' THEN 'Authentication Failure'
        WHEN 'admin_action' THEN 'Administrative Action'
        WHEN 'emergency_access' THEN 'Emergency/Break-Glass Access'
        WHEN 'security_violation' THEN 'Security Violation'
        ELSE a.event_type
    END AS event_type_label,
    a.target_type,
    a.target_id,
    a.action,
    CASE a.action
        WHEN 'create' THEN 'Created'
        WHEN 'read' THEN 'Read'
        WHEN 'update' THEN 'Updated'
        WHEN 'delete' THEN 'Deleted'
        WHEN 'pseudonymize' THEN 'Pseudonymized'
        WHEN 'emergency_override' THEN 'Emergency Override'
        WHEN 'tenant_mismatch' THEN 'Tenant Mismatch'
        ELSE a.action
    END AS action_label,
    a.actor_id,
    a.actor_role,
    a.ip_address,
    a.user_agent,
    a.justification,
    a.details,
    a.prev_hash,
    a.tenant_id
FROM ehr_system.audit_event a;

-- HIPAA accounting of disclosures — who accessed which patient data
CREATE OR REPLACE VIEW ehr_views.v_access_log AS
SELECT
    a.id AS access_event_id,
    a.created_at AS access_time,
    a.actor_id AS accessor_id,
    a.actor_role AS accessor_role,
    a.ip_address,
    a.target_type AS resource_type,
    a.target_id AS resource_id,
    a.action,
    a.justification,
    e.id AS ehr_id,
    e.subject_id AS patient_id,
    e.subject_namespace AS patient_namespace,
    a.tenant_id
FROM ehr_system.audit_event a
LEFT JOIN ehr_system.composition c
    ON a.target_type = 'composition' AND a.target_id = c.id
LEFT JOIN ehr_system.ehr e
    ON (a.target_type = 'ehr' AND a.target_id = e.id)
    OR (a.target_type = 'composition' AND c.ehr_id = e.id)
WHERE a.event_type IN ('data_access', 'data_modify', 'emergency_access');

-- Audit events grouped by patient
CREATE OR REPLACE VIEW ehr_views.v_audit_by_patient AS
SELECT
    e.id AS ehr_id,
    e.subject_id AS patient_id,
    e.subject_namespace,
    a.event_type,
    a.action,
    a.actor_id,
    a.actor_role,
    a.created_at,
    a.target_type,
    a.target_id,
    a.justification,
    a.tenant_id
FROM ehr_system.audit_event a
JOIN ehr_system.composition c ON a.target_id = c.id AND a.target_type = 'composition'
JOIN ehr_system.ehr e ON c.ehr_id = e.id
UNION ALL
SELECT
    e.id AS ehr_id,
    e.subject_id AS patient_id,
    e.subject_namespace,
    a.event_type,
    a.action,
    a.actor_id,
    a.actor_role,
    a.created_at,
    a.target_type,
    a.target_id,
    a.justification,
    a.tenant_id
FROM ehr_system.audit_event a
JOIN ehr_system.ehr e ON a.target_id = e.id AND a.target_type = 'ehr';

-- Composition changes with version info (current + history)
CREATE OR REPLACE VIEW ehr_views.v_data_modifications AS
SELECT
    c.id AS composition_id,
    c.ehr_id,
    e.subject_id AS patient_id,
    c.template_name,
    c.sys_version,
    c.change_type,
    c.committed_at,
    c.committer_name,
    c.committer_id,
    c.contribution_id,
    false AS is_historical,
    c.sys_tenant AS tenant_id
FROM ehr_system.composition c
JOIN ehr_system.ehr e ON c.ehr_id = e.id
UNION ALL
SELECT
    ch.id AS composition_id,
    ch.ehr_id,
    e.subject_id AS patient_id,
    ch.template_name,
    ch.sys_version,
    ch.change_type,
    ch.committed_at,
    ch.committer_name,
    ch.committer_id,
    ch.contribution_id,
    true AS is_historical,
    ch.sys_tenant AS tenant_id
FROM ehr_system.composition_history ch
JOIN ehr_system.ehr e ON ch.ehr_id = e.id;

-- Current consent per patient (latest per consent_type)
CREATE OR REPLACE VIEW ehr_views.v_consent_status AS
SELECT DISTINCT ON (c.ehr_id, c.consent_type)
    c.id AS consent_id,
    c.ehr_id,
    e.subject_id AS patient_id,
    e.subject_namespace,
    c.consent_type,
    c.status,
    c.scope,
    c.granted_at,
    c.expires_at,
    c.withdrawn_at,
    c.granted_by,
    CASE
        WHEN c.status = 'withdrawn' THEN 'Withdrawn'
        WHEN c.expires_at IS NOT NULL AND c.expires_at < now() THEN 'Expired'
        WHEN c.status = 'active' THEN 'Active'
        ELSE c.status
    END AS effective_status,
    c.sys_tenant AS tenant_id
FROM ehr_system.consent c
JOIN ehr_system.ehr e ON c.ehr_id = e.id
ORDER BY c.ehr_id, c.consent_type, c.granted_at DESC;

-- Full consent change history
CREATE OR REPLACE VIEW ehr_views.v_consent_history AS
SELECT
    c.id AS consent_id,
    c.ehr_id,
    e.subject_id AS patient_id,
    e.subject_namespace,
    c.consent_type,
    c.status,
    c.scope,
    c.granted_at,
    c.expires_at,
    c.withdrawn_at,
    c.granted_by,
    c.details,
    c.sys_tenant AS tenant_id
FROM ehr_system.consent c
JOIN ehr_system.ehr e ON c.ehr_id = e.id
ORDER BY c.ehr_id, c.consent_type, c.granted_at;

-- =====================================================================
-- 3. FOLDER HIERARCHY VIEWS
-- =====================================================================

-- Folder tree with ltree operators and depth calculation
CREATE OR REPLACE VIEW ehr_views.v_folder_tree AS
SELECT
    f.id AS folder_id,
    f.ehr_id,
    f.parent_id,
    f.path,
    f.name AS folder_name,
    f.archetype_node_id,
    f.sys_version,
    f.change_type,
    f.committed_at,
    f.committer_name,
    ext.nlevel(f.path) AS depth,
    ext.ltree2text(f.path) AS path_text,
    f.sys_tenant AS tenant_id
FROM ehr_system.ehr_folder f
WHERE upper_inf(f.valid_period);

-- Folder contents: folders with their compositions
CREATE OR REPLACE VIEW ehr_views.v_folder_contents AS
SELECT
    f.id AS folder_id,
    f.ehr_id,
    f.path,
    f.name AS folder_name,
    fi.composition_id,
    c.template_name,
    c.archetype_id,
    c.composer_name,
    c.committed_at,
    c.sys_version AS composition_version,
    c.change_type,
    f.sys_tenant AS tenant_id
FROM ehr_system.ehr_folder f
JOIN ehr_system.ehr_folder_item fi ON f.id = fi.folder_id
JOIN ehr_system.composition c ON fi.composition_id = c.id
WHERE upper_inf(f.valid_period);

-- =====================================================================
-- 4. MATERIALIZED VIEW: COMPLIANCE DASHBOARD
-- =====================================================================

CREATE MATERIALIZED VIEW ehr_views.mv_compliance_dashboard AS
SELECT
    a.tenant_id,
    date_trunc('day', a.created_at) AS report_date,
    count(*) FILTER (WHERE a.event_type = 'emergency_access') AS emergency_access_count,
    count(*) FILTER (WHERE a.event_type = 'data_access') AS data_access_count,
    count(*) FILTER (WHERE a.event_type = 'data_modify') AS data_modify_count,
    count(*) FILTER (WHERE a.event_type = 'auth_failure') AS auth_failure_count,
    count(DISTINCT a.actor_id) FILTER (
        WHERE a.event_type IN ('data_access', 'data_modify')) AS unique_data_accessors
FROM ehr_system.audit_event a
GROUP BY a.tenant_id, date_trunc('day', a.created_at)
WITH NO DATA;

CREATE UNIQUE INDEX idx_mv_compliance_dashboard_pk
    ON ehr_views.mv_compliance_dashboard (tenant_id, report_date);

-- =====================================================================
-- 5. REGISTER STATIC VIEWS IN CATALOG
-- =====================================================================

INSERT INTO ehr_system.view_catalog (view_name, view_schema, view_type, source, description, sys_tenant) VALUES
    ('v_audit_trail', 'ehr_views', 'compliance', 'static',
        'All audit events with human-readable event type and action labels', 1),
    ('v_access_log', 'ehr_views', 'compliance', 'static',
        'HIPAA accounting of disclosures — who accessed which patient data and when', 1),
    ('v_audit_by_patient', 'ehr_views', 'compliance', 'static',
        'Audit events grouped by patient/EHR for compliance reporting', 1),
    ('v_data_modifications', 'ehr_views', 'compliance', 'static',
        'Composition changes with version info across current and history tables', 1),
    ('v_consent_status', 'ehr_views', 'compliance', 'static',
        'Current effective consent state per patient (latest per consent type)', 1),
    ('v_consent_history', 'ehr_views', 'compliance', 'static',
        'Full consent change history per patient over time', 1),
    ('v_folder_tree', 'ehr_views', 'folder', 'static',
        'Folder hierarchy with ltree path, depth, and current versions only', 1),
    ('v_folder_contents', 'ehr_views', 'folder', 'static',
        'Folder-to-composition mappings with template and version info', 1),
    ('mv_compliance_dashboard', 'ehr_views', 'materialized', 'static',
        'Daily compliance KPIs: emergency accesses, data access counts, auth failures', 1);
