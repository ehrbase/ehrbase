-- Fix issue with V62__add_entry_history_missing_columns.sql

DO
$$
BEGIN
        IF
         (EXISTS
            (SELECT 1
             FROM information_schema.tables
             WHERE table_schema = 'ehr'
               AND table_name = 'flyway_schema_history'
            ))
        THEN
           IF
            (EXISTS
              (SELECT 1
               FROM ehr.flyway_schema_history
               WHERE (version, checksum) = ('62', -307543225)
              ))
           THEN
              UPDATE ehr.flyway_schema_history
              SET checksum = 1440169380
             WHERE (version, checksum) = ('62', -307543225);
           END IF;
        END IF;
END
$$;

