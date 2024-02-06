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
package org.ehrbase.dao.access.jooq;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.ehrbase.jooq.pg.Tables.TENANT;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_TenantAccess;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.Tables;
import org.ehrbase.jooq.pg.tables.records.AdminDeleteTenantFullRecord;
import org.ehrbase.jooq.pg.tables.records.TenantRecord;
import org.ehrbase.openehr.sdk.util.functional.ExceptionalSupplier;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Result;

public class TenantAccess implements I_TenantAccess {

    private final TenantRecord record;

    public TenantAccess(DSLContext ctx, Tenant tenant) {
        this(ctx, initTenantRec(ctx, tenant));
    }

    private TenantAccess(DSLContext ctx, TenantRecord rec) {
        this.record = rec;
    }

    private static TenantRecord initTenantRec(DSLContext ctx, Tenant tenant) {
        TenantRecord rec = ctx.newRecord(Tables.TENANT);
        rec.setTenantId(tenant.getTenantId());
        rec.setTenantName(tenant.getTenantName());
        String json = mapToJson.apply(tenant.getTenantProperties());
        rec.setTenantProperties(isNotBlank(json) ? JSON.json(json) : null);
        return rec;
    }

    @Override
    public Short commit() {
        record.store();
        return record.getId();
    }

    public static List<I_TenantAccess> getAll(DSLContext ctx) {
        Result<TenantRecord> allRecs = ctx.fetch(Tables.TENANT);
        return StreamSupport.stream(allRecs.spliterator(), false)
                .map(rec -> new TenantAccess(ctx, rec))
                .collect(Collectors.toList());
    }

    public static I_TenantAccess retrieveInstanceBy(DSLContext ctx, String tenantId) {
        return Optional.ofNullable(ctx.fetchOne(Tables.TENANT, Tables.TENANT.TENANT_ID.eq(tenantId)))
                .map(rec -> new TenantAccess(ctx, rec))
                .orElse(null);
    }

    public static Map<String, Short> getSysTenants(DSLContext ctx) {
        return ctx.fetch(Tables.TENANT).stream()
                .collect(Collectors.toMap(TenantRecord::getTenantId, TenantRecord::getId));
    }

    private static Function<Map<String, Object>, String> mapToJson = map -> {
        if (map == null) return null;

        ExceptionalSupplier<String, Exception> sup = () -> new ObjectMapper().writeValueAsString(map);
        return sup.get();
    };

    @SuppressWarnings("unchecked")
    private static Function<JSON, Map<String, Object>> jsonToMap = json -> {
        if (json == null || StringUtils.isEmpty(json.data())) return Collections.emptyMap();

        ExceptionalSupplier<Map<String, Object>, Exception> sup =
                () -> (Map<String, Object>) new ObjectMapper().readValue(json.data(), Map.class);
        return sup.get();
    };

    @Override
    public Tenant convert() {
        return new Tenant() {
            public String getTenantId() {
                return record.getTenantId();
            }

            public String getTenantName() {
                return record.getTenantName();
            }

            public Map<String, Object> getTenantProperties() {
                return jsonToMap.apply(record.getTenantProperties());
            }
        };
    }

    private static final String ERR_TENANT_ID = "Updating tenant id[%s] is not allowed";

    @Override
    public Tenant update(Tenant tenant) {
        if (!record.getTenantId().equals(tenant.getTenantId()))
            throw new InternalServerException(String.format(ERR_TENANT_ID, tenant.getTenantId()));

        record.setTenantName(tenant.getTenantName());
        String json = mapToJson.apply(tenant.getTenantProperties());
        record.setTenantProperties(isNotBlank(json) ? JSON.json(json) : null);
        record.update();

        return convert();
    }

    public static void deleteTenant(DSLContext ctx, String tenantId) {
        Result<AdminDeleteTenantFullRecord> result =
                Routines.adminDeleteTenantFull(ctx.configuration(), getSysTenant(ctx, tenantId));

        if (result.isEmpty() || !Boolean.TRUE.equals(result.get(0).getDeleted())) {
            throw new InternalServerException("Deletion of tenant failed!");
        }
    }

    private static Short getSysTenant(DSLContext ctx, String tenantId) {
        TenantRecord tenantRecord = ctx.fetchOne(TENANT, TENANT.TENANT_ID.eq(tenantId));
        return Optional.ofNullable(tenantRecord)
                .map(TenantRecord::getId)
                .orElseThrow(() -> new InternalServerException("Tenant System ID cannot be empty/null"));
    }

    public static boolean hasTenant(I_DomainAccess domainAccess, String tenantId) {
        return domainAccess.getContext().fetchExists(TENANT, TENANT.TENANT_ID.eq(tenantId));
    }
}
