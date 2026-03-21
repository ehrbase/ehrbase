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

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.service.AuditEventService;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.TenantGuard;
import org.ehrbase.service.TimeProvider;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for EHR and EHR_STATUS CRUD operations.
 * Uses JOOQ with the new {@code ehr_system.ehr} and {@code ehr_system.ehr_status} tables.
 * Same versioning pattern as composition (explicit app-code, no triggers).
 */
@Repository
public class EhrRepository {

    private static final Logger log = LoggerFactory.getLogger(EhrRepository.class);

    private static final org.jooq.Table<?> EHR = table(name("ehr_system", "ehr"));
    private static final org.jooq.Table<?> EHR_STATUS = table(name("ehr_system", "ehr_status"));
    private static final org.jooq.Table<?> EHR_STATUS_HISTORY = table(name("ehr_system", "ehr_status_history"));

    private final DSLContext dsl;
    private final TimeProvider timeProvider;
    private final AuditEventService auditService;
    private final TenantGuard tenantGuard;
    private final RequestContext requestContext;

    public EhrRepository(
            DSLContext dsl,
            TimeProvider timeProvider,
            AuditEventService auditService,
            TenantGuard tenantGuard,
            RequestContext requestContext) {
        this.dsl = dsl;
        this.timeProvider = timeProvider;
        this.auditService = auditService;
        this.tenantGuard = tenantGuard;
        this.requestContext = requestContext;
    }

    @Transactional
    public UUID createEhr(UUID ehrId, EhrStatus status, UUID contributionId) {
        OffsetDateTime now = timeProvider.getNow();
        short tenantId = requestContext.getTenantId();
        String committerName = requestContext.getUserId();

        // Extract subject info from EhrStatus
        String subjectId = null;
        String subjectNamespace = null;
        if (status.getSubject() instanceof PartySelf self && self.getExternalRef() != null) {
            PartyRef ref = self.getExternalRef();
            subjectId = ref.getId() != null ? ref.getId().getValue() : null;
            subjectNamespace = ref.getNamespace();
        }

        boolean isQueryable = Boolean.TRUE.equals(status.isQueryable());
        boolean isModifiable = Boolean.TRUE.equals(status.isModifiable());

        // INSERT into ehr_system.ehr
        if (ehrId == null) {
            Record1<UUID> result = dsl.insertInto(EHR)
                    .set(field(name("subject_id"), String.class), subjectId)
                    .set(field(name("subject_namespace"), String.class), subjectNamespace)
                    .set(field(name("is_queryable"), Boolean.class), isQueryable)
                    .set(field(name("is_modifiable"), Boolean.class), isModifiable)
                    .set(field(name("creation_date"), OffsetDateTime.class), now)
                    .set(field(name("sys_tenant"), Short.class), tenantId)
                    .returningResult(field(name("id"), UUID.class))
                    .fetchOne();
            ehrId = result != null ? result.value1() : null;
        } else {
            dsl.insertInto(EHR)
                    .set(field(name("id"), UUID.class), ehrId)
                    .set(field(name("subject_id"), String.class), subjectId)
                    .set(field(name("subject_namespace"), String.class), subjectNamespace)
                    .set(field(name("is_queryable"), Boolean.class), isQueryable)
                    .set(field(name("is_modifiable"), Boolean.class), isModifiable)
                    .set(field(name("creation_date"), OffsetDateTime.class), now)
                    .set(field(name("sys_tenant"), Short.class), tenantId)
                    .execute();
        }

        // INSERT into ehr_system.ehr_status (version 1)
        String archetypeNodeId =
                status.getArchetypeNodeId() != null ? status.getArchetypeNodeId() : "openEHR-EHR-EHR_STATUS.generic.v1";
        String statusName = status.getName() != null ? status.getName().getValue() : "EHR Status";

        dsl.insertInto(EHR_STATUS)
                .set(field(name("ehr_id"), UUID.class), ehrId)
                .set(field(name("is_queryable"), Boolean.class), isQueryable)
                .set(field(name("is_modifiable"), Boolean.class), isModifiable)
                .set(field(name("subject_id"), String.class), subjectId)
                .set(field(name("subject_namespace"), String.class), subjectNamespace)
                .set(field(name("archetype_node_id"), String.class), archetypeNodeId)
                .set(field(name("name"), String.class), statusName)
                .set(field(name("sys_version"), Integer.class), 1)
                .set(field(name("contribution_id"), UUID.class), contributionId)
                .set(field(name("change_type"), String.class), "creation")
                .set(field(name("committed_at"), OffsetDateTime.class), now)
                .set(field(name("committer_name"), String.class), committerName)
                .set(field(name("committer_id"), String.class), committerName)
                .set(field(name("sys_tenant"), Short.class), tenantId)
                .execute();

        auditService.recordEvent("data_modify", "ehr", ehrId, "create", null, null);
        log.debug("Created EHR: id={}", ehrId);
        return ehrId;
    }

