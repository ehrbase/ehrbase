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

import java.util.Optional;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.service.ApplicationContextProvider;

public final class TenantSupport {

    private TenantSupport() {
        // NOP
    }

    public static Short currentSysTenant() {
        return Optional.ofNullable(ApplicationContextProvider.getApplicationContext())
                .map(applicationContext -> applicationContext.getBean(TenantService.class))
                .map(TenantService::getCurrentSysTenant)
                .orElseThrow(() -> new IllegalArgumentException("Could not retrieve tenant"));
    }
}
