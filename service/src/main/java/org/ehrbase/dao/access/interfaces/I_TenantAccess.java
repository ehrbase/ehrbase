/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.dao.access.interfaces;

import java.util.List;
import java.util.UUID;
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.dao.access.jooq.TenantAccess;
import org.ehrbase.dao.access.support.TenantSupport;
import org.jooq.DSLContext;

public interface I_TenantAccess {

    public static String currentTenantIdentifier() {
        return TenantSupport.currentTenantIdentifier();
    }

    static I_TenantAccess getNewInstance(DSLContext ctx, Tenant tenant) {
        return new TenantAccess(ctx, tenant);
    }

    static List<I_TenantAccess> getAll(DSLContext ctx) {
        return TenantAccess.getAll(ctx);
    }

    static I_TenantAccess retrieveInstanceBy(DSLContext ctx, String tenantId) {
        return TenantAccess.retrieveInstanceBy(ctx, tenantId);
    }

    public Tenant update(Tenant tenant);

    public UUID commit();

    public Tenant convert();
}
