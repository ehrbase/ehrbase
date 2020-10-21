-- fix bug 320: syncing history table with main table
ALTER TABLE ehr.contribution_history
    DROP COLUMN system_id,
    DROP COLUMN committer,
    DROP COLUMN time_committed,
    DROP COLUMN time_committed_tzid, -- timezone id
    DROP COLUMN change_type,
    DROP COLUMN description;

-- fix bug 318: directory foreign key in ehr table
ALTER TABLE ehr.ehr
    DROP COLUMN directory,
    ADD COLUMN directory UUID references ehr.folder(id);