/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.pg;

import org.ehrbase.jooq.pg.tables.Access;
import org.ehrbase.jooq.pg.tables.Attestation;
import org.ehrbase.jooq.pg.tables.AttestationRef;
import org.ehrbase.jooq.pg.tables.AttestedView;
import org.ehrbase.jooq.pg.tables.AuditDetails;
import org.ehrbase.jooq.pg.tables.CompoXref;
import org.ehrbase.jooq.pg.tables.Composition;
import org.ehrbase.jooq.pg.tables.Concept;
import org.ehrbase.jooq.pg.tables.Contribution;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.ehrbase.jooq.pg.tables.Entry;
import org.ehrbase.jooq.pg.tables.Entry2;
import org.ehrbase.jooq.pg.tables.EventContext;
import org.ehrbase.jooq.pg.tables.FlywaySchemaHistory;
import org.ehrbase.jooq.pg.tables.Folder;
import org.ehrbase.jooq.pg.tables.FolderHierarchy;
import org.ehrbase.jooq.pg.tables.FolderHierarchyHistory;
import org.ehrbase.jooq.pg.tables.FolderHistory;
import org.ehrbase.jooq.pg.tables.FolderItems;
import org.ehrbase.jooq.pg.tables.FolderItemsHistory;
import org.ehrbase.jooq.pg.tables.Heading;
import org.ehrbase.jooq.pg.tables.Identifier;
import org.ehrbase.jooq.pg.tables.Language;
import org.ehrbase.jooq.pg.tables.ObjectRef;
import org.ehrbase.jooq.pg.tables.ObjectRefHistory;
import org.ehrbase.jooq.pg.tables.Participation;
import org.ehrbase.jooq.pg.tables.PartyIdentified;
import org.ehrbase.jooq.pg.tables.Plugin;
import org.ehrbase.jooq.pg.tables.SessionLog;
import org.ehrbase.jooq.pg.tables.Status;
import org.ehrbase.jooq.pg.tables.StoredQuery;
import org.ehrbase.jooq.pg.tables.System;
import org.ehrbase.jooq.pg.tables.TemplateStore;
import org.ehrbase.jooq.pg.tables.TerminologyProvider;
import org.ehrbase.jooq.pg.tables.Territory;
import org.ehrbase.jooq.pg.tables.records.AccessRecord;
import org.ehrbase.jooq.pg.tables.records.AttestationRecord;
import org.ehrbase.jooq.pg.tables.records.AttestationRefRecord;
import org.ehrbase.jooq.pg.tables.records.AttestedViewRecord;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.ehrbase.jooq.pg.tables.records.CompoXrefRecord;
import org.ehrbase.jooq.pg.tables.records.CompositionRecord;
import org.ehrbase.jooq.pg.tables.records.ConceptRecord;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.ehrbase.jooq.pg.tables.records.Entry2Record;
import org.ehrbase.jooq.pg.tables.records.EntryRecord;
import org.ehrbase.jooq.pg.tables.records.EventContextRecord;
import org.ehrbase.jooq.pg.tables.records.FlywaySchemaHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderHierarchyHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderHierarchyRecord;
import org.ehrbase.jooq.pg.tables.records.FolderHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderItemsHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.FolderItemsRecord;
import org.ehrbase.jooq.pg.tables.records.FolderRecord;
import org.ehrbase.jooq.pg.tables.records.HeadingRecord;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.LanguageRecord;
import org.ehrbase.jooq.pg.tables.records.ObjectRefHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.ObjectRefRecord;
import org.ehrbase.jooq.pg.tables.records.ParticipationRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.jooq.pg.tables.records.PluginRecord;
import org.ehrbase.jooq.pg.tables.records.SessionLogRecord;
import org.ehrbase.jooq.pg.tables.records.StatusRecord;
import org.ehrbase.jooq.pg.tables.records.StoredQueryRecord;
import org.ehrbase.jooq.pg.tables.records.SystemRecord;
import org.ehrbase.jooq.pg.tables.records.TemplateStoreRecord;
import org.ehrbase.jooq.pg.tables.records.TerminologyProviderRecord;
import org.ehrbase.jooq.pg.tables.records.TerritoryRecord;
import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

