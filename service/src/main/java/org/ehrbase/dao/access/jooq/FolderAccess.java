/*
 * Copyright (c) 2019 Vitasystems GmbH, Hannover Medical School, and Luis Marco-Ruiz (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_ContributionAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.FolderHierarchy;
import org.ehrbase.jooq.pg.tables.records.FolderHierarchyRecord;
import org.ehrbase.jooq.pg.tables.records.FolderItemsRecord;
import org.ehrbase.jooq.pg.tables.records.FolderRecord;
import org.ehrbase.jooq.pg.tables.records.ObjectRefRecord;
import org.ehrbase.serialisation.CanonicalJson;
import org.joda.time.DateTime;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static org.ehrbase.jooq.pg.Tables.*;
import static org.jooq.impl.DSL.*;

/***
 *@Created by Luis Marco-Ruiz on Jun 13, 2019
 */
public class FolderAccess extends DataAccess implements I_FolderAccess, Comparable<FolderAccess> {

    private static final Logger log = LogManager.getLogger(FolderAccess.class);

    private  ItemStructure details;
    private   List<ObjectRef> items = new ArrayList<>();
    private Map<UUID, I_FolderAccess> subfoldersList = new TreeMap<UUID, I_FolderAccess>();
    private List<I_FolderAccess> subFoldersInsertList = new ArrayList<>();
    private I_ContributionAccess contributionAccess;
    private UUID ehrId;
    private FolderRecord folderRecord;

    /********Constructors*******/

    public FolderAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
        this.folderRecord = getContext().newRecord(org.ehrbase.jooq.pg.tables.Folder.FOLDER);

