/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.cache;

import com.jayway.jsonpath.DocumentContext;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;

public interface CacheProvider {
    EhrBaseCache<String, WebTemplate> INTROSPECT_CACHE =
            new EhrBaseCache<>("introspectCache", String.class, WebTemplate.class);
    EhrBaseCache<String, UUID> TEMPLATE_ID_UUID_CACHE =
            new EhrBaseCache<>("TemplateIdUuidCache", String.class, UUID.class);
    EhrBaseCache<UUID, String> TEMPLATE_UUID_ID_CACHE =
            new EhrBaseCache<>("TemplateUuidIdCache", UUID.class, String.class);
    EhrBaseCache<String, UUID> USER_ID_CACHE = new EhrBaseCache<>("userIdCache", String.class, UUID.class);
    EhrBaseCache<String, DocumentContext> EXTERNAL_FHIR_TERMINOLOGY_CACHE =
            new EhrBaseCache<>("externalFhirTerminologyCache", String.class, DocumentContext.class);
    EhrBaseCache<String, QueryDefinitionResultDto> STORED_QUERY_CACHE =
            new EhrBaseCache<>("StoredQueryCache", String.class, QueryDefinitionResultDto.class);

    record EhrBaseCache<K, V>(String name, Class<K> kexClass, Class<V> valueClass) {}

    <V, K> V get(EhrBaseCache<K, V> cache, K key, Callable<V> valueLoader);

    <V, K> void evict(EhrBaseCache<K, V> cache, K key);
}
