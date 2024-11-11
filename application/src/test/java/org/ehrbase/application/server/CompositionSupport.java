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
package org.ehrbase.application.server;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Function;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestCompositionEndpoint;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.junit.jupiter.api.Assertions;

public class CompositionSupport {

    private final CanonicalJson json = new CanonicalJson();
    private final Function<UUID, DefaultRestCompositionEndpoint> endpoint;

    private static String compo;

    static {
        URL url = CompositionSupport.class.getResource("composition.json");
        Path path = Paths.get(URI.create(url.toString()));
        try {
            compo = Files.readString(path);
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    public CompositionSupport(OpenEhrClientConfig cfg) {
        endpoint = uuid -> new DefaultRestCompositionEndpoint(new DefaultRestClient(cfg), uuid);
    }

    public ObjectVersionId create(UUID ehrId) {
        Composition composition = json.unmarshal(compo, Composition.class);
        ObjectVersionId versionId = endpoint.apply(ehrId).mergeRaw(composition);
        return versionId;
    }
}
