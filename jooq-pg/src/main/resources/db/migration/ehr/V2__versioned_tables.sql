-- EHRbase v2: Versioned Tables
-- All versioned entities use PG18 temporal constraints (WITHOUT OVERLAPS)
-- History tables populated by APPLICATION CODE (NOT triggers)
-- PostgreSQL 18+ required

SET search_path TO ehr_system, ext;

-- ============================================================
-- Contribution (groups related changes — like git commits)
-- ============================================================
CREATE TABLE ehr_system.contribution (
    id                    UUID DEFAULT uuidv7() PRIMARY KEY,
    ehr_id                UUID NOT NULL REFERENCES ehr_system.ehr(id),
    contribution_type     TEXT NOT NULL,
    change_type           TEXT NOT NULL,
    committer_name        TEXT NOT NULL,
    committer_id          TEXT,
    time_committed        TIMESTAMPTZ NOT NULL DEFAULT now(),
    description           TEXT,
    attestation_signature TEXT,
    sys_tenant            SMALLINT NOT NULL REFERENCES ehr_system.tenant(id)
);

CREATE INDEX idx_contribution_ehr ON ehr_system.contribution (ehr_id);

-- ============================================================
-- Composition (clinical document metadata)
-- PG18: WITHOUT OVERLAPS prevents conflicting version periods
-- ============================================================
CREATE TABLE ehr_system.composition (
    id              UUID DEFAULT uuidv7(),
    ehr_id          UUID NOT NULL REFERENCES ehr_system.ehr(id),
    template_id     UUID NOT NULL REFERENCES ehr_system.template(id),
    valid_period    TSTZRANGE NOT NULL DEFAULT tstzrange(now(), NULL),

    -- openEHR metadata
    archetype_id    TEXT NOT NULL,
    template_name   TEXT NOT NULL,
    composer_name   TEXT NOT NULL,
    composer_id     TEXT,
    language        TEXT,
    territory       TEXT,
    category_code   TEXT,
    feeder_audit    JSONB,
    participations  JSONB DEFAULT '[]',

    -- Versioning
    sys_version     INT NOT NULL DEFAULT 1,
    contribution_id UUID REFERENCES ehr_system.contribution(id),
    change_type     TEXT NOT NULL,
    committed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    committer_name  TEXT NOT NULL,
    committer_id    TEXT,

    sys_tenant      SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),

    -- PG18 temporal primary key
    PRIMARY KEY (id, valid_period WITHOUT OVERLAPS)
);

CREATE INDEX idx_composition_ehr ON ehr_system.composition (ehr_id);
CREATE INDEX idx_composition_template ON ehr_system.composition (template_id);

-- Composition history: populated by application code on UPDATE/DELETE
CREATE TABLE ehr_system.composition_history (
    LIKE ehr_system.composition INCLUDING ALL
);

-- ============================================================
-- EHR Status (versioned EHR metadata)
-- ============================================================
CREATE TABLE ehr_system.ehr_status (
    id              UUID DEFAULT uuidv7(),
    ehr_id          UUID NOT NULL REFERENCES ehr_system.ehr(id),
    valid_period    TSTZRANGE NOT NULL DEFAULT tstzrange(now(), NULL),

    -- Status fields
    is_queryable    BOOLEAN NOT NULL DEFAULT true,
    is_modifiable   BOOLEAN NOT NULL DEFAULT true,
    subject_id      TEXT,
    subject_namespace TEXT,
    archetype_node_id TEXT NOT NULL DEFAULT 'openEHR-EHR-EHR_STATUS.generic.v1',
    name            TEXT NOT NULL DEFAULT 'EHR Status',

    -- Versioning
    sys_version     INT NOT NULL DEFAULT 1,
    contribution_id UUID REFERENCES ehr_system.contribution(id),
    change_type     TEXT NOT NULL DEFAULT 'creation',
    committed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    committer_name  TEXT NOT NULL,
    committer_id    TEXT,

    sys_tenant      SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),

    PRIMARY KEY (id, valid_period WITHOUT OVERLAPS)
);

CREATE INDEX idx_ehr_status_ehr ON ehr_system.ehr_status (ehr_id);

-- EHR Status history
CREATE TABLE ehr_system.ehr_status_history (
    LIKE ehr_system.ehr_status INCLUDING ALL
);

-- ============================================================
-- EHR Folder (hierarchical folder structure using ltree)
-- ============================================================
CREATE TABLE ehr_system.ehr_folder (
    id                UUID DEFAULT uuidv7(),
    ehr_id            UUID NOT NULL REFERENCES ehr_system.ehr(id),
    parent_id         UUID,
    path              ext.LTREE NOT NULL,
    name              TEXT NOT NULL,
    archetype_node_id TEXT,
    valid_period      TSTZRANGE NOT NULL DEFAULT tstzrange(now(), NULL),

    -- Versioning
    sys_version       INT NOT NULL DEFAULT 1,
    contribution_id   UUID REFERENCES ehr_system.contribution(id),
    change_type       TEXT NOT NULL DEFAULT 'creation',
    committed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    committer_name    TEXT NOT NULL,
    committer_id      TEXT,

    sys_tenant        SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),

    PRIMARY KEY (id, valid_period WITHOUT OVERLAPS)
);

CREATE INDEX idx_ehr_folder_ehr ON ehr_system.ehr_folder (ehr_id);
CREATE INDEX idx_ehr_folder_path ON ehr_system.ehr_folder USING GIST (path);

-- EHR Folder history
CREATE TABLE ehr_system.ehr_folder_history (
    LIKE ehr_system.ehr_folder INCLUDING ALL
);

-- Folder items (links folders to compositions)
CREATE TABLE ehr_system.ehr_folder_item (
    folder_id      UUID NOT NULL,
    composition_id UUID NOT NULL,
    sys_tenant     SMALLINT NOT NULL REFERENCES ehr_system.tenant(id),
    PRIMARY KEY (folder_id, composition_id)
);
