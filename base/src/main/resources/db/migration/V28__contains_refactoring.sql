ALTER TABLE ehr.entry
  ADD COLUMN name ehr.dv_coded_text NOT NULL DEFAULT ('_DEFAULT_NAME',NULL,NULL,NULL,NULL)::ehr.dv_coded_text ;


CREATE OR REPLACE FUNCTION ehr.json_entry_migrate(jsonb_entry jsonb, OUT out_composition_name TEXT, OUT out_new_entry JSONB)
AS $$
DECLARE
  composition_name TEXT;
  composition_idx int;
  str_left text;
  str_right text;
  new_entry jsonb;
BEGIN

  composition_idx := strpos(jsonb_entry::text, 'and name/value=');
  str_left := left(jsonb_entry::text, composition_idx - 2);
  -- get the right part from 'and name/value'
  str_right := substr(jsonb_entry::text, composition_idx+16);
  composition_idx := strpos(str_right, ']'); -- skip the name
  composition_name := left(str_right, composition_idx - 2); -- remove trailing single-quote, closing bracket
  str_right := substr(str_right::text, composition_idx);

  new_entry := (str_left||str_right)::jsonb;

  SELECT composition_name, new_entry INTO out_composition_name, out_new_entry;

  -- 	RAISE NOTICE 'left : %, right: %', str_left, str_right;

END

$$ LANGUAGE plpgsql;

-- use f.e. select (ehr.json_entry_migrate(entry.entry)).out_composition_name,  (ehr.json_entry_migrate(entry.entry)).out_new_entry from ehr.entry

-- Perform the migration
UPDATE ehr.entry
SET
  entry = ((ehr.json_entry_migrate(entry.entry)).out_new_entry)::jsonb,
  name = ((ehr.json_entry_migrate(entry.entry)).out_composition_name,NULL,NULL,NULL,NULL)::ehr.dv_coded_text;

-- fix to support composition name as a DvCodedText
CREATE OR REPLACE FUNCTION ehr.js_composition(composition_uuid UUID, server_node_id TEXT)
  RETURNS JSON AS
$$
BEGIN
  RETURN (
    WITH composition_data AS (
      SELECT
        composition.id as composition_id,
        composition.language  as language,
        composition.territory as territory,
        composition.composer  as composer,
        event_context.id      as context_id,
        territory.twoletter   as territory_code,
        entry.template_id     as template_id,
        entry.archetype_id    as archetype_id,
        entry.name 			  as composition_name,
        concept.conceptid     as category_defining_code,
        concept.description   as category_description,
        entry.entry           as content,
        to_jsonb(jsonb_each(to_jsonb(jsonb_each((entry.entry)::jsonb)))) #>> '{value}' as json_content
      FROM ehr.composition
             INNER JOIN ehr.entry ON entry.composition_id = composition.id
             LEFT JOIN ehr.event_context ON event_context.composition_id = composition.id
             LEFT JOIN ehr.territory ON territory.code = composition.territory
             LEFT JOIN ehr.concept ON concept.id = entry.category
      WHERE composition.id = composition_uuid
    ),
         entry_content AS (
           SELECT * FROM composition_data
           WHERE json_content::text like '{"%/content%' OR json_content = '{}'
         )
    SELECT
      jsonb_strip_nulls(
          jsonb_build_object(
              '_type', 'COMPOSITION',
              'name', entry_content.composition_name,
              'archetype_details', ehr.js_archetype_details(entry_content.archetype_id, entry_content.template_id),
              'archetype_node_id', entry_content.archetype_id,
              'uid', ehr.js_object_version_id(ehr.composition_uid(entry_content.composition_id, server_node_id)),
              'language', ehr.js_code_phrase(language, 'ISO_639-1'),
              'territory', ehr.js_code_phrase(territory_code, 'ISO_3166-1'),
              'composer', ehr.js_party(composer),
              'category',
              ehr.js_dv_coded_text(category_description, ehr.js_code_phrase(category_defining_code :: TEXT, 'openehr')),
              'context', ehr.js_context(context_id),
              'content', entry_content.json_content::jsonb
            )
        )
    FROM entry_content
  );
END
$$
  LANGUAGE plpgsql;

-- table ehr.containment is no more used with the new containment resolution strategy
drop table ehr.containment;