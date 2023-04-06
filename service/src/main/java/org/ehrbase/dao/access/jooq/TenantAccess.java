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
import org.ehrbase.dao.access.interfaces.I_TenantAccess;
import org.ehrbase.functional.ExceptionalSupplier;
import org.ehrbase.jooq.pg.Tables;
import org.ehrbase.jooq.pg.tables.records.TenantRecord;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantAccess implements I_TenantAccess {

    private static final Logger LOG = LoggerFactory.getLogger(TenantAccess.class);

    private static final String COULD_NOT_RETRIEVE_SYS_TENANT = "Could not retrieve sys tenant by tenant id: {}";

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
        rec.setTenantProperties(JSON.json(json));
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

    private static final String ERR_TENANT_ID = "Updateing tenant id[%s] is not allowed";

    public Tenant update(Tenant tenant) {
        if (!record.getTenantId().equals(tenant.getTenantId()))
            new InternalServerException(String.format(ERR_TENANT_ID, tenant.getTenantId()));

        record.setTenantName(tenant.getTenantName());
        String json = mapToJson.apply(tenant.getTenantProperties());
        record.setTenantProperties(JSON.json(json));
        record.update();
        return convert();
    }

    public static Short retrieveSysTenantByTenantId(DSLContext dslContext, String tenantId) {

        Record tenant;

        try {
            tenant = dslContext
                    .select(TENANT.ID)
                    .from(TENANT)
                    .where(TENANT.TENANT_ID.eq(tenantId))
                    .fetchOne();

        } catch (Exception e) {
            throw new IllegalArgumentException("Could not retrieve sys tenant.", e);
        }

        if (tenant == null || tenant.size() == 0) {
            LOG.warn(COULD_NOT_RETRIEVE_SYS_TENANT, tenantId);
            return null;
        }

        return (Short) tenant.getValue(0);
    }
}
