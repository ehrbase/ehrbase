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
package org.ehrbase.api.tenant;

public interface TenantAuthentication<T> {

    public static final String DEFAULT_TENANT_ID = "1f332a66-0e57-11ed-861d-0242ac120002";
    public static final Short DEFAULT_SYS_TENANT = 1;

    /**
     * Returns the tenant ID associated with this authentication object.
     *
     * @return The tenant ID of the authenticated principal.
     */
    public String getTenantId();

    /**
     * Returns the default tenant ID.
     *
     * @return The default tenant ID.
     */
    public static String getDefaultTenantId() {
        return DEFAULT_TENANT_ID;
    }
    /**
     * Returns the default sys tenant.
     *
     * @return The default sys tenant.
     */
    public static Short getDefaultSysTenant() {
        return DEFAULT_SYS_TENANT;
    }

    /**
     * Returns the name associated with this authentication object.
     * <p>
     * This method is intended to have the same semantic meaning as the {@link java.security.Principal#getName()}
     * method, and returns different values depending on the type of authentication used.
     * For example, if the authentication was performed using OAuth2, the name may correspond to the "sub" (subject)
     * claim of the authentication token. If the authentication was performed using basic authentication,
     * the name may correspond to the username used for authentication.
     * <p>
     * Note: Please note that this method does not return the tenant ID or tenant name.
     *
     * @return The name of the authenticated principal.
     */
    public String getName();

    /**
     * Returns the authentication object.
     *
     * @return The authentication object.
     */
    public T getAuthentication();
}
