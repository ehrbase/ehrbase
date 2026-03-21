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
package org.ehrbase.service.graphql.fetcher;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.service.graphql.subscription.CompositionChangeSink;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

/**
 * Subscription data fetcher for composition change events.
 * Returns a Reactive Streams Publisher that emits events from PostgreSQL LISTEN/NOTIFY.
 */
@Component
public class CompositionSubscriptionFetcher implements DataFetcher<Publisher<Map<String, Object>>> {

    private final CompositionChangeSink compositionChangeSink;

    public CompositionSubscriptionFetcher(CompositionChangeSink compositionChangeSink) {
        this.compositionChangeSink = compositionChangeSink;
    }

    @Override
    public Publisher<Map<String, Object>> get(DataFetchingEnvironment env) {
        String ehrIdArg = env.getArgument("ehrId");

        if (ehrIdArg != null) {
            UUID ehrId = UUID.fromString(ehrIdArg);
            return compositionChangeSink.streamForEhr(ehrId).map(CompositionChangeSink.CompositionEvent::toMap);
        }

        return compositionChangeSink.stream().map(CompositionChangeSink.CompositionEvent::toMap);
    }
}
