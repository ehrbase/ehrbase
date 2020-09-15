-- ====================================================================
-- Author: Axel Siebert (axel.siebert@vitagroup.ag)
-- Create date: 2020-09-08
-- Description: Retrieves a list of compositions uuids that are using a template
-- Parameters:
--    @target_id - Template id to search entries for, e.g. 'RIPPLE - Conformance Test template'
-- Returns: Table with compositions uuids that use the template
-- =====================================================================
DROP FUNCTION IF EXISTS ehr.admin_get_template_usage;

CREATE OR REPLACE FUNCTION ehr.admin_get_template_usage(target_id TEXT)
RETURNS TABLE (composition_id uuid)
AS $$
BEGIN
	RETURN query
		SELECT e.composition_id
		FROM ehr.entry e
		WHERE e.template_id = target_id
		UNION (
		    SELECT eh.composition_id
		    FROM ehr.entry_history eh
		    WHERE eh.template_id = target_id
		);
END;$$ LANGUAGE plpgsql;

-- ====================================================================
-- Author: Axel Siebert (axel.siebert@vitagroup.ag)
-- Create date: 2020-09-09
-- Description: Replace content of a given template with the new one
-- Parameters:
--    @target_id - Template id to replace content for, e.g. 'RIPPLE - Conformance Test template'
--    @update_content - New content to put into db
-- Returns: New content of the template after the update
-- =====================================================================
DROP FUNCTION IF EXISTS ehr.admin_update_template;

CREATE OR REPLACE FUNCTION ehr.admin_update_template(target_id TEXT, update_content TEXT)
RETURNS TEXT
AS $$
DECLARE
  new_template TEXT;
BEGIN
	UPDATE ehr.template_store
	SET "content" = update_content
	WHERE template_id = target_id;
	SELECT ts."content" INTO new_template
	FROM ehr.template_store ts
	WHERE ts.template_id = target_id;
	RETURN new_template;
END;$$ LANGUAGE plpgsql;

-- ====================================================================
-- Author: Axel Siebert (axel.siebert@vitagroup.ag)
-- Create date: 2020-08-24
-- Description: Removes all templates from database
-- Returns: Number of deleted rows
-- =====================================================================
DROP FUNCTION IF EXISTS ehr.admin_delete_all_templates;

CREATE OR REPLACE FUNCTION ehr.admin_delete_all_templates()
RETURNS integer
AS $$
DECLARE
  deleted integer;
BEGIN
	SELECT count(*) INTO deleted FROM ehr.template_store;
	DELETE FROM ehr.template_store ts WHERE ts.id NOTNULL;
	RETURN deleted;
END;$$ LANGUAGE plpgsql;

-- ====================================================================
-- Author: Axel Siebert (axel.siebert@vitagroup.ag)
-- Create date: 2020-09-11
-- Description: Removes one dedicated template from database
-- Returns: Number of deleted rows
-- =====================================================================
DROP FUNCTION IF EXISTS ehr.admin_delete_template;

CREATE OR REPLACE FUNCTION ehr.admin_delete_template(target_id TEXT)
RETURNS integer
AS $$
BEGIN
	DELETE FROM ehr.template_store ts WHERE ts.template_id = target_id;
	RETURN 1;
END;$$ LANGUAGE plpgsql;
