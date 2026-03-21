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
package org.ehrbase.service.graphql.subscription;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Reactive sink for audit event notifications.
 * Fed by {@link PgNotificationListener}, consumed by GraphQL subscription fetchers.
 */
@Component
public class AuditEventSink {

    private final Sinks.Many<AuditEventNotification> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(AuditEventNotification event) {
        sink.tryEmitNext(event);
    }

    public Flux<AuditEventNotification> stream() {
        return sink.asFlux();
    }

    public record AuditEventNotification(
            String eventType,
            String targetType,
            String targetId,
            String action,
            String actorId,
            OffsetDateTime createdAt) {

        public Map<String, Object> toMap() {
            return Map.of(
                    "eventType",
                    eventType,
                    "targetType",
                    targetType,
                    "targetId",
                    targetId != null ? targetId : "",
                    "action",
                    action,
                    "actorId",
                    actorId != null ? actorId : "",
                    "createdAt",
                    createdAt.toString());
        }
    }
}
