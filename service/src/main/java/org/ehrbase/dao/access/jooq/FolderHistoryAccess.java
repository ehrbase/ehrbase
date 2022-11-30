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

import static org.ehrbase.jooq.pg.Tables.FOLDER;
import static org.ehrbase.jooq.pg.Tables.FOLDER_HIERARCHY;
import static org.ehrbase.jooq.pg.Tables.FOLDER_HIERARCHY_HISTORY;
import static org.ehrbase.jooq.pg.Tables.FOLDER_HISTORY;
import static org.ehrbase.jooq.pg.Tables.FOLDER_ITEMS;
import static org.ehrbase.jooq.pg.Tables.FOLDER_ITEMS_HISTORY;
import static org.ehrbase.jooq.pg.Tables.OBJECT_REF;
import static org.ehrbase.jooq.pg.Tables.OBJECT_REF_HISTORY;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.dao.access.util.FolderUtils;
import org.ehrbase.jooq.binding.OtherDetailsJsonbBinder;
import org.ehrbase.jooq.binding.SysPeriodBinder;
import org.ehrbase.jooq.pg.tables.records.FolderHierarchyHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderItemsHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderRecord;
import org.ehrbase.jooq.pg.tables.records.ObjectRefHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.ObjectRefRecord;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Record12;
import org.jooq.Record17;
import org.jooq.Result;
import org.jooq.Table;

/**
 * Persistence operations on Folder history.
 *
 * @author Luis Marco-Ruiz
 * @since 1.0.0
 */
public class FolderHistoryAccess extends DataAccess implements I_FolderAccess, Comparable<FolderHistoryAccess> {

    private List<ObjectRef<? extends ObjectId>> items = new ArrayList<>();
    private Map<UUID, I_FolderAccess> subfoldersList = new TreeMap<>();
    private I_ContributionAccess contributionAccess;
    private UUID ehrId;
    private FolderRecord folderRecord;

    public FolderHistoryAccess(I_DomainAccess domainAccess, String tenantIdentifier) {
        super(domainAccess);
        this.folderRecord = getContext().newRecord(org.ehrbase.jooq.pg.tables.Folder.FOLDER);
        this.folderRecord.setNamespace(tenantIdentifier);

        // associate a contribution with this composition
        this.contributionAccess = I_ContributionAccess.getInstance(this, this.ehrId, tenantIdentifier);
        this.contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    }

    public FolderHistoryAccess(
            I_DomainAccess domainAccess, UUID ehrId, I_ContributionAccess contributionAccess, String tenantIdentifier) {
        super(domainAccess);
        this.ehrId = ehrId;
        this.folderRecord = getContext().newRecord(org.ehrbase.jooq.pg.tables.Folder.FOLDER);
        this.folderRecord.setNamespace(tenantIdentifier);
        this.contributionAccess = contributionAccess;
        // associate a contribution with this composition, if needed.
        if (contributionAccess == null) {
            this.contributionAccess = I_ContributionAccess.getInstance(this, this.ehrId, tenantIdentifier);
        }
        this.contributionAccess.getEhrId();
        this.contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    }

    public static boolean deleteFlatBy(I_DomainAccess domainAccess, UUID folderId, UUID contributionId) {
        FolderHistoryRecord folder = deleteFolderBy(domainAccess, folderId, contributionId);
        deleteFolderHierarchyBy(domainAccess, folderId, contributionId);
        Result<FolderItemsHistoryRecord> fih = deleteFolderItemBy(domainAccess, folder);
        fih.forEach(e -> deleteObjectRefBy(domainAccess, e));
        return true;
    }

    private static FolderHistoryRecord deleteFolderBy(I_DomainAccess domainAccess, UUID folderId, UUID contributionId) {
        FolderHistoryRecord folder = domainAccess
                .getContext()
                .select()
                .from(FOLDER_HISTORY)
                .where(FOLDER_HISTORY.ID.eq(folderId).and(FOLDER_HISTORY.IN_CONTRIBUTION.eq(contributionId)))
                .fetchOneInto(FOLDER_HISTORY);
        if (folder != null) folder.delete();
        return folder;
    }

    private static Result<FolderItemsHistoryRecord> deleteFolderItemBy(
            I_DomainAccess domainAccess, FolderHistoryRecord folder) {
        Result<FolderItemsHistoryRecord> folderItems = domainAccess
                .getContext()
                .select()
                .from(FOLDER_ITEMS_HISTORY)
                .where(FOLDER_ITEMS_HISTORY
                        .FOLDER_ID
                        .eq(folder.getId())
                        .and(FOLDER_ITEMS_HISTORY.IN_CONTRIBUTION.eq(folder.getInContribution())))
                .fetchInto(FOLDER_ITEMS_HISTORY);
        folderItems.forEach(fi -> {
            fi.delete();
        });
        return folderItems;
    }

