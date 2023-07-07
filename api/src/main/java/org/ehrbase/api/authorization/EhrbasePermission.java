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
package org.ehrbase.api.authorization;

public enum EhrbasePermission {
    EHRBASE_DENY_ALL("ehrbase:deny:all"),
    EHRBASE_ADMIN_ACCESS("ehrbase:admin:access"),

    EHRBASE_COMPOSITION_CREATE("ehrbase:composition:create"),
    EHRBASE_COMPOSITION_DELETE("ehrbase:composition:delete"),
    EHRBASE_COMPOSITION_READ("ehrbase:composition:read"),
    EHRBASE_COMPOSITION_UPDATE("ehrbase:composition:update"),
    EHRBASE_CONTRIBUTION_COMPENSATE("ehrbase:contribution:compensate"),
    EHRBASE_CONTRIBUTION_CREATE("ehrbase:contribution:create"),
    EHRBASE_CONTRIBUTION_DELETE("ehrbase:contribution:delete"),
    EHRBASE_CONTRIBUTION_READ("ehrbase:contribution:read"),
    EHRBASE_CONTRIBUTION_UPDATE("ehrbase:contribution:update"),
    EHRBASE_DIRECTORY_CREATE("ehrbase:directory:create"),
    EHRBASE_DIRECTORY_DELETE("ehrbase:directory:delete"),
    EHRBASE_DIRECTORY_READ("ehrbase:directory:read"),
    EHRBASE_DIRECTORY_UPDATE("ehrbase:directory:update"),
    EHRBASE_EHR_CREATE("ehrbase:ehr:create"),
    EHRBASE_EHR_DELETE("ehrbase:ehr:delete"),
    EHRBASE_EHR_READ("ehrbase:ehr:read"),
    EHRBASE_EHR_READ_STATUS("ehrbase:ehr:read_status"),
    EHRBASE_EHR_UPDATE("ehrbase:ehr:update"),
    EHRBASE_EHR_UPDATE_STATUS("ehrbase:ehr:update_status"),
    EHRBASE_QUERY_CREATE("ehrbase:query:create"),
    EHRBASE_QUERY_READ("ehrbase:query:read"),
    EHRBASE_QUERY_DELETE("ehrbase:query:delete"),
    EHRBASE_QUERY_SEARCH("ehrbase:query:search"),
    EHRBASE_QUERY_SEARCH_AD_HOC("ehrbase:query:search_ad_hoc"),
    EHRBASE_SYSTEM_MONITORING("ehrbase:system:monitoring"),
    EHRBASE_SYSTEM_STATUS("ehrbase:system:read"),
    EHRBASE_TEMPLATE_CREATE("ehrbase:template:create"),
    EHRBASE_TEMPLATE_DELETE("ehrbase:template:delete"),
    EHRBASE_TEMPLATE_EXAMPLE("ehrbase:template:example"),
    EHRBASE_TEMPLATE_READ("ehrbase:template:read"),
    EHRBASE_TEMPLATE_UPDATE("ehrbase:template:update"),
    EHRBASE_TENANT_CREATE("ehrbase:tenant:create"),
    EHRBASE_TENANT_READ("ehrbase:tenant:read"),
    EHRBASE_TENANT_UPDATE("ehrbase:tenant:update"),
    EHRBASE_TENANT_DELETE("ehrbase:tenant:delete"),
    EHRBASE_TRIGGER_CREATE("ehrbase:trigger:create"),
    EHRBASE_TRIGGER_DELETE("ehrbase:trigger:delete"),
    EHRBASE_TRIGGER_READ("ehrbase:trigger:read"),
    EHRBASE_TRIGGER_UPDATE("ehrbase:trigger:update"),
    DEMOGRAPHIC_METADATA_READ("demographic:metadata:read"),
    DEMOGRAPHIC_PARAMETERS_CREATE("demographic:parameters:create"),
    DEMOGRAPHIC_RESOURCE_CREATE("demographic:resource:create"),
    DEMOGRAPHIC_RESOURCE_DELETE("demographic:resource:delete"),
    DEMOGRAPHIC_RESOURCE_READ("demographic:resource:read"),
    DEMOGRAPHIC_RESOURCE_SEARCH("demographic:resource:search"),
    DEMOGRAPHIC_RESOURCE_UPDATE("demographic:resource:update");

    private final String permission;

    private EhrbasePermission(String permission) {
        this.permission = permission;
    }

    public String permission() {
        return permission;
    }
}
