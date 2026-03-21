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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Maintains a dedicated JDBC connection (NOT from HikariCP pool) that LISTENs on
 * PostgreSQL NOTIFY channels for composition changes and audit events.
 *
 * <p>Incoming notifications are parsed and published to {@link CompositionChangeSink}
 * and {@link AuditEventSink} for consumption by GraphQL subscriptions.
 *
 * <p>Runs on a virtual thread with exponential backoff reconnection.
 */
@Component
public class PgNotificationListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PgNotificationListener.class);
    private static final String COMPOSITION_CHANNEL = "ehrbase:composition_change";
    private static final String AUDIT_CHANNEL = "ehrbase:audit_event";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CompositionChangeSink compositionSink;
    private final AuditEventSink auditSink;
    private final DataSource dataSource;

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    private volatile boolean running;
    private Thread listenerThread;

    public PgNotificationListener(
            CompositionChangeSink compositionSink, AuditEventSink auditSink, DataSource dataSource) {
        this.compositionSink = compositionSink;
        this.auditSink = auditSink;
        this.dataSource = dataSource;
    }

    @Override
    public void start() {
        running = true;
        listenerThread = Thread.ofVirtual().name("pg-notify-listener").start(this::listenLoop);
        log.info("PostgreSQL LISTEN/NOTIFY listener started");
    }

    @Override
    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        log.info("PostgreSQL LISTEN/NOTIFY listener stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void listenLoop() {
        int backoffMs = 1000;

        while (running) {
            try (Connection conn = createDedicatedConnection()) {
                PGConnection pgConn = conn.unwrap(PGConnection.class);

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("LISTEN \"" + COMPOSITION_CHANNEL + "\"");
                    stmt.execute("LISTEN \"" + AUDIT_CHANNEL + "\"");
                }

                log.info("LISTEN registered on channels: {}, {}", COMPOSITION_CHANNEL, AUDIT_CHANNEL);
                backoffMs = 1000;

                while (running) {
                    PGNotification[] notifications = pgConn.getNotifications(5000);
                    if (notifications == null) {
                        continue;
                    }
                    for (PGNotification notification : notifications) {
                        processNotification(notification);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (running) {
                    log.warn("LISTEN connection lost, reconnecting in {}ms: {}", backoffMs, e.getMessage());
                    sleepQuietly(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 30000);
                }
            }
        }
    }

    private void processNotification(PGNotification notification) {
        String channel = notification.getName();
        String payload = notification.getParameter();

        try {
            JsonNode json = MAPPER.readTree(payload);

            if (COMPOSITION_CHANNEL.equals(channel)) {
                compositionSink.publish(new CompositionChangeSink.CompositionEvent(
                        json.path("action").asText(),
                        UUID.fromString(json.path("ehr_id").asText()),
                        UUID.fromString(json.path("composition_id").asText()),
                        json.path("template_name").asText(null),
                        json.path("sys_version").asInt(1),
                        json.path("change_type").asText(),
                        OffsetDateTime.parse(json.path("committed_at").asText())));
            } else if (AUDIT_CHANNEL.equals(channel)) {
                auditSink.publish(new AuditEventSink.AuditEventNotification(
                        json.path("event_type").asText(),
                        json.path("target_type").asText(),
                        json.path("target_id").asText(null),
                        json.path("action").asText(),
                        json.path("actor_id").asText(null),
                        OffsetDateTime.parse(json.path("created_at").asText())));
            }
        } catch (Exception e) {
            log.error("Failed to process notification on channel {}: {}", channel, e.getMessage());
        }
    }

    private Connection createDedicatedConnection() throws Exception {
        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(true);
        return conn;
    }

    private static void sleepQuietly(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
