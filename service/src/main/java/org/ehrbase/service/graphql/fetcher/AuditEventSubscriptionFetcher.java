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
import org.ehrbase.service.graphql.subscription.AuditEventSink;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

/**
 * Subscription data fetcher for audit event notifications.
 * Returns a Reactive Streams Publisher that emits events from PostgreSQL LISTEN/NOTIFY.
 */
@Component
public class AuditEventSubscriptionFetcher implements DataFetcher<Publisher<Map<String, Object>>> {

    private final AuditEventSink auditEventSink;

    public AuditEventSubscriptionFetcher(AuditEventSink auditEventSink) {
        this.auditEventSink = auditEventSink;
    }

    @Override
    public Publisher<Map<String, Object>> get(DataFetchingEnvironment env) {
        return auditEventSink.stream().map(AuditEventSink.AuditEventNotification::toMap);
    }
}
