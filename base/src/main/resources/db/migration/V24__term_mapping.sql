-- this migration implements term mapping in DvCodedText at DB level
-- NB. it deprecates using table CONCEPT as well
create type ehr.dv_coded_text_embedded as (
  value text,
  defining_code ehr.code_phrase,
  formatting text,
  language ehr.code_phrase,
  encoding ehr.code_phrase
  );

create type ehr.dv_coded_text_term_mapping as (
  match CHAR(1),
  purpose ehr.dv_coded_text_embedded,
  target ehr.code_phrase
  );

-- alter defined ehr.dv_coded_text
-- This representation is used as a clean typed definition fails at read time (jooq 3.12)
alter type ehr.dv_coded_text
  add attribute term_mapping TEXT[]; -- array : match, purpose: value, terminology, code, target: terminology, code, delimited by '|'

-- prepare the table migration
CREATE OR REPLACE FUNCTION ehr.migrate_category(category UUID)
  RETURNS ehr.dv_coded_text AS
$$
BEGIN
  RETURN (
    WITH concept_val AS (
      SELECT
        conceptid as code,
        description
      FROM ehr.concept
      WHERE concept.id = category
      LIMIT 1
    )
    select ARRAY[(concept_val.code, ('openehr', concept_val.description)::ehr.code_phrase, null, null, null, null)::ehr.dv_coded_text]
    from concept_val
  );
END
$$
  LANGUAGE plpgsql;

-- alter table entry & entry_history to use the new type
alter table ehr.entry drop constraint entry_category_fkey;

alter table ehr.entry
  alter column category type ehr.dv_coded_text
    using ehr.migrate_category(category);

alter table ehr.entry_history
  alter column category type ehr.dv_coded_text
    using ehr.migrate_category(category);

-- do the same with table event_context & event_context_history
alter table ehr.event_context drop constraint event_context_setting_fkey;

alter table ehr.event_context
  alter column setting type ehr.dv_coded_text
    using ehr.migrate_category(setting);

alter table ehr.event_context_history
  alter column setting type ehr.dv_coded_text
    using ehr.migrate_category(setting);

