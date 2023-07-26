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
package org.ehrbase.service;

import java.util.List;
import java.util.UUID;
import org.ehrbase.aql.sql.queryimpl.ItemInfo;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;

public interface IntrospectService {
    WebTemplate getQueryOptMetaData(UUID uuid);

    WebTemplate getQueryOptMetaData(String templateId);

    List<String> multiValued(String templateId);

    I_KnowledgeCache getKnowledge();

    ItemInfo getInfo(String templateId, String aql);
}