    private static Result<FolderHierarchyHistoryRecord> deleteFolderHierarchyBy(
            I_DomainAccess domainAccess, UUID folderId, UUID contributionId) {
        Result<FolderHierarchyHistoryRecord> folderHierarchy = domainAccess
                .getContext()
                .select()
                .from(FOLDER_HIERARCHY_HISTORY)
                .where(FOLDER_HIERARCHY_HISTORY
                        .PARENT_FOLDER
                        .eq(folderId)
                        .and(FOLDER_HIERARCHY_HISTORY.IN_CONTRIBUTION.eq(contributionId)))
                .fetchInto(FOLDER_HIERARCHY_HISTORY);
        folderHierarchy.forEach(fh -> {
            fh.delete();
        });
        return folderHierarchy;
    }

    private static ObjectRefHistoryRecord deleteObjectRefBy(I_DomainAccess domainAccess, FolderItemsHistoryRecord fhh) {
        ObjectRefHistoryRecord objectRef = domainAccess
                .getContext()
                .select()
                .from(OBJECT_REF_HISTORY)
                .where(OBJECT_REF_HISTORY
                        .ID
                        .eq(fhh.getObjectRefId())
                        .and(OBJECT_REF_HISTORY.IN_CONTRIBUTION.eq(fhh.getInContribution())))
                .fetchOneInto(OBJECT_REF_HISTORY);
        objectRef.delete();
        return objectRef;
    }

    /*************Data Access and modification methods*****************/
    @Override
    public UUID commit(LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
        return null;
    }

    @Override
    public UUID commit(LocalDateTime timestamp, UUID contribution) {
        return null;
    }

    @Override
    public boolean update(
            LocalDateTime timestamp,
            UUID committerId,
            UUID systemId,
            String description,
            ContributionChangeType changeType) {
        return false;
    }

    @Override
    public boolean update(LocalDateTime timestamp, UUID contribution) {
        return false;
    }

    @Override
    public int delete(LocalDateTime timestamp, UUID committerId, UUID systemId, String description) {
        return 0;
    }

    @Override
    public int delete(LocalDateTime timestamp, UUID contribution) {
        return 0;
    }

    private static FolderHistoryAccess buildFolderAccessFromGenericRecord(
            final Record record_, final I_DomainAccess domainAccess) {

        Record17<
                        UUID,
                        UUID,
                        UUID,
                        Timestamp,
                        Object,
                        String,
                        UUID,
                        Timestamp,
                        UUID,
                        UUID,
                        String,
                        String,
                        Boolean,
                        JSONB,
                        Timestamp,
                        Object,
                        String>
                record = (Record17<
                                UUID,
                                UUID,
                                UUID,
                                Timestamp,
                                Object,
                                String,
                                UUID,
                                Timestamp,
                                UUID,
                                UUID,
                                String,
                                String,
                                Boolean,
                                JSONB,
                                Timestamp,
                                Object,
                                String>)
                        record_;

        FolderHistoryAccess folderAccess = new FolderHistoryAccess(domainAccess, record.value6());
        folderAccess.folderRecord = new FolderRecord();
        folderAccess.folderRecord.setNamespace(record.value6());

        folderAccess.setFolderId(record.value1());
        folderAccess.setInContribution(record.value3());
        folderAccess.setFolderName(record.value11());
        folderAccess.setFolderNArchetypeNodeId(record.value12());
        folderAccess.setIsFolderActive(record.value13());
        // Due to generic type from JOIN The ItemStructure binding does not cover the details
        // and we have to parse it manually
        folderAccess.setFolderDetails(new OtherDetailsJsonbBinder().converter().from(record.value14()));
        folderAccess.setFolderSysTransaction(record.value15());
        folderAccess.setFolderSysPeriod(new SysPeriodBinder().converter().from(record.value16()));
        folderAccess
                .getItems()
                .addAll(FolderHistoryAccess.retrieveItemsByFolderAndContributionId(
                        record.value1(), record.value3(), domainAccess));
        return folderAccess;
    }

    /**
     * Create a new FolderAccess from a {@link FolderRecord} DB record
     *
     * @param record_      containing the information of a {@link  Folder} in the DB.
     * @param domainAccess containing the DB connection information.
     * @return FolderAccess instance corresponding to the org.ehrbase.jooq.pg.tables.records.FolderRecord
     * provided.
     */
    private static FolderHistoryAccess buildFolderAccessFromFolderRecord(
            final FolderRecord record, final I_DomainAccess domainAccess) {

        String tenantId = record.getNamespace();

        FolderHistoryAccess folderAccess = new FolderHistoryAccess(domainAccess, tenantId);
        folderAccess.folderRecord = new FolderRecord();
        folderAccess.folderRecord.setNamespace(tenantId);

        folderAccess.setFolderId(record.getId());
        folderAccess.setInContribution(record.getInContribution());
        folderAccess.setFolderName(record.getName());
        folderAccess.setFolderNArchetypeNodeId(record.getArchetypeNodeId());
        folderAccess.setIsFolderActive(record.getActive());
        folderAccess.setFolderDetails(record.getDetails());
        folderAccess.setFolderSysTransaction(record.getSysTransaction());
        folderAccess.setFolderSysPeriod(record.getSysPeriod());
        folderAccess
                .getItems()
                .addAll(FolderHistoryAccess.retrieveItemsByFolderAndContributionId(
                        record.getId(), record.getInContribution(), domainAccess));
        return folderAccess;
    }

