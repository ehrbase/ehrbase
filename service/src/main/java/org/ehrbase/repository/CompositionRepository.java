/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.repository;

import static org.ehrbase.jooq.pg.Tables.COMP_DATA;
import static org.ehrbase.jooq.pg.Tables.COMP_DATA_HISTORY;
import static org.ehrbase.jooq.pg.Tables.COMP_VERSION;
import static org.ehrbase.jooq.pg.Tables.COMP_VERSION_HISTORY;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.tables.records.CompDataHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.CompDataRecord;
import org.ehrbase.jooq.pg.tables.records.CompVersionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.CompVersionRecord;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.service.TimeProvider;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CompositionRepository
        extends AbstractVersionedObjectRepository<
                CompVersionRecord, CompDataRecord, CompVersionHistoryRecord, CompDataHistoryRecord, Composition> {

    private final KnowledgeCacheService knowledgeCache;

    public CompositionRepository(
            DSLContext context,
            ContributionRepository contributionRepository,
            SystemService systemService,
            KnowledgeCacheService knowledgeCache,
            TimeProvider timeProvider) {
        super(
                AuditDetailsTargetType.COMPOSITION,
                COMP_VERSION,
                COMP_DATA,
                COMP_VERSION_HISTORY,
                COMP_DATA_HISTORY,
                context,
                contributionRepository,
                systemService,
                timeProvider);
        this.knowledgeCache = knowledgeCache;
    }

    @Override
    protected Class<Composition> getLocatableClass() {
        return Composition.class;
    }

    @Override
    protected List<TableField<CompVersionRecord, ?>> getVersionDataJoinFields() {
        return List.of(COMP_VERSION.VO_ID);
    }

    @Transactional
    public void commit(UUID ehrId, Composition composition, @Nullable UUID contributionId, @Nullable UUID auditId) {
        UUID templateId = Optional.of(composition)
                .map(Composition::getArchetypeDetails)
                .map(Archetyped::getTemplateId)
                .map(TemplateId::getValue)
                .flatMap(knowledgeCache::findUuidByTemplateId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Unknown or missing template in composition to be stored"));

        String rootConcept = AslRmTypeAndConcept.toEntityConcept(composition.getArchetypeNodeId());

        commitHead(
                ehrId,
                composition,
                contributionId,
                auditId,
                ContributionChangeType.creation,
                r -> {
                    r.setTemplateId(templateId);
                    r.setRootConcept(rootConcept);
                },
                (n, r) -> {});
    }

    @Transactional
    public void delete(UUID ehrId, UUID compId, int version, @Nullable UUID contributionId, @Nullable UUID auditId) {

        delete(
                ehrId,
                singleCompositionInEhrCondition(ehrId, compId, tables.versionHead()),
                version,
                contributionId,
                auditId,
                "No Composition found with: %s ".formatted(compId));
    }

    public boolean isTemplateUsed(String templateId) {
        Optional<UUID> templateUuid = knowledgeCache.findUuidByTemplateId(templateId);
        if (templateUuid.isEmpty()) {
            return false;
        }

        return context.select(COMP_VERSION.VO_ID)
                .from(COMP_VERSION)
                .where(COMP_VERSION.TEMPLATE_ID.eq(templateUuid.get()), COMP_VERSION.SYS_VERSION.eq(1))
                .limit(1)
                .unionAll(context.select(COMP_VERSION_HISTORY.VO_ID)
                        .from(COMP_VERSION_HISTORY)
                        .where(
                                COMP_VERSION_HISTORY.TEMPLATE_ID.eq(templateUuid.get()),
                                COMP_VERSION_HISTORY.SYS_VERSION.eq(1))
                        .limit(1))
                .limit(1)
                .fetchOptional()
                .isPresent();
    }

    @Transactional
    public void update(UUID ehrId, Composition composition, @Nullable UUID contributionId, @Nullable UUID auditId) {

        UUID rootId = extractUid(composition.getUid());
        UUID templateId = Optional.of(composition)
                .map(Composition::getArchetypeDetails)
                .map(Archetyped::getTemplateId)
                .map(TemplateId::getValue)
                .flatMap(knowledgeCache::findUuidByTemplateId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Unknown or missing template in composition to be stored"));

        String rootConcept = AslRmTypeAndConcept.toEntityConcept(composition.getArchetypeNodeId());

        update(
                ehrId,
                composition,
                singleCompositionInEhrCondition(ehrId, rootId, tables.versionHead()),
                singleCompositionInEhrCondition(ehrId, rootId, tables.versionHistory()),
                contributionId,
                auditId,
                r -> {
                    r.setTemplateId(templateId);
                    r.setRootConcept(rootConcept);
                },
                (n, r) -> {},
                "No COMPOSITION with given id: %s".formatted(rootId));
    }

    public boolean exists(UUID compId) {

        return context.selectOne()
                        .from(COMP_VERSION)
                        .where(COMP_VERSION.VO_ID.eq(compId))
                        .unionAll(context.selectOne()
                                .from(COMP_VERSION_HISTORY)
                                .where(COMP_VERSION_HISTORY.VO_ID.eq(compId)))
                        .fetchAny()
                != null;
    }

    public Optional<Integer> getLatestVersionNumber(UUID compId) {
        return context.select(COMP_VERSION.SYS_VERSION)
                .from(COMP_VERSION)
                .where(COMP_VERSION.VO_ID.eq(compId))
                .unionAll(context.select(COMP_VERSION_HISTORY.SYS_VERSION)
                        .from(COMP_VERSION_HISTORY)
                        .where(COMP_VERSION_HISTORY.VO_ID.eq(compId)))
                .orderBy(COMP_VERSION.SYS_VERSION.desc())
                .limit(1)
                .fetchOptional(Record1::value1);
    }

    public Optional<Integer> getLatestVersionNumber(UUID ehrId, UUID compId) {
        return context.select(COMP_VERSION.SYS_VERSION)
                .from(COMP_VERSION)
                .where(COMP_VERSION.EHR_ID.eq(ehrId).and(COMP_VERSION.VO_ID.eq(compId)))
                .unionAll(context.select(COMP_VERSION_HISTORY.SYS_VERSION)
                        .from(COMP_VERSION_HISTORY)
                        .where(COMP_VERSION_HISTORY.EHR_ID.eq(ehrId).and(COMP_VERSION_HISTORY.VO_ID.eq(compId))))
                .orderBy(COMP_VERSION.SYS_VERSION.desc())
                .limit(1)
                .fetchOptional(Record1::value1);
    }

    public boolean isDeleted(UUID ehrId, UUID compId, Integer version) {
        return isDeleted(
                singleCompositionInEhrCondition(ehrId, compId, tables.versionHead()),
                singleCompositionInEhrCondition(ehrId, compId, tables.versionHistory()),
                version);
    }

    private Condition singleCompositionInEhrCondition(UUID ehrId, UUID compId, Table<?> versionTable) {

        return versionTable
                .field(VERSION_PROTOTYPE.EHR_ID)
                .eq(ehrId)
                .and(versionTable.field(VERSION_PROTOTYPE.VO_ID).eq(compId));
    }

    public Optional<Composition> findByVersion(UUID ehrId, UUID compId, int version) {

        return findByVersion(
                singleCompositionInEhrCondition(ehrId, compId, tables.versionHead()),
                singleCompositionInEhrCondition(ehrId, compId, tables.versionHistory()),
                version);
    }

    public Optional<Composition> findHead(UUID ehrId, UUID compId) {
        return findHead(singleCompositionInEhrCondition(ehrId, compId, tables.versionHead()));
    }

    private Optional<CompVersionHistoryRecord> findRootRecordByVersion(UUID ehrId, UUID compId, int version) {
        return findRootRecordByVersion(
                singleCompositionInEhrCondition(ehrId, compId, tables.versionHead()),
                singleCompositionInEhrCondition(ehrId, compId, tables.versionHistory()),
                version);
    }

    public Optional<VersionedComposition> getVersionedComposition(UUID ehrId, UUID composition) {
        return findRootRecordByVersion(ehrId, composition, 1).map(root -> {
            VersionedComposition versionedComposition = new VersionedComposition();
            versionedComposition.setUid(new HierObjectId(root.getVoId().toString()));
            versionedComposition.setOwnerId(new ObjectRef<>(new HierObjectId(ehrId.toString()), "local", "ehr"));
            versionedComposition.setTimeCreated(new DvDateTime(root.getSysPeriodLower()));
            return versionedComposition;
        });
    }

    public Optional<String> findTemplateId(UUID compId) {
        return context.select(COMP_VERSION.TEMPLATE_ID)
                .from(tables.versionHead())
                .where(COMP_VERSION.VO_ID.eq(compId), COMP_VERSION.SYS_VERSION.eq(1))
                .unionAll(context.select(COMP_VERSION_HISTORY.TEMPLATE_ID)
                        .from(tables.versionHistory())
                        .where(COMP_VERSION_HISTORY.VO_ID.eq(compId), COMP_VERSION_HISTORY.SYS_VERSION.eq(1)))
                .fetchOptional(Record1::value1)
                .flatMap(knowledgeCache::findTemplateIdByUuid);
    }

    public Optional<UUID> findEHRforComposition(UUID compId) {
        return context.select(COMP_VERSION.EHR_ID)
                .from(tables.versionHead())
                .where(COMP_VERSION.VO_ID.eq(compId))
                .limit(1)
                .unionAll(context.select(COMP_VERSION_HISTORY.EHR_ID)
                        .from(COMP_VERSION_HISTORY)
                        .where(COMP_VERSION_HISTORY.VO_ID.eq(compId))
                        .limit(1))
                .limit(1)
                .fetchOptional()
                .map(Record1::value1);
    }

    public Optional<OriginalVersion<Composition>> getOriginalVersionComposition(
            UUID ehrUid, UUID versionedObjectUid, int version) {

        return getOriginalVersion(
                singleCompositionInEhrCondition(ehrUid, versionedObjectUid, tables.versionHead()),
                singleCompositionInEhrCondition(ehrUid, versionedObjectUid, tables.versionHistory()),
                version);
    }

    public Optional<Integer> findVersionByTime(UUID compositionId, OffsetDateTime time) {

        return findVersionByTime(
                        COMP_VERSION.VO_ID.eq(compositionId), COMP_VERSION_HISTORY.VO_ID.eq(compositionId), time)
                .map(AbstractVersionedObjectRepository::extractVersion);
    }

    @Transactional
    public void adminDelete(UUID compId) {
        context.delete(COMP_VERSION_HISTORY)
                .where(COMP_VERSION_HISTORY.VO_ID.eq(compId))
                .execute();
        context.delete(COMP_VERSION).where(COMP_VERSION.VO_ID.eq(compId)).execute();
    }

    @Transactional
    public void adminDeleteAll(UUID ehrId) {
        context.delete(COMP_VERSION_HISTORY)
                .where(COMP_VERSION_HISTORY.EHR_ID.eq(ehrId))
                .execute();
        context.delete(COMP_VERSION).where(COMP_VERSION.EHR_ID.eq(ehrId)).execute();
    }
}