    @Transactional
    public void updateEhrStatus(UUID ehrId, EhrStatus status, int expectedVersion, UUID contributionId) {
        OffsetDateTime now = timeProvider.getNow();
        short tenantId = requestContext.getTenantId();
        String committerName = requestContext.getUserId();

        // Optimistic locking
        Record current = dsl.select()
                .from(EHR_STATUS)
                .where(field(name("ehr_id"), UUID.class).eq(ehrId))
                .and(field(name("sys_version"), Integer.class).eq(expectedVersion))
                .fetchOne();

        if (current == null) {
            throw new PreconditionFailedException(
                    "EHR_STATUS version mismatch: expected %d".formatted(expectedVersion));
        }

        int newVersion = expectedVersion + 1;

        // Archive old to _history
        dsl.execute(
                "INSERT INTO ehr_system.ehr_status_history SELECT * FROM ehr_system.ehr_status WHERE ehr_id = ?",
                ehrId);
        dsl.execute(
                "UPDATE ehr_system.ehr_status_history SET valid_period = tstzrange(lower(valid_period), ?) WHERE ehr_id = ? AND upper(valid_period) IS NULL",
                now,
                ehrId);

        // Delete old from current
        dsl.deleteFrom(EHR_STATUS)
                .where(field(name("ehr_id"), UUID.class).eq(ehrId))
                .execute();

        // Extract updated fields
        String subjectId = null;
        String subjectNamespace = null;
        if (status.getSubject() instanceof PartySelf self && self.getExternalRef() != null) {
            subjectId = self.getExternalRef().getId() != null
                    ? self.getExternalRef().getId().getValue()
                    : null;
            subjectNamespace = self.getExternalRef().getNamespace();
        }

        boolean isQueryable = Boolean.TRUE.equals(status.isQueryable());
        boolean isModifiable = Boolean.TRUE.equals(status.isModifiable());

        // INSERT new version
        dsl.insertInto(EHR_STATUS)
                .set(field(name("ehr_id"), UUID.class), ehrId)
                .set(field(name("is_queryable"), Boolean.class), isQueryable)
                .set(field(name("is_modifiable"), Boolean.class), isModifiable)
                .set(field(name("subject_id"), String.class), subjectId)
                .set(field(name("subject_namespace"), String.class), subjectNamespace)
                .set(
                        field(name("archetype_node_id"), String.class),
                        status.getArchetypeNodeId() != null
                                ? status.getArchetypeNodeId()
                                : "openEHR-EHR-EHR_STATUS.generic.v1")
                .set(
                        field(name("name"), String.class),
                        status.getName() != null ? status.getName().getValue() : "EHR Status")
                .set(field(name("sys_version"), Integer.class), newVersion)
                .set(field(name("contribution_id"), UUID.class), contributionId)
                .set(field(name("change_type"), String.class), "modification")
                .set(field(name("committed_at"), OffsetDateTime.class), now)
                .set(field(name("committer_name"), String.class), committerName)
                .set(field(name("committer_id"), String.class), committerName)
                .set(field(name("sys_tenant"), Short.class), tenantId)
                .execute();

        // Update ehr table flags
        dsl.update(EHR)
                .set(field(name("is_queryable"), Boolean.class), isQueryable)
                .set(field(name("is_modifiable"), Boolean.class), isModifiable)
                .set(field(name("subject_id"), String.class), subjectId)
                .set(field(name("subject_namespace"), String.class), subjectNamespace)
                .where(field(name("id"), UUID.class).eq(ehrId))
                .execute();

        auditService.recordEvent("data_modify", "ehr", ehrId, "update", null, null);
        log.debug("Updated EHR status: ehr={} version={}->{}", ehrId, expectedVersion, newVersion);
    }

    public Optional<EhrStatus> findCurrentStatus(UUID ehrId) {
        Record row = dsl.select()
                .from(EHR_STATUS)
                .where(field(name("ehr_id"), UUID.class).eq(ehrId))
                .fetchOne();

        if (row == null) {
            return Optional.empty();
        }

        auditService.recordEvent("data_access", "ehr", ehrId, "read", null, null);
        return Optional.of(mapToEhrStatus(row, ehrId));
    }

