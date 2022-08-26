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

import org.ehrbase.jooq.pg.tables.Attestation;
import org.ehrbase.jooq.pg.tables.AttestedView;
import org.ehrbase.jooq.pg.tables.CompoXref;
import org.ehrbase.jooq.pg.tables.Composition;
import org.ehrbase.jooq.pg.tables.CompositionHistory;
import org.ehrbase.jooq.pg.tables.Concept;
import org.ehrbase.jooq.pg.tables.Contribution;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.ehrbase.jooq.pg.tables.Entry;
import org.ehrbase.jooq.pg.tables.Entry2;
import org.ehrbase.jooq.pg.tables.EntryHistory;
import org.ehrbase.jooq.pg.tables.EventContext;
import org.ehrbase.jooq.pg.tables.EventContextHistory;
import org.ehrbase.jooq.pg.tables.FlywaySchemaHistory;
import org.ehrbase.jooq.pg.tables.Folder;
import org.ehrbase.jooq.pg.tables.FolderHierarchy;
import org.ehrbase.jooq.pg.tables.FolderHierarchyHistory;
import org.ehrbase.jooq.pg.tables.FolderHistory;
import org.ehrbase.jooq.pg.tables.FolderItems;
import org.ehrbase.jooq.pg.tables.FolderItemsHistory;
import org.ehrbase.jooq.pg.tables.Identifier;
import org.ehrbase.jooq.pg.tables.ObjectRef;
import org.ehrbase.jooq.pg.tables.ObjectRefHistory;
import org.ehrbase.jooq.pg.tables.Participation;
import org.ehrbase.jooq.pg.tables.ParticipationHistory;
import org.ehrbase.jooq.pg.tables.PartyIdentified;
import org.ehrbase.jooq.pg.tables.Status;
import org.ehrbase.jooq.pg.tables.StatusHistory;
import org.ehrbase.jooq.pg.tables.System;
import org.ehrbase.jooq.pg.tables.Territory;
import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

