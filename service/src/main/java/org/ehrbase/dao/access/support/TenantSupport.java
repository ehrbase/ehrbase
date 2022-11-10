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
package org.ehrbase.dao.access.support;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.tenant.TenantAuthentication;
import org.ehrbase.functional.Try;
import org.ehrbase.tenant.DefaultTenantAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantSupport {

    private TenantSupport() {
        // NOP
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSupport.class);

    private static final String WARN_NOT_TENEANT_IDENT =
            "No tenent identifier provided, falling back to default tenant identifier {}";

    public static String currentTenantIdentifier() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Objects::nonNull)
                .filter(DefaultTenantAuthentication.class::isInstance)
                .map(DefaultTenantAuthentication.class::cast)
                .map(DefaultTenantAuthentication::getTenantId)
                .filter(StringUtils::isNotEmpty)
                .orElseGet(() -> {
                    LOGGER.warn(WARN_NOT_TENEANT_IDENT, TenantAuthentication.getDefaultTenantId());
                    return TenantAuthentication.getDefaultTenantId();
                });
    }

    private static final String ERR_TENANT_ID_MISSMATCH = "Provided tenant id[%s] does not match session tenant id[%s]";

    public static Try<String, InternalServerException> isValidTenantId(
            String tenantId, Supplier<String> currentTenant) {
        String currentTenantIdentifier = currentTenant.get();

        return currentTenantIdentifier.equals(tenantId)
                ? Try.success(tenantId)
                : Try.failure(new InternalServerException(
                        String.format(ERR_TENANT_ID_MISSMATCH, tenantId, currentTenantIdentifier)));
    }
}