    public Optional<EhrStatus> findStatusByVersion(UUID ehrId, int version) {
        Record row = dsl.resultQuery(
                        "SELECT * FROM ehr_system.ehr_status WHERE ehr_id = ? AND sys_version = ?"
                                + " UNION ALL"
                                + " SELECT * FROM ehr_system.ehr_status_history WHERE ehr_id = ? AND sys_version = ?"
                                + " LIMIT 1",
                        ehrId,
                        version,
                        ehrId,
                        version)
                .fetchOne();

        if (row == null) {
            return Optional.empty();
        }

        return Optional.of(mapToEhrStatus(row, ehrId));
    }

    public Optional<EhrStatus> findStatusAtTime(UUID ehrId, OffsetDateTime timestamp) {
        Record row = dsl.resultQuery(
                        "SELECT * FROM ehr_system.ehr_status WHERE ehr_id = ? AND valid_period @> ?::timestamptz"
                                + " UNION ALL"
                                + " SELECT * FROM ehr_system.ehr_status_history WHERE ehr_id = ? AND valid_period @> ?::timestamptz"
                                + " LIMIT 1",
                        ehrId,
                        timestamp,
                        ehrId,
                        timestamp)
                .fetchOne();
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(mapToEhrStatus(row, ehrId));
    }

    public boolean isModifiable(UUID ehrId) {
        Record1<Boolean> result = dsl.select(field(name("is_modifiable"), Boolean.class))
                .from(EHR)
                .where(field(name("id"), UUID.class).eq(ehrId))
                .fetchOne();
        return result != null && Boolean.TRUE.equals(result.value1());
    }

    public boolean ehrExists(UUID ehrId) {
        return dsl.fetchExists(
                dsl.selectOne().from(EHR).where(field(name("id"), UUID.class).eq(ehrId)));
    }

    public Optional<UUID> findBySubject(String subjectId, String namespace) {
        Record1<UUID> result = dsl.select(field(name("id"), UUID.class))
                .from(EHR)
                .where(field(name("subject_id"), String.class).eq(subjectId))
                .and(field(name("subject_namespace"), String.class).eq(namespace))
                .fetchOne();
        return result != null ? Optional.of(result.value1()) : Optional.empty();
    }

    public Optional<OffsetDateTime> getCreationTime(UUID ehrId) {
        Record1<OffsetDateTime> result = dsl.select(field(name("creation_date"), OffsetDateTime.class))
                .from(EHR)
                .where(field(name("id"), UUID.class).eq(ehrId))
                .fetchOne();
        return result != null ? Optional.of(result.value1()) : Optional.empty();
    }

    public Optional<Integer> getLatestStatusVersion(UUID ehrId) {
        Record1<Integer> result = dsl.select(field(name("sys_version"), Integer.class))
                .from(EHR_STATUS)
                .where(field(name("ehr_id"), UUID.class).eq(ehrId))
                .fetchOne();
        return result != null ? Optional.of(result.value1()) : Optional.empty();
    }

    public void checkEhrExistsAndIsModifiable(UUID ehrId) {
        if (!ehrExists(ehrId)) {
            throw new ObjectNotFoundException("ehr", ehrId.toString());
        }
        if (!isModifiable(ehrId)) {
            throw new StateConflictException("EHR with id %s is not modifiable".formatted(ehrId));
        }
    }

    private EhrStatus mapToEhrStatus(Record row, UUID ehrId) {
        EhrStatus status = new EhrStatus();
        status.setArchetypeNodeId(row.get(field(name("archetype_node_id"), String.class)));
        status.setName(new com.nedap.archie.rm.datavalues.DvText(row.get(field(name("name"), String.class))));

        Boolean queryable = row.get(field(name("is_queryable"), Boolean.class));
        Boolean modifiable = row.get(field(name("is_modifiable"), Boolean.class));
        status.setQueryable(queryable != null ? queryable : true);
        status.setModifiable(modifiable != null ? modifiable : true);

        // Subject
        String subjectId = row.get(field(name("subject_id"), String.class));
        String subjectNamespace = row.get(field(name("subject_namespace"), String.class));
        PartySelf subject = new PartySelf();
        if (subjectId != null) {
            subject.setExternalRef(new PartyRef(new HierObjectId(subjectId), subjectNamespace, "PERSON"));
        }
        status.setSubject(subject);

        // UID
        UUID statusId = row.get(field(name("id"), UUID.class));
        Integer version = row.get(field(name("sys_version"), Integer.class));
        if (statusId != null && version != null) {
            status.setUid(new com.nedap.archie.rm.support.identification.ObjectVersionId(
                    statusId.toString() + "::" + "local.ehrbase.org" + "::" + version));
        }

        return status;
    }
}
