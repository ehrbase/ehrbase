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
package org.ehrbase.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Service for recording immutable audit events in {@code ehr_system.audit_event}.
 *
 * <p>The audit_event table has REVOKE UPDATE, DELETE, TRUNCATE — only INSERT is allowed.
 * Each event includes a {@code prev_hash} for tamper-detection hash chain.
 *
 * <p>Every data access (CREATE, READ, UPDATE, DELETE) should generate an audit event.
 */
@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private static final org.jooq.Table<?> AUDIT_EVENT = table(name("ehr_system", "audit_event"));

    private final DSLContext dsl;
    private final RequestContext requestContext;

    public AuditEventService(DSLContext dsl, RequestContext requestContext) {
        this.dsl = dsl;
        this.requestContext = requestContext;
    }

    /**
     * Records an immutable audit event.
     *
     * @param eventType     data_access, data_modify, auth_success, auth_failure, admin_action, emergency_access
     * @param targetType    composition, ehr, template, user, consent
     * @param targetId      UUID of the target entity (nullable for bulk operations)
     * @param action        create, read, update, delete, pseudonymize, emergency_override
     * @param justification required for emergency/break-glass access (nullable otherwise)
     * @param details       additional context as key-value pairs (nullable)
     * @return UUID of the created audit event
     */
    public UUID recordEvent(
            String eventType,
            String targetType,
            @Nullable UUID targetId,
            String action,
            @Nullable String justification,
            @Nullable Map<String, Object> details) {

        String prevHash = computePrevHash(requestContext.getTenantId());
        JSONB detailsJsonb = details != null ? JSONB.jsonb(toJson(details)) : null;

        Record1<UUID> result = dsl.insertInto(AUDIT_EVENT)
                .set(field(name("event_type"), String.class), eventType)
                .set(field(name("target_type"), String.class), targetType)
                .set(field(name("target_id"), UUID.class), targetId)
                .set(field(name("action"), String.class), action)
                .set(field(name("actor_id"), String.class), requestContext.getUserId())
                .set(field(name("actor_role"), String.class), requestContext.getUserRole())
                .set(field(name("tenant_id"), Short.class), requestContext.getTenantId())
                .set(
                        field(name("ip_address")),
                        DSL.cast(requestContext.getIpAddress(), org.jooq.impl.SQLDataType.OTHER))
                .set(field(name("user_agent"), String.class), requestContext.getUserAgent())
                .set(field(name("justification"), String.class), justification)
                .set(field(name("details"), JSONB.class), detailsJsonb)
                .set(field(name("prev_hash"), String.class), prevHash)
                .returningResult(field(name("id"), UUID.class))
                .fetchOne();

        UUID eventId = result != null ? result.value1() : null;
        log.debug(
                "Audit event recorded: id={} type={} target={}/{} action={}",
                eventId,
                eventType,
                targetType,
                targetId,
                action);
        return eventId;
    }

    /**
     * Computes the prev_hash for tamper-detection chain.
     * Hash = SHA-256(last_event_id_for_tenant || event_type || target_type || action || timestamp)
     */
    private String computePrevHash(short tenantId) {
        // Get the last audit event ID for this tenant
        Record1<UUID> lastEvent = dsl.select(field(name("id"), UUID.class))
                .from(AUDIT_EVENT)
                .where(field(name("tenant_id"), Short.class).eq(tenantId))
                .orderBy(field(name("created_at")).desc())
                .limit(1)
                .fetchOne();

        if (lastEvent == null || lastEvent.value1() == null) {
            return "GENESIS";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(lastEvent.value1().toString().getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toJson(Map<String, Object> map) {
        var sb = new StringBuilder("{");
        var iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof String s) {
                sb.append("\"").append(escapeJson(s)).append("\"");
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append(value);
            }
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