    /**
     * Given a UUID for a folder creates the corresponding FolderAccess from the information conveyed
     * by the {@link Result} provided. Alternatively queries the DB if the information needed is not
     * in {@link Result}. * @param id of the folder to define a {@link FolderHistoryAccess} from. *
     *
     * @param {@link Result} containing the Records that represent the rows to retrieve from the DB
     *               corresponding to the children hierarchy.
     * @return a FolderAccess corresponding to the Folder id provided
     */
    private static FolderHistoryAccess buildFolderAccessFromFolderId(
            final UUID id,
            final UUID contributionId,
            final I_DomainAccess domainAccess,
            final Result<Record> folderSelectedRecordSub) {

        for (Record record : folderSelectedRecordSub) {
            // if the FOLDER items were returned in the recursive query use them and avoid a DB transaction
            if (record.getValue("parent_folder").equals(id)) {

                return buildFolderAccessFromGenericRecord(record, domainAccess);
            }
        }

        // if no data from the Folder has been already recovered for the id of the folder, then query the DB for it.
        FolderRecord folderSelectedRecord = getFolderRecordByUidAndContribution(id, contributionId, domainAccess);

        if (folderSelectedRecord == null || folderSelectedRecord.size() < 1) {
            throw new ObjectNotFoundException("folder", "Folder with id " + id + " could not be found");
        }

        return buildFolderAccessFromFolderRecord(folderSelectedRecord, domainAccess);
    }

    /**
     * Makes the union of FOLDER and FOLDER_HISTORY table retrieving all the information for a folder
     * specified by its UUID and contribution UUID.
     *
     * @return
     */
    private static FolderRecord getFolderRecordByUidAndContribution(
            final UUID folderId, final UUID contributionId, final I_DomainAccess domainAccess) {
        Table<?> united_hierarchies_table1 = table(select().from(FOLDER)
                        .where(FOLDER.ID.eq(folderId).and(FOLDER.IN_CONTRIBUTION.eq(contributionId)))
                        .union(select().from(FOLDER_HISTORY)
                                .where(FOLDER_HISTORY
                                        .ID
                                        .eq(folderId)
                                        .and(FOLDER_HISTORY.IN_CONTRIBUTION.eq(contributionId)))))
                .asTable("folder_union_fol_hist");

        FolderRecord folderSelectedRecord = (FolderRecord)
                domainAccess.getContext().selectFrom(united_hierarchies_table1).fetchOne();

        return folderSelectedRecord;
    }

