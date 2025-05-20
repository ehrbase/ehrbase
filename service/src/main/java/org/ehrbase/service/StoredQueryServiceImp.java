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

import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.GeneralRequestProcessingException;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.openehr.sdk.aql.parser.AqlParseException;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.repository.StoredQueryRepository;
import org.ehrbase.util.InvalidVersionFormatException;
import org.ehrbase.util.SemVer;
import org.ehrbase.util.SemVerUtil;
import org.ehrbase.util.StoredQueryQualifiedName;
import org.ehrbase.util.VersionConflictException;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

@Service
public class StoredQueryServiceImp implements StoredQueryService {

    private final StoredQueryRepository storedQueryRepository;
    private final CacheProvider cacheProvider;

    @Value("${ehrbase.cache.stored-query-init-on-startup:false}")
    private boolean initStoredQueryCache = false;

    public StoredQueryServiceImp(StoredQueryRepository storedQueryRepository, CacheProvider cacheProvider) {

        this.storedQueryRepository = storedQueryRepository;
        this.cacheProvider = cacheProvider;
    }

    @PostConstruct
    public void init() {
        if (initStoredQueryCache) {
            storedQueryRepository.retrieveAllLatest().forEach(l -> {
                SemVerUtil.streamAllResolutions(SemVer.parse(l.getVersion())).forEach(v -> {
                    StoredQueryQualifiedName storedQueryQualifiedName =
                            StoredQueryQualifiedName.create(l.getQualifiedName(), v);
                    CacheProvider.STORED_QUERY_CACHE.get(
                            cacheProvider, storedQueryQualifiedName.toQualifiedNameString(), () -> l);
                });
            });
        }
    }

    // === DEFINITION: manage stored queries
    @Override
    public List<QueryDefinitionResultDto> retrieveStoredQueries(String fullyQualifiedName) {
        String name = StringUtils.defaultIfEmpty(fullyQualifiedName, null);
        try {
            return storedQueryRepository.retrieveQualifiedList(name);

        } catch (DataAccessException e) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + e.getCause().getMessage(), e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Could not retrieve stored query, reason: " + e, e);
        }
    }

    @Override
    public QueryDefinitionResultDto retrieveStoredQuery(String qualifiedName, String version) {

        SemVer requestedVersion = parseRequestSemVer(version);
        StoredQueryQualifiedName storedQueryQualifiedName =
                StoredQueryQualifiedName.create(qualifiedName, requestedVersion);

        QueryDefinitionResultDto result;
        try {
            result = CacheProvider.STORED_QUERY_CACHE.get(
                    cacheProvider,
                    storedQueryQualifiedName.toQualifiedNameString(),
                    () -> retrieveStoredQueryInternal(storedQueryQualifiedName));
        } catch (Cache.ValueRetrievalException e) {
            throw e.getCause() instanceof RuntimeException cause ? cause : e;
        }
        return result;
    }

    private QueryDefinitionResultDto retrieveStoredQueryInternal(StoredQueryQualifiedName storedQueryQualifiedName) {

        Optional<QueryDefinitionResultDto> storedQueryAccess;
        try {
            storedQueryAccess = storedQueryRepository.retrieveQualified(storedQueryQualifiedName);
        } catch (DataAccessException e) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + e.getCause().getMessage(), e);
        } catch (RuntimeException e) {
            throw new InternalServerException(e.getMessage());
        }

        return storedQueryAccess.orElseThrow(() -> new ObjectNotFoundException(
                "QUERY", "Could not retrieve stored query for qualified name: " + storedQueryQualifiedName.toName()));
    }

    @Override
    public QueryDefinitionResultDto createStoredQuery(String qualifiedName, String version, String queryString) {

        SemVer requestedVersion = parseRequestSemVer(version);
        StoredQueryQualifiedName queryQualifiedName = StoredQueryQualifiedName.create(qualifiedName, requestedVersion);

        // validate the query syntax
        try {
            AqlQueryParser.parse(queryString);
        } catch (AqlParseException e) {
            throw new IllegalArgumentException("Invalid query, reason:" + e, e);
        }

        // lookup version in db
        SemVer dbSemVer = storedQueryRepository
                .retrieveQualified(queryQualifiedName)
                .map(q -> SemVer.parse(q.getVersion()))
                .orElse(SemVer.NO_VERSION);

        checkVersionCombination(requestedVersion, dbSemVer);

        SemVer newVersion = SemVerUtil.determineVersion(requestedVersion, dbSemVer);
        StoredQueryQualifiedName newQueryQualifiedName = StoredQueryQualifiedName.create(qualifiedName, newVersion);

        // if not final version and already existing: update
        boolean isUpdate = dbSemVer.isPreRelease();

        try {
            if (isUpdate) {
                storedQueryRepository.update(newQueryQualifiedName, queryString);
            } else {
                storedQueryRepository.store(newQueryQualifiedName, queryString);
            }
        } catch (DataAccessException e) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error: " + e.getCause().getMessage(), e);
        } catch (VersionConflictException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        // clear partially cached versions
        evictAllResolutions(newQueryQualifiedName);

        return retrieveStoredQueryInternal(newQueryQualifiedName);
    }

    private static void checkVersionCombination(SemVer requestSemVer, SemVer dbSemVer) {
        if (dbSemVer.isNoVersion()) {
            // NOOP: no issue
        } else if (dbSemVer.isPartial()) {
            throw new IllegalStateException("The database contains stored queries with partial versions");

        } else if (dbSemVer.isPreRelease()) {
            if (!requestSemVer.isPreRelease()) {
                throw new RuntimeException(
                        "Pre-release " + dbSemVer + " was provided when " + requestSemVer + " was requested");
            }
        } else {
            // release
            if (requestSemVer.isPreRelease()) {
                throw new RuntimeException(
                        "Version " + dbSemVer + " was provided when pre-release " + requestSemVer + " was requested");

            } else if (requestSemVer.isRelease()) {
                throw new StateConflictException("Version already exists");
            }
        }
    }

    @Override
    public void deleteStoredQuery(String qualifiedName, String version) {

        SemVer requestedVersion = parseRequestSemVer(version);
        if (requestedVersion.isNoVersion() || requestedVersion.isPartial()) {
            throw new InvalidApiParameterException("A qualified version has to be specified");
        }

        StoredQueryQualifiedName storedQueryQualifiedName =
                StoredQueryQualifiedName.create(qualifiedName, requestedVersion);

        try {
            storedQueryRepository.delete(storedQueryQualifiedName);

        } catch (ObjectNotFoundException e) {
            throw e;
        } catch (DataAccessException dae) {
            throw new GeneralRequestProcessingException(
                    "Data Access Error:" + dae.getCause().getMessage());
        } catch (RuntimeException e) {
            throw new InternalServerException(e.getMessage());
        } finally {
            evictAllResolutions(storedQueryQualifiedName);
        }
    }

    private void evictAllResolutions(StoredQueryQualifiedName qualifiedName) {
        SemVerUtil.streamAllResolutions(qualifiedName.semVer())
                .forEach(v -> CacheProvider.STORED_QUERY_CACHE.evict(
                        cacheProvider,
                        new StoredQueryQualifiedName(qualifiedName.reverseDomainName(), qualifiedName.semanticId(), v)
                                .toQualifiedNameString()));
    }

    private static SemVer parseRequestSemVer(String version) {
        try {
            return SemVer.parse(version);
        } catch (InvalidVersionFormatException e) {
            throw new InvalidApiParameterException("Incorrect version. Use the SEMVER format.", e);
        }
    }
}
