-- Fix issues with V62__add_entry_history_missing_columns.sql etc.

DO
$$
    BEGIN
        IF (EXISTS
            (SELECT 1
             FROM information_schema.tables
             WHERE table_schema = 'ehr'
               AND table_name = 'flyway_schema_history'))
        THEN

            UPDATE ehr.flyway_schema_history
            SET checksum = 1370938853
            WHERE (version, checksum) = ('16', -2009117355);

            UPDATE ehr.flyway_schema_history
            SET checksum = -55836684
            WHERE (version, checksum) = ('61', -1745413492);

            UPDATE ehr.flyway_schema_history
            SET checksum = 1440169380
            WHERE (version, checksum) = ('62', -307543225);

            UPDATE ehr.flyway_schema_history
            SET checksum = 1953744080
            WHERE (version, checksum) = ('71', -1047639409);

            UPDATE ehr.flyway_schema_history
            SET checksum = 861164608
            WHERE (version, checksum) = ('83', -1840801783)
               OR (version, checksum) = ('83', 1051439940);

        END IF;
    END
$$;