/**
 * A class modelling indexes of tables in ehr.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index ARCHETYPE_IDX = Internal.createIndex(
            DSL.name("archetype_idx"),
            Entry2.ENTRY2,
            new OrderField[] {Entry2.ENTRY2.ENTITY_CONCEPT, Entry2.ENTRY2.FIELD_IDX_LEN, Entry2.ENTRY2.EHR_ID},
            false);

    public static final Index ATTESTATION_REFERENCE_IDX = Internal.createIndex(
            DSL.name("attestation_reference_idx"),
            Attestation.ATTESTATION,
            new OrderField[] {Attestation.ATTESTATION.REFERENCE},
            false);
    public static final Index ATTESTED_VIEW_ATTESTATION_IDX = Internal.createIndex(
            DSL.name("attested_view_attestation_idx"),
            AttestedView.ATTESTED_VIEW,
            new OrderField[] {AttestedView.ATTESTED_VIEW.ATTESTATION_ID},
            false);
    public static final Index COMPO_XREF_CHILD_IDX = Internal.createIndex(
            DSL.name("compo_xref_child_idx"),
            CompoXref.COMPO_XREF,
            new OrderField[] {CompoXref.COMPO_XREF.CHILD_UUID},
            false);
    public static final Index COMPOSITION_COMPOSER_IDX = Internal.createIndex(
            DSL.name("composition_composer_idx"),
            Composition.COMPOSITION,
            new OrderField[] {Composition.COMPOSITION.COMPOSER},
            false);
    public static final Index COMPOSITION_EHR_IDX = Internal.createIndex(
            DSL.name("composition_ehr_idx"),
            Composition.COMPOSITION,
            new OrderField[] {Composition.COMPOSITION.EHR_ID},
            false);
    public static final Index COMPOSITION_HISTORY_EHR_IDX = Internal.createIndex(
            DSL.name("composition_history_ehr_idx"),
            CompositionHistory.COMPOSITION_HISTORY,
            new OrderField[] {CompositionHistory.COMPOSITION_HISTORY.EHR_ID},
            false);
    public static final Index CONTEXT_COMPOSITION_ID_IDX = Internal.createIndex(
            DSL.name("context_composition_id_idx"),
            EventContext.EVENT_CONTEXT,
            new OrderField[] {EventContext.EVENT_CONTEXT.COMPOSITION_ID},
            true);
    public static final Index CONTEXT_FACILITY_IDX = Internal.createIndex(
            DSL.name("context_facility_idx"),
            EventContext.EVENT_CONTEXT,
            new OrderField[] {EventContext.EVENT_CONTEXT.FACILITY},
            false);
    public static final Index CONTEXT_PARTICIPATION_INDEX = Internal.createIndex(
            DSL.name("context_participation_index"),
            Participation.PARTICIPATION,
            new OrderField[] {Participation.PARTICIPATION.EVENT_CONTEXT},
            false);
    public static final Index CONTRIBUTION_EHR_IDX = Internal.createIndex(
            DSL.name("contribution_ehr_idx"),
            Contribution.CONTRIBUTION,
            new OrderField[] {Contribution.CONTRIBUTION.EHR_ID},
            false);
    public static final Index EHR_COMPO_XREF = Internal.createIndex(
            DSL.name("ehr_compo_xref"),
            CompoXref.COMPO_XREF,
            new OrderField[] {CompoXref.COMPO_XREF.MASTER_UUID},
            false);
    public static final Index EHR_COMPOSITION_HISTORY = Internal.createIndex(
            DSL.name("ehr_composition_history"),
            CompositionHistory.COMPOSITION_HISTORY,
            new OrderField[] {CompositionHistory.COMPOSITION_HISTORY.ID},
            false);
    public static final Index EHR_CONCEPT_ID_LANGUAGE_IDX = Internal.createIndex(
            DSL.name("ehr_concept_id_language_idx"),
            Concept.CONCEPT,
            new OrderField[] {Concept.CONCEPT.CONCEPTID, Concept.CONCEPT.LANGUAGE},
            false);
    public static final Index EHR_ENTRY_HISTORY = Internal.createIndex(
            DSL.name("ehr_entry_history"),
            EntryHistory.ENTRY_HISTORY,
            new OrderField[] {EntryHistory.ENTRY_HISTORY.ID},
            false);
    public static final Index EHR_EVENT_CONTEXT_HISTORY = Internal.createIndex(
            DSL.name("ehr_event_context_history"),
            EventContextHistory.EVENT_CONTEXT_HISTORY,
            new OrderField[] {EventContextHistory.EVENT_CONTEXT_HISTORY.ID},
            false);
    public static final Index EHR_FOLDER_IDX =
            Internal.createIndex(DSL.name("ehr_folder_idx"), Ehr.EHR_, new OrderField[] {Ehr.EHR_.DIRECTORY}, true);
    public static final Index EHR_IDENTIFIER_PARTY_IDX = Internal.createIndex(
            DSL.name("ehr_identifier_party_idx"),
            Identifier.IDENTIFIER,
            new OrderField[] {Identifier.IDENTIFIER.PARTY},
            false);
    public static final Index EHR_PARTICIPATION_HISTORY = Internal.createIndex(
            DSL.name("ehr_participation_history"),
            ParticipationHistory.PARTICIPATION_HISTORY,
            new OrderField[] {ParticipationHistory.PARTICIPATION_HISTORY.ID},
            false);
    public static final Index EHR_STATUS_HISTORY = Internal.createIndex(
            DSL.name("ehr_status_history"),
            StatusHistory.STATUS_HISTORY,
            new OrderField[] {StatusHistory.STATUS_HISTORY.ID},
            false);
    public static final Index EHR_SYSTEM_SETTINGS_IDX = Internal.createIndex(
            DSL.name("ehr_system_settings_idx"), System.SYSTEM, new OrderField[] {System.SYSTEM.SETTINGS}, true);
    public static final Index EHR_TERRITORY_TWOLETTER_IDX = Internal.createIndex(
            DSL.name("ehr_territory_twoletter_idx"),
            Territory.TERRITORY,
            new OrderField[] {Territory.TERRITORY.TWOLETTER},
            true);
    public static final Index ENTRY_HISTORY_COMPOSITION_IDX = Internal.createIndex(
            DSL.name("entry_history_composition_idx"),
            EntryHistory.ENTRY_HISTORY,
            new OrderField[] {EntryHistory.ENTRY_HISTORY.COMPOSITION_ID},
            false);
    public static final Index EVENT_CONTEXT_HISTORY_COMPOSITION_IDX = Internal.createIndex(
            DSL.name("event_context_history_composition_idx"),
            EventContextHistory.EVENT_CONTEXT_HISTORY,
            new OrderField[] {EventContextHistory.EVENT_CONTEXT_HISTORY.COMPOSITION_ID},
            false);
    public static final Index FKI_FOLDER_HIERARCHY_PARENT_FK = Internal.createIndex(
            DSL.name("fki_folder_hierarchy_parent_fk"),
            FolderHierarchy.FOLDER_HIERARCHY,
            new OrderField[] {FolderHierarchy.FOLDER_HIERARCHY.PARENT_FOLDER},
            false);
    public static final Index FLYWAY_SCHEMA_HISTORY_S_IDX = Internal.createIndex(
            DSL.name("flyway_schema_history_s_idx"),
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            new OrderField[] {FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.SUCCESS},
            false);
    public static final Index FOLDER_HIERARCHY_HISTORY_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("folder_hierarchy_history_contribution_idx"),
            FolderHierarchyHistory.FOLDER_HIERARCHY_HISTORY,
            new OrderField[] {FolderHierarchyHistory.FOLDER_HIERARCHY_HISTORY.IN_CONTRIBUTION},
            false);
    public static final Index FOLDER_HIERARCHY_IN_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("folder_hierarchy_in_contribution_idx"),
            FolderHierarchy.FOLDER_HIERARCHY,
            new OrderField[] {FolderHierarchy.FOLDER_HIERARCHY.IN_CONTRIBUTION},
            false);
    public static final Index FOLDER_HIST_IDX = Internal.createIndex(
            DSL.name("folder_hist_idx"),
            FolderItemsHistory.FOLDER_ITEMS_HISTORY,
            new OrderField[] {
                FolderItemsHistory.FOLDER_ITEMS_HISTORY.FOLDER_ID,
                FolderItemsHistory.FOLDER_ITEMS_HISTORY.OBJECT_REF_ID,
                FolderItemsHistory.FOLDER_ITEMS_HISTORY.IN_CONTRIBUTION
            },
            false);
    public static final Index FOLDER_HISTORY_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("folder_history_contribution_idx"),
            FolderHistory.FOLDER_HISTORY,
            new OrderField[] {FolderHistory.FOLDER_HISTORY.IN_CONTRIBUTION},
            false);
    public static final Index FOLDER_IN_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("folder_in_contribution_idx"),
            Folder.FOLDER,
            new OrderField[] {Folder.FOLDER.IN_CONTRIBUTION},
            false);
    public static final Index FOLDER_ITEMS_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("folder_items_contribution_idx"),
            FolderItems.FOLDER_ITEMS,
            new OrderField[] {FolderItems.FOLDER_ITEMS.IN_CONTRIBUTION},
            false);
    public static final Index FOLDER_ITEMS_HISTORY_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("folder_items_history_contribution_idx"),
            FolderItemsHistory.FOLDER_ITEMS_HISTORY,
            new OrderField[] {FolderItemsHistory.FOLDER_ITEMS_HISTORY.IN_CONTRIBUTION},
            false);
    public static final Index GIN_ENTRY_PATH_IDX = Internal.createIndex(
            DSL.name("gin_entry_path_idx"), Entry.ENTRY, new OrderField[] {Entry.ENTRY.ENTRY_}, false);
    public static final Index IDENTIFIER_VALUE_IDX = Internal.createIndex(
            DSL.name("identifier_value_idx"),
            Identifier.IDENTIFIER,
            new OrderField[] {Identifier.IDENTIFIER.ID_VALUE},
            false);
    public static final Index OBJ_REF_IN_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("obj_ref_in_contribution_idx"),
            ObjectRef.OBJECT_REF,
            new OrderField[] {ObjectRef.OBJECT_REF.IN_CONTRIBUTION},
            false);
    public static final Index OBJECT_REF_HISTORY_CONTRIBUTION_IDX = Internal.createIndex(
            DSL.name("object_ref_history_contribution_idx"),
            ObjectRefHistory.OBJECT_REF_HISTORY,
            new OrderField[] {ObjectRefHistory.OBJECT_REF_HISTORY.IN_CONTRIBUTION},
            false);
    public static final Index PARTICIPATION_HISTORY_EVENT_CONTEXT_IDX = Internal.createIndex(
            DSL.name("participation_history_event_context_idx"),
            ParticipationHistory.PARTICIPATION_HISTORY,
            new OrderField[] {ParticipationHistory.PARTICIPATION_HISTORY.EVENT_CONTEXT},
            false);
    public static final Index PARTY_IDENTIFIED_NAMESPACE_VALUE_IDX = Internal.createIndex(
            DSL.name("party_identified_namespace_value_idx"),
            PartyIdentified.PARTY_IDENTIFIED,
            new OrderField[] {
                PartyIdentified.PARTY_IDENTIFIED.PARTY_REF_NAMESPACE, PartyIdentified.PARTY_IDENTIFIED.PARTY_REF_VALUE
            },
            false);
    public static final Index PARTY_IDENTIFIED_PARTY_REF_IDX = Internal.createIndex(
            DSL.name("party_identified_party_ref_idx"),
            PartyIdentified.PARTY_IDENTIFIED,
            new OrderField[] {
                PartyIdentified.PARTY_IDENTIFIED.PARTY_REF_NAMESPACE,
                PartyIdentified.PARTY_IDENTIFIED.PARTY_REF_SCHEME,
                PartyIdentified.PARTY_IDENTIFIED.PARTY_REF_VALUE
            },
            false);
    public static final Index PARTY_IDENTIFIED_PARTY_TYPE_IDX = Internal.createIndex(
            DSL.name("party_identified_party_type_idx"),
            PartyIdentified.PARTY_IDENTIFIED,
            new OrderField[] {PartyIdentified.PARTY_IDENTIFIED.PARTY_TYPE, PartyIdentified.PARTY_IDENTIFIED.NAME},
            false);
    public static final Index STATUS_HISTORY_EHR_IDX = Internal.createIndex(
            DSL.name("status_history_ehr_idx"),
            StatusHistory.STATUS_HISTORY,
            new OrderField[] {StatusHistory.STATUS_HISTORY.EHR_ID},
            false);
    public static final Index STATUS_PARTY_IDX = Internal.createIndex(
            DSL.name("status_party_idx"), Status.STATUS, new OrderField[] {Status.STATUS.PARTY}, false);
    public static final Index TEMPLATE_ENTRY_IDX = Internal.createIndex(
            DSL.name("template_entry_idx"), Entry.ENTRY, new OrderField[] {Entry.ENTRY.TEMPLATE_ID}, false);
    public static final Index TERRITORY_CODE_INDEX = Internal.createIndex(
            DSL.name("territory_code_index"), Territory.TERRITORY, new OrderField[] {Territory.TERRITORY.CODE}, true);
    public static final Index TYPE_IDX = Internal.createIndex(
            DSL.name("type_idx"),
            Entry2.ENTRY2,
            new OrderField[] {Entry2.ENTRY2.RM_ENTITY, Entry2.ENTRY2.FIELD_IDX_LEN, Entry2.ENTRY2.EHR_ID},
            false);
}
