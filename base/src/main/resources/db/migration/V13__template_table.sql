-- Create table ehr.template_store

CREATE TABLE ehr.template_store
(
    id              uuid PRIMARY KEY,
    template_id     text unique,
    content         text,
    sys_transaction TIMESTAMP NOT NULL
)
