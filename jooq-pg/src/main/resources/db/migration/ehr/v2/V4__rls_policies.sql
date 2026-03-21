-- EHRbase v2: Row-Level Security Policies
-- Tenant isolation + user-level access + queryable enforcement
-- PostgreSQL 18+ required

SET search_path TO ehr_system, ext;

-- ============================================================
-- Helper: Tenant isolation policy (applied to ALL tables)
-- ============================================================

-- ehr
ALTER TABLE ehr_system.ehr ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.ehr FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.ehr
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- users
ALTER TABLE ehr_system.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.users FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.users
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- template
ALTER TABLE ehr_system.template ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.template FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.template
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- schema_registry
ALTER TABLE ehr_system.schema_registry ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.schema_registry FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.schema_registry
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- contribution
ALTER TABLE ehr_system.contribution ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.contribution FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.contribution
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- composition + history
ALTER TABLE ehr_system.composition ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.composition FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.composition
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

ALTER TABLE ehr_system.composition_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.composition_history FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.composition_history
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- ehr_status + history
ALTER TABLE ehr_system.ehr_status ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.ehr_status FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.ehr_status
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

ALTER TABLE ehr_system.ehr_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.ehr_status_history FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.ehr_status_history
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- ehr_folder + history
ALTER TABLE ehr_system.ehr_folder ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.ehr_folder FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.ehr_folder
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

ALTER TABLE ehr_system.ehr_folder_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.ehr_folder_history FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.ehr_folder_history
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- ehr_folder_item
ALTER TABLE ehr_system.ehr_folder_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.ehr_folder_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.ehr_folder_item
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- consent
ALTER TABLE ehr_system.consent ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.consent FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.consent
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- access_grants
ALTER TABLE ehr_system.access_grants ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.access_grants FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.access_grants
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- item_tag
ALTER TABLE ehr_system.item_tag ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.item_tag FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.item_tag
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- plugin_config
ALTER TABLE ehr_system.plugin_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.plugin_config FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.plugin_config
    USING (sys_tenant = current_setting('ehrbase.current_tenant')::smallint);

-- audit_event (tenant isolation on reads — INSERT always allowed)
ALTER TABLE ehr_system.audit_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE ehr_system.audit_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_policy ON ehr_system.audit_event
    USING (tenant_id = current_setting('ehrbase.current_tenant')::smallint);

-- ============================================================
-- User-level access control (composition access via access_grants)
-- ============================================================
CREATE POLICY access_policy ON ehr_system.composition
    USING (
        ehr_id IN (
            SELECT ehr_id FROM ehr_system.access_grants
            WHERE user_id = current_setting('ehrbase.actor_id')::uuid
        )
        OR current_setting('ehrbase.user_role') = 'admin'
        OR current_setting('ehrbase.emergency_access', true) = 'true'
    );

-- ============================================================
-- Queryable enforcement (is_queryable = false hides EHR data)
-- ============================================================
CREATE POLICY queryable_policy ON ehr_system.composition
    USING (
        EXISTS (
            SELECT 1 FROM ehr_system.ehr
            WHERE id = composition.ehr_id AND is_queryable = true
        )
        OR current_setting('ehrbase.user_role') = 'admin'
    );