    /**
     * Retrieves a list containing the items as ObjectRefs of the folder corresponding to the id
     * provided.
     *
     * @param folderId        of the FOLDER that the items correspond to.
     * @param in_contribution contribution that establishes the reference between a FOLDER and its
     *                        item.
     * @param domainAccess    connection DB data.
     * @return
     */
    private static List<ObjectRef<?>> retrieveItemsByFolderAndContributionId(
            UUID folderId, UUID in_contribution, I_DomainAccess domainAccess) {

        Table<?> table_items_and_objref = table(select(
                        FOLDER_ITEMS.FOLDER_ID,
                        FOLDER_ITEMS.OBJECT_REF_ID.as("item_object_ref_id"),
                        FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"),
                        FOLDER_ITEMS.SYS_TRANSACTION,
                        FOLDER_ITEMS.SYS_PERIOD,
                        FOLDER_ITEMS.NAMESPACE,
                        OBJECT_REF.ID_NAMESPACE,
                        OBJECT_REF.TYPE,
                        OBJECT_REF.ID.as("obj_ref_id"),
                        OBJECT_REF.IN_CONTRIBUTION.as("obj_ref_in_cont"),
                        OBJECT_REF.SYS_TRANSACTION.as("objRefSysTran"),
                        OBJECT_REF.SYS_PERIOD.as("oref_sysperiod"))
                .from(FOLDER_ITEMS)
                .leftJoin(OBJECT_REF)
                .on(FOLDER_ITEMS
                        .FOLDER_ID
                        .eq(folderId)
                        .and(FOLDER_ITEMS
                                .IN_CONTRIBUTION
                                .eq(in_contribution)
                                .and(OBJECT_REF
                                        .ID
                                        .eq(FOLDER_ITEMS.OBJECT_REF_ID)
                                        .and(OBJECT_REF.IN_CONTRIBUTION.eq(FOLDER_ITEMS.IN_CONTRIBUTION)))))
                .where(FOLDER_ITEMS.FOLDER_ID.eq(folderId).and(FOLDER_ITEMS.IN_CONTRIBUTION.eq(in_contribution))));

        Table<?> table_items_and_objref_hist = table(select(
                        FOLDER_ITEMS_HISTORY.FOLDER_ID,
                        FOLDER_ITEMS_HISTORY.OBJECT_REF_ID.as("item_object_ref_id"),
                        FOLDER_ITEMS_HISTORY.IN_CONTRIBUTION.as("item_in_contribution"),
                        FOLDER_ITEMS_HISTORY.SYS_TRANSACTION,
                        FOLDER_ITEMS_HISTORY.SYS_PERIOD,
                        FOLDER_ITEMS_HISTORY.NAMESPACE,
                        OBJECT_REF_HISTORY.ID_NAMESPACE,
                        OBJECT_REF_HISTORY.TYPE,
                        OBJECT_REF_HISTORY.ID.as("obj_ref_id"),
                        OBJECT_REF_HISTORY.IN_CONTRIBUTION.as("obj_ref_in_cont"),
                        OBJECT_REF_HISTORY.SYS_TRANSACTION.as("objRefSysTran"),
                        OBJECT_REF_HISTORY.SYS_PERIOD.as("oref_sysperiod"))
                .from(FOLDER_ITEMS_HISTORY)
                .leftJoin(OBJECT_REF_HISTORY)
                .on(FOLDER_ITEMS_HISTORY
                        .FOLDER_ID
                        .eq(folderId)
                        .and(FOLDER_ITEMS_HISTORY
                                .IN_CONTRIBUTION
                                .eq(in_contribution)
                                .and(OBJECT_REF_HISTORY
                                        .ID
                                        .eq(FOLDER_ITEMS_HISTORY.OBJECT_REF_ID)
                                        .and(OBJECT_REF_HISTORY.IN_CONTRIBUTION.eq(
                                                FOLDER_ITEMS_HISTORY.IN_CONTRIBUTION)))))
                .where(FOLDER_ITEMS_HISTORY
                        .FOLDER_ID
                        .eq(folderId)
                        .and(FOLDER_ITEMS_HISTORY.IN_CONTRIBUTION.eq(in_contribution))));

        Table<?> table_all_items_and_objref =
                table(select().from(table_items_and_objref).union(select().from(table_items_and_objref_hist)));

        Result<Record> retrievedRecords = domainAccess
                .getContext()
                .select()
                .from(table_all_items_and_objref)
                .fetch();

        List<ObjectRef<?>> result = new ArrayList<>();
        for (Record recordRecord : retrievedRecords) {
            Record12<
                            UUID,
                            UUID,
                            UUID,
                            Timestamp,
                            Timestamp,
                            String,
                            String,
                            String,
                            UUID,
                            UUID,
                            Timestamp,
                            AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>
                    recordParam = (Record12<
                                    UUID,
                                    UUID,
                                    UUID,
                                    Timestamp,
                                    Timestamp,
                                    String,
                                    String,
                                    String,
                                    UUID,
                                    UUID,
                                    Timestamp,
                                    AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>>)
                            recordRecord;
            ObjectRefRecord objectRef = new ObjectRefRecord();
            objectRef.setIdNamespace(recordParam.value7());
            objectRef.setNamespace(recordParam.value6());
            objectRef.setType(recordParam.value8());
            objectRef.setId(recordParam.value9());
            objectRef.setInContribution(recordParam.value10());
            objectRef.setSysTransaction(recordParam.value11());
            objectRef.setSysPeriod(recordParam.value12());
            objectRef.setId(recordParam.value9());
            result.add(parseObjectRefRecordIntoObjectRef(objectRef, domainAccess));
        }
        return result;
    }

    /**
     * Transforms a ObjectRef DB record into a Reference Model object.
     *
     * @param objectRefRecord
     * @param domainAccess
     * @return the reference model object.
     */
    @SuppressWarnings("rawtypes")
    private static ObjectRef parseObjectRefRecordIntoObjectRef(
            ObjectRefRecord objectRefRecord, I_DomainAccess domainAccess) {
        ObjectRef result = new ObjectRef();

        ObjectRefId oref = new FolderHistoryAccess(domainAccess, objectRefRecord.getNamespace())
        .new ObjectRefId(objectRefRecord.getId().toString());

        result.setId(oref);
        result.setType(objectRefRecord.getType());
        result.setNamespace(objectRefRecord.getIdNamespace());
        return result;
    }

    /**
     * Returns the last version number of a given folder by counting all previous versions of a given
     * folder id. If there are no previous versions in the history table the version number will be 1.
     * Otherwise the current version equals the count of entries in the folder history table plus 1.
     *
     * @param domainAccess - Database connection access context
     * @param folderId     - UUID of the folder to check for the last version
     * @return Latest version number for the folder
     */
    public static Integer getLastVersionNumber(I_DomainAccess domainAccess, UUID folderId) {

        if (!hasPreviousVersion(domainAccess, folderId)) {
            return 1;
        }
        // Get number of entries as the history table of folders
        int versionCount = domainAccess.getContext().fetchCount(FOLDER_HISTORY, FOLDER_HISTORY.ID.eq(folderId));
        // Latest version will be entries plus actual entry count (always 1)
        return versionCount + 1;
    }

    /**
     * Checks if there are existing entries for given folder uuid at the folder history table. If
     * there are entries existing, the folder has been modified during previous actions and there are
     * older versions existing.
     *
     * @param domainAccess - Database connection access context
     * @param folderId     - UUID of folder to check
     * @return Folder has previous versions or not
     */
    public static boolean hasPreviousVersion(I_DomainAccess domainAccess, UUID folderId) {
        return domainAccess.getContext().fetchExists(FOLDER_HISTORY, FOLDER_HISTORY.ID.eq(folderId));
    }

