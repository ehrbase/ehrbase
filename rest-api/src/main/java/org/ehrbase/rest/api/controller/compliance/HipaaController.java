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
package org.ehrbase.rest.api.controller.compliance;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.rest.api.controller.BaseApiController;
import org.ehrbase.service.RequestContext;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for HIPAA compliance.
 * Provides accounting of disclosures — who accessed which patient data and when.
 */
@RestController
@RequestMapping("/api/v2/hipaa")
@Tag(name = "HIPAA Compliance", description = "Accounting of disclosures per patient")
@ConditionalOnProperty(name = "ehrbase.features.hipaa", havingValue = "true")
public class HipaaController extends BaseApiController {

    private final DSLContext dsl;
    private final EhrService ehrService;
    private final RequestContext requestContext;

    public HipaaController(DSLContext dsl, EhrService ehrService, RequestContext requestContext) {
        this.dsl = dsl;
        this.ehrService = ehrService;
        this.requestContext = requestContext;
    }

    @GetMapping(value = "/accounting/{ehr_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Accounting of disclosures",
            description = "Returns all access events for a patient. Filterable by date range, actor, action.")
    public ResponseEntity<List<Map<String, Object>>> getAccountingOfDisclosures(
            @PathVariable("ehr_id") String ehrIdStr,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "actor", required = false) String actor,
            @RequestParam(value = "action", required = false) String action) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        var query = dsl.select()
                .from(table(name("ehr_views", "v_access_log")))
                .where(field(name("ehr_id"), UUID.class).eq(ehrId));

        if (actor != null && !actor.isBlank()) {
            query = query.and(field(name("accessor_id"), String.class).eq(actor));
        }
        if (action != null && !action.isBlank()) {
            query = query.and(field(name("action"), String.class).eq(action));
        }

        List<Map<String, Object>> disclosures = query
                .orderBy(field(name("access_time")).desc())
                .fetch()
                .stream()
                .map(Record::intoMap)
                .toList();

        return ResponseEntity.ok(disclosures);
    }
}
