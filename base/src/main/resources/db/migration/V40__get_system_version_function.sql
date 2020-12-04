-- ====================================================================
-- Author: Axel Siebert (axel.siebert@vitagroup.ag)
-- Create date: 2020-11-24
-- Description: Retrieves all information on running db system including environment os by running VERSION() function.
--
-- Returns: Version string of running db server including os information
-- =====================================================================
DROP FUNCTION IF EXISTS ehr.get_system_version;

CREATE OR REPLACE FUNCTION ehr.get_system_version()
RETURNS TEXT
AS $$
DECLARE
  version_string TEXT;
BEGIN
	SELECT VERSION() INTO version_string;
	RETURN version_string;
END; $$ LANGUAGE plpgsql;
