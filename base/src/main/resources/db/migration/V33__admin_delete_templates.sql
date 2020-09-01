-- Physically delete all templates from database

CREATE OR REPLACE FUNCTION ehr.admin_delete_templates ()
RETURNS integer AS $deleted$
DECLARE
  deleted integer;
BEGIN
	SELECT count(*) INTO deleted FROM ehr.template_store;
	DELETE FROM ehr.template_store ts WHERE ts.id NOTNULL;
	RETURN deleted;
END;
$deleted$ LANGUAGE plpgsql;
END
