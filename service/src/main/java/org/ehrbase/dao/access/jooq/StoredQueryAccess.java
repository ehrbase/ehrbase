/*
 * Copyright (c) 2015 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.jooq;

import static org.ehrbase.jooq.pg.Tables.STORED_QUERY;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StoredQueryAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.SemVer;
import org.ehrbase.dao.access.util.StoredQueryQualifiedName;
import org.ehrbase.jooq.pg.tables.records.StoredQueryRecord;
import org.joda.time.DateTime;
import org.jooq.Condition;
import org.jooq.OrderField;
import org.jooq.SelectOrderByStep;
import org.jooq.SelectWhereStep;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * Created by Christian Chevalley on 4/20/2015.
 */
public class StoredQueryAccess extends DataAccess implements I_StoredQueryAccess {

    static final Logger log = LoggerFactory.getLogger(StoredQueryAccess.class);
    private StoredQueryRecord storedQueryRecord;

    public StoredQueryAccess(I_DomainAccess domainAccess, StoredQueryRecord queryRecord, Short sysTenant) {
        super(domainAccess);
        this.storedQueryRecord = queryRecord;
        this.storedQueryRecord.setSysTenant(sysTenant);
    }

    public StoredQueryAccess(
            I_DomainAccess domainAccess,
            String qualifiedQueryName,
            SemVer version,
            String sourceAqlText,
            Short sysTenant) {
        super(domainAccess);

        storedQueryRecord = domainAccess.getContext().newRecord(STORED_QUERY);
        storedQueryRecord.setSysTenant(sysTenant);

        StoredQueryQualifiedName qn = new StoredQueryQualifiedName(qualifiedQueryName, version);

        storedQueryRecord.setReverseDomainName(qn.reverseDomainName());
        storedQueryRecord.setSemanticId(qn.semanticId());
        storedQueryRecord.setSemver(version.toVersionString());
        storedQueryRecord.setQueryText(sourceAqlText);
        storedQueryRecord.setType("AQL");
    }

    /**
     * return null if couldn't retrieve instance with given settings
     */
    public static Optional<StoredQueryAccess> retrieveQualified(
            I_DomainAccess domainAccess, String qualifiedName, @NonNull SemVer version) {

        // Split the qualified name in fields
        StoredQueryQualifiedName qn = new StoredQueryQualifiedName(qualifiedName, version);

        SemVer semVer = qn.semVer();

        Condition condition = nameConstraint(qn);
        condition = condition.and(versionConstraint(semVer));
        var unordered = domainAccess.getContext().selectFrom(STORED_QUERY).where(condition);

        Optional<StoredQueryRecord> queryRecord;
        if (semVer.isRelease() || semVer.isPreRelease()) {
            // equals => only one result
            queryRecord = unordered.fetchOptional();
        } else {
            var ordered = unordered.orderBy(orderBySemVerStream(SortOrder.DESC).toList());
            queryRecord = ordered.limit(1).fetchOptional();
        }

        return queryRecord.map(r -> new StoredQueryAccess(domainAccess, r, r.getSysTenant()));
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

    /**
     * Retrieves list of all stored queries on the system matched by qualifiedQueryName as pattern.
     * If pattern should be given in the format of [{namespace}::]{query-name},
     * and when is empty, it will be treated as "wildcard" in the search.
     */
    public static List<StoredQueryAccess> retrieveQualifiedList(
            I_DomainAccess domainAccess, String qualifiedQueryName) {
        SelectWhereStep<StoredQueryRecord> selection = domainAccess.getContext().selectFrom(STORED_QUERY);

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
            return stream.map(r -> new StoredQueryAccess(domainAccess, r, r.getSysTenant()))
                    .toList();
        }
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

    @Override
    public StoredQueryAccess commit(Timestamp transactionTime) {
        storedQueryRecord.setCreationDate(transactionTime);
        storedQueryRecord.store();
        return this;
    }

    @Override
    public StoredQueryAccess commit() {
        return commit(new Timestamp(DateTime.now().getMillis()));
    }

    @Override
    public Boolean update(Timestamp transactionTime) {

        storedQueryRecord.setCreationDate(transactionTime);

        if (storedQueryRecord.changed()) {
            return storedQueryRecord.update() > 0;
        }

        return false;
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        return update(transactionTime);
    }

    @Override
    public Integer delete() {
        return storedQueryRecord.delete();
    }

    @Override
    public String getReverseDomainName() {
        return storedQueryRecord.getReverseDomainName();
    }

    @Override
    public String getSemanticId() {
        return storedQueryRecord.getSemanticId();
    }

    @Override
    public String getSemver() {
        return storedQueryRecord.getSemver();
    }

    @Override
    public String getQueryText() {
        return storedQueryRecord.getQueryText();
    }

    @Override
    public void setQueryText(String queryText) {
        storedQueryRecord.setQueryText(queryText);
    }

    @Override
    public Timestamp getCreationDate() {
        return storedQueryRecord.getCreationDate();
    }

    @Override
    public String getQueryType() {
        return storedQueryRecord.getType();
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
