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

final class AuditMsgBuilder implements I_AuditMsgBuilder {
    private Set<Object> ehrs;
    private String query;
    private String queryId;
    private Integer version;
    private String location;
    private String templateId;
    private String compositionId;

    private static final ThreadLocal<I_AuditMsgBuilder> auditMsgBuilderThreadLocal =
            ThreadLocal.withInitial(AuditMsgBuilder::new);

    static I_AuditMsgBuilder getBuilderInstance() {
        return auditMsgBuilderThreadLocal.get();
    }

    @Override
    public I_AuditMsgBuilder setEhrIds(Set<Object> ehrs) {
        this.ehrs = ehrs;
        return this;
    }

    @Override
    public I_AuditMsgBuilder setQuery(String query) {
        this.query = query;
        return this;
    }

    @Override
    public I_AuditMsgBuilder setQueryId(String queryId) {
        this.queryId = queryId;
        return this;
    }

    @Override
    public I_AuditMsgBuilder setVersion(int version) {
        this.version = version;
        return this;
    }

    @Override
    public I_AuditMsgBuilder setLocation(String location) {
        this.location = location;
        return this;
    }

    @Override
    public I_AuditMsgBuilder setTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }

    @Override
    public I_AuditMsgBuilder setCompositionId(String compositionId) {
        this.compositionId = compositionId;
        return this;
    }

    @Override
    public void clean() {
        this.setEhrIds(null);
        this.setCompositionId(null);
        this.setLocation(null);
        this.setQuery(null);
        this.setQueryId(null);
        this.setTemplateId(null);
        this.setVersion(0);
    }

    @Override
    public AuditEhrMsg buildEhr() {
        return new AuditEhrMsg(location, ehrs, version);
    }

    @Override
    public AuditQueryMsg buildQuery() {
        return new AuditQueryMsg(location, ehrs, version, query, queryId);
    }

    @Override
    public AuditCompositionMsg buildComposition() {
        return new AuditCompositionMsg(location, ehrs, version, compositionId, templateId);
    }
}