    /****Getters and Setters for the FolderRecord to store****/
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

    public void setSubfoldersList(final Map<UUID, I_FolderAccess> subfolders) {
        this.subfoldersList = subfolders;
    }

    public Map<UUID, I_FolderAccess> getSubfoldersList() {
        return this.subfoldersList;
    }

    public List<ObjectRef<? extends ObjectId>> getItems() {
        return this.items;
    }

    public UUID getFolderId() {

        return this.folderRecord.getId();
    }

    public void setFolderId(UUID folderId) {

        this.folderRecord.setId(folderId);
    }

    public UUID getInContribution() {
        return this.folderRecord.getInContribution();
    }

    public void setInContribution(UUID inContribution) {

        this.folderRecord.setInContribution(inContribution);
    }

    public String getFolderName() {

        return this.folderRecord.getName();
    }

    public void setFolderName(String folderName) {

        this.folderRecord.setName(folderName);
    }

    public String getFolderArchetypeNodeId() {

        return this.folderRecord.getArchetypeNodeId();
    }

    public void setFolderNArchetypeNodeId(String folderArchetypeNodeId) {

        this.folderRecord.setArchetypeNodeId(folderArchetypeNodeId);
    }

    public boolean isFolderActive() {

        return this.folderRecord.getActive();
    }

    public void setIsFolderActive(boolean folderActive) {

        this.folderRecord.setActive(folderActive);
    }

    public ItemStructure getFolderDetails() {

        return this.folderRecord.getDetails();
    }

    public void setFolderDetails(ItemStructure folderDetails) {

        this.folderRecord.setDetails(folderDetails);
    }

    public void setFolderSysTransaction(Timestamp folderSysTransaction) {

        this.folderRecord.setSysTransaction(folderSysTransaction);
    }

    public Timestamp getFolderSysTransaction() {

        return this.folderRecord.getSysTransaction();
    }

    public AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> getFolderSysPeriod() {

        return this.folderRecord.getSysPeriod();
    }

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
    public int compareTo(final FolderHistoryAccess o) {
        return o.getFolderRecord().getId().compareTo(this.folderRecord.getId());
    }

    private class ObjectRefId extends ObjectId {

        public ObjectRefId(final String value) {
            super(value);
        }
    }

    /**
     * @throws IllegalArgumentException when no version in compliance with timestamp is available
     * @throws InternalServerException  on problem with SQL statement or input
     */
    public static int getVersionFromTimeStamp(I_DomainAccess domainAccess, UUID vFolderUid, Timestamp timeCommitted)
            throws InternalServerException {

        if (timeCommitted == null) {
            return getLastVersionNumber(domainAccess, vFolderUid);
        }
        // get the latest FOLDER time (available in ehr.folder) table
        Record result;
        try {
            result = domainAccess
                    .getContext()
                    .select(max(FOLDER.SYS_TRANSACTION).as("mostRecentInTable"))
                    .from(FOLDER)
                    .where(FOLDER.ID.eq(vFolderUid))
                    .fetchOne();
        } catch (RuntimeException e) { // generalize SQL exceptions
            throw new InternalServerException("Problem with SQL statement or input", e);
        }
        Timestamp latestCompoTime = (Timestamp) result.get("mostRecentInTable");

        // get the latest version (if more than one) time (available in ehr.FOLDER_history) table
        Record result2;
        try {
            result2 = domainAccess
                    .getContext()
                    .select(count().as("countVersionInTable"))
                    .from(FOLDER)
                    .where(FOLDER_HISTORY
                            .SYS_TRANSACTION
                            .lessOrEqual(timeCommitted)
                            .and(FOLDER_HISTORY.ID.eq(vFolderUid)))
                    .fetchOne();
        } catch (RuntimeException e) { // generalize SQL exceptions
            throw new InternalServerException("Problem with SQL statement or input", e);
        }
        int versionComHist = (int) result2.get("countVersionInTable");
        if (timeCommitted.compareTo(latestCompoTime)
                >= 0) { // if the timestamp is after or equal to the sys_transaction of the latest folder available, add
            // one since its version has not been counted for being the one stored in the ehr.folder table
            versionComHist++;
        }
        if (versionComHist == 0) {
            throw new ObjectNotFoundException(
                    "FOLDER VERSION",
                    "There are no versions available prior to date " + timeCommitted + " for the the FOLDER with id: "
                            + vFolderUid);
        }
        return versionComHist;
    }

