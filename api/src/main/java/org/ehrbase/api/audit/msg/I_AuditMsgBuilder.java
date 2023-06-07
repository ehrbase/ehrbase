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

public interface I_AuditMsgBuilder {

    static I_AuditMsgBuilder getInstance() {
        return AuditMsgBuilder.getBuilderInstance();
    }

    I_AuditMsgBuilder setLocation(String location);

    I_AuditMsgBuilder setEhrIds(Set<Object> ehrId);

    I_AuditMsgBuilder setVersion(int version);

    I_AuditMsgBuilder setTemplateId(String templateId);

    I_AuditMsgBuilder setCompositionId(String compositionId);

    I_AuditMsgBuilder setQuery(String query);

    I_AuditMsgBuilder setQueryId(String queryId);

    void clean();

    AuditEhrMsg buildEhr();

    AuditCompositionMsg buildComposition();

    AuditQueryMsg buildQuery();
}
