-- EHRbase v2: Core Tables
-- Non-versioned base tables in ehr_system schema
-- PostgreSQL 18+ required

SET search_path TO ehr_system, ext;

-- ============================================================
-- Tenant (multi-tenancy)
-- ============================================================
CREATE TABLE ehr_system.tenant (
    id     SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   TEXT NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true
);

-- Default tenant
INSERT INTO ehr_system.tenant (name) VALUES ('default');

-- ============================================================
-- EHR (patient health record container)
-- ============================================================
CREATE TABLE ehr_system.ehr (
    id                UUID DEFAULT uuidv7() PRIMARY KEY,
    subject_id        TEXT,
    subject_namespace TEXT,
    is_queryable      BOOLEAN NOT NULL DEFAULT true,
    is_modifiable     BOOLEAN NOT NULL DEFAULT true,
    creation_date     TIMESTAMPTZ NOT NULL DEFAULT now(),
    sys_tenant        SMALLINT NOT NULL REFERENCES ehr_system.tenant(id)
);

CREATE INDEX idx_ehr_subject ON ehr_system.ehr (subject_id, subject_namespace);

-- ============================================================
-- Users (internal user accounts per tenant)
-- ============================================================
CREATE TABLE ehr_system.users (
    id         UUID DEFAULT uuidv7() PRIMARY KEY,
    username   TEXT NOT NULL,
    sys_tenant SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),
    UNIQUE (username, sys_tenant)
);

-- ============================================================
-- System (server identity — single row, persisted on first startup)
-- ============================================================
CREATE TABLE ehr_system.system (
    id         UUID DEFAULT uuidv7() PRIMARY KEY,
    system_id  TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- Template (OPT storage with ADL version and format tracking)
-- ============================================================
CREATE TABLE ehr_system.template (
    id            UUID DEFAULT uuidv7() PRIMARY KEY,
    template_id   TEXT NOT NULL,
    adl_version   TEXT NOT NULL DEFAULT '1.4',
    format        TEXT NOT NULL DEFAULT 'xml',
    content       TEXT NOT NULL,
    web_template  JSONB,
    version       TEXT NOT NULL DEFAULT '1',
    schema_name   TEXT,
    creation_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    sys_tenant    SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),
    UNIQUE (template_id, adl_version, sys_tenant)
);

-- ============================================================
-- Schema Registry (tracks auto-generated template tables)
-- ============================================================
CREATE TABLE ehr_system.schema_registry (
    id          UUID DEFAULT uuidv7() PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES ehr_system.template(id),
    table_name  TEXT NOT NULL,
    schema_name TEXT NOT NULL DEFAULT 'ehr_data',
    ddl_hash    TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    status      TEXT NOT NULL DEFAULT 'active',
    sys_tenant  SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),
    UNIQUE (table_name, sys_tenant)
);

-- ============================================================
-- Plugin Config (replaces file-based plugin configuration)
-- ============================================================
CREATE TABLE ehr_system.plugin_config (
    id           UUID DEFAULT uuidv7() PRIMARY KEY,
    plugin_id    TEXT NOT NULL,
    config_key   TEXT NOT NULL,
    config_value JSONB NOT NULL,
    updated_at   TIMESTAMPTZ DEFAULT now(),
    updated_by   TEXT,
    sys_tenant   SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),
    UNIQUE (plugin_id, config_key, sys_tenant)
);

-- ============================================================
-- Item Tag (key-value tagging — behind feature flag)
-- ============================================================
CREATE TABLE ehr_system.item_tag (
    id          UUID DEFAULT uuidv7() PRIMARY KEY,
    target_id   UUID NOT NULL,
    target_type TEXT NOT NULL,
    key         TEXT NOT NULL,
    value       TEXT,
    owner_id    TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sys_tenant  SMALLINT NOT NULL REFERENCES ehr_system.tenant(id)
);

CREATE INDEX idx_item_tag_target ON ehr_system.item_tag (target_id, target_type);
