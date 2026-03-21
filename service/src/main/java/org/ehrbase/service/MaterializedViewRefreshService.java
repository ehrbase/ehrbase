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
package org.ehrbase.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Background service that refreshes materialized views in {@code ehr_views} schema.
 * Uses {@code REFRESH MATERIALIZED VIEW CONCURRENTLY} for zero reader downtime.
 */
@Service
public class MaterializedViewRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MaterializedViewRefreshService.class);

    private static final org.jooq.Table<?> VIEW_CATALOG = table(name("ehr_system", "view_catalog"));

    private final DSLContext dsl;
    private final AuditEventService auditService;

    public MaterializedViewRefreshService(DSLContext dsl, AuditEventService auditService) {
        this.dsl = dsl;
        this.auditService = auditService;
    }

    /**
     * Refreshes the compliance dashboard materialized view.
     * Runs every hour by default (configurable via ehrbase.views.compliance-refresh).
     */
    @Scheduled(fixedDelayString = "${ehrbase.views.compliance-refresh:3600000}")
    public void refreshComplianceDashboard() {
        refreshView("ehr_views", "mv_compliance_dashboard");
    }

    /**
     * Refreshes all materialized views registered in the view catalog.
     * Runs daily at 2 AM by default (configurable via ehrbase.views.refresh-cron).
     */
    @Scheduled(cron = "${ehrbase.views.refresh-cron:0 0 2 * * ?}")
    public void refreshAllMaterializedViews() {
        log.info("Starting scheduled refresh of all materialized views");

        var matViews = dsl.select(field(name("view_name"), String.class), field(name("view_schema"), String.class))
                .from(VIEW_CATALOG)
                .where(field(name("is_materialized"), Boolean.class).isTrue())
                .fetch();

        int refreshed = 0;
        for (Record row : matViews) {
            String schema = row.get(field(name("view_schema"), String.class));
            String viewName = row.get(field(name("view_name"), String.class));
            if (refreshView(schema, viewName)) {
                refreshed++;
            }
        }

        log.info("Materialized view refresh completed: {}/{} views refreshed", refreshed, matViews.size());
    }

    /**
     * Refreshes a specific materialized view by name.
     * Uses CONCURRENTLY to avoid blocking readers (requires a unique index on the view).
     *
     * @return true if refresh succeeded, false on error
     */
    public boolean refreshView(String schema, String viewName) {
        String fqn = schema + "." + viewName;
        try {
            long start = System.currentTimeMillis();
            dsl.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + fqn);
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Refreshed materialized view {} in {}ms", fqn, elapsed);
            return true;
        } catch (Exception e) {
            log.error("Failed to refresh materialized view {}: {}", fqn, e.getMessage());
            return false;
        }
    }
}
