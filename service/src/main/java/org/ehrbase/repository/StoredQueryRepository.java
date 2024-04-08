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
package org.ehrbase.repository;

import static org.ehrbase.jooq.pg.Tables.STORED_QUERY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.jooq.pg.tables.records.StoredQueryRecord;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.service.TimeProvider;
import org.ehrbase.util.SemVer;
import org.ehrbase.util.StoredQueryQualifiedName;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.SelectOrderByStep;
import org.jooq.SelectWhereStep;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class StoredQueryRepository {

    private final DSLContext context;
    private final TimeProvider timeProvider;

    public StoredQueryRepository(DSLContext context, TimeProvider timeProvider) {
        this.context = context;
        this.timeProvider = timeProvider;
    }

    public void store(String qualifiedQueryName, SemVer version, String sourceAqlText) {

        StoredQueryRecord storedQueryRecord = createStoredQueryRecord(qualifiedQueryName, version, sourceAqlText);
        storedQueryRecord.insert();
    }

    public void update(String qualifiedQueryName, SemVer version, String sourceAqlText) {

        StoredQueryRecord storedQueryRecord = createStoredQueryRecord(qualifiedQueryName, version, sourceAqlText);
        storedQueryRecord.update();
    }

    public Optional<StoredQueryRecord> retrieveQualified(String qualifiedName, @NonNull SemVer version) {

        // Split the qualified name in fields
        StoredQueryQualifiedName qn = new StoredQueryQualifiedName(qualifiedName, version);

        SemVer semVer = qn.semVer();

        Condition condition = nameConstraint(qn);
        condition = condition.and(versionConstraint(semVer));
        var unordered = context.selectFrom(STORED_QUERY).where(condition);

        Optional<StoredQueryRecord> queryRecord;
        if (semVer.isRelease() || semVer.isPreRelease()) {
            // equals => only one result
            queryRecord = unordered.fetchOptional();
        } else {
            var ordered = unordered.orderBy(orderBySemVerStream(SortOrder.DESC).toList());
            queryRecord = ordered.limit(1).fetchOptional();
        }

        return queryRecord;
    }

    public void delete(String qualifiedName, @NonNull SemVer version) {

        StoredQueryRecord storedQuery = retrieveQualified(qualifiedName, version)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "STORED_QUERY", "No Stored Query with %s and %s".formatted(qualifiedName, version)));

        storedQuery.delete();
    }

    public List<QueryDefinitionResultDto> retrieveQualifiedList(String qualifiedQueryName) {
        SelectWhereStep<StoredQueryRecord> selection = context.selectFrom(STORED_QUERY);

        // Determine {query-name} and {namespace}
        String semanticId;
        String reverseDomainName;
        if (StringUtils.isEmpty(qualifiedQueryName)) {
            semanticId = null;
            reverseDomainName = null;
        } else {
            // qualifiedQueryName is optional
            int pos = qualifiedQueryName.indexOf("::");
            if (pos < 0) {
                reverseDomainName = null;
                semanticId = qualifiedQueryName;
            } else {
                reverseDomainName = StringUtils.defaultIfEmpty(qualifiedQueryName.substring(0, pos), null);
                semanticId = StringUtils.defaultIfEmpty(qualifiedQueryName.substring(pos + 2), null);
            }
        }

        List<Condition> constraints = new ArrayList<>();
        List<OrderField<?>> orderFields = new ArrayList<>();

        if (reverseDomainName == null) {
            orderFields.add(STORED_QUERY.REVERSE_DOMAIN_NAME.sort(SortOrder.ASC));
        } else {
            constraints.add(STORED_QUERY.REVERSE_DOMAIN_NAME.eq(reverseDomainName));
        }

        if (semanticId == null) {
            orderFields.add(STORED_QUERY.SEMANTIC_ID.sort(SortOrder.ASC));
        } else {
            constraints.add(STORED_QUERY.SEMANTIC_ID.eq(semanticId));
        }

        SelectOrderByStep<StoredQueryRecord> unordered;
        if (constraints.isEmpty()) {
            unordered = selection;
        } else {
            unordered = selection.where(constraints);
        }

        var sortOrder = Stream.concat(orderFields.stream(), orderBySemVerStream(SortOrder.DESC))
                .toList();

        try (Stream<StoredQueryRecord> stream = unordered.orderBy(sortOrder).stream()) {
            return stream.map(StoredQueryRepository::mapToQueryDefinitionDto).toList();
        }
    }

    public static QueryDefinitionResultDto mapToQueryDefinitionDto(StoredQueryRecord storedQueryAccess) {
        QueryDefinitionResultDto dto = new QueryDefinitionResultDto();
        dto.setSaved(storedQueryAccess.getCreationDate().toZonedDateTime());
        dto.setQualifiedName(storedQueryAccess.getReverseDomainName() + "::" + storedQueryAccess.getSemanticId());
        dto.setVersion(storedQueryAccess.getSemver());
        dto.setQueryText(storedQueryAccess.getQueryText());
        dto.setType(storedQueryAccess.getType());
        return dto;
    }

    private static @NonNull Condition versionConstraint(SemVer semVer) {
        if (semVer.isRelease() || semVer.isPreRelease()) {
            return STORED_QUERY.SEMVER.eq(semVer.toVersionString());
        }
        Condition noPreRelease = STORED_QUERY.SEMVER.notContains("-");
        if (semVer.isNoVersion()) {
            return noPreRelease;
        }
        Condition prefixMatch = STORED_QUERY.SEMVER.like(semVer.toVersionString() + ".%");
        return prefixMatch.and(noPreRelease);
    }

    private static Condition nameConstraint(StoredQueryQualifiedName storedQueryQualifiedName) {
        return STORED_QUERY
                .REVERSE_DOMAIN_NAME
                .eq(storedQueryQualifiedName.reverseDomainName())
                .and(STORED_QUERY.SEMANTIC_ID.eq(storedQueryQualifiedName.semanticId()));
    }

    private static Stream<OrderField<?>> orderBySemVerStream(SortOrder sortOrder) {
        return Stream.of(
                DSL.splitPart(STORED_QUERY.SEMVER, ".", 1).cast(Integer.class).sort(sortOrder),
                DSL.splitPart(STORED_QUERY.SEMVER, ".", 2).cast(Integer.class).sort(sortOrder),
                DSL.splitPart(DSL.splitPart(STORED_QUERY.SEMVER, ".", 3), "-", 1)
                        .cast(Integer.class)
                        .sort(sortOrder),
                DSL.splitPart(DSL.splitPart(STORED_QUERY.SEMVER, ".", 3), "-", 2)
                        .sort(sortOrder));
    }

    private StoredQueryRecord createStoredQueryRecord(String qualifiedQueryName, SemVer version, String sourceAqlText) {
        StoredQueryRecord storedQueryRecord = context.newRecord(STORED_QUERY);

        StoredQueryQualifiedName qn = new StoredQueryQualifiedName(qualifiedQueryName, version);

        storedQueryRecord.setReverseDomainName(qn.reverseDomainName());
        storedQueryRecord.setSemanticId(qn.semanticId());
        storedQueryRecord.setSemver(version.toVersionString());
        storedQueryRecord.setQueryText(sourceAqlText);
        storedQueryRecord.setType("AQL");

        storedQueryRecord.setCreationDate(timeProvider.getNow());
        return storedQueryRecord;
    }
}