    /**
     * Retrieves a table with all the information to rebuild the directory related to the folders that
     * have at least one child. If the folderUid provider corresponds to a leave folder then the
     * result will be empty.
     *
     * @param folderUid The top folder UID of the directory or subdirectory that will be retrieved as
     *                  a table.
     * @param ts        The timestamp which used to determine which version to retrieve. The method
     *                  will use the closest version available before or equal to the timestamp
     *                  provided.
     * @return A table with all the information related to the hierarchy joint to the information of
     * each of the folders that have some child.
     */
    private static Result<Record> buildUnionOfFolderHierarchiesTable(
            UUID folderUid, Timestamp ts, I_DomainAccess domainAccess) {

        // TODO: Quick fix for timestamp precision problems with Java and Postgres Timestamps
        // TODO: See this issue: https://github.com/ehrbase/ehrbase/issues/291
        Timestamp timestamp = Timestamp.from(ts.toInstant().plusMillis(1));

        Table<?> united_hierarchies_table1 = table(select().from(FOLDER_HIERARCHY)
                        .where(FOLDER_HIERARCHY.SYS_TRANSACTION.le(timestamp))
                        .union(select().from(FOLDER_HIERARCHY_HISTORY)
                                .where(FOLDER_HIERARCHY_HISTORY.SYS_TRANSACTION.le(timestamp))))
                .asTable("united_hierarchies_table1");

        Table<?> united_hierarchies_table2 = table(select().from(FOLDER_HIERARCHY)
                        .where(FOLDER_HIERARCHY.SYS_TRANSACTION.le(timestamp))
                        .union(select().from(FOLDER_HIERARCHY_HISTORY)
                                .where(FOLDER_HIERARCHY_HISTORY.SYS_TRANSACTION.le(timestamp))))
                .asTable("united_hierarchies_table2");

        Table<?> united_hierarchies_tableFileted = select().from(united_hierarchies_table1)
                .where(united_hierarchies_table1
                        .field("sys_transaction", FOLDER_HIERARCHY.SYS_TRANSACTION.getType())
                        .eq(select(max(united_hierarchies_table2.field(
                                        "sys_transaction", FOLDER_HIERARCHY.SYS_TRANSACTION.getType())))
                                .from(united_hierarchies_table2)
                                .where((united_hierarchies_table1.field(
                                                "parent_folder", FOLDER_HIERARCHY.PARENT_FOLDER.getType()))
                                        .eq(united_hierarchies_table2.field(
                                                "parent_folder", FOLDER_HIERARCHY.PARENT_FOLDER.getType()))
                                        .and(united_hierarchies_table1
                                                .field("child_folder", FOLDER_HIERARCHY.CHILD_FOLDER.getType())
                                                .eq(united_hierarchies_table2.field(
                                                        "child_folder", FOLDER_HIERARCHY.CHILD_FOLDER.getType()))))))
                .asTable("united_hierarchies_tableFileted");

        /*filter by the provided timestamp only the ones equal or older*/
        Table<?> fhf_timestamp1 = select().from(united_hierarchies_tableFileted).asTable();
        Table<?> fhf_timestamp2 = select().from(united_hierarchies_tableFileted).asTable();

        /*Retrieve for each partent folder the the latest transaction time so as to ignore previous versions*/
        Table<?> fhf_timestamp_version2 = select(
                        fhf_timestamp1
                                .field("parent_folder", FOLDER.ID.getType())
                                .as("parent_folder_id"),
                        max(fhf_timestamp2.field("sys_transaction", FOLDER.SYS_TRANSACTION.getType()))
                                .as("latest_sys_transaction"))
                .from(fhf_timestamp1)
                .groupBy(fhf_timestamp1.field("parent_folder", FOLDER.ID.getType()))
                .asTable();

        /*make the unified table with only the rows that correspond to the latest transactions*/
        Table<?> filteredHierarchicalTable = select().from(united_hierarchies_tableFileted, fhf_timestamp_version2)
                .where(united_hierarchies_tableFileted
                        .field("parent_folder", FOLDER.ID.getType())
                        .eq(fhf_timestamp_version2.field("parent_folder_id", FOLDER.ID.getType()))
                        .and(united_hierarchies_tableFileted
                                .field("sys_transaction", FOLDER.SYS_TRANSACTION.getType())
                                .eq(fhf_timestamp_version2.field(
                                        "latest_sys_transaction", FOLDER.SYS_TRANSACTION.getType()))))
                .asTable();

        Field<UUID> subfolderParentFolderRef = field(name("subfolders", "parent_folder"), UUID.class);

        Table<?> allFolderRowsFolderTable = domainAccess
                .getContext()
                .select(
                        FOLDER.ID,
                        FOLDER.IN_CONTRIBUTION,
                        FOLDER.NAME,
                        FOLDER.ARCHETYPE_NODE_ID,
                        FOLDER.ACTIVE,
                        FOLDER.DETAILS,
                        FOLDER.SYS_TRANSACTION,
                        FOLDER.SYS_PERIOD,
                        FOLDER.NAMESPACE)
                .from(FOLDER, filteredHierarchicalTable)
                .where(FOLDER.ID
                        .eq(filteredHierarchicalTable.field("parent_folder", UUID.class))
                        .and(FOLDER.IN_CONTRIBUTION.eq(filteredHierarchicalTable.field("in_contribution", UUID.class))))
                .asTable();

        Table<?> allFolderRowsFolderHistoryTable = domainAccess
                .getContext()
                .select(
                        FOLDER_HISTORY.ID,
                        FOLDER_HISTORY.IN_CONTRIBUTION,
                        FOLDER_HISTORY.NAME,
                        FOLDER_HISTORY.ARCHETYPE_NODE_ID,
                        FOLDER_HISTORY.ACTIVE,
                        FOLDER_HISTORY.DETAILS,
                        FOLDER_HISTORY.SYS_TRANSACTION,
                        FOLDER_HISTORY.SYS_PERIOD,
                        FOLDER_HISTORY.NAMESPACE)
                .from(FOLDER_HISTORY, filteredHierarchicalTable)
                .where(FOLDER_HISTORY
                        .ID
                        .eq(filteredHierarchicalTable.field("parent_folder", UUID.class))
                        .and(FOLDER_HISTORY.IN_CONTRIBUTION.eq(
                                filteredHierarchicalTable.field("in_contribution", UUID.class))))
                .asTable();

        Table<?> allFolderRowsUnifiedAndFilteredInitial = domainAccess
                .getContext()
                .select(
                        allFolderRowsFolderTable.field("id", UUID.class),
                        allFolderRowsFolderTable
                                .field("in_contribution", UUID.class)
                                .as("in_contribution_folder_info"),
                        allFolderRowsFolderTable.field("name", FOLDER.NAME.getType()),
                        allFolderRowsFolderTable.field("archetype_node_id", FOLDER.ARCHETYPE_NODE_ID.getType()),
                        allFolderRowsFolderTable.field("active", FOLDER.ACTIVE.getType()),
                        allFolderRowsFolderTable.field("details", FOLDER.DETAILS.getType()),
                        allFolderRowsFolderTable
                                .field("sys_transaction", FOLDER.SYS_TRANSACTION.getType())
                                .as("sys_transaction_folder"),
                        allFolderRowsFolderTable
                                .field("sys_period", FOLDER.SYS_PERIOD.getType())
                                .as("sys_period_folder"),
                        allFolderRowsFolderTable.field("namespace", FOLDER.NAME.getType()))
                .from(allFolderRowsFolderTable)
                .union(select(
                                allFolderRowsFolderHistoryTable.field("id", UUID.class),
                                allFolderRowsFolderHistoryTable
                                        .field("in_contribution", UUID.class)
                                        .as("in_contribution_folder_info"),
                                allFolderRowsFolderHistoryTable.field("name", FOLDER.NAME.getType()),
                                allFolderRowsFolderHistoryTable.field(
                                        "archetype_node_id", FOLDER.ARCHETYPE_NODE_ID.getType()),
                                allFolderRowsFolderHistoryTable.field("active", FOLDER.ACTIVE.getType()),
                                allFolderRowsFolderHistoryTable.field("details", FOLDER.DETAILS.getType()),
                                allFolderRowsFolderHistoryTable
                                        .field("sys_transaction", FOLDER.SYS_TRANSACTION.getType())
                                        .as("sys_transaction_folder"),
                                allFolderRowsFolderHistoryTable
                                        .field("sys_period", FOLDER.SYS_PERIOD.getType())
                                        .as("sys_period_folder"),
                                allFolderRowsFolderHistoryTable.field("namespace", FOLDER.NAMESPACE.getType()))
                        .from(allFolderRowsFolderHistoryTable))
                .asTable();

        Table<?> allFolderRowsUnifiedAndFilteredIterative = domainAccess
                .getContext()
                .select(
                        allFolderRowsFolderTable.field("id", UUID.class),
                        allFolderRowsFolderTable
                                .field("in_contribution", UUID.class)
                                .as("in_contribution_folder_info"),
                        allFolderRowsFolderTable.field("name", FOLDER.NAME.getType()),
                        allFolderRowsFolderTable.field("archetype_node_id", FOLDER.ARCHETYPE_NODE_ID.getType()),
                        allFolderRowsFolderTable.field("active", FOLDER.ACTIVE.getType()),
                        allFolderRowsFolderTable.field("details", FOLDER.DETAILS.getType()),
                        allFolderRowsFolderTable
                                .field("sys_transaction", FOLDER.SYS_TRANSACTION.getType())
                                .as("sys_transaction_folder"),
                        allFolderRowsFolderTable
                                .field("sys_period", FOLDER.SYS_PERIOD.getType())
                                .as("sys_period_folder"),
                        allFolderRowsFolderTable.field("namespace", FOLDER.NAMESPACE.getType()))
                .from(allFolderRowsFolderTable)
                .union(select(
                                allFolderRowsFolderHistoryTable.field("id", UUID.class),
                                allFolderRowsFolderHistoryTable
                                        .field("in_contribution", UUID.class)
                                        .as("in_contribution_folder_info"),
                                allFolderRowsFolderHistoryTable.field("name", FOLDER.NAME.getType()),
                                allFolderRowsFolderHistoryTable.field(
                                        "archetype_node_id", FOLDER.ARCHETYPE_NODE_ID.getType()),
                                allFolderRowsFolderHistoryTable.field("active", FOLDER.ACTIVE.getType()),
                                allFolderRowsFolderHistoryTable.field("details", FOLDER.DETAILS.getType()),
                                allFolderRowsFolderHistoryTable
                                        .field("sys_transaction", FOLDER.SYS_TRANSACTION.getType())
                                        .as("sys_transaction_folder"),
                                allFolderRowsFolderHistoryTable
                                        .field("sys_period", FOLDER.SYS_PERIOD.getType())
                                        .as("sys_period_folder"),
                                allFolderRowsFolderHistoryTable.field("namespace", FOLDER.NAMESPACE.getType()))
                        .from(allFolderRowsFolderHistoryTable))
                .asTable();

        Field<UUID> subfolderChildFolder = field(
                "subfolders.{0}",
                FOLDER_HIERARCHY.CHILD_FOLDER.getDataType(), FOLDER_HIERARCHY.CHILD_FOLDER.getUnqualifiedName());
        Field<Timestamp> subfolderSysTran = field(
                "\"subfolders\".\"sys_transaction\"",
                FOLDER_HIERARCHY.SYS_TRANSACTION.getDataType(),
                FOLDER_HIERARCHY.SYS_TRANSACTION.getUnqualifiedName());

        Table<?> initial_table2 = (select().from(filteredHierarchicalTable)
                        .leftJoin(allFolderRowsUnifiedAndFilteredInitial)
                        .on(filteredHierarchicalTable
                                .field("parent_folder", UUID.class)
                                .eq(allFolderRowsUnifiedAndFilteredInitial.field("id", UUID.class)))
                        .where(filteredHierarchicalTable
                                .field("parent_folder", UUID.class)
                                .eq(folderUid)))
                .asTable();

        return domainAccess
                .getContext()
                .withRecursive("subfolders")
                .as(select(initial_table2.fields())
                        .from(initial_table2)
                        .union((select(ArrayUtils.addAll(
                                                filteredHierarchicalTable.fields(),
                                                allFolderRowsUnifiedAndFilteredIterative.fields()))
                                        .from(filteredHierarchicalTable)
                                        .innerJoin("subfolders")
                                        .on(filteredHierarchicalTable
                                                .field("parent_folder", FOLDER_HIERARCHY.PARENT_FOLDER.getType())
                                                .eq(subfolderChildFolder)))
                                .leftJoin(allFolderRowsUnifiedAndFilteredIterative)
                                .on(allFolderRowsUnifiedAndFilteredIterative
                                        .field("id", FOLDER.ID.getType())
                                        .eq(subfolderChildFolder))))
                .select()
                .from(table(name("subfolders")))
                .fetch();
    }

