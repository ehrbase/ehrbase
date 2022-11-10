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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.dao.access.interfaces.I_TenantAccess;
import org.ehrbase.jooq.pg.Tables;
import org.ehrbase.jooq.pg.tables.records.TenantRecord;
import org.jooq.DSLContext;
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
        return rec;
    }

    @Override
    public UUID commit() {
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

    @Override
    public Tenant convert() {
        return new Tenant() {
            public String getTenantId() {
                return record.getTenantId();
            }

            public String getTenantName() {
                return record.getTenantName();
            }
        };
    }

    private static final String ERR_TENANT_ID = "Updateing tenant id[%s] is not allowed";

    public Tenant update(Tenant tenant) {
        if (!record.getTenantId().equals(tenant.getTenantId()))
            new InternalServerException(String.format(ERR_TENANT_ID, tenant.getTenantId()));

        record.setTenantName(tenant.getTenantName());
        record.update();
        return convert();
    }
}
