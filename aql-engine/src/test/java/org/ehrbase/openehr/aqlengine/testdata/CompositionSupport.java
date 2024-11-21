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
package org.ehrbase.openehr.aqlengine.testdata;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import org.apache.commons.io.IOUtils;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestCompositionEndpoint;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

public class CompositionSupport {

    private final CanonicalJson json = new CanonicalJson();
    private final Function<UUID, DefaultRestCompositionEndpoint> endpoint;

    private final TemplateSupport templateSupport;

    private static final String COMPOSITION;

    static {
        try {
            URL url = CompositionSupport.class.getResource("composition.json");
            COMPOSITION = IOUtils.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public CompositionSupport(OpenEhrClientConfig cfg) {
        endpoint = uuid -> new DefaultRestCompositionEndpoint(new DefaultRestClient(cfg), uuid);
        templateSupport = new TemplateSupport(cfg);
    }

    public ObjectVersionId create(UUID ehrId) {
        Composition composition = json.unmarshal(COMPOSITION, Composition.class);
        templateSupport.ensureTemplateExistence(composition);
        return endpoint.apply(ehrId).mergeRaw(composition);
    }
}
