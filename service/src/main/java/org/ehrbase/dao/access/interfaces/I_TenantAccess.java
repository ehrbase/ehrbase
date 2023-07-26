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
import org.ehrbase.api.tenant.Tenant;
import org.ehrbase.dao.access.jooq.TenantAccess;
import org.jooq.DSLContext;

public interface I_TenantAccess {

    static I_TenantAccess getNewInstance(DSLContext ctx, Tenant tenant) {
        return new TenantAccess(ctx, tenant);
    }

    static List<I_TenantAccess> getAll(DSLContext ctx) {
        return TenantAccess.getAll(ctx);
    }

    static I_TenantAccess retrieveInstanceBy(DSLContext ctx, String tenantId) {
        return TenantAccess.retrieveInstanceBy(ctx, tenantId);
    }

    Tenant update(Tenant tenant);

    Short commit();

    Tenant convert();

    static void deleteTenant(DSLContext ctx, String tenantId) {
        TenantAccess.deleteTenant(ctx, tenantId);
    }

    static boolean hasTenant(I_DomainAccess domainAccess, String tenantId) {
        return TenantAccess.hasTenant(domainAccess, tenantId);
    }
}
