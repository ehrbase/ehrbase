/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.audit.msg;

import java.util.Set;

public class AuditMsgBuilder {
    private String query;
    private String queryId;
    private String location;
    private Integer version;
    private Object[] ehrIds;
    private String templateId;
    private String compositionId;
    private String directoryId;
    private String contributionId;
    private Set<String> removedPatients;
    private Boolean isQueryExecuteEndpoint;
    private static final ThreadLocal<AuditMsgBuilder> auditMsgTL = ThreadLocal.withInitial(AuditMsgBuilder::new);

    public static AuditMsgBuilder getInstance() {
        return auditMsgTL.get();
    }

    public static void removeInstance() {
        auditMsgTL.remove();
    }

    public AuditMsgBuilder setEhrIds(Object... ehrIds) {
        this.ehrIds = ehrIds;
        return this;
    }

    public AuditMsgBuilder setRemovedPatients(Set<String> removedPatients) {
        this.removedPatients = removedPatients;
        return this;
    }

    public AuditMsgBuilder setQuery(String query) {
        this.query = query;
        return this;
    }

    public AuditMsgBuilder setQueryId(String queryId) {
        this.queryId = queryId;
        return this;
    }

    public AuditMsgBuilder setVersion(int version) {
        this.version = version;
        return this;
    }

    public AuditMsgBuilder setLocation(String location) {
        this.location = location;
        return this;
    }

    public AuditMsgBuilder setTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }

    public AuditMsgBuilder setCompositionId(String compositionId) {
        this.compositionId = compositionId;
        return this;
    }

    public AuditMsgBuilder setContributionId(String contributionId) {
        this.contributionId = contributionId;
        return this;
    }

    public AuditMsgBuilder setDirectoryId(String directoryId) {
        this.directoryId = directoryId;
        return this;
    }

    public AuditMsgBuilder setIsQueryExecuteEndpoint(Boolean isQueryExecuteEndpoint) {
        this.isQueryExecuteEndpoint = isQueryExecuteEndpoint;
        return this;
    }

    public void clean() {
        this.setEhrIds();
        this.setVersion(0);
        this.setQuery(null);
        this.setQueryId(null);
        this.setLocation(null);
        this.setTemplateId(null);
        this.setCompositionId(null);
        this.setContributionId(null);
        this.setRemovedPatients(null);
        this.setIsQueryExecuteEndpoint(false);
        this.setDirectoryId(null);
    }

    public AuditMsg build() {
        return new AuditMsg.Builder()
                .query(this.query)
                .ehrIds(this.ehrIds)
                .queryId(this.queryId)
                .version(this.version)
                .location(this.location)
                .templateId(this.templateId)
                .compositionId(this.compositionId)
                .contributionId(this.contributionId)
                .removedPatients(this.removedPatients)
                .isQueryExecuteEndpoint(this.isQueryExecuteEndpoint)
                .directoryId(this.directoryId)
                .build();
    }
}