    public static I_FolderAccess getInstanceForExistingFolder(
            I_DomainAccess domainAccess, ObjectVersionId folderId, Timestamp timestamp) {
        return FolderHistoryAccess.retrieveInstanceForExistingFolder(
                domainAccess, FolderUtils.extractUuidFromObjectVersionId(folderId), timestamp);
    }

    public static I_FolderAccess retrieveInstanceForExistingFolder(
            I_DomainAccess domainAccess, UUID folderId, Timestamp timestamp) {
        Result<Record> folderSelectedRecordSub = buildUnionOfFolderHierarchiesTable(folderId, timestamp, domainAccess);
        /**2-Reconstruct hierarchical structure from DB result**/
        Map<UUID, Map<UUID, I_FolderAccess>> fHierarchyMap = new TreeMap<UUID, Map<UUID, I_FolderAccess>>();
        for (Record record : folderSelectedRecordSub) {
            // 1-create a folder access for the record if needed
            if (!fHierarchyMap.containsKey((UUID) record.getValue("parent_folder"))) {
                fHierarchyMap.put((UUID) record.getValue("parent_folder"), new TreeMap<>());
            }
            fHierarchyMap
                    .get(record.getValue("parent_folder"))
                    .put(
                            (UUID) record.getValue("child_folder"),
                            buildFolderAccessFromFolderId(
                                    (UUID) record.getValue("child_folder"),
                                    (UUID) record.getValue("in_contribution"),
                                    domainAccess,
                                    folderSelectedRecordSub));
        }

        /**3-populate result and return**/
        return FolderHistoryAccess.buildFolderAccessHierarchy(
                fHierarchyMap, folderId, null, folderSelectedRecordSub, domainAccess);
    }

    private static I_FolderAccess buildFolderAccessHierarchy(
            final Map<UUID, Map<UUID, I_FolderAccess>> fHierarchyMap,
            final UUID currentFolder,
            final I_FolderAccess parentFa,
            final Result<Record> folderSelectedRecordSub,
            final I_DomainAccess domainAccess) {
        if ((parentFa != null) && (parentFa.getSubfoldersList().keySet().contains(currentFolder))) {
            return parentFa.getSubfoldersList().get(currentFolder);
        }

        UUID contributionUid = null;
        for (Record record : folderSelectedRecordSub) {
            if (((UUID) record.getValue("parent_folder")).equals(currentFolder)) {
                contributionUid = (UUID) record.getValue("in_contribution");
            } else if (((UUID) record.getValue("child_folder")).equals(currentFolder)) {
                contributionUid = (UUID) record.getValue("in_contribution");
            }
        }
        I_FolderAccess folderAccess =
                buildFolderAccessFromFolderId(currentFolder, contributionUid, domainAccess, folderSelectedRecordSub);
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

    @Override
    public void adminDeleteFolder() {
        // needed because the interface declares it, but the ACTUAL admin delete handling is done at non-history level
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