        //associate a contribution with this composition
        this.contributionAccess = I_ContributionAccess.getInstance(this, this.ehrId);
        this.contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    }

    public FolderAccess(I_DomainAccess domainAccess, UUID ehrId, I_ContributionAccess contributionAccess) {
        super(domainAccess);
        this.ehrId=ehrId;
        this.folderRecord = getContext().newRecord(org.ehrbase.jooq.pg.tables.Folder.FOLDER);
        this.contributionAccess = contributionAccess;
        //associate a contribution with this composition, if needed.
        if(contributionAccess == null){
            this.contributionAccess = I_ContributionAccess.getInstance(this, this.ehrId);
        }
        UUID ehrIdLoc = this.contributionAccess.getEhrId();
        this.contributionAccess.setState(ContributionDef.ContributionState.COMPLETE);
    }

    /*************Data Access and modification methods*****************/

    @Override
    public Boolean update(Timestamp transactionTime) {
        return this.update(transactionTime, true);
    }

    @Override
    public Boolean update(final Timestamp transactionTime, final boolean force){
        /*create new contribution*/
        UUID old_contribution = this.folderRecord.getInContribution();
        UUID new_contribution = this.folderRecord.getInContribution();

        UUID ehrId =this.contributionAccess.getEhrId();
        /*save the EHR id from old_contribution since it will be the same as this is an update operation*/
        if(this.contributionAccess.getEhrId() == null){
            final Record1<UUID>  result1= getContext().select(CONTRIBUTION.EHR_ID).from(CONTRIBUTION).where(CONTRIBUTION.ID.eq(old_contribution)).fetch().get(0);
            ehrId = result1.value1();
        }
        this.contributionAccess.setEhrId(ehrId);

        this.contributionAccess.commit(transactionTime, null, null, ContributionDataType.folder, ContributionDef.ContributionState.COMPLETE, I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
        this.getFolderRecord().setInContribution(this.contributionAccess.getId());
        new_contribution=folderRecord.getInContribution();

        //delete so folder can be overwritten
        this.delete(folderRecord.getId());

        return this.update(transactionTime, true, true, null,old_contribution, new_contribution);
    }

    private Boolean update(final Timestamp transactionTime, final boolean force, final boolean topFolder, UUID parentFolder,UUID oldContribution, UUID newContribution){

        boolean result = false;

        DSLContext dslContext =  getContext();
        dslContext.attach(this.folderRecord);

        folderRecord.setInContribution(newContribution);

        /*update items*/
        this.saveFolderItems(oldContribution, newContribution, transactionTime, getContext());

        /*copy into new instance and attach to DB context*/
        FolderRecord updatedFolderrecord = new FolderRecord();
        UUID updatedRecordId =  folderRecord.getId();
        if(folderRecord.getId()==null) {
            updatedRecordId=UUID.randomUUID();
        }
        updatedFolderrecord.setId(updatedRecordId);
        updatedFolderrecord.setInContribution(newContribution);
        updatedFolderrecord.setName(this.getFolderName());
        updatedFolderrecord.setArchetypeNodeId(this.getFolderArchetypeNodeId());
        updatedFolderrecord.setActive(this.isFolderActive());
        updatedFolderrecord.setDetails(folderRecord.getDetails());
        updatedFolderrecord.setSysTransaction(transactionTime);
        updatedFolderrecord.setSysPeriod(PGObjectParser.parseSysPeriod(folderRecord.getSysPeriod()));


        /*attach to context DB*/
        dslContext.attach(updatedFolderrecord);

        /*store new instance*/
        result = updatedFolderrecord.store() > 0;
        //Folder hierarchy structure
        if(parentFolder!=null) {
            FolderHierarchyRecord updatedFhR = new FolderHierarchyRecord();
            updatedFhR.setParentFolder(parentFolder);
            updatedFhR.setChildFolder(updatedRecordId);
            updatedFhR.setInContribution(newContribution);
            updatedFhR.setSysTransaction(transactionTime);
            updatedFhR.setSysPeriod(PGObjectParser.parseSysPeriod(folderRecord.getSysPeriod()));
            dslContext.attach(updatedFhR);
            updatedFhR.store();
        }

        boolean anySubfolderModified = this.getSubfoldersList()
                .values()
                .stream()
                .allMatch(subfolder -> (
                        ((FolderAccess) subfolder).update(transactionTime, force, false, folderRecord.getId(), oldContribution, newContribution)
                ));

        return result || anySubfolderModified;
    }

    private void saveFolderItems(final UUID old_contribution, final UUID new_contribution, final Timestamp transactionTime, DSLContext context){

        //delete folder items fot the corresponding folder and contribution, the current items will override the previous one for this folder_id and conmtribution_id, those from other folder or contribution wont be affected.
        context.deleteFrom(FOLDER_ITEMS).where(FOLDER_ITEMS.FOLDER_ID.eq(this.getFolderId())).and(FOLDER_ITEMS.IN_CONTRIBUTION.eq(old_contribution)).execute();

        for(ObjectRef or : this.getItems()){

            //insert in object_ref
            ObjectRefRecord orr = new ObjectRefRecord(or.getNamespace(), or.getType(),UUID.fromString( or.getId().getValue()), new_contribution, transactionTime, PGObjectParser.parseSysPeriod(folderRecord.getSysPeriod()));
            context.attach(orr);
            orr.store();

            //insert in folder_item
            FolderItemsRecord fir = new FolderItemsRecord(this.getFolderId(), UUID.fromString(or.getId().getValue()), new_contribution, transactionTime, PGObjectParser.parseSysPeriod(folderRecord.getSysPeriod()));
            context.attach(fir);
            fir.store();
        }
    }

    @Override
    public Boolean update() {
        return  this.update(new Timestamp(DateTime.now().getMillis()), true);
    }

    @Override
    public Boolean update(Boolean force){
        return  this.update(new Timestamp(DateTime.now().getMillis()), force);
    }

    @Override
    public Integer delete(){
        return this.delete(this.getFolderId());
    }

    @Override
    public UUID commit(Timestamp transactionTime){
        // Create Contribution entry for all folders
        this.contributionAccess.commit(
                transactionTime,
                null,
                null,
                ContributionDataType.folder,
                ContributionDef.ContributionState.COMPLETE,
                I_ConceptAccess.ContributionChangeType.CREATION,
                null
        );
        this.getFolderRecord().setInContribution(this.contributionAccess.getId());

        // Save the folder record to database
        this.getFolderRecord().store();

        //Save folder items
        this.saveFolderItems(this.contributionAccess.getContributionId(), this.contributionAccess.getContributionId(), new Timestamp(DateTime.now().getMillis()), getContext());

        // Save list of sub folders to database with parent <-> child ID relations
        this.getSubfoldersList().forEach((child_id, child) -> {
            child.commit();
            FolderHierarchyRecord fhRecord = this.buildFolderHierarchyRecord(
                    this.getFolderRecord().getId(),
                    ((FolderAccess)child).getFolderRecord().getId(),
                    this.contributionAccess.getId(),
                    new Timestamp(DateTime.now().getMillis()),
                    null
            );
            fhRecord.store();
        });
        return this.getFolderRecord().getId();
    }

    @Override
    public UUID commit(){
        Timestamp timestamp = new Timestamp(DateTime.now().getMillis());
        return this.commit(timestamp);
    }

    /**
     * Retrieve instance of {@link I_FolderAccess} with the information needed retrieve the folder and its sub-folders.
     * @param domainAccess providing the information about the DB connection.
     * @param folderId {@link java.util.UUID} of the {@link  com.nedap.archie.rm.directory.Folder} to be fetched from the DB.
     * @return the {@link I_FolderAccess} that provides DB access to the {@link  com.nedap.archie.rm.directory.Folder} that corresponds to the provided folderId param.
     * @throws Exception
     */
    public static I_FolderAccess retrieveInstanceForExistingFolder(I_DomainAccess domainAccess, UUID folderId){

        /***1-retrieve CTE as a table that contains all the rows that allow to infer each parent-child relationship***/
        FolderHierarchy sf =  FOLDER_HIERARCHY.as("sf");

        Table<?> sf_table = table(
                select()
                        .from(FOLDER_HIERARCHY));

        Table<?> folder_table = table(
                select()
                        .from(FOLDER)).as("t_folder1");
        Table<?> folder_table2 = table(
                select()
                        .from(FOLDER)).as("t_folder2");

        Table<?> initial_table = table(
                select()
                        .from(FOLDER_HIERARCHY)
                        .where(
                                FOLDER_HIERARCHY.PARENT_FOLDER.eq(folderId)));

        Field<UUID> subfolderChildFolder = field("subfolders.{0}", FOLDER_HIERARCHY.CHILD_FOLDER.getDataType(), FOLDER_HIERARCHY.CHILD_FOLDER.getUnqualifiedName());
        Field<UUID> subfolderParentFolderRef = field(name("subfolders", "parent_folder"), UUID.class);
        Result<Record> folderSelectedRecordSub = domainAccess.getContext().withRecursive("subfolders").as(
                select().
                        from(initial_table).
                        leftJoin(folder_table).on(initial_table.field("parent_folder", FOLDER_HIERARCHY.PARENT_FOLDER.getType()).eq(
                        folder_table.field("id", FOLDER.ID.getType()))).
                        union(
                                (select().from(sf_table).
                                        innerJoin("subfolders").on(sf_table.field("parent_folder", FOLDER_HIERARCHY.PARENT_FOLDER.getType()).
                                        eq(subfolderChildFolder))).leftJoin(folder_table2).on(
                                        folder_table2.field("id", FOLDER.ID.getType()).eq(subfolderChildFolder)))
        ).select().from(table(name("subfolders"))).fetch();

        /**2-Reconstruct hierarchical structure from DB result**/
        Map<UUID, Map<UUID, I_FolderAccess>> fHierarchyMap = new TreeMap<UUID, Map<UUID, I_FolderAccess>>();
        for(Record record : folderSelectedRecordSub){

            //1-create a folder access for the record if needed
            if(!fHierarchyMap.containsKey((UUID) record.getValue("parent_folder"))){
                fHierarchyMap.put((UUID) record.getValue("parent_folder"), new TreeMap<>());
            }
            fHierarchyMap.get(record.getValue("parent_folder")).put((UUID) record.getValue("child_folder"), buildFolderAccessFromFolderId((UUID)record.getValue("child_folder"), domainAccess, folderSelectedRecordSub));
        }

        /**3-populate result and return**/
        return FolderAccess.buildFolderAccessHierarchy(fHierarchyMap, folderId, null, folderSelectedRecordSub, domainAccess);
    }

    /**
     * Builds the {@link I_FolderAccess} for persisting the {@link  com.nedap.archie.rm.directory.Folder} provided as param.
     * @param domainAccess providing the information about the DB connection.
     * @param folder to define the {@link I_FolderAccess} that allows its DB access.
     * @param dateTime that will be set as transaction date when the {@link  com.nedap.archie.rm.directory.Folder} is persisted
     * @param ehrId of the {@link com.nedap.archie.rm.ehr.Ehr} that references the {@link  com.nedap.archie.rm.directory.Folder} provided as param.
     * @return {@link I_FolderAccess} with the information to persist the provided {@link  com.nedap.archie.rm.directory.Folder}
     */
    public static I_FolderAccess getNewFolderAccessInstance(final  I_DomainAccess domainAccess, final  Folder folder, final  DateTime dateTime, final  UUID ehrId){
        return buildFolderAccessTreeRecursively(domainAccess, folder, null, dateTime, ehrId, null);
    }

    /**
     * Deletes the FOLDER identified with the Folder.id provided and all its subfolders recursively.
     * @param folderId of the {@link  com.nedap.archie.rm.directory.Folder} to delete.
     * @return number of the total {@link  com.nedap.archie.rm.directory.Folder} deleted recursively.
     */
    private Integer delete(final UUID folderId){

        if(folderId==null){
            throw new IllegalArgumentException("The folder UID provided for performing a delete operation cannot be null.");
        }

        /**SQL code for the recursive call generated inside the delete that retrieves children iteratively.
         * WITH RECURSIVE subfolders AS (
         * 		SELECT parent_folder, child_folder, in_contribution, sys_transaction
         * 		FROM ehr.folder_hierarchy
         * 		WHERE parent_folder = '00550555-ec91-4025-838d-09ddb4e999cb'
         * 	UNION
         * 		SELECT sf.parent_folder, sf.child_folder, sf.in_contribution, sf.sys_transaction
         * 		FROM ehr.folder_hierarchy sf
         * 		INNER JOIN subfolders s ON sf.parent_folder=s.child_folder
         * ) SELECT * FROM subfolders
         */
        int result;

        Table<?> sf_table = table(
                select()
                        .from(FOLDER_HIERARCHY));

        Table<?> initial_table = table(
                select()
                        .from(FOLDER_HIERARCHY)
                        .where(
                                FOLDER_HIERARCHY.PARENT_FOLDER.eq(folderId)));

        Field<UUID> subfolderChildFolder = field("subfolders.{0}", FOLDER_HIERARCHY.CHILD_FOLDER.getDataType(), FOLDER_HIERARCHY.CHILD_FOLDER.getUnqualifiedName());

        result = this.getContext().delete(FOLDER).where(FOLDER.ID.in(this.getContext().withRecursive("subfolders").as(
                select().
                        from(initial_table).
                        union(
                                (select().from(sf_table).
                                        innerJoin("subfolders").on(sf_table.field("parent_folder", FOLDER_HIERARCHY.PARENT_FOLDER.getType()).
                                        eq(subfolderChildFolder))))
                ).select()
                        .from(table(name("subfolders")))
                        .fetch()
                        .getValues(field(name("child_folder")))
        ))
                .or(FOLDER.ID.eq(folderId))
                .execute();

        return result;
    }


    /**
     * Create a new FolderAccess that contains the full hierarchy of its corresponding {@link I_FolderAccess} children that represents the subfolders.
     * @param fHierarchyMap {@link java.util.Map} containing as key the UUID of each Folder, and as value an internal Map. For the internal Map the key is the the UUID of a child {@link  com.nedap.archie.rm.directory.Folder}, and the value is the {@link I_FolderAccess} for enabling DB access to this child.
     * @param currentFolder {@link java.util.UUID} of the current {@link  com.nedap.archie.rm.directory.Folder} to treat in the current recursive call of the method.
     * @param parentFa the parent {@link I_FolderAccess} that corresponds to the parent  {@link  com.nedap.archie.rm.directory.Folder} of the {@link  com.nedap.archie.rm.directory.Folder} identified as current.
     * @param folderSelectedRecordSub {@link org.jooq.Result} containing the Records that represent the rows to retrieve from the DB corresponding to the children hierarchy.
     * @param domainAccess containing the information of the DB connection.
     * @return I_FolderAccess populated with its appropriate subfolders as FolderAccess objects.
     * @throws Exception
     */
    private static I_FolderAccess buildFolderAccessHierarchy(final Map<UUID, Map<UUID, I_FolderAccess>> fHierarchyMap, final UUID currentFolder, final I_FolderAccess parentFa, final Result<Record> folderSelectedRecordSub, final I_DomainAccess domainAccess){
        if ((parentFa != null) && (parentFa.getSubfoldersList().keySet().contains(currentFolder))){
            return parentFa.getSubfoldersList().get(currentFolder);
        }
        I_FolderAccess folderAccess = buildFolderAccessFromFolderId(currentFolder, domainAccess, folderSelectedRecordSub);
        if (parentFa != null) {
            parentFa.getSubfoldersList().put(currentFolder, folderAccess);
        }
        if (fHierarchyMap.get(currentFolder) != null) {//if not leave node call children

            for (UUID newChild : fHierarchyMap.get(currentFolder).keySet()) {
                buildFolderAccessHierarchy(fHierarchyMap, newChild, folderAccess, folderSelectedRecordSub, domainAccess);
            }
        }
        return folderAccess;
    }

    /**
     * Create a new {@link FolderAccess} from a {@link org.jooq.Record} DB record
     * @param record_  record containing all the information to build one folder-subfolder relationship.
     * @param domainAccess containing the DB connection information.
     * @return FolderAccess instance
     */
    private static FolderAccess buildFolderAccessFromGenericRecord(final Record record_, final I_DomainAccess domainAccess){

        Record13<UUID, UUID, UUID, Timestamp, Object, UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, Timestamp> record = (Record13<UUID, UUID, UUID, Timestamp, Object, UUID, UUID, String, String, Boolean, ItemStructure, Timestamp, Timestamp>)record_;
        FolderAccess folderAccess = new FolderAccess(domainAccess);
        folderAccess.folderRecord = new FolderRecord();
        folderAccess.folderRecord.setId(record.value1());
        folderAccess.folderRecord.setInContribution(record.value7());
        folderAccess.folderRecord.setName(record.value8());
        folderAccess.folderRecord.setArchetypeNodeId(record.value9());
        folderAccess.folderRecord.setActive(record.value10());
        folderAccess.folderRecord.setDetails(record.value11());
        folderAccess.folderRecord.setSysTransaction(record.value12());
        folderAccess.folderRecord.setSysPeriod(record.value13());
        folderAccess.getItems().addAll(FolderAccess.retrieveItemsByFolderAndContributionId(record.value1(), record.value7(), domainAccess));

        return folderAccess;
    }

    /**
     * Create a new FolderAccess from a {@link FolderRecord} DB record
     * @param record_ containing the information of a {@link  com.nedap.archie.rm.directory.Folder} in the DB.
     * @param domainAccess containing the DB connection information.
     * @return FolderAccess instance corresponding to the org.ehrbase.jooq.pg.tables.records.FolderRecord provided.
     */
    private static FolderAccess buildFolderAccessFromFolderRecord(final FolderRecord record_, final I_DomainAccess domainAccess){
        // FIXME FOLDER_DETAILS: so does this methods really need to set the access or records vars? both now for testing
        FolderRecord record = record_;
        FolderAccess folderAccess = new FolderAccess(domainAccess);
        folderAccess.folderRecord = new FolderRecord();
        folderAccess.folderRecord.setId(record.getId());
        folderAccess.setFolderId(record.getId());
        folderAccess.folderRecord.setInContribution(record.getInContribution());
        folderAccess.setInContribution(record.getInContribution());
        folderAccess.folderRecord.setName(record.getName());
        folderAccess.setFolderName(record.getName());
        folderAccess.folderRecord.setArchetypeNodeId(record.getArchetypeNodeId());
        folderAccess.setFolderNArchetypeNodeId(record.getArchetypeNodeId());
        folderAccess.folderRecord.setActive(record.getActive());
        folderAccess.setIsFolderActive(record.getActive());
        folderAccess.folderRecord.setDetails(record.getDetails());
        folderAccess.setDetails(record.getDetails());
        folderAccess.folderRecord.setSysTransaction(record.getSysTransaction());
        folderAccess.setFolderSysTransaction(record.getSysTransaction());
        folderAccess.folderRecord.setSysPeriod(record.getSysPeriod());
        folderAccess.setFolderSysPeriod(record.getSysPeriod());
        folderAccess.getItems().addAll(FolderAccess.retrieveItemsByFolderAndContributionId(record.getId(), record.getInContribution(), domainAccess));
        return folderAccess;
    }

    /**
     * Given a UUID for a folder creates the corresponding FolderAccess from the information conveyed by the {@link org.jooq.Result} provided. Alternatively queries the DB if the information needed is not in {@link org.jooq.Result}.
     * * @param id of the folder to define a {@link FolderAccess} from.
     * * @param {@link org.jooq.Result} containing the Records that represent the rows to retrieve from the DB corresponding to the children hierarchy.
     * @return a FolderAccess corresponding to the Folder id provided
     */
    private static FolderAccess buildFolderAccessFromFolderId(final UUID id, final I_DomainAccess domainAccess, final Result<Record> folderSelectedRecordSub){

        for(Record record : folderSelectedRecordSub){
            //if the FOLDER items were returned in the recursive query use them and avoid a DB transaction
            if(record.getValue("parent_folder").equals(id)){

                FolderAccess fa = buildFolderAccessFromGenericRecord(record, domainAccess);
                return fa;
            }
        }

        //if no data from the Folder has been already recovered for the id of the folder, then query the DB for it.
        FolderRecord folderSelectedRecord = domainAccess.getContext().selectFrom(FOLDER).where(FOLDER.ID.eq(id)).fetchOne();

        if (folderSelectedRecord == null || folderSelectedRecord.size() < 1) {
            throw new ObjectNotFoundException(
                    "folder", "Folder with id " + id + " could not be found"
            );
        }


        FolderAccess fa = buildFolderAccessFromFolderRecord(folderSelectedRecord, domainAccess);
        return fa;

    }

    /**
     * Builds the FolderAccess with the collection of subfolders empty.
     * @param domainAccess providing the information about the DB connection.
     * @param folder to define a corresponding {@link I_FolderAccess} for allowing its persistence.
     * @param dateTime that will be set as transaction date when the {@link  com.nedap.archie.rm.directory.Folder} is persisted
     * @param ehrId of the {@link com.nedap.archie.rm.ehr.Ehr} that references this {@link  com.nedap.archie.rm.directory.Folder}
     * @return {@link I_FolderAccess} with the information to persist the provided {@link  com.nedap.archie.rm.directory.Folder}
     */
    public static I_FolderAccess buildPlainFolderAccess(final  I_DomainAccess domainAccess, final Folder folder, final  DateTime dateTime, final  UUID ehrId, final I_ContributionAccess contributionAccess){

        FolderAccess folderAccessInstance = new FolderAccess(domainAccess, ehrId, contributionAccess);
        folderAccessInstance.setEhrId(ehrId);
        // In case of creation we have no folderId since it will be created from DB
        if (folder.getUid() != null) {
            folderAccessInstance.getFolderRecord().setId(UUID.fromString(folder.getUid().getValue()));
        }
        folderAccessInstance.getFolderRecord().setInContribution(folderAccessInstance.getContributionAccess().getId());
        folderAccessInstance.getFolderRecord().setName(folder.getName().getValue());
        folderAccessInstance.getFolderRecord().setArchetypeNodeId(folder.getArchetypeNodeId());
        folderAccessInstance.getFolderRecord().setActive(true);

        PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        try {
            jsonObject.setValue("{\"s\": \"s\"}");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //pstmt.setObject(11, jsonObject);

        if (folder.getDetails() != null) {
            folderAccessInstance.getFolderRecord().setDetails(folder.getDetails());
        }

        if(!folder.getItems().isEmpty()){
            folderAccessInstance.getItems().addAll(folder.getItems());
        }

        folderAccessInstance.getFolderRecord().setSysTransaction(new Timestamp(DateTime.now().getMillis()));
        return folderAccessInstance;
    }

    /**
     * Retrieves a list containing the items as ObjectRefs of the folder corresponding to the id provided.
     * @param folderId of the FOLDER that the items correspond to.
     * @param in_contribution contribution that establishes the reference between a FOLDER and its item.
     * @param domainAccess connection DB data.
     * @return
     */
    private static List<ObjectRef> retrieveItemsByFolderAndContributionId(UUID folderId, UUID in_contribution, I_DomainAccess domainAccess){
        Result<Record> retrievedRecords = domainAccess.getContext().with("folderItemsSelect").as(
                select(FOLDER_ITEMS.OBJECT_REF_ID.as("object_ref_id"), FOLDER_ITEMS.IN_CONTRIBUTION.as("item_in_contribution"))
                        .from(FOLDER_ITEMS)
                        .where(FOLDER_ITEMS.FOLDER_ID.eq(folderId)))
                .select()
                .from(OBJECT_REF, table(name("folderItemsSelect")))

                .where(field(name("object_ref_id"), FOLDER_ITEMS.OBJECT_REF_ID.getType()).eq(OBJECT_REF.ID)
                        .and(field(name("item_in_contribution"), FOLDER_ITEMS.IN_CONTRIBUTION.getType()).eq(OBJECT_REF.IN_CONTRIBUTION))).fetch();


        List<ObjectRef> result = new ArrayList<>();
        for(Record recordRecord : retrievedRecords){
            Record8<String, String, UUID, UUID, Timestamp, Object, UUID, UUID>  recordParam =  (Record8<String, String, UUID, UUID, Timestamp, Object, UUID, UUID>) recordRecord;
            ObjectRefRecord objectRef = new ObjectRefRecord();
            objectRef.setIdNamespace(recordParam.value1());
            objectRef.setType(recordParam.value2());
            objectRef.setId(recordParam.value3());
            objectRef.setInContribution(recordParam.value4());
            objectRef.setSysTransaction(recordParam.value5());
            objectRef.setSysPeriod(recordParam.value6());
            objectRef.setId(recordParam.value7());
            result.add(parseObjectRefRecordIntoObjectRef(objectRef, domainAccess));
        }
        return result;
    }

    /**
     * Transforms a ObjectRef DB record into a Reference Model object.
     * @param objectRefRecord
     * @param domainAccess
     * @return the reference model object.
     */
    private static  ObjectRef parseObjectRefRecordIntoObjectRef(ObjectRefRecord objectRefRecord, I_DomainAccess domainAccess){
        ObjectRef result = new ObjectRef();
        ObjectRefId oref = new FolderAccess(domainAccess).new ObjectRefId(objectRefRecord.getId().toString());
        result.setId(oref);
        result.setType(objectRefRecord.getType());
        result.setNamespace(objectRefRecord.getIdNamespace());
        return result;
    }


    /**
     * Recursive method for populating the hierarchy of {@link I_FolderAccess}  for a given {@link  com.nedap.archie.rm.directory.Folder}.
     * @param domainAccess providing the information about the DB connection.
     * @param current {@link  com.nedap.archie.rm.directory.Folder} explored in the current iteration.
     * @param parent folder of the {@link  com.nedap.archie.rm.directory.Folder} procided as the current parameter.
     * @param dateTime of the transaction that will be stored inthe DB.
     * @param ehrId of the {@link com.nedap.archie.rm.ehr.Ehr} referencing the current {@link  com.nedap.archie.rm.directory.Folder}.
     * @param contributionAccess that corresponds to the contribution that the {@link  com.nedap.archie.rm.directory.Folder} refers to.
     * @return {@link I_FolderAccess} with the complete hierarchy of sub-folders represented as {@link I_FolderAccess}.
     * @throws Exception
     */
    private static I_FolderAccess buildFolderAccessTreeRecursively(final  I_DomainAccess domainAccess, final Folder current, final FolderAccess parent, final  DateTime dateTime, final  UUID ehrId, final I_ContributionAccess contributionAccess) {
        I_FolderAccess folderAccess = null;

        //if the parent already contains the FolderAccess for the specified folder return the corresponding FolderAccess
        if((parent!= null) && (parent.getSubfoldersList().containsKey(UUID.fromString(current.getUid().getValue())))){
            return parent.getSubfoldersList().get(current.getUid());
        }
        //create the corresponding FolderAccess for the current folder
        folderAccess = FolderAccess.buildPlainFolderAccess(domainAccess, current, DateTime.now(), ehrId, contributionAccess);
        //add to parent subfolder list
        if(parent!= null){
            parent.getSubfoldersList().put(((FolderAccess)folderAccess).getFolderRecord().getId(), folderAccess);
        }
        for(Folder child : current.getFolders()){
            buildFolderAccessTreeRecursively(domainAccess, child, (FolderAccess) folderAccess, dateTime, ehrId, ((FolderAccess) folderAccess).getContributionAccess());
        }
        return folderAccess;
    }

    /**
     * Recursively build the FolderAccess instances for all new folders which
     * should be inserted. These instances have no UUID set by the application
     * to leave this completely to the database functions for setting new IDs.
     *
     * @param domainAccess - DB connection context
     * @param folder - Folder to create access for
     * @param timeStamp - Current time for transaction audit
     * @param ehrId - Corresponding EHR
     * @param contributionAccess - Contribution instance for creation of all folders
     * @return FolderAccess instance for folder
     */
    public static I_FolderAccess buildFolderAccessForInsert(
            final I_DomainAccess domainAccess,
            final Folder folder,
            final DateTime timeStamp,
            final UUID ehrId,
            final I_ContributionAccess contributionAccess
    ) {
        // Create access for the current folder
        I_FolderAccess folderAccess = buildPlainFolderAccess(
                domainAccess,
                folder,
                timeStamp,
                ehrId,
                contributionAccess
        );

        if (folder.getFolders() != null) {
            // Iterate over sub folders and create FolderAccess for each sub folder
            folder.getFolders().forEach(child -> {
                // Call recursive creation of folderAccess for children without uid
                I_FolderAccess childFolderAccess = buildFolderAccessForInsert(
                        domainAccess,
                        child,
                        timeStamp,
                        ehrId,
                        contributionAccess
                );
                folderAccess.getSubFoldersInsertList().add(childFolderAccess);
            });
        }
        return folderAccess;
    }

    public static I_FolderAccess buildUpdateSubFolderAccess(
            final I_DomainAccess domainAccess,
            final Folder folder,
            final Timestamp timestamp,
            final UUID ehrId,
            final I_ContributionAccess contributionAccess
    ) {

        I_FolderAccess newFolderAccess = buildPlainFolderAccess(
                domainAccess,
                folder,
                new DateTime(timestamp),
                ehrId,
                contributionAccess
        );

        if (folder.getFolders() != null && !folder.getFolders().isEmpty()) {

            // Create new list of sub folders
            folder.getFolders().forEach(childFolder ->
                    newFolderAccess
                            .getSubfoldersList()
                            .put(UUID.randomUUID(), buildUpdateSubFolderAccess(
                                    domainAccess,
                                    childFolder,
                                    timestamp,
                                    ehrId,
                                    contributionAccess
                            ))
            );
        }

        return newFolderAccess;
    }

    /**
     *
     * @param parentFolder identifier.
     * @param childFolder identifier to define the {@link FolderHierarchyRecord} from.
     * @param inContribution contribution that the {@link  com.nedap.archie.rm.directory.Folder} refers to.
     * @param sysTransaction date of the transaction.
     * @param sysPeriod period of validity of the entity persisted.
     * @return the {@link FolderHierarchyRecord} for persisting the folder identified by the childFolder param.
     */
    private  final FolderHierarchyRecord buildFolderHierarchyRecord(final UUID parentFolder, final UUID childFolder, final UUID inContribution, final Timestamp sysTransaction, final Timestamp sysPeriod){
        FolderHierarchyRecord fhRecord = getContext().newRecord(FolderHierarchy.FOLDER_HIERARCHY);
        fhRecord.setParentFolder(parentFolder);
        fhRecord.setChildFolder(childFolder);
        fhRecord.setInContribution(inContribution);
        fhRecord.setSysTransaction(sysTransaction);
        //fhRecord.setSysPeriod(sysPeriod); sys period can be left to null so the system sets it for the temporal tables.
        return fhRecord;
    }

    /**
     * Returns the last version number of a given folder by counting all
     * previous versions of a given folder id. If there are no previous versions
     * in the history table the version number will be 1. Otherwise the current
     * version equals the count of entries in the folder history table plus 1.
     *
     * @param domainAccess - Database connection access context
     * @param folderId - UUID of the folder to check for the last version
     * @return Latest version number for the folder
     */
    public static Integer getLastVersionNumber(
            I_DomainAccess domainAccess,
            UUID folderId
    ) {

        if (!hasPreviousVersion(domainAccess, folderId)) {
            return 1;
        }
        // Get number of entries as the history table of folders
        int versionCount = domainAccess
                .getContext()
                .fetchCount(FOLDER_HISTORY, FOLDER_HISTORY.ID.eq(folderId));
        // Latest version will be entries plus actual entry count (always 1)
        return versionCount + 1;
    }

    /**
     * Checks if there are existing entries for given folder uuid at the folder
     * history table. If there are entries existing, the folder has been
     * modified during previous actions and there are older versions existing.
     *
     * @param domainAccess - Database connection access context
     * @param folderId - UUID of folder to check
     * @return Folder has previous versions or not
     */
    public static boolean hasPreviousVersion(
            I_DomainAccess domainAccess,
            UUID folderId
    ) {
        return domainAccess
                .getContext()
                .fetchExists(FOLDER_HISTORY, FOLDER_HISTORY.ID.eq(folderId));
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
        this.subfoldersList=subfolders;

    }

    public  Map<UUID, I_FolderAccess>  getSubfoldersList() {
        return this.subfoldersList;
    }

    public  void setDetails(final ItemStructure details){
        this.details = details;}

    public List<I_FolderAccess> getSubFoldersInsertList() {
        return this.subFoldersInsertList;
    }

    public  ItemStructure getDetails(){
        return this.details;
    }

    public  List<ObjectRef> getItems(){
        return this.items;
    }

    public UUID getFolderId(){

        return this.folderRecord.getId();
    }

    public void setFolderId(UUID folderId){

        this.folderRecord.setId(folderId);
    }

    public UUID getInContribution(){
        return this.folderRecord.getInContribution();

    }

    public void setInContribution(UUID inContribution){

        this.folderRecord.setInContribution(inContribution);
    }

    public String getFolderName(){

        return this.folderRecord.getName();
    }

    public void setFolderName(String folderName){

        this.folderRecord.setName(folderName);
    }

    public String getFolderArchetypeNodeId(){

        return this.folderRecord.getArchetypeNodeId();
    }

    public void setFolderNArchetypeNodeId(String folderArchetypeNodeId){

        this.folderRecord.setArchetypeNodeId(folderArchetypeNodeId);
    }

    public boolean isFolderActive(){

        return this.folderRecord.getActive();
    }

    public void setIsFolderActive(boolean folderActive){

        this.folderRecord.setActive(folderActive);
    }

    public ItemStructure getFolderDetails(){

        return this.folderRecord.getDetails();
    }

    public void setFolderDetails(ItemStructure folderDetails){

        this.folderRecord.setDetails(folderDetails);
    }

    public void setFolderSysTransaction(Timestamp folderSysTransaction){

        this.folderRecord.setSysTransaction(folderSysTransaction);
    }

    public Timestamp getFolderSysTransaction(){

        return this.folderRecord.getSysTransaction();
    }

    public Object getFolderSysPeriod(){

        return this.folderRecord.getSysPeriod();
    }

    public void setFolderSysPeriod(Object folderSysPeriod){

        this.folderRecord.setSysPeriod(folderSysPeriod);
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public int compareTo(final FolderAccess o) {
        return o.getFolderRecord().getId().compareTo(this.folderRecord.getId());
    }

    /**
     * Utility class to parse joow PGObjects that are not automatically managed by jooq.
     */
    private static class PGObjectParser{
        public static Field<Object> parseSysPeriod(Object sysPeriodToParse){
            String sysPeriodVal ="[\"0001-01-01 15:12:15.841936+00\",)";//sample default value with non-sense date.
            if(sysPeriodToParse!=null){
                sysPeriodVal = sysPeriodToParse.toString().replaceAll("::tstzrange", "").replaceAll("'", "");
            }
            return DSL.field(DSL.val(sysPeriodVal) + "::tstzrange");
        }

        public static Field<Object> parseDetails(Object detailsToParse){
            String  detailsVal = "{\"s\":\"no details set\"}";//sample default value
            if(detailsToParse!=null){
                detailsVal = detailsToParse.toString().replaceAll("::jsonb", "").replaceAll("'", "");
            }
            return DSL.field(DSL.val(detailsVal) + "::jsonb");
        }
    }

    private class ObjectRefId extends ObjectId{
        public ObjectRefId(final String value) {
            super(value);
        }
    }
}