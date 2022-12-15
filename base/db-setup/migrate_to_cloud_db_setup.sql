-- use this script to apply triggers for temporal tables (either using the versioning() function  or
-- temporal_tables extension
-- NB. This script should be run after DB migration is done (mvn flyway:migrate)
-- This is f.e. required when performing
-- DROP EXTENSION 'temporal_tables' CASCADE

CREATE TRIGGER versioning_trigger
  BEFORE INSERT OR DELETE OR UPDATE
  ON ehr.folder
  FOR EACH ROW
EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.folder_history', 'true');

CREATE TRIGGER versioning_trigger
  BEFORE INSERT OR DELETE OR UPDATE
  ON ehr.folder_hierarchy
  FOR EACH ROW
EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.folder_hierarchy_history', 'true');

CREATE TRIGGER versioning_trigger
  BEFORE INSERT OR DELETE OR UPDATE
  ON ehr.folder_items
  FOR EACH ROW
EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.folder_items_history', 'true');

CREATE TRIGGER versioning_trigger
  BEFORE INSERT OR UPDATE OR DELETE
  ON ehr.status
  FOR EACH ROW
EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.status_history', true);

CREATE TRIGGER versioning_trigger
  BEFORE INSERT OR UPDATE OR DELETE
  ON ehr.composition
  FOR EACH ROW
EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.composition_history', true);

CREATE TRIGGER versioning_trigger
  BEFORE INSERT OR UPDATE OR DELETE
  ON ehr.event_context
  FOR EACH ROW
EXECUTE PROCEDURE ext.versioning('sys_period', 'ehr.event_context_history', true);