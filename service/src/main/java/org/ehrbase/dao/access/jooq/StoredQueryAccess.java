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
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_StoredQueryAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.dao.access.util.StoredQueryQualifiedName;
import org.ehrbase.jooq.pg.tables.records.StoredQueryRecord;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Christian Chevalley on 4/20/2015.
 */
public class StoredQueryAccess extends DataAccess implements I_StoredQueryAccess {

    static final Logger log = LoggerFactory.getLogger(StoredQueryAccess.class);
    private StoredQueryRecord storedQueryRecord;

    public StoredQueryAccess(I_DomainAccess domainAccess, StoredQueryRecord queryRecord, String tenantIdentifier) {
        super(domainAccess);
        this.storedQueryRecord = queryRecord;
        this.storedQueryRecord.setNamespace(tenantIdentifier);
    }

    public StoredQueryAccess(
            I_DomainAccess domainAccess, String qualifiedQueryName, String sourceAqlText, String tenantIdentifier) {
        super(domainAccess);

        storedQueryRecord = domainAccess.getContext().newRecord(STORED_QUERY);
        storedQueryRecord.setNamespace(tenantIdentifier);

        StoredQueryQualifiedName storedQueryQualifiedName = new StoredQueryQualifiedName(qualifiedQueryName);

        storedQueryRecord.setReverseDomainName(storedQueryQualifiedName.reverseDomainName());
        storedQueryRecord.setSemanticId(storedQueryQualifiedName.semanticId());
        if (storedQueryQualifiedName.isSetSemVer()) storedQueryRecord.setSemver(storedQueryQualifiedName.semVer());
        storedQueryRecord.setQueryText(sourceAqlText);
        storedQueryRecord.setType("AQL");
    }

    /**
     * @throws IllegalArgumentException if couldn't retrieve instance with given settings
     */
    public static StoredQueryAccess retrieveQualified(I_DomainAccess domainAccess, String qualifiedName) {

        // Split the qualified name in fields
        StoredQueryQualifiedName storedQueryQualifiedName = new StoredQueryQualifiedName(qualifiedName);

        String reverseDomainName = storedQueryQualifiedName.reverseDomainName();
        String semanticId = storedQueryQualifiedName.semanticId();
        String semVer = storedQueryQualifiedName.semVer();

        StoredQueryRecord queryRecord;

        // TODO: retrieve a stored query using a partial SEMVER

        if (semVer != null && !semVer.toUpperCase().equals("LATEST")) {
            // we do a seq search for query with SIMILAR to semver
            queryRecord = domainAccess
                    .getContext()
                    .selectFrom(STORED_QUERY)
                    .where(STORED_QUERY
                            .REVERSE_DOMAIN_NAME
                            .eq(reverseDomainName)
                            .and(STORED_QUERY.SEMANTIC_ID.eq(semanticId))
                            .and(STORED_QUERY.SEMVER.like(semVer + "%")))
                    .orderBy(STORED_QUERY.SEMVER.desc())
                    .limit(1)
                    .fetchOne();
        } else { // no semver specified, retrieve the latest by lexicographic order
            queryRecord = domainAccess
                    .getContext()
                    .selectFrom(STORED_QUERY)
                    .where(STORED_QUERY
                            .REVERSE_DOMAIN_NAME
                            .eq(reverseDomainName)
                            .and(STORED_QUERY.SEMANTIC_ID.eq(semanticId)))
                    .orderBy(STORED_QUERY.SEMVER.desc())
                    .limit(1)
                    .fetchOne();
        }

        if (queryRecord == null) {
            log.warn("Could not retrieve stored query for qualified name:" + qualifiedName);
            throw new IllegalArgumentException("Could not retrieve stored query for qualified name:" + qualifiedName);
        } else return new StoredQueryAccess(domainAccess, queryRecord, queryRecord.getNamespace());
    }

    /**
     * @throws IllegalArgumentException if couldn't retrieve instance with given settings
     */
    public static List<StoredQueryAccess> retrieveQualifiedList(I_DomainAccess domainAccess, String qualifiedName) {

        // Split the qualified name in fields
        StoredQueryQualifiedName storedQueryQualifiedName = new StoredQueryQualifiedName(qualifiedName);

        String reverseDomainName = storedQueryQualifiedName.reverseDomainName();
        String semanticId = storedQueryQualifiedName.semanticId();

        List<StoredQueryRecord> queryRecords;

        // TODO: retrieve a stored query using a partial SEMVER

        // no semver specified, retrieve the latest by lexicographic order
        queryRecords = domainAccess
                .getContext()
                .selectFrom(STORED_QUERY)
                .where(STORED_QUERY
                        .REVERSE_DOMAIN_NAME
                        .eq(reverseDomainName)
                        .and(STORED_QUERY.SEMANTIC_ID.eq(semanticId)))
                .orderBy(STORED_QUERY.SEMVER.desc())
                .fetch();

        List<StoredQueryAccess> storedQueryAccesses = new ArrayList<>();
        if (queryRecords == null) {
            log.warn("Could not retrieve Aql Text for qualified name:" + qualifiedName);
        } else {
            for (StoredQueryRecord storedQueryRecord : queryRecords) {
                storedQueryAccesses.add(
                        new StoredQueryAccess(domainAccess, storedQueryRecord, storedQueryRecord.getNamespace()));
            }
        }

        return storedQueryAccesses;
    }

    /**
     * retrieve the whole set of stored queries
     * @param domainAccess
     * @return
     */
    public static List<StoredQueryAccess> retrieveQualifiedList(I_DomainAccess domainAccess) {
        List<StoredQueryRecord> queryRecords;

        // no semver specified, retrieve the latest by lexicographic order
        queryRecords = domainAccess
                .getContext()
                .selectFrom(STORED_QUERY)
                .orderBy(STORED_QUERY.SEMVER.desc())
                .fetch();

        List<StoredQueryAccess> storedQueryAccesses = new ArrayList<>();
        if (queryRecords == null) {
            log.warn("Empty stored query set");
        } else {
            for (StoredQueryRecord storedQueryRecord : queryRecords) {
                storedQueryAccesses.add(
                        new StoredQueryAccess(domainAccess, storedQueryRecord, storedQueryRecord.getNamespace()));
            }
        }

        return storedQueryAccesses;
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
    public String getQualifiedName() {
        return new StoredQueryQualifiedName(
                        storedQueryRecord.getReverseDomainName(),
                        storedQueryRecord.getSemanticId(),
                        storedQueryRecord.getSemver())
                .toString();
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