/**
 * A class modelling foreign key relationships and constraints of tables in ehr.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AccessRecord> ACCESS_PKEY =
            Internal.createUniqueKey(Access.ACCESS, DSL.name("access_pkey"), new TableField[] {Access.ACCESS.ID}, true);
    public static final UniqueKey<AttestationRecord> ATTESTATION_PKEY = Internal.createUniqueKey(
            Attestation.ATTESTATION, DSL.name("attestation_pkey"), new TableField[] {Attestation.ATTESTATION.ID}, true);
    public static final UniqueKey<AttestationRefRecord> ATTESTATION_REF_PKEY = Internal.createUniqueKey(
            AttestationRef.ATTESTATION_REF,
            DSL.name("attestation_ref_pkey"),
            new TableField[] {AttestationRef.ATTESTATION_REF.REF},
            true);
    public static final UniqueKey<AttestedViewRecord> ATTESTED_VIEW_PKEY = Internal.createUniqueKey(
            AttestedView.ATTESTED_VIEW,
            DSL.name("attested_view_pkey"),
            new TableField[] {AttestedView.ATTESTED_VIEW.ID},
            true);
    public static final UniqueKey<AuditDetailsRecord> AUDIT_DETAILS_PKEY = Internal.createUniqueKey(
            AuditDetails.AUDIT_DETAILS,
            DSL.name("audit_details_pkey"),
            new TableField[] {AuditDetails.AUDIT_DETAILS.ID},
            true);
    public static final UniqueKey<CompositionRecord> COMPOSITION_PKEY = Internal.createUniqueKey(
            Composition.COMPOSITION, DSL.name("composition_pkey"), new TableField[] {Composition.COMPOSITION.ID}, true);
    public static final UniqueKey<ConceptRecord> CONCEPT_PKEY = Internal.createUniqueKey(
            Concept.CONCEPT, DSL.name("concept_pkey"), new TableField[] {Concept.CONCEPT.ID}, true);
    public static final UniqueKey<ContributionRecord> CONTRIBUTION_PKEY = Internal.createUniqueKey(
            Contribution.CONTRIBUTION,
            DSL.name("contribution_pkey"),
            new TableField[] {Contribution.CONTRIBUTION.ID},
            true);
    public static final UniqueKey<EhrRecord> EHR_PKEY =
            Internal.createUniqueKey(Ehr.EHR_, DSL.name("ehr_pkey"), new TableField[] {Ehr.EHR_.ID}, true);
    public static final UniqueKey<EntryRecord> ENTRY_COMPOSITION_ID_KEY = Internal.createUniqueKey(
            Entry.ENTRY, DSL.name("entry_composition_id_key"), new TableField[] {Entry.ENTRY.COMPOSITION_ID}, true);
    public static final UniqueKey<EntryRecord> ENTRY_PKEY =
            Internal.createUniqueKey(Entry.ENTRY, DSL.name("entry_pkey"), new TableField[] {Entry.ENTRY.ID}, true);
    public static final UniqueKey<Entry2Record> ENTRY2_PKEY = Internal.createUniqueKey(
            Entry2.ENTRY2, DSL.name("entry2_pkey"), new TableField[] {Entry2.ENTRY2.COMP_ID, Entry2.ENTRY2.NUM}, true);

    public static final UniqueKey<EventContextRecord> EVENT_CONTEXT_PKEY = Internal.createUniqueKey(
            EventContext.EVENT_CONTEXT,
            DSL.name("event_context_pkey"),
            new TableField[] {EventContext.EVENT_CONTEXT.ID},
            true);
    public static final UniqueKey<FlywaySchemaHistoryRecord> FLYWAY_SCHEMA_HISTORY_PK = Internal.createUniqueKey(
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            DSL.name("flyway_schema_history_pk"),
            new TableField[] {FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.INSTALLED_RANK},
            true);
    public static final UniqueKey<FolderRecord> FOLDER_PKEY =
            Internal.createUniqueKey(Folder.FOLDER, DSL.name("folder_pkey"), new TableField[] {Folder.FOLDER.ID}, true);
    public static final UniqueKey<FolderHierarchyRecord> FOLDER_HIERARCHY_PKEY = Internal.createUniqueKey(
            FolderHierarchy.FOLDER_HIERARCHY,
            DSL.name("folder_hierarchy_pkey"),
            new TableField[] {
                FolderHierarchy.FOLDER_HIERARCHY.PARENT_FOLDER, FolderHierarchy.FOLDER_HIERARCHY.CHILD_FOLDER
            },
            true);
    public static final UniqueKey<FolderHierarchyRecord> UQ_FOLDERHIERARCHY_PARENT_CHILD = Internal.createUniqueKey(
            FolderHierarchy.FOLDER_HIERARCHY,
            DSL.name("uq_folderhierarchy_parent_child"),
            new TableField[] {
                FolderHierarchy.FOLDER_HIERARCHY.PARENT_FOLDER, FolderHierarchy.FOLDER_HIERARCHY.CHILD_FOLDER
            },
            true);
    public static final UniqueKey<FolderHierarchyHistoryRecord> FOLDER_HIERARCHY_HISTORY_PKEY =
            Internal.createUniqueKey(
                    FolderHierarchyHistory.FOLDER_HIERARCHY_HISTORY,
                    DSL.name("folder_hierarchy_history_pkey"),
                    new TableField[] {
                        FolderHierarchyHistory.FOLDER_HIERARCHY_HISTORY.PARENT_FOLDER,
                        FolderHierarchyHistory.FOLDER_HIERARCHY_HISTORY.CHILD_FOLDER,
                        FolderHierarchyHistory.FOLDER_HIERARCHY_HISTORY.IN_CONTRIBUTION
                    },
                    true);
    public static final UniqueKey<FolderHistoryRecord> FOLDER_HISTORY_PKEY = Internal.createUniqueKey(
            FolderHistory.FOLDER_HISTORY,
            DSL.name("folder_history_pkey"),
            new TableField[] {FolderHistory.FOLDER_HISTORY.ID, FolderHistory.FOLDER_HISTORY.IN_CONTRIBUTION},
            true);
    public static final UniqueKey<FolderItemsRecord> FOLDER_ITEMS_PKEY = Internal.createUniqueKey(
            FolderItems.FOLDER_ITEMS,
            DSL.name("folder_items_pkey"),
            new TableField[] {
                FolderItems.FOLDER_ITEMS.FOLDER_ID,
                FolderItems.FOLDER_ITEMS.OBJECT_REF_ID,
                FolderItems.FOLDER_ITEMS.IN_CONTRIBUTION
            },
            true);
    public static final UniqueKey<FolderItemsHistoryRecord> FOLDER_ITEMS_HIST_PKEY = Internal.createUniqueKey(
            FolderItemsHistory.FOLDER_ITEMS_HISTORY,
            DSL.name("folder_items_hist_pkey"),
            new TableField[] {
                FolderItemsHistory.FOLDER_ITEMS_HISTORY.FOLDER_ID,
                FolderItemsHistory.FOLDER_ITEMS_HISTORY.OBJECT_REF_ID,
                FolderItemsHistory.FOLDER_ITEMS_HISTORY.IN_CONTRIBUTION
            },
            true);
    public static final UniqueKey<HeadingRecord> HEADING_PKEY = Internal.createUniqueKey(
            Heading.HEADING, DSL.name("heading_pkey"), new TableField[] {Heading.HEADING.CODE}, true);
    public static final UniqueKey<LanguageRecord> LANGUAGE_PKEY = Internal.createUniqueKey(
            Language.LANGUAGE, DSL.name("language_pkey"), new TableField[] {Language.LANGUAGE.CODE}, true);
    public static final UniqueKey<ObjectRefRecord> OBJECT_REF_PKEY = Internal.createUniqueKey(
            ObjectRef.OBJECT_REF,
            DSL.name("object_ref_pkey"),
            new TableField[] {ObjectRef.OBJECT_REF.ID, ObjectRef.OBJECT_REF.IN_CONTRIBUTION},
            true);
    public static final UniqueKey<ObjectRefHistoryRecord> OBJECT_REF_HIST_PKEY = Internal.createUniqueKey(
            ObjectRefHistory.OBJECT_REF_HISTORY,
            DSL.name("object_ref_hist_pkey"),
            new TableField[] {
                ObjectRefHistory.OBJECT_REF_HISTORY.ID, ObjectRefHistory.OBJECT_REF_HISTORY.IN_CONTRIBUTION
            },
            true);
    public static final UniqueKey<ParticipationRecord> PARTICIPATION_PKEY = Internal.createUniqueKey(
            Participation.PARTICIPATION,
            DSL.name("participation_pkey"),
            new TableField[] {Participation.PARTICIPATION.ID},
            true);
    public static final UniqueKey<PartyIdentifiedRecord> PARTY_IDENTIFIED_PKEY = Internal.createUniqueKey(
            PartyIdentified.PARTY_IDENTIFIED,
            DSL.name("party_identified_pkey"),
            new TableField[] {PartyIdentified.PARTY_IDENTIFIED.ID},
            true);
    public static final UniqueKey<PluginRecord> PLUGIN_PKEY =
            Internal.createUniqueKey(Plugin.PLUGIN, DSL.name("plugin_pkey"), new TableField[] {Plugin.PLUGIN.ID}, true);
    public static final UniqueKey<SessionLogRecord> SESSION_LOG_PKEY = Internal.createUniqueKey(
            SessionLog.SESSION_LOG, DSL.name("session_log_pkey"), new TableField[] {SessionLog.SESSION_LOG.ID}, true);
    public static final UniqueKey<StatusRecord> STATUS_EHR_ID_KEY = Internal.createUniqueKey(
            Status.STATUS, DSL.name("status_ehr_id_key"), new TableField[] {Status.STATUS.EHR_ID}, true);
    public static final UniqueKey<StatusRecord> STATUS_PKEY =
            Internal.createUniqueKey(Status.STATUS, DSL.name("status_pkey"), new TableField[] {Status.STATUS.ID}, true);
    public static final UniqueKey<StoredQueryRecord> PK_QUALIFIED_NAME = Internal.createUniqueKey(
            StoredQuery.STORED_QUERY,
            DSL.name("pk_qualified_name"),
            new TableField[] {
                StoredQuery.STORED_QUERY.REVERSE_DOMAIN_NAME,
                StoredQuery.STORED_QUERY.SEMANTIC_ID,
                StoredQuery.STORED_QUERY.SEMVER
            },
            true);
    public static final UniqueKey<SystemRecord> SYSTEM_PKEY =
            Internal.createUniqueKey(System.SYSTEM, DSL.name("system_pkey"), new TableField[] {System.SYSTEM.ID}, true);
    public static final UniqueKey<TemplateStoreRecord> TEMPLATE_STORE_PKEY = Internal.createUniqueKey(
            TemplateStore.TEMPLATE_STORE,
            DSL.name("template_store_pkey"),
            new TableField[] {TemplateStore.TEMPLATE_STORE.ID},
            true);
    public static final UniqueKey<TerminologyProviderRecord> TERMINOLOGY_PROVIDER_PKEY = Internal.createUniqueKey(
            TerminologyProvider.TERMINOLOGY_PROVIDER,
            DSL.name("terminology_provider_pkey"),
            new TableField[] {TerminologyProvider.TERMINOLOGY_PROVIDER.CODE},
            true);
    public static final UniqueKey<TerritoryRecord> TERRITORY_PKEY = Internal.createUniqueKey(
            Territory.TERRITORY, DSL.name("territory_pkey"), new TableField[] {Territory.TERRITORY.CODE}, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<AttestationRecord, AuditDetailsRecord> ATTESTATION__ATTESTATION_HAS_AUDIT_FKEY =
            Internal.createForeignKey(
                    Attestation.ATTESTATION,
                    DSL.name("attestation_has_audit_fkey"),
                    new TableField[] {Attestation.ATTESTATION.HAS_AUDIT},
                    Keys.AUDIT_DETAILS_PKEY,
                    new TableField[] {AuditDetails.AUDIT_DETAILS.ID},
                    true);
    public static final ForeignKey<AttestationRecord, AttestationRefRecord> ATTESTATION__ATTESTATION_REFERENCE_FKEY =
            Internal.createForeignKey(
                    Attestation.ATTESTATION,
                    DSL.name("attestation_reference_fkey"),
                    new TableField[] {Attestation.ATTESTATION.REFERENCE},
                    Keys.ATTESTATION_REF_PKEY,
                    new TableField[] {AttestationRef.ATTESTATION_REF.REF},
                    true);
    public static final ForeignKey<AttestedViewRecord, AttestationRecord>
            ATTESTED_VIEW__ATTESTED_VIEW_ATTESTATION_ID_FKEY = Internal.createForeignKey(
                    AttestedView.ATTESTED_VIEW,
                    DSL.name("attested_view_attestation_id_fkey"),
                    new TableField[] {AttestedView.ATTESTED_VIEW.ATTESTATION_ID},
                    Keys.ATTESTATION_PKEY,
                    new TableField[] {Attestation.ATTESTATION.ID},
                    true);
    public static final ForeignKey<AuditDetailsRecord, PartyIdentifiedRecord>
            AUDIT_DETAILS__AUDIT_DETAILS_COMMITTER_FKEY = Internal.createForeignKey(
                    AuditDetails.AUDIT_DETAILS,
                    DSL.name("audit_details_committer_fkey"),
                    new TableField[] {AuditDetails.AUDIT_DETAILS.COMMITTER},
                    Keys.PARTY_IDENTIFIED_PKEY,
                    new TableField[] {PartyIdentified.PARTY_IDENTIFIED.ID},
                    true);
    public static final ForeignKey<AuditDetailsRecord, SystemRecord> AUDIT_DETAILS__AUDIT_DETAILS_SYSTEM_ID_FKEY =
            Internal.createForeignKey(
                    AuditDetails.AUDIT_DETAILS,
                    DSL.name("audit_details_system_id_fkey"),
                    new TableField[] {AuditDetails.AUDIT_DETAILS.SYSTEM_ID},
                    Keys.SYSTEM_PKEY,
                    new TableField[] {System.SYSTEM.ID},
                    true);
    public static final ForeignKey<CompoXrefRecord, CompositionRecord> COMPO_XREF__COMPO_XREF_CHILD_UUID_FKEY =
            Internal.createForeignKey(
                    CompoXref.COMPO_XREF,
                    DSL.name("compo_xref_child_uuid_fkey"),
                    new TableField[] {CompoXref.COMPO_XREF.CHILD_UUID},
                    Keys.COMPOSITION_PKEY,
                    new TableField[] {Composition.COMPOSITION.ID},
                    true);
    public static final ForeignKey<CompoXrefRecord, CompositionRecord> COMPO_XREF__COMPO_XREF_MASTER_UUID_FKEY =
            Internal.createForeignKey(
                    CompoXref.COMPO_XREF,
                    DSL.name("compo_xref_master_uuid_fkey"),
                    new TableField[] {CompoXref.COMPO_XREF.MASTER_UUID},
                    Keys.COMPOSITION_PKEY,
                    new TableField[] {Composition.COMPOSITION.ID},
                    true);
    public static final ForeignKey<CompositionRecord, AttestationRefRecord>
            COMPOSITION__COMPOSITION_ATTESTATION_REF_FKEY = Internal.createForeignKey(
                    Composition.COMPOSITION,
                    DSL.name("composition_attestation_ref_fkey"),
                    new TableField[] {Composition.COMPOSITION.ATTESTATION_REF},
                    Keys.ATTESTATION_REF_PKEY,
                    new TableField[] {AttestationRef.ATTESTATION_REF.REF},
                    true);
    public static final ForeignKey<CompositionRecord, PartyIdentifiedRecord> COMPOSITION__COMPOSITION_COMPOSER_FKEY =
            Internal.createForeignKey(
                    Composition.COMPOSITION,
                    DSL.name("composition_composer_fkey"),
                    new TableField[] {Composition.COMPOSITION.COMPOSER},
                    Keys.PARTY_IDENTIFIED_PKEY,
                    new TableField[] {PartyIdentified.PARTY_IDENTIFIED.ID},
                    true);
    public static final ForeignKey<CompositionRecord, EhrRecord> COMPOSITION__COMPOSITION_EHR_ID_FKEY =
            Internal.createForeignKey(
                    Composition.COMPOSITION,
                    DSL.name("composition_ehr_id_fkey"),
                    new TableField[] {Composition.COMPOSITION.EHR_ID},
                    Keys.EHR_PKEY,
                    new TableField[] {Ehr.EHR_.ID},
                    true);
    public static final ForeignKey<CompositionRecord, AuditDetailsRecord> COMPOSITION__COMPOSITION_HAS_AUDIT_FKEY =
            Internal.createForeignKey(
                    Composition.COMPOSITION,
                    DSL.name("composition_has_audit_fkey"),
                    new TableField[] {Composition.COMPOSITION.HAS_AUDIT},
                    Keys.AUDIT_DETAILS_PKEY,
                    new TableField[] {AuditDetails.AUDIT_DETAILS.ID},
                    true);
    public static final ForeignKey<CompositionRecord, ContributionRecord>
            COMPOSITION__COMPOSITION_IN_CONTRIBUTION_FKEY = Internal.createForeignKey(
                    Composition.COMPOSITION,
                    DSL.name("composition_in_contribution_fkey"),
                    new TableField[] {Composition.COMPOSITION.IN_CONTRIBUTION},
                    Keys.CONTRIBUTION_PKEY,
                    new TableField[] {Contribution.CONTRIBUTION.ID},
                    true);
    public static final ForeignKey<CompositionRecord, LanguageRecord> COMPOSITION__COMPOSITION_LANGUAGE_FKEY =
            Internal.createForeignKey(
                    Composition.COMPOSITION,
                    DSL.name("composition_language_fkey"),
                    new TableField[] {Composition.COMPOSITION.LANGUAGE},
                    Keys.LANGUAGE_PKEY,
                    new TableField[] {Language.LANGUAGE.CODE},
                    true);
    public static final ForeignKey<CompositionRecord, TerritoryRecord> COMPOSITION__COMPOSITION_TERRITORY_FKEY =
            Internal.createForeignKey(
                    Composition.COMPOSITION,
                    DSL.name("composition_territory_fkey"),
                    new TableField[] {Composition.COMPOSITION.TERRITORY},
                    Keys.TERRITORY_PKEY,
                    new TableField[] {Territory.TERRITORY.CODE},
                    true);
    public static final ForeignKey<ConceptRecord, LanguageRecord> CONCEPT__CONCEPT_LANGUAGE_FKEY =
            Internal.createForeignKey(
                    Concept.CONCEPT,
                    DSL.name("concept_language_fkey"),
                    new TableField[] {Concept.CONCEPT.LANGUAGE},
                    Keys.LANGUAGE_PKEY,
                    new TableField[] {Language.LANGUAGE.CODE},
                    true);
    public static final ForeignKey<ContributionRecord, EhrRecord> CONTRIBUTION__CONTRIBUTION_EHR_ID_FKEY =
            Internal.createForeignKey(
                    Contribution.CONTRIBUTION,
                    DSL.name("contribution_ehr_id_fkey"),
                    new TableField[] {Contribution.CONTRIBUTION.EHR_ID},
                    Keys.EHR_PKEY,
                    new TableField[] {Ehr.EHR_.ID},
                    true);
    public static final ForeignKey<ContributionRecord, AuditDetailsRecord> CONTRIBUTION__CONTRIBUTION_HAS_AUDIT_FKEY =
            Internal.createForeignKey(
                    Contribution.CONTRIBUTION,
                    DSL.name("contribution_has_audit_fkey"),
                    new TableField[] {Contribution.CONTRIBUTION.HAS_AUDIT},
                    Keys.AUDIT_DETAILS_PKEY,
                    new TableField[] {AuditDetails.AUDIT_DETAILS.ID},
                    true);
    public static final ForeignKey<EhrRecord, AccessRecord> EHR__EHR_ACCESS_FKEY = Internal.createForeignKey(
            Ehr.EHR_,
            DSL.name("ehr_access_fkey"),
            new TableField[] {Ehr.EHR_.ACCESS},
            Keys.ACCESS_PKEY,
            new TableField[] {Access.ACCESS.ID},
            true);
    public static final ForeignKey<EhrRecord, FolderRecord> EHR__EHR_DIRECTORY_FKEY = Internal.createForeignKey(
            Ehr.EHR_,
            DSL.name("ehr_directory_fkey"),
            new TableField[] {Ehr.EHR_.DIRECTORY},
            Keys.FOLDER_PKEY,
            new TableField[] {Folder.FOLDER.ID},
            true);
    public static final ForeignKey<EhrRecord, SystemRecord> EHR__EHR_SYSTEM_ID_FKEY = Internal.createForeignKey(
            Ehr.EHR_,
            DSL.name("ehr_system_id_fkey"),
            new TableField[] {Ehr.EHR_.SYSTEM_ID},
            Keys.SYSTEM_PKEY,
            new TableField[] {System.SYSTEM.ID},
            true);
    public static final ForeignKey<EntryRecord, CompositionRecord> ENTRY__ENTRY_COMPOSITION_ID_FKEY =
            Internal.createForeignKey(
                    Entry.ENTRY,
                    DSL.name("entry_composition_id_fkey"),
                    new TableField[] {Entry.ENTRY.COMPOSITION_ID},
                    Keys.COMPOSITION_PKEY,
                    new TableField[] {Composition.COMPOSITION.ID},
                    true);
    public static final ForeignKey<Entry2Record, EhrRecord> ENTRY2__ENTRY2_EHR_ID_FKEY = Internal.createForeignKey(
            Entry2.ENTRY2,
            DSL.name("entry2_ehr_id_fkey"),
            new TableField[] {Entry2.ENTRY2.EHR_ID},
            Keys.EHR_PKEY,
            new TableField[] {Ehr.EHR_.ID},
            true);

    public static final ForeignKey<EventContextRecord, CompositionRecord>
            EVENT_CONTEXT__EVENT_CONTEXT_COMPOSITION_ID_FKEY = Internal.createForeignKey(
                    EventContext.EVENT_CONTEXT,
                    DSL.name("event_context_composition_id_fkey"),
                    new TableField[] {EventContext.EVENT_CONTEXT.COMPOSITION_ID},
                    Keys.COMPOSITION_PKEY,
                    new TableField[] {Composition.COMPOSITION.ID},
                    true);
    public static final ForeignKey<EventContextRecord, PartyIdentifiedRecord>
            EVENT_CONTEXT__EVENT_CONTEXT_FACILITY_FKEY = Internal.createForeignKey(
                    EventContext.EVENT_CONTEXT,
                    DSL.name("event_context_facility_fkey"),
                    new TableField[] {EventContext.EVENT_CONTEXT.FACILITY},
                    Keys.PARTY_IDENTIFIED_PKEY,
                    new TableField[] {PartyIdentified.PARTY_IDENTIFIED.ID},
                    true);
    public static final ForeignKey<FolderRecord, AuditDetailsRecord> FOLDER__FOLDER_HAS_AUDIT_FKEY =
            Internal.createForeignKey(
                    Folder.FOLDER,
                    DSL.name("folder_has_audit_fkey"),
                    new TableField[] {Folder.FOLDER.HAS_AUDIT},
                    Keys.AUDIT_DETAILS_PKEY,
                    new TableField[] {AuditDetails.AUDIT_DETAILS.ID},
                    true);
    public static final ForeignKey<FolderRecord, ContributionRecord> FOLDER__FOLDER_IN_CONTRIBUTION_FKEY =
            Internal.createForeignKey(
                    Folder.FOLDER,
                    DSL.name("folder_in_contribution_fkey"),
                    new TableField[] {Folder.FOLDER.IN_CONTRIBUTION},
                    Keys.CONTRIBUTION_PKEY,
                    new TableField[] {Contribution.CONTRIBUTION.ID},
                    true);
    public static final ForeignKey<FolderHierarchyRecord, ContributionRecord>
            FOLDER_HIERARCHY__FOLDER_HIERARCHY_IN_CONTRIBUTION_FK = Internal.createForeignKey(
                    FolderHierarchy.FOLDER_HIERARCHY,
                    DSL.name("folder_hierarchy_in_contribution_fk"),
                    new TableField[] {FolderHierarchy.FOLDER_HIERARCHY.IN_CONTRIBUTION},
                    Keys.CONTRIBUTION_PKEY,
                    new TableField[] {Contribution.CONTRIBUTION.ID},
                    true);
    public static final ForeignKey<FolderHierarchyRecord, FolderRecord> FOLDER_HIERARCHY__FOLDER_HIERARCHY_PARENT_FK =
            Internal.createForeignKey(
                    FolderHierarchy.FOLDER_HIERARCHY,
                    DSL.name("folder_hierarchy_parent_fk"),
                    new TableField[] {FolderHierarchy.FOLDER_HIERARCHY.PARENT_FOLDER},
                    Keys.FOLDER_PKEY,
                    new TableField[] {Folder.FOLDER.ID},
                    true);
    public static final ForeignKey<FolderItemsRecord, FolderRecord> FOLDER_ITEMS__FOLDER_ITEMS_FOLDER_FKEY =
            Internal.createForeignKey(
                    FolderItems.FOLDER_ITEMS,
                    DSL.name("folder_items_folder_fkey"),
                    new TableField[] {FolderItems.FOLDER_ITEMS.FOLDER_ID},
                    Keys.FOLDER_PKEY,
                    new TableField[] {Folder.FOLDER.ID},
                    true);
    public static final ForeignKey<FolderItemsRecord, ContributionRecord>
            FOLDER_ITEMS__FOLDER_ITEMS_IN_CONTRIBUTION_FKEY = Internal.createForeignKey(
                    FolderItems.FOLDER_ITEMS,
                    DSL.name("folder_items_in_contribution_fkey"),
                    new TableField[] {FolderItems.FOLDER_ITEMS.IN_CONTRIBUTION},
                    Keys.CONTRIBUTION_PKEY,
                    new TableField[] {Contribution.CONTRIBUTION.ID},
                    true);
    public static final ForeignKey<FolderItemsRecord, ObjectRefRecord> FOLDER_ITEMS__FOLDER_ITEMS_OBJ_REF_FKEY =
            Internal.createForeignKey(
                    FolderItems.FOLDER_ITEMS,
                    DSL.name("folder_items_obj_ref_fkey"),
                    new TableField[] {FolderItems.FOLDER_ITEMS.IN_CONTRIBUTION, FolderItems.FOLDER_ITEMS.OBJECT_REF_ID},
                    Keys.OBJECT_REF_PKEY,
                    new TableField[] {ObjectRef.OBJECT_REF.IN_CONTRIBUTION, ObjectRef.OBJECT_REF.ID},
                    true);
    public static final ForeignKey<IdentifierRecord, PartyIdentifiedRecord> IDENTIFIER__IDENTIFIER_PARTY_FKEY =
            Internal.createForeignKey(
                    Identifier.IDENTIFIER,
                    DSL.name("identifier_party_fkey"),
                    new TableField[] {Identifier.IDENTIFIER.PARTY},
                    Keys.PARTY_IDENTIFIED_PKEY,
                    new TableField[] {PartyIdentified.PARTY_IDENTIFIED.ID},
                    true);
    public static final ForeignKey<ObjectRefRecord, ContributionRecord> OBJECT_REF__OBJECT_REF_IN_CONTRIBUTION_FKEY =
            Internal.createForeignKey(
                    ObjectRef.OBJECT_REF,
                    DSL.name("object_ref_in_contribution_fkey"),
                    new TableField[] {ObjectRef.OBJECT_REF.IN_CONTRIBUTION},
                    Keys.CONTRIBUTION_PKEY,
                    new TableField[] {Contribution.CONTRIBUTION.ID},
                    true);
    public static final ForeignKey<ParticipationRecord, EventContextRecord>
            PARTICIPATION__PARTICIPATION_EVENT_CONTEXT_FKEY = Internal.createForeignKey(
                    Participation.PARTICIPATION,
                    DSL.name("participation_event_context_fkey"),
                    new TableField[] {Participation.PARTICIPATION.EVENT_CONTEXT},
                    Keys.EVENT_CONTEXT_PKEY,
                    new TableField[] {EventContext.EVENT_CONTEXT.ID},
                    true);
    public static final ForeignKey<ParticipationRecord, PartyIdentifiedRecord>
            PARTICIPATION__PARTICIPATION_PERFORMER_FKEY = Internal.createForeignKey(
                    Participation.PARTICIPATION,
                    DSL.name("participation_performer_fkey"),
                    new TableField[] {Participation.PARTICIPATION.PERFORMER},
                    Keys.PARTY_IDENTIFIED_PKEY,
                    new TableField[] {PartyIdentified.PARTY_IDENTIFIED.ID},
                    true);
    public static final ForeignKey<StatusRecord, AttestationRefRecord> STATUS__STATUS_ATTESTATION_REF_FKEY =
            Internal.createForeignKey(
                    Status.STATUS,
                    DSL.name("status_attestation_ref_fkey"),
                    new TableField[] {Status.STATUS.ATTESTATION_REF},
                    Keys.ATTESTATION_REF_PKEY,
                    new TableField[] {AttestationRef.ATTESTATION_REF.REF},
                    true);
    public static final ForeignKey<StatusRecord, EhrRecord> STATUS__STATUS_EHR_ID_FKEY = Internal.createForeignKey(
            Status.STATUS,
            DSL.name("status_ehr_id_fkey"),
            new TableField[] {Status.STATUS.EHR_ID},
            Keys.EHR_PKEY,
            new TableField[] {Ehr.EHR_.ID},
            true);
    public static final ForeignKey<StatusRecord, AuditDetailsRecord> STATUS__STATUS_HAS_AUDIT_FKEY =
            Internal.createForeignKey(
                    Status.STATUS,
                    DSL.name("status_has_audit_fkey"),
                    new TableField[] {Status.STATUS.HAS_AUDIT},
                    Keys.AUDIT_DETAILS_PKEY,
                    new TableField[] {AuditDetails.AUDIT_DETAILS.ID},
                    true);
    public static final ForeignKey<StatusRecord, ContributionRecord> STATUS__STATUS_IN_CONTRIBUTION_FKEY =
            Internal.createForeignKey(
                    Status.STATUS,
                    DSL.name("status_in_contribution_fkey"),
                    new TableField[] {Status.STATUS.IN_CONTRIBUTION},
                    Keys.CONTRIBUTION_PKEY,
                    new TableField[] {Contribution.CONTRIBUTION.ID},
                    true);
    public static final ForeignKey<StatusRecord, PartyIdentifiedRecord> STATUS__STATUS_PARTY_FKEY =
            Internal.createForeignKey(
                    Status.STATUS,
                    DSL.name("status_party_fkey"),
                    new TableField[] {Status.STATUS.PARTY},
                    Keys.PARTY_IDENTIFIED_PKEY,
                    new TableField[] {PartyIdentified.PARTY_IDENTIFIED.ID},
                    true);
}
