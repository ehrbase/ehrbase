/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.jooq;

import static java.lang.String.format;
import static org.ehrbase.jooq.pg.Tables.AUDIT_DETAILS;
import static org.ehrbase.jooq.pg.Tables.CONTRIBUTION;
import static org.ehrbase.jooq.pg.Tables.FOLDER;
import static org.ehrbase.jooq.pg.Tables.FOLDER_HIERARCHY;
import static org.ehrbase.jooq.pg.Tables.FOLDER_HISTORY;
import static org.ehrbase.jooq.pg.Tables.FOLDER_ITEMS;
import static org.ehrbase.jooq.pg.Tables.OBJECT_REF;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_AuditDetailsAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.dao.access.util.FolderUtils;
import org.ehrbase.dao.access.util.TransactionTime;
import org.ehrbase.jooq.binding.OtherDetailsJsonbBinder;
import org.ehrbase.jooq.binding.SysPeriodBinder;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.FolderHierarchy;
import org.ehrbase.jooq.pg.tables.FolderItems;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.jooq.pg.tables.records.FolderHierarchyRecord;
import org.ehrbase.jooq.pg.tables.records.FolderHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderItemsRecord;
import org.ehrbase.jooq.pg.tables.records.FolderRecord;
import org.ehrbase.jooq.pg.tables.records.ObjectRefRecord;
import org.ehrbase.util.UuidGenerator;
import org.joda.time.DateTime;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record9;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.IdentityConverter;

/**
 * Persistence operations on Folder.
 *
 * @author Luis Marco-Ruiz
 * @since 1.0.0
 */
public class FolderAccess extends DataAccess implements I_FolderAccess, Comparable<FolderAccess> {

    public static final String SUBFOLDERS = "subfolders";
    public static final String PARENT_FOLDER = "parent_folder";
    public static final String CHILD_FOLDER = "child_folder";
    public static final String CALLED_INVALID_ACCESS_LAYER_METHOD = "Called invalid access layer method.";

    private final List<ObjectRef<? extends ObjectId>> items = new ArrayList<>();
    private final Map<UUID, I_FolderAccess> subfoldersList = new TreeMap<>();
    private I_ContributionAccess contributionAccess;
    private I_AuditDetailsAccess auditDetailsAccess; // audit associated with this folder version
    private UUID ehrId;
    private FolderRecord folderRecord;

    private static final String ERR_NO_FOLDER = "No folder[%s] found";

    public static boolean isDeleted(I_DomainAccess domainAccess, UUID versionedObjectId) {
        if (domainAccess.getContext().fetchExists(FOLDER, FOLDER.ID.eq(versionedObjectId))) return false;

        if (!domainAccess.getContext().fetchExists(FOLDER_HISTORY, FOLDER_HISTORY.ID.eq(versionedObjectId)))
            throw new ObjectNotFoundException("folder", format(ERR_NO_FOLDER, versionedObjectId));

        Result<FolderHistoryRecord> historyRecordsRes = domainAccess
                .getContext()
                .selectFrom(FOLDER_HISTORY)
                .where(FOLDER_HISTORY.ID.eq(versionedObjectId))
                .orderBy(FOLDER_HISTORY.SYS_TRANSACTION.desc())
                .fetch();

        AuditDetailsRecord audit = domainAccess
                .getContext()
                .fetchOne(
                        AUDIT_DETAILS,
                        AUDIT_DETAILS.ID.eq(historyRecordsRes.get(0).getHasAudit()));

        if (audit == null) throw new InternalServerException("DB inconsistency: couldn't retrieve referenced audit");

        if (audit.getChangeType().equals(org.ehrbase.jooq.pg.enums.ContributionChangeType.deleted)) return true;

        throw new InternalServerException("Problem processing FolderAccess.isDeleted(..)");
    }

    /******** Constructors *******/
    FolderAccess(I_DomainAccess domainAccess, String tenantIdentifier) {
        super(domainAccess);
        init(null, tenantIdentifier);
    }

    private FolderAccess(
            I_DomainAccess domainAccess, UUID ehrId, I_ContributionAccess contributionAccess, String tenantIdentifier) {
        super(domainAccess);
        this.ehrId = ehrId;
        init(contributionAccess, tenantIdentifier);
    }

    private void init(I_ContributionAccess contributionAccess, String tenantIdentifier) {
        this.folderRecord = getContext().newRecord(org.ehrbase.jooq.pg.tables.Folder.FOLDER);

        if (contributionAccess != null) {
            this.contributionAccess = contributionAccess;
            this.folderRecord.setNamespace(contributionAccess.getNamespace());
        } else {
            this.contributionAccess = I_ContributionAccess.getInstance(this, this.ehrId, tenantIdentifier);
            this.folderRecord.setNamespace(tenantIdentifier);
        }

        this.contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
        // associate folder's own audit with this folder version access instance
        auditDetailsAccess = I_AuditDetailsAccess.getInstance(getDataAccess(), tenantIdentifier);
    }

    // *************Data Access and modification methods*****************

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(
            final LocalDateTime transactionTime,
            UUID systemId,
            UUID committerId,
            String description,
            ContributionChangeType changeType) {
        /* create new contribution */
        UUID oldContribution = this.folderRecord.getInContribution();
        UUID newContribution;

        // No custom contribution is provided, so create a new one
        UUID contributionAccessEhrId = this.contributionAccess.getEhrId();
        /*
         * save the EHR id from oldContribution since it will be the same as this is an
         * update operation
         */
        if (this.contributionAccess.getEhrId() == null) {
            ContributionRecord rec = getContext().fetchOne(CONTRIBUTION, CONTRIBUTION.ID.eq(oldContribution));
            contributionAccessEhrId = rec.getEhrId();
        }
        this.contributionAccess.setEhrId(contributionAccessEhrId);

        this.contributionAccess.commit(
                Timestamp.valueOf(transactionTime),
                committerId,
                systemId,
                ContributionDataType.folder,
                ContributionDef.ContributionState.COMPLETE,
                changeType,
                description);
        this.getFolderRecord().setInContribution(this.contributionAccess.getId());

        newContribution = folderRecord.getInContribution();

        return this.internalUpdate(
                Timestamp.valueOf(transactionTime),
                true,
                null,
                oldContribution,
                newContribution,
                systemId,
                committerId,
                description,
                changeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(final LocalDateTime transactionTime, UUID contribution) {
        /* create new contribution */
        UUID oldContribution = this.folderRecord.getInContribution();
        UUID newContribution;

        // Custom contribution is provided, so use given one
        this.getFolderRecord().setInContribution(contribution);

        newContribution = folderRecord.getInContribution();

        var newContributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), newContribution);
        UUID systemId = newContributionAccess.getAuditsSystemId();
        UUID committerId = newContributionAccess.getAuditsCommitter();
        String description = newContributionAccess.getAuditsDescription();
        ContributionChangeType changeType = newContributionAccess.getAuditsChangeType();

        return this.internalUpdate(
                Timestamp.valueOf(transactionTime),
                true,
                null,
                oldContribution,
                newContribution,
                systemId,
                committerId,
                description,
                changeType);
    }

    @SuppressWarnings("java:S107")
    private Boolean internalUpdate(
            final Timestamp transactionTime,
            boolean rootFolder,
            UUID parentFolder,
            UUID oldContribution,
            UUID newContribution,
            UUID systemId,
            UUID committerId,
            String description,
            ContributionChangeType contributionChangeType) {

        boolean result;

        UUID oldFolderId = getFolderId();

        // Set new Contribution for MODIFY
        this.setInContribution(newContribution);

        // create new folder audit with given values
        auditDetailsAccess = new AuditDetailsAccess(this, getFolderRecord().getNamespace());
        auditDetailsAccess.setSystemId(systemId);
        auditDetailsAccess.setCommitter(committerId);
        auditDetailsAccess.setDescription(description);
        auditDetailsAccess.setChangeType(I_ConceptAccess.fetchContributionChangeType(this, contributionChangeType));
        UUID auditId = this.auditDetailsAccess.commit();

        if (rootFolder) { // if it is the root folder preserve the original id, otherwise let the DB
            // provide a new one
            // for the overridden subfolders.
            folderRecord.setInContribution(newContribution);
            folderRecord.setSysTransaction(transactionTime);
            getContext().attach(folderRecord);
            result = folderRecord.update() > 0;
        } else {
            // Copy into new instance and attach to DB context.
            var updatedFolderRecord = new FolderRecord();

            updatedFolderRecord.setInContribution(newContribution);
            updatedFolderRecord.setName(this.getFolderName());
            updatedFolderRecord.setArchetypeNodeId(this.getFolderArchetypeNodeId());
            updatedFolderRecord.setActive(this.isFolderActive());
            updatedFolderRecord.setDetails(this.getFolderDetails());
            updatedFolderRecord.setSysTransaction(transactionTime);
            updatedFolderRecord.setSysPeriod(this.getFolderSysPeriod());
            updatedFolderRecord.setHasAudit(auditId);
            updatedFolderRecord.setNamespace(getFolderRecord().getNamespace());

            // attach to context DB
            getContext().attach(updatedFolderRecord);
            // Save new Folder entry to the database
            result = updatedFolderRecord.insert() > 0;
            // Finally overwrite original FolderRecord on this FolderAccess instance to have
            // the
            // new data available at service layer. Thus we do not need to re-fetch the
            // updated folder
            // tree from DB
            this.folderRecord = updatedFolderRecord;

            // Create FolderHierarchy entries this sub folder instance
            var updatedFhR = new FolderHierarchyRecord();
            updatedFhR.setParentFolder(parentFolder);
            updatedFhR.setChildFolder(updatedFolderRecord.getId());
            updatedFhR.setInContribution(newContribution);
            updatedFhR.setSysTransaction(transactionTime);
            updatedFhR.setSysPeriod(folderRecord.getSysPeriod());
            updatedFhR.setNamespace(getFolderRecord().getNamespace());
            getContext().attach(updatedFhR);
            updatedFhR.store();
        }
        // Get new folder id for folder items and hierarchy
        UUID updatedFolderId = this.folderRecord.getId();

        // delete old
        getDataAccess()
                .getContext()
                .delete(FolderItems.FOLDER_ITEMS)
                .where(FolderItems.FOLDER_ITEMS.FOLDER_ID.eq(oldFolderId))
                .execute();

        // Update items -> Save new list of all items in this folder
        this.saveFolderItems(
                updatedFolderId,
                oldContribution,
                newContribution,
                transactionTime,
                getContext(),
                getFolderRecord().getNamespace());

        boolean anySubfolderModified = this.getSubfoldersList() // Map of sub folders with UUID
                .values() // Get all I_FolderAccess entries
                .stream() // Iterate over the I_FolderAccess entries
                .map(subfolder ->
                        ( // Update each entry and return if there has been at least one entry updated
                        ((FolderAccess) subfolder)
                                .internalUpdate(
                                        transactionTime,
                                        false,
                                        updatedFolderId,
                                        oldContribution,
                                        newContribution,
                                        systemId,
                                        committerId,
                                        description,
                                        contributionChangeType)))
                .reduce((b1, b2) -> b1 || b2)
                .orElse(false);

        return result || anySubfolderModified;
    }

    private void saveFolderItems(
            final UUID folderId,
            final UUID oldContribution,
            final UUID newContribution,
            final Timestamp transactionTime,
            DSLContext context,
            String tenantIdentifier) {

        for (ObjectRef<?> or : this.getItems()) {

            // insert in object_ref
            ObjectRefRecord orr = new ObjectRefRecord(
                    or.getNamespace(),
                    or.getType(),
                    UUID.fromString(or.getId().getValue()),
                    newContribution,
                    transactionTime,
                    folderRecord.getSysPeriod(),
                    tenantIdentifier);
            context.attach(orr);
            orr.store();

            // insert in folder_item
            FolderItemsRecord fir = new FolderItemsRecord(
                    folderId,
                    UUID.fromString(or.getId().getValue()),
                    newContribution,
                    transactionTime,
                    folderRecord.getSysPeriod(),
                    tenantIdentifier);
            context.attach(fir);
            fir.store();
        }
    }

    /**
     * {@inheritDoc} Additional commit method to store a new entry of folder to the
     * database and get all of inserted sub folders connected by one contribution
     * which has been created before.
     *
     * @param transactionTime - Timestamp which will be applied to all folder
     *                        sys_transaction values
     * @param systemId        System ID for audit
     * @param committerId     Committer ID for audit
     * @param description     Optional description for audit
     * @return UUID of the new created root folder
     */
    @Override
    public UUID commit(LocalDateTime transactionTime, UUID systemId, UUID committerId, String description) {
        // Create Contribution entry for all folders
        this.contributionAccess.commit(
                Timestamp.valueOf(transactionTime),
                committerId,
                systemId,
                ContributionDataType.folder,
                ContributionDef.ContributionState.COMPLETE,
                I_ConceptAccess.ContributionChangeType.CREATION,
                description);

        return this.commit(transactionTime, this.contributionAccess.getContributionId());
    }

    /**
     * {@inheritDoc} Additional commit method to store a new entry of folder to the
     * database and get all of inserted sub folders connected by one contribution
     * which has been created before.
     *
     * @param transactionTime - Timestamp which will be applied to all folder
     *                        sys_transaction values
     * @param contributionId  - ID of contribution for CREATE applied to all folders
     *                        that will be created
     * @return UUID of the new created root folder
     */
    @Override
    public UUID commit(LocalDateTime transactionTime, UUID contributionId) {

        this.getFolderRecord().setInContribution(contributionId);
        var inputContributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contributionId);
        // create new folder audit with given values
        auditDetailsAccess = new AuditDetailsAccess(this, getFolderRecord().getNamespace());
        auditDetailsAccess.setSystemId(inputContributionAccess.getAuditsSystemId());
        auditDetailsAccess.setCommitter(inputContributionAccess.getAuditsCommitter());
        auditDetailsAccess.setDescription(inputContributionAccess.getAuditsDescription());
        auditDetailsAccess.setChangeType(
                I_ConceptAccess.fetchContributionChangeType(this, I_ConceptAccess.ContributionChangeType.CREATION));
        UUID auditId = this.auditDetailsAccess.commit();
        this.setAudit(auditId);

        // Save the folder record to database
        this.getFolderRecord().store();

        // Save folder items
        this.saveFolderItems(
                this.getFolderRecord().getId(),
                contributionId,
                contributionId,
                Timestamp.valueOf(transactionTime),
                getContext(),
                getFolderRecord().getNamespace());

        // Save list of sub folders to database with parent <-> child ID relations
        this.getSubfoldersList().values().forEach(child -> {
            child.commit(transactionTime, contributionId);
            FolderHierarchyRecord fhRecord = this.buildFolderHierarchyRecord(
                    this.getFolderRecord().getId(),
                    ((FolderAccess) child).getFolderRecord().getId(),
                    contributionId,
                    Timestamp.valueOf(transactionTime));
            fhRecord.store();
        });
        return this.getFolderRecord().getId();
    }

    /**
     * Retrieve instance of {@link I_FolderAccess} with the information needed
     * retrieve the folder and its sub-folders.
     *
     * @param domainAccess providing the information about the DB connection.
     * @param folderId     {@link java.util.UUID} of the
     *                     {@link com.nedap.archie.rm.directory.Folder} to be
     *                     fetched from the DB.
     * @return the {@link I_FolderAccess} that provides DB access to the
     *         {@link com.nedap.archie.rm.directory.Folder} that corresponds to the
     *         provided folderId param.
     */
    public static I_FolderAccess retrieveInstanceForExistingFolder(I_DomainAccess domainAccess, UUID folderId) {

        // 1 - retrieve CTE as a table that contains all the rows that allow to infer
        // each parent-child relationship
        Table<?> sfTable = table(select().from(FOLDER_HIERARCHY));

        Table<?> folderTable = table(select().from(FOLDER)).as("t_folder1");
        Table<?> folderTable2 = table(select().from(FOLDER)).as("t_folder2");

        Table<?> initialTable =
                table(select().from(FOLDER_HIERARCHY).where(FOLDER_HIERARCHY.PARENT_FOLDER.eq(folderId)));

        Field<UUID> subfolderChildFolder = field(
                "subfolders.{0}",
                FOLDER_HIERARCHY.CHILD_FOLDER.getDataType(), FOLDER_HIERARCHY.CHILD_FOLDER.getUnqualifiedName());
        Result<Record> folderSelectedRecordSub = domainAccess
                .getContext()
                .withRecursive(SUBFOLDERS)
                .as(select(ArrayUtils.addAll(initialTable.fields(), folderTable.fields()))
                        .from(initialTable)
                        .leftJoin(folderTable)
                        .on(initialTable
                                .field(PARENT_FOLDER, FOLDER_HIERARCHY.PARENT_FOLDER.getType())
                                .eq(folderTable.field("id", FOLDER.ID.getType())))
                        .union((select(ArrayUtils.addAll(sfTable.fields(), folderTable2.fields()))
                                        .from(sfTable)
                                        .innerJoin(SUBFOLDERS)
                                        .on(sfTable.field(PARENT_FOLDER, FOLDER_HIERARCHY.PARENT_FOLDER.getType())
                                                .eq(subfolderChildFolder)))
                                .leftJoin(folderTable2)
                                .on(folderTable2
                                        .field("id", FOLDER.ID.getType())
                                        .eq(subfolderChildFolder))))
                .select()
                .from(table(name(SUBFOLDERS)))
                .fetch();

        // 2 - Reconstruct hierarchical structure from DB result
        Map<UUID, Map<UUID, I_FolderAccess>> fHierarchyMap = new TreeMap<>();
        for (Record rec : folderSelectedRecordSub) {

            // 1-create a folder access for the record if needed
            if (!fHierarchyMap.containsKey(rec.getValue(PARENT_FOLDER, UUID.class))) {
                fHierarchyMap.put((UUID) rec.getValue(PARENT_FOLDER), new TreeMap<>());
            }
            fHierarchyMap
                    .get(rec.getValue(PARENT_FOLDER, UUID.class))
                    .put(
                            (UUID) rec.getValue(CHILD_FOLDER),
                            buildFolderAccessFromFolderId(
                                    (UUID) rec.getValue(CHILD_FOLDER), domainAccess, folderSelectedRecordSub));
        }

        // 3 - populate result and return
        return FolderAccess.buildFolderAccessHierarchy(
                fHierarchyMap, folderId, null, folderSelectedRecordSub, domainAccess);
    }

    /**
     * Retrieve a set of all folders from a given contribution.
     *
     * @param domainAccess Domain access object
     * @param contribution Given contribution ID to check for
     * @param nodeName     Nodename (e.g. "[...]::NODENAME::[...]") from the service
     *                     layer, which is not accessible in the access layer
     * @return Set of version ID of matching folders
     */
    public static Set<ObjectVersionId> retrieveFolderVersionIdsInContribution(
            I_DomainAccess domainAccess, UUID contribution, String nodeName) {
        Set<UUID> folders = new HashSet<>(); // Set, because of unique values
        // add all folders having a link to given contribution
        domainAccess
                .getContext()
                .select(FOLDER.ID)
                .from(FOLDER)
                .where(FOLDER.IN_CONTRIBUTION.eq(contribution))
                .fetch()
                .forEach(rec -> folders.add(rec.value1()));
        // and older versions or deleted ones, too
        domainAccess
                .getContext()
                .select(FOLDER_HISTORY.ID)
                .from(FOLDER_HISTORY)
                .where(FOLDER_HISTORY.IN_CONTRIBUTION.eq(contribution))
                .fetch()
                .forEach(rec -> folders.add(rec.value1()));

        // get whole "version map" of each matching folder and do fine-grain check for
        // matching contribution
        // precondition: each UUID in `folders` set is unique, so for each the "version
        // map" is only created once below
        // (meta: can't do that as jooq query, because the specific version number isn't
        // stored in DB)
        Set<ObjectVersionId> result = new HashSet<>();

        for (UUID folderId : folders) {
            Map<Record, Integer> map = getVersionMapOfFolder(domainAccess, folderId);
            // fine-grained contribution ID check
            for (Map.Entry<Record, Integer> entry : map.entrySet()) {
                // record can be of type FolderRecord or FolderHistoryRecord
                if (entry.getKey().getClass().equals(FolderRecord.class)) {
                    FolderRecord rec = (FolderRecord) entry.getKey();
                    if (rec.getInContribution().equals(contribution))
                    // set version ID
                    {
                        result.add(new ObjectVersionId(
                                rec.getId().toString() + "::" + nodeName + "::" + entry.getValue()));
                    }
                } else if (entry.getKey().getClass().equals(FolderHistoryRecord.class)) {
                    FolderHistoryRecord rec = (FolderHistoryRecord) entry.getKey();
                    if (rec.getInContribution().equals(contribution))
                    // set version ID
                    {
                        result.add(new ObjectVersionId(
                                rec.getId().toString() + "::" + nodeName + "::" + entry.getValue()));
                    }
                }
            }
        }

        return result;
    }

    public static I_FolderAccess retrieveByVersion(I_DomainAccess domainAccess, UUID folderId, int version) {
        Integer lastestVersion = getLastVersionNumber(domainAccess, folderId);
        if (lastestVersion == version) return retrieveInstanceForExistingFolder(domainAccess, folderId);

        Map<Integer, Record> allVersions = getVersionMapOfFolder(domainAccess, folderId).entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey()));

        if (!allVersions.containsKey(Integer.valueOf(version))) return null;

        Record record = allVersions.get(Integer.valueOf(version));
        Timestamp timestamp = record.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.SYS_TRANSACTION);

        I_FolderAccess retrieveInstanceForExistingFolder =
                FolderHistoryAccess.retrieveInstanceForExistingFolder(domainAccess, folderId, timestamp);
        return retrieveInstanceForExistingFolder;
    }

    /**
     * Helper to create a Map, which contains a record and the version number, for
     * each version of a versioned object.
     *
     * @param domainAccess Domain access object
     * @param folderId     Given versioned object folder ID
     * @return Map with a record and the version number, for each version of the
     *         versioned object folder with the given ID
     */
    private static Map<Record, Integer> getVersionMapOfFolder(I_DomainAccess domainAccess, UUID folderId) {
        Map<Record, Integer> versionMap = new HashMap<>();

        // create counter with highest version, to keep track of version number and
        // allow check in the end
        Integer versionCounter = getLastVersionNumber(domainAccess, folderId);

        // fetch matching entry
        FolderRecord rec = domainAccess.getContext().fetchOne(FOLDER, FOLDER.ID.eq(folderId));
        if (rec != null) {
            versionMap.put(rec, versionCounter);

            versionCounter--;
        }

        // if composition was removed (i.e. from "COMPOSITION" table) *or* other
        // versions are existing
        Result<FolderHistoryRecord> historyRecords = domainAccess
                .getContext()
                .selectFrom(FOLDER_HISTORY)
                .where(FOLDER_HISTORY.ID.eq(folderId))
                .orderBy(FOLDER_HISTORY.SYS_TRANSACTION.desc())
                .fetch();

        for (FolderHistoryRecord historyRecord : historyRecords) {
            versionMap.put(historyRecord, versionCounter);
            versionCounter--;
        }

        if (versionCounter != 0) {
            throw new InternalServerException("Version Map generation failed");
        }

        return versionMap;
    }

    /**
     * Builds the {@link I_FolderAccess} for persisting the
     * {@link com.nedap.archie.rm.directory.Folder} provided as param.
     *
     * @param domainAccess providing the information about the DB connection.
     * @param folder       to define the {@link I_FolderAccess} that allows its DB
     *                     access.
     * @param dateTime     that will be set as transaction date when the
     *                     {@link com.nedap.archie.rm.directory.Folder} is persisted
     * @param ehrId        of the {@link com.nedap.archie.rm.ehr.Ehr} that
     *                     references the
     *                     {@link com.nedap.archie.rm.directory.Folder} provided as
     *                     param.
     * @return {@link I_FolderAccess} with the information to persist the provided
     *         {@link com.nedap.archie.rm.directory.Folder}
     */
    public static I_FolderAccess getNewFolderAccessInstance(
            final I_DomainAccess domainAccess,
            final Folder folder,
            final DateTime dateTime,
            final UUID ehrId,
            String tenantIdentifier) {
        return buildFolderAccessTreeRecursively(domainAccess, folder, null, dateTime, ehrId, null, tenantIdentifier);
    }

    /**
     * {@inheritDoc} <br>
     * Includes sub-folders.
     */
    @Override
    public int delete(LocalDateTime timestamp, UUID systemId, UUID committerId, String description) {
        // create new contribution for this deletion action (with embedded
        // contribution.audit handling), overwrite old contribution with new one
        contributionAccess = I_ContributionAccess.getInstance(
                getDataAccess(),
                contributionAccess.getEhrId(),
                getFolderRecord().getNamespace());
        var contribution = contributionAccess.commit(
                TransactionTime.millis(),
                committerId,
                systemId,
                null,
                ContributionDef.ContributionState.COMPLETE,
                I_ConceptAccess.ContributionChangeType.DELETED,
                description);

        return this.delete(this.getFolderId(), contribution, systemId, committerId, description);
    }

    /**
     * {@inheritDoc} <br>
     * Includes sub-folders.
     */
    @Override
    public int delete(LocalDateTime timestamp, UUID contribution) {
        var newContributionAccess = I_ContributionAccess.retrieveInstance(this.getDataAccess(), contribution);
        UUID systemId = newContributionAccess.getAuditsSystemId();
        UUID committerId = newContributionAccess.getAuditsCommitter();
        String description = newContributionAccess.getAuditsDescription();

        return this.delete(this.getFolderId(), contribution, systemId, committerId, description);
    }

    /**
     * Deletes the FOLDER identified with the Folder.id provided and all its
     * subfolders recursively.
     *
     * @param folderId     of the {@link com.nedap.archie.rm.directory.Folder} to
     *                     delete.
     * @param contribution Optional contribution. Provide null to create a new one.
     * @param systemId     System ID for audit
     * @param committerId  Committer ID for audit
     * @param description  Optional description for audit
     * @return number of the total folders deleted recursively.
     */
    private Integer delete(
            final UUID folderId, UUID contribution, UUID systemId, UUID committerId, String description) {

        if (folderId == null) {
            throw new IllegalArgumentException(
                    "The folder UID provided for performing a delete operation cannot be null.");
        }

        // create new deletion audit
        var delAudit = I_AuditDetailsAccess.getInstance(
                this,
                systemId,
                committerId,
                I_ConceptAccess.ContributionChangeType.DELETED,
                description,
                this.folderRecord.getNamespace());
        UUID delAuditId = delAudit.commit();

        // Collect directly linked entities before applying changes:
        // Collect all linked hierarchy entries and linked (children) folders
        var hierarchyRecord = getContext()
                .fetch(
                        FOLDER_HIERARCHY,
                        FOLDER_HIERARCHY.PARENT_FOLDER.eq(folderId).or(FOLDER_HIERARCHY.CHILD_FOLDER.eq(folderId)));
        // Collect all linked item entries
        var itemsRecord = getContext().fetch(FOLDER_ITEMS, FOLDER_ITEMS.FOLDER_ID.eq(folderId));

        var result = 0;

        for (FolderHierarchyRecord rec : hierarchyRecord) {
            // Delete child folder, and actual children only. While later removing all
            // hierarchies anyway.
            if (rec.getParentFolder().equals(folderId)) {
                result += delete(rec.getChildFolder(), contribution, systemId, committerId, description);
            }
            // Delete whole hierarchy entity
            rec.delete();
        }

        // Delete each linked items entity
        for (FolderItemsRecord rec : itemsRecord) {
            rec.delete();
        }

        // .delete() moves the old version to _history table.
        var folderRec = getContext().fetchOne(FOLDER, FOLDER.ID.eq(folderId));
        result += folderRec.delete();

        // create new, BUT already moved to _history, version documenting the deletion
        newOrUpdate(folderRec, delAuditId, contribution);

        return result;
    }

    private void newOrUpdate(FolderRecord folderRecord, UUID delAuditId, UUID contrib) {
        Condition condition =
                FOLDER_HISTORY.ID.eq(folderRecord.getId()).and(FOLDER_HISTORY.IN_CONTRIBUTION.eq(contrib));
        FolderHistoryRecord record = getDataAccess().getContext().fetchOne(FOLDER_HISTORY, condition);
        if (record == null)
            populateChangesAndPersist(
                    getDataAccess().getContext().newRecord(FOLDER_HISTORY), folderRecord, delAuditId, contrib);
        else populateChangesAndPersist(record, folderRecord, delAuditId, contrib);
    }

    private void populateChangesAndPersist(FolderHistoryRecord trgt, FolderRecord src, UUID delAuditId, UUID contrib) {
        // a bit hacky: create new, BUT already moved to _history, version documenting
        // the deletion
        // (Normal approach of first .update() then .delete() won't work, because
        // postgres' transaction optimizer will
        // just skip the update if it will get deleted anyway.)
        // so copy values, but add deletion meta data
        trgt.setId(src.getId());
        trgt.setInContribution(contrib);
        trgt.setName(src.getName());
        trgt.setArchetypeNodeId(src.getArchetypeNodeId());
        trgt.setNamespace(src.getNamespace());
        trgt.setActive(src.getActive());
        trgt.setDetails(src.getDetails());
        trgt.setHasAudit(delAuditId);
        trgt.setSysTransaction(TransactionTime.millis());
        trgt.setSysPeriod(new AbstractMap.SimpleEntry<>(OffsetDateTime.now(), null));

        if (trgt.store() != 1) {
            // commit and throw error if nothing was inserted into DB
            throw new InternalServerException("DB inconsistency");
        }
    }

    /**
     * Create a new FolderAccess that contains the full hierarchy of its
     * corresponding {@link I_FolderAccess} children that represents the subfolders.
     *
     * @param fHierarchyMap           {@link java.util.Map} containing as key the
     *                                UUID of each Folder, and as value an internal
     *                                Map. For the internal Map the key is the the
     *                                UUID of a child
     *                                {@link com.nedap.archie.rm.directory.Folder},
     *                                and the value is the {@link I_FolderAccess}
     *                                for enabling DB access to this child.
     * @param currentFolder           {@link java.util.UUID} of the current
     *                                {@link com.nedap.archie.rm.directory.Folder}
     *                                to treat in the current recursive call of the
     *                                method.
     * @param parentFa                the parent {@link I_FolderAccess} that
     *                                corresponds to the parent
     *                                {@link com.nedap.archie.rm.directory.Folder}
     *                                of the
     *                                {@link com.nedap.archie.rm.directory.Folder}
     *                                identified as current.
     * @param folderSelectedRecordSub {@link org.jooq.Result} containing the Records
     *                                that represent the rows to retrieve from the
     *                                DB corresponding to the children hierarchy.
     * @param domainAccess            containing the information of the DB
     *                                connection.
     * @return I_FolderAccess populated with its appropriate subfolders as
     *         FolderAccess objects.
     */
    private static I_FolderAccess buildFolderAccessHierarchy(
            final Map<UUID, Map<UUID, I_FolderAccess>> fHierarchyMap,
            final UUID currentFolder,
            final I_FolderAccess parentFa,
            final Result<Record> folderSelectedRecordSub,
            final I_DomainAccess domainAccess) {
        if ((parentFa != null) && (parentFa.getSubfoldersList().containsKey(currentFolder))) {
            return parentFa.getSubfoldersList().get(currentFolder);
        }
        I_FolderAccess folderAccess =
                buildFolderAccessFromFolderId(currentFolder, domainAccess, folderSelectedRecordSub);
        if (parentFa != null) {
            parentFa.getSubfoldersList().put(currentFolder, folderAccess);
        }
        if (fHierarchyMap.get(currentFolder) != null) { // if not leave node call children

            for (UUID newChild : fHierarchyMap.get(currentFolder).keySet()) {
                buildFolderAccessHierarchy(
                        fHierarchyMap, newChild, folderAccess, folderSelectedRecordSub, domainAccess);
            }
        }
        return folderAccess;
    }

    /**
     * Create a new {@link FolderAccess} from a {@link org.jooq.Record} DB record
     *
     * @param folderRecord record containing all the information to build one
     *                     folder-subfolder relationship.
     * @param domainAccess containing the DB connection information.
     * @return FolderAccess instance
     */
    private static FolderAccess buildFolderAccessFromGenericRecord(
            final Record folderRecord, final I_DomainAccess domainAccess) {

        UUID folderId = folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.ID);
        UUID contributionId = folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.IN_CONTRIBUTION);

        String tenantIdentifier = folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.NAMESPACE);

        FolderAccess folderAccess = new FolderAccess(domainAccess, tenantIdentifier);
        folderAccess.folderRecord = new FolderRecord();
        folderAccess.folderRecord.setNamespace(tenantIdentifier);
        folderAccess.setFolderId(folderId);
        folderAccess.setInContribution(contributionId);
        folderAccess.setFolderName(folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.NAME));
        folderAccess.setFolderNArchetypeNodeId(
                folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.ARCHETYPE_NODE_ID));
        folderAccess.setIsFolderActive(folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.ACTIVE));

        // This must be done due to the fact that ....History details are JSONB objects
        Object object = folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.DETAILS.getName());
        if (object instanceof ItemStructure)
            folderAccess.setFolderDetails(folderRecord.get(
                    org.ehrbase.jooq.pg.tables.Folder.FOLDER.DETAILS.getName(),
                    new IdentityConverter<ItemStructure>(ItemStructure.class)));
        else
            folderAccess.setFolderDetails(folderRecord.get(
                    org.ehrbase.jooq.pg.tables.Folder.FOLDER.DETAILS.getName(),
                    new OtherDetailsJsonbBinder().converter()));

        folderAccess.setFolderSysTransaction(
                folderRecord.get(org.ehrbase.jooq.pg.tables.Folder.FOLDER.SYS_TRANSACTION));
        folderAccess.setFolderSysPeriod(folderRecord.get(
                org.ehrbase.jooq.pg.tables.Folder.FOLDER.SYS_PERIOD.getName(), new SysPeriodBinder().converter()));

        folderAccess
                .getItems()
                .addAll(FolderAccess.retrieveItemsByFolderAndContributionId(folderId, contributionId, domainAccess));

        return folderAccess;
    }

    /**
     * Create a new FolderAccess from a {@link FolderRecord} DB record
     *
     * @param folderRecord containing the information of a
     *                     {@link com.nedap.archie.rm.directory.Folder} in the DB.
     * @param domainAccess containing the DB connection information.
     * @return FolderAccess instance corresponding to the
     *         org.ehrbase.jooq.pg.tables.records.FolderRecord provided.
     */
    private static FolderAccess buildFolderAccessFromFolderRecord(
            final FolderRecord folderRecord, final I_DomainAccess domainAccess) {
        var folderAccess = new FolderAccess(domainAccess, folderRecord.getNamespace());
        folderAccess.folderRecord = folderRecord;
        folderAccess
                .getItems()
                .addAll(FolderAccess.retrieveItemsByFolderAndContributionId(
                        folderRecord.getId(), folderRecord.getInContribution(), domainAccess));
        return folderAccess;
    }

    /**
     * Given a UUID for a folder creates the corresponding FolderAccess from the
     * information conveyed by the {@link org.jooq.Result} provided. Alternatively
     * queries the DB if the information needed is not in {@link org.jooq.Result}.
     * * @param id of the folder to define a {@link FolderAccess} from. * @param
     * {@link org.jooq.Result} containing the Records that represent the rows to
     * retrieve from the DB corresponding to the children hierarchy.
     *
     * @return a FolderAccess corresponding to the Folder id provided
     */
    private static FolderAccess buildFolderAccessFromFolderId(
            final UUID id, final I_DomainAccess domainAccess, final Result<Record> folderSelectedRecordSub) {

        for (Record current : folderSelectedRecordSub) {
            // if the FOLDER items were returned in the recursive query use them and avoid a
            // DB transaction
            if (current.getValue(PARENT_FOLDER).equals(id)) {

                return buildFolderAccessFromGenericRecord(current, domainAccess);
            }
        }

        // if no data from the Folder has been already recovered for the id of the
        // folder, then query the DB for it.
        FolderRecord folderSelectedRecord = domainAccess
                .getContext()
                .selectFrom(FOLDER)
                .where(FOLDER.ID.eq(id))
                .fetchOne();

        if (folderSelectedRecord == null || folderSelectedRecord.size() < 1) {
            throw new ObjectNotFoundException("folder", "Folder with id " + id + " could not be found");
        }

        return buildFolderAccessFromFolderRecord(folderSelectedRecord, domainAccess);
    }

    /**
     * Builds the FolderAccess with the collection of subfolders empty.
     *
     * @param domainAccess providing the information about the DB connection.
     * @param folder       to define a corresponding {@link I_FolderAccess} for
     *                     allowing its persistence.
     * @param timestamp    that will be set as transaction date when the
     *                     {@link com.nedap.archie.rm.directory.Folder} is persisted
     * @param ehrId        of the {@link com.nedap.archie.rm.ehr.Ehr} that
     *                     references this
     *                     {@link com.nedap.archie.rm.directory.Folder}
     * @return {@link I_FolderAccess} with the information to persist the provided
     *         {@link com.nedap.archie.rm.directory.Folder}
     */
    public static I_FolderAccess buildPlainFolderAccess(
            final I_DomainAccess domainAccess,
            final Folder folder,
            final Timestamp timestamp,
            final UUID ehrId,
            final I_ContributionAccess contributionAccess,
            String tenantIdentifier) {

        FolderAccess folderAccessInstance = new FolderAccess(domainAccess, ehrId, contributionAccess, tenantIdentifier);
        folderAccessInstance.setEhrId(ehrId);
        // In case of creation we have no folderId since it will be created from DB
        if (folder.getUid() != null) {
            UIDBasedId uid = folder.getUid();
            int i = uid.getValue().indexOf("::");
            String uidString;
            if (i < 0) {
                uidString = uid.getValue();
            } else {
                uidString = uid.getValue().substring(0, i);
            }
            folderAccessInstance.setFolderId(UUID.fromString(uidString));
        }
        folderAccessInstance.setInContribution(
                folderAccessInstance.getContributionAccess().getId());
        folderAccessInstance.setFolderName(folder.getName().getValue());
        folderAccessInstance.setFolderNArchetypeNodeId(folder.getArchetypeNodeId());
        folderAccessInstance.setIsFolderActive(true);
        folderAccessInstance.setFolderDetails(folder.getDetails());

        if (folder.getItems() != null && !folder.getItems().isEmpty()) {
            folderAccessInstance.getItems().addAll(folder.getItems());
        }

        folderAccessInstance.setFolderSysTransaction(
                new Timestamp(DateTime.now().getMillis()));
        return folderAccessInstance;
    }

    /**
     * Retrieves a list containing the items as ObjectRefs of the folder
     * corresponding to the id provided.
     *
     * @param folderId       of the FOLDER that the items correspond to.
     * @param inContribution contribution that establishes the reference between a
     *                       FOLDER and its item.
     * @param domainAccess   connection DB data.
     * @return
     */
    private static List<ObjectRef<?>> retrieveItemsByFolderAndContributionId(
            UUID folderId, UUID inContribution, I_DomainAccess domainAccess) {
        Result<Record> retrievedRecords = domainAccess
                .getContext()
                .with("folderItemsSelect")
                .as(select(
                                FOLDER_ITEMS.OBJECT_REF_ID.as("object_ref_id"),
                                FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"))
                        .from(FOLDER_ITEMS)
                        .where(FOLDER_ITEMS.FOLDER_ID.eq(folderId)))
                .select()
                .from(OBJECT_REF, table(name("folderItemsSelect")))
                .where(field(name("object_ref_id"), FOLDER_ITEMS.OBJECT_REF_ID.getType())
                        .eq(OBJECT_REF.ID)
                        .and(field(name("item_in_contribution"), FOLDER_ITEMS.IN_CONTRIBUTION.getType())
                                .eq(OBJECT_REF.IN_CONTRIBUTION)))
                .fetch();

        List<ObjectRef<?>> result = new ArrayList<>();
        for (Record recordRecord : retrievedRecords) {
            Record9<
                            String,
                            String,
                            UUID,
                            UUID,
                            Timestamp,
                            AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>,
                            String,
                            UUID,
                            UUID>
                    recordParam = (Record9<
                                    String,
                                    String,
                                    UUID,
                                    UUID,
                                    Timestamp,
                                    AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>,
                                    String,
                                    UUID,
                                    UUID>)
                            recordRecord;
            ObjectRefRecord objectRef = new ObjectRefRecord();
            objectRef.setIdNamespace(recordParam.value1());
            objectRef.setType(recordParam.value2());
            objectRef.setId(recordParam.value3());
            objectRef.setInContribution(recordParam.value4());
            objectRef.setSysTransaction(recordParam.value5());
            objectRef.setSysPeriod(new SysPeriodBinder().converter().from(recordParam.value6()));
            objectRef.setNamespace(recordParam.value7());
            objectRef.setId(recordParam.value8());
            result.add(parseObjectRefRecordIntoObjectRef(objectRef));
        }
        return result;
    }

    /**
     * Transforms a ObjectRef DB record into a Reference Model object.
     *
     * @param objectRefRecord
     * @return the reference model object.
     */
    private static ObjectRef<ObjectVersionId> parseObjectRefRecordIntoObjectRef(ObjectRefRecord objectRefRecord) {
        ObjectRef<ObjectVersionId> result = new ObjectRef<>();
        ObjectRefId oref = new ObjectRefId(objectRefRecord.getId().toString());
        result.setId(new ObjectVersionId(oref.getValue()));
        result.setType(objectRefRecord.getType());
        result.setNamespace(objectRefRecord.getIdNamespace());
        return result;
    }

    /**
     * Recursive method for populating the hierarchy of {@link I_FolderAccess} for a
     * given {@link com.nedap.archie.rm.directory.Folder}.
     *
     * @param domainAccess       providing the information about the DB connection.
     * @param current            {@link com.nedap.archie.rm.directory.Folder}
     *                           explored in the current iteration.
     * @param parent             folder of the
     *                           {@link com.nedap.archie.rm.directory.Folder}
     *                           procided as the current parameter.
     * @param dateTime           of the transaction that will be stored inthe DB.
     * @param ehrId              of the {@link com.nedap.archie.rm.ehr.Ehr}
     *                           referencing the current
     *                           {@link com.nedap.archie.rm.directory.Folder}.
     * @param contributionAccess that corresponds to the contribution that the
     *                           {@link com.nedap.archie.rm.directory.Folder} refers
     *                           to.
     * @return {@link I_FolderAccess} with the complete hierarchy of sub-folders
     *         represented as {@link I_FolderAccess}.
     */
    private static I_FolderAccess buildFolderAccessTreeRecursively(
            final I_DomainAccess domainAccess,
            final Folder current,
            final FolderAccess parent,
            final DateTime dateTime,
            final UUID ehrId,
            final I_ContributionAccess contributionAccess,
            String tenantIdentifier) {
        I_FolderAccess folderAccess;

        // if the parent already contains the FolderAccess for the specified folder
        // return the corresponding
        // FolderAccess
        if ((parent != null)
                && (parent.getSubfoldersList()
                        .containsKey(UUID.fromString(current.getUid().getValue())))) {
            return parent.getSubfoldersList().get(current.getUid());
        }
        // create the corresponding FolderAccess for the current folder
        folderAccess = FolderAccess.buildPlainFolderAccess(
                domainAccess, current, Timestamp.from(Instant.now()), ehrId, contributionAccess, tenantIdentifier);
        // add to parent subfolder list
        if (parent != null) {
            parent.getSubfoldersList()
                    .put(((FolderAccess) folderAccess).getFolderRecord().getId(), folderAccess);
        }
        for (Folder child : current.getFolders()) {
            buildFolderAccessTreeRecursively(
                    domainAccess,
                    child,
                    (FolderAccess) folderAccess,
                    dateTime,
                    ehrId,
                    ((FolderAccess) folderAccess).getContributionAccess(),
                    tenantIdentifier);
        }
        return folderAccess;
    }

    /**
     * Builds a folderAccess hierarchy recursively by iterating over all sub folders
     * of given folder instance. This works for all folders, i.e. from root for an
     * insert as well for a sub folder hierarchy for update.
     *
     * @param domainAccess       - DB connection context
     * @param folder             - Folder to create access for
     * @param timeStamp          - Current time for transaction audit
     * @param ehrId              - Corresponding EHR
     * @param contributionAccess - Contribution instance for creation of all folders
     * @return FolderAccess instance for folder
     */
    public static I_FolderAccess buildNewFolderAccessHierarchy(
            final I_DomainAccess domainAccess,
            final Folder folder,
            final Timestamp timeStamp,
            final UUID ehrId,
            final I_ContributionAccess contributionAccess,
            String tenantIdentifier) {
        // Create access for the current folder
        I_FolderAccess folderAccess =
                buildPlainFolderAccess(domainAccess, folder, timeStamp, ehrId, contributionAccess, tenantIdentifier);

        if (folder.getFolders() != null && !folder.getFolders().isEmpty()) {
            // Iterate over sub folders and create FolderAccess for each sub folder
            folder.getFolders().forEach(child -> {
                // Call recursive creation of folderAccess for children without uid
                I_FolderAccess childFolderAccess = buildNewFolderAccessHierarchy(
                        domainAccess, child, timeStamp, ehrId, contributionAccess, tenantIdentifier);
                folderAccess.getSubfoldersList().put(UuidGenerator.randomUUID(), childFolderAccess);
            });
        }
        return folderAccess;
    }

    /**
     * @param parentFolder   identifier.
     * @param childFolder    identifier to define the {@link FolderHierarchyRecord}
     *                       from.
     * @param inContribution contribution that the
     *                       {@link com.nedap.archie.rm.directory.Folder} refers to.
     * @param sysTransaction date of the transaction.
     * @return the {@link FolderHierarchyRecord} for persisting the folder
     *         identified by the childFolder param.
     */
    private FolderHierarchyRecord buildFolderHierarchyRecord(
            final UUID parentFolder,
            final UUID childFolder,
            final UUID inContribution,
            final Timestamp sysTransaction) {
        FolderHierarchyRecord fhRecord = getContext().newRecord(FolderHierarchy.FOLDER_HIERARCHY);
        fhRecord.setParentFolder(parentFolder);
        fhRecord.setChildFolder(childFolder);
        fhRecord.setInContribution(inContribution);
        fhRecord.setSysTransaction(sysTransaction);
        fhRecord.setNamespace(getFolderRecord().getNamespace());
        // fhRecord.setSysPeriod(sysPeriod); sys period can be left to null so the
        // system sets it for the temporal
        // tables.
        return fhRecord;
    }

    /**
     * Returns the last version number of a given folder by counting all previous
     * versions of a given folder id. If there are no previous versions in the
     * history table the version number will be 1. Otherwise the current version
     * equals the count of entries in the folder history table plus 1.
     *
     * @param domainAccess - Database connection access context
     * @param folderId     - ObjectVersionUid of the folder to check for the last
     *                     version
     * @return Latest version number for the folder
     */
    public static Integer getLastVersionNumber(I_DomainAccess domainAccess, ObjectVersionId folderId) {

        UUID folderUuid = FolderUtils.extractUuidFromObjectVersionId(folderId);

        return getLastVersionNumber(domainAccess, folderUuid);
    }

    // whole ObjectVersionId is just not necessary for DB query, so this works on
    // access layer (without info like the
    // nodeName), too.
    private static Integer getLastVersionNumber(I_DomainAccess domainAccess, UUID folderUuid) {

        if (!hasPreviousVersion(domainAccess, folderUuid)) {
            return 1;
        }
        // Get number of entries as the history table of folders
        int versionCount = domainAccess.getContext().fetchCount(FOLDER_HISTORY, FOLDER_HISTORY.ID.eq(folderUuid));
        // Latest version will be entries plus actual entry count (always 1)
        return versionCount + 1;
    }

    /**
     * Checks if there are existing entries for given folder uuid at the folder
     * history table. If there are entries existing, the folder has been modified
     * during previous actions and there are older versions existing.
     *
     * @param domainAccess - Database connection access context
     * @param folderId     - UUID of folder to check
     * @return Folder has previous versions or not
     */
    public static boolean hasPreviousVersion(I_DomainAccess domainAccess, UUID folderId) {
        return domainAccess.getContext().fetchExists(FOLDER_HISTORY, FOLDER_HISTORY.ID.eq(folderId));
    }

    /**
     * Evaluates the version for a folder at a given timestamp by counting all rows
     * from folder history with root folder id and a sys_period timestamp before or
     * at given timestamp value as also from the current folder entry if the
     * sys_period provided is also newer than the sys_period of the current folder.
     *
     * @param domainAccess   - Database access instance
     * @param rootFolderId   - Root folder id
     * @param sysTransaction - Timestamp to get version for
     * @return - Version number that has been current at that point in time
     */
    public static int getVersionNumberAtTime(
            I_DomainAccess domainAccess, final ObjectVersionId rootFolderId, final Timestamp sysTransaction) {

        UUID folderUuid = FolderUtils.extractUuidFromObjectVersionId(rootFolderId);

        // Check if the timestamp also includes the current folder
        int folderCount = domainAccess
                .getContext()
                .fetchCount(
                        FOLDER, FOLDER.ID.equal(folderUuid).and(FOLDER.SYS_TRANSACTION.lessOrEqual(sysTransaction)));

        // Count all history entries for the root folder
        int folderHistoryCount = domainAccess
                .getContext()
                .fetchCount(
                        FOLDER_HISTORY,
                        FOLDER_HISTORY
                                .ID
                                .equal(folderUuid)
                                .and(FOLDER_HISTORY.SYS_TRANSACTION.lessOrEqual(sysTransaction)));

        if (folderHistoryCount <= 0) {
            // No history entries found

            if (folderCount <= 0) {
                // Also no current entries
                throw new ObjectNotFoundException(
                        "directory",
                        "No folder found for " + rootFolderId + " at time "
                                + sysTransaction.toLocalDateTime().toString());
            }

            return folderCount;
        }

        // If we found entries in both tables return the sum. If there is no current
        // entry the count will be 0
        return folderHistoryCount + folderCount;
    }

    public static Timestamp getTimestampForVersion(
            I_DomainAccess domainAccess, final ObjectVersionId rootFolderId, Integer version) {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        UUID rootFolderUuid = FolderUtils.extractUuidFromObjectVersionId(rootFolderId);
        // Get latest version number
        int currentVersion = FolderAccess.getVersionNumberAtTime(domainAccess, rootFolderId, timestamp);

        if (currentVersion > version) {
            // Select number of rows from folder history record that are required
            Result<FolderHistoryRecord> folderHistoryRecords = domainAccess
                    .getContext()
                    .selectFrom(FOLDER_HISTORY)
                    .where(FOLDER_HISTORY.ID.equal(rootFolderUuid))
                    .orderBy(FOLDER_HISTORY.SYS_TRANSACTION.desc())
                    .limit(currentVersion - version)
                    .fetch();
            // Return sys_transaction timestamp of last entry if existing
            if (!folderHistoryRecords.isEmpty()) {
                timestamp = folderHistoryRecords
                        .get(folderHistoryRecords.size() - 1)
                        .get(FOLDER_HISTORY.SYS_TRANSACTION);
            }
        }
        // The timestamp now contains either the last entry found in folder history or
        // the current time if the desired
        // version matches or is greater than the latest.
        return timestamp;
    }

    /**** Getters and Setters for the FolderRecord to store ****/
    public UUID getEhrId() {
        return ehrId;
    }

    public void setEhrId(final UUID ehrId) {
        this.ehrId = ehrId;
    }

    public I_ContributionAccess getContributionAccess() {
        return contributionAccess;
    }

    public void setContributionAccess(final I_ContributionAccess contributionAccess) {
        this.contributionAccess = contributionAccess;
    }

    FolderRecord getFolderRecord() {
        return folderRecord;
    }

    @Override
    public Map<UUID, I_FolderAccess> getSubfoldersList() {
        return this.subfoldersList;
    }

    @Override
    public List<ObjectRef<? extends ObjectId>> getItems() {
        return this.items;
    }

    @Override
    public UUID getFolderId() {

        return this.folderRecord.getId();
    }

    @Override
    public void setFolderId(UUID folderId) {

        this.folderRecord.setId(folderId);
    }

    @Override
    public UUID getInContribution() {
        return this.folderRecord.getInContribution();
    }

    @Override
    public void setInContribution(UUID inContribution) {

        this.folderRecord.setInContribution(inContribution);
    }

    @Override
    public String getFolderName() {

        return this.folderRecord.getName();
    }

    @Override
    public void setFolderName(String folderName) {

        this.folderRecord.setName(folderName);
    }

    @Override
    public String getFolderArchetypeNodeId() {

        return this.folderRecord.getArchetypeNodeId();
    }

    @Override
    public void setFolderNArchetypeNodeId(String folderArchetypeNodeId) {

        this.folderRecord.setArchetypeNodeId(folderArchetypeNodeId);
    }

    @Override
    public boolean isFolderActive() {

        return this.folderRecord.getActive();
    }

    @Override
    public void setIsFolderActive(boolean folderActive) {

        this.folderRecord.setActive(folderActive);
    }

    @Override
    public ItemStructure getFolderDetails() {

        return this.folderRecord.getDetails();
    }

    @Override
    public void setFolderDetails(ItemStructure folderDetails) {

        this.folderRecord.setDetails(folderDetails);
    }

    @Override
    public void setFolderSysTransaction(Timestamp folderSysTransaction) {

        this.folderRecord.setSysTransaction(folderSysTransaction);
    }

    @Override
    public Timestamp getFolderSysTransaction() {
        return this.folderRecord.getSysTransaction();
    }

    @Override
    public AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> getFolderSysPeriod() {

        return this.folderRecord.getSysPeriod();
    }

    @Override
    public void setFolderSysPeriod(AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> folderSysPeriod) {

        this.folderRecord.setSysPeriod(folderSysPeriod);
    }

    @Override
    public UUID getAudit() {
        return this.getFolderRecord().getHasAudit();
    }

    @Override
    public void setAudit(UUID auditId) {
        this.getFolderRecord().setHasAudit(auditId);
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public int compareTo(final FolderAccess o) {
        return o.getFolderRecord().getId().compareTo(this.folderRecord.getId());
    }

    @Override
    public void adminDeleteFolder() {
        AdminApiUtils adminApi = new AdminApiUtils(getContext());
        adminApi.deleteFolder(this.getFolderId(), true);
    }

    public static class ObjectRefId extends ObjectId {
        public ObjectRefId(final String value) {
            super(value);
        }
    }

    @Override
    public Timestamp getSysTransaction() {
        return this.getFolderSysTransaction();
    }

    @Override
    public UUID getContributionId() {
        return this.getInContribution();
    }

    @Override
    public UUID getId() {
        return this.getFolderId();
    }
}
