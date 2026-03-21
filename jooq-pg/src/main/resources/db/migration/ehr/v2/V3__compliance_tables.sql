-- EHRbase v2: Compliance Tables
-- Immutable audit log, consent management, access grants, retention policies
-- PostgreSQL 18+ required

SET search_path TO ehr_system, ext;

-- ============================================================
-- Audit Event (DATABASE-LEVEL IMMUTABLE)
-- INSERT-only: app role CANNOT update, delete, or truncate
-- Hash chain for tamper detection
-- ============================================================
CREATE TABLE ehr_system.audit_event (
    id            UUID DEFAULT uuidv7() PRIMARY KEY,
    event_type    TEXT NOT NULL,
    target_type   TEXT NOT NULL,
    target_id     UUID,
    action        TEXT NOT NULL,
    actor_id      TEXT NOT NULL,
    actor_role    TEXT NOT NULL,
    tenant_id     SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),
    ip_address    INET,
    user_agent    TEXT,
    justification TEXT,
    details       JSONB,
    prev_hash     TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_event_target ON ehr_system.audit_event (target_id, target_type);
CREATE INDEX idx_audit_event_actor ON ehr_system.audit_event (actor_id);
CREATE INDEX idx_audit_event_time ON ehr_system.audit_event (created_at);

-- IMMUTABLE: revoke destructive operations from application role
-- (ehrbase_app can INSERT only — no UPDATE, DELETE, TRUNCATE)
-- Note: These GRANTs/REVOKEs execute after default privileges are applied

-- ============================================================
-- Consent Management
-- ============================================================
CREATE TABLE ehr_system.consent (
    id           UUID DEFAULT uuidv7() PRIMARY KEY,
    ehr_id       UUID NOT NULL REFERENCES ehr_system.ehr(id),
    consent_type TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'active',
    scope        JSONB,
    granted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ,
    granted_by   TEXT,
    withdrawn_at TIMESTAMPTZ,
    details      JSONB,
    sys_tenant   SMALLINT NOT NULL REFERENCES ehr_system.tenant(id)
);

CREATE INDEX idx_consent_ehr ON ehr_system.consent (ehr_id);
CREATE INDEX idx_consent_status ON ehr_system.consent (ehr_id, status);

-- ============================================================
-- Access Grants (user-level patient access control)
-- ============================================================
CREATE TABLE ehr_system.access_grants (
    id           UUID DEFAULT uuidv7() PRIMARY KEY,
    user_id      UUID NOT NULL,
    ehr_id       UUID NOT NULL REFERENCES ehr_system.ehr(id),
    access_level TEXT NOT NULL DEFAULT 'read',
    granted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by   UUID,
    sys_tenant   SMALLINT NOT NULL REFERENCES ehr_system.tenant(id)
);

CREATE INDEX idx_access_grants_user ON ehr_system.access_grants (user_id);
CREATE INDEX idx_access_grants_ehr ON ehr_system.access_grants (ehr_id);

-- ============================================================
-- Retention Policy (data lifecycle management)
-- ============================================================
CREATE TABLE ehr_system.retention_policy (
    id               UUID DEFAULT uuidv7() PRIMARY KEY,
    policy_name      TEXT NOT NULL UNIQUE,
    retention_period INTERVAL NOT NULL,
    applies_to       TEXT NOT NULL DEFAULT 'all',
    action           TEXT NOT NULL DEFAULT 'pseudonymize',
    requires_approval BOOLEAN NOT NULL DEFAULT true,
    approved_by      TEXT,
    approved_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    sys_tenant       SMALLINT NOT NULL REFERENCES ehr_system.tenant(id)
);
