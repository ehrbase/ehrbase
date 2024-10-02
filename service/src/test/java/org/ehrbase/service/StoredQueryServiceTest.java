/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.StoredQueryService;
import org.ehrbase.cache.CacheProvider;
import org.ehrbase.cache.CacheProviderImp;
import org.ehrbase.jooq.pg.tables.records.StoredQueryRecord;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.QueryDefinitionResultDto;
import org.ehrbase.repository.StoredQueryRepository;
import org.ehrbase.util.SemVer;
import org.ehrbase.util.StoredQueryQualifiedName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

public class StoredQueryServiceTest {

    private final StoredQueryRepository mockStoredQueryRepository = mock("Mock Repository");
    private SimpleCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        Mockito.reset(mockStoredQueryRepository);

        cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(new ConcurrentMapCache(CacheProvider.STORED_QUERY_CACHE.name())));
        cacheManager.initializeCaches();
    }

    private StoredQueryService service(StoredQueryRecord... records) {
        // mock responses
        for (StoredQueryRecord record : records) {
            StoredQueryQualifiedName storedQueryQualifiedName =
                    StoredQueryQualifiedName.create(record.getReverseDomainName(), SemVer.parse(record.getSemver()));
            doReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record)))
                    .when(mockStoredQueryRepository)
                    .retrieveQualified(storedQueryQualifiedName);
        }

        var cacheProvider = new CacheProviderImp(cacheManager);
        return new StoredQueryServiceImp(mockStoredQueryRepository, cacheProvider);
    }

    @Test
    void createStoredQueryNew() {

        StoredQueryRecord record = new StoredQueryRecord(
                "test::crate", "id", "0.5.0", "SELECT es FROM EHR_STATUS es", "test", OffsetDateTime.now());

        when(mockStoredQueryRepository.retrieveQualified(any()))
                .thenReturn(Optional.empty()) // #1 call nothing stored
                .thenReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record))); // #2 call final result

        QueryDefinitionResultDto result =
                service().createStoredQuery("test::name", "0.5.0", "SELECT es FROM EHR_STATUS es");
        assertEquals("test::crate::id", result.getQualifiedName());
        assertEquals("0.5.0", result.getVersion());
        assertEquals("test", result.getType());
        assertEquals("SELECT es FROM EHR_STATUS es", result.getQueryText());
        assertEquals(
                record.getCreationDate().atZoneSameInstant(ZoneOffset.UTC),
                result.getSaved().toOffsetDateTime().atZoneSameInstant(ZoneOffset.UTC));
    }

    @Test
    void createStoredQueryFailVersionAlreadyExists() {

        StoredQueryRecord record = new StoredQueryRecord(
                "test::crate", "id", "0.5.0", "SELECT es FROM EHR_STATUS es", "test", OffsetDateTime.now());

        when(mockStoredQueryRepository.retrieveQualified(any()))
                .thenReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record)));

        StateConflictException reason = assertThrows(StateConflictException.class, () -> service()
                .createStoredQuery("test::name", "0.5.0", "SELECT es FROM EHR_STATUS es"));
        assertEquals("Version already exists", reason.getMessage());
    }

    @Test
    void createStoredQueryFailPartialVersion() {

        StoredQueryRecord record = new StoredQueryRecord(
                "test::crate", "id", "0.5", "SELECT es FROM EHR_STATUS es", "test", OffsetDateTime.now());

        when(mockStoredQueryRepository.retrieveQualified(any()))
                .thenReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record)));

        IllegalStateException reason = assertThrows(IllegalStateException.class, () -> service()
                .createStoredQuery("test::name", "0.3.0", "SELECT es FROM EHR_STATUS es"));
        assertEquals("The database contains stored queries with partial versions", reason.getMessage());
    }

    @Test
    void deleteStoredQuery() {

        service().deleteStoredQuery("test::delete", "1.0.0");
        StoredQueryQualifiedName storedQueryQualifiedName =
                StoredQueryQualifiedName.create("test::delete", SemVer.parse("1.0.0"));

        verify(mockStoredQueryRepository).delete(storedQueryQualifiedName);
    }

    @Test
    void deleteStoredQueryEvictCache() {

        StoredQueryQualifiedName storedQueryQualifiedName =
                StoredQueryQualifiedName.create("test::delete", SemVer.parse("1.0.0"));

        cacheManager
                .getCache(CacheProvider.STORED_QUERY_CACHE.name())
                .put(storedQueryQualifiedName.toQualifiedNameString(), new Object());

        service().deleteStoredQuery("test::delete", "1.0.0");

        assertNull(cacheManager
                .getCache(CacheProvider.STORED_QUERY_CACHE.name())
                .get(storedQueryQualifiedName.toQualifiedNameString()));
    }

    @Test
    void retrieveStoredQuery() {

        StoredQueryRecord record = new StoredQueryRecord(
                "test::name", "id", "1.0.0", "SELECT es FROM EHR_STATUS es", "test", OffsetDateTime.now());

        StoredQueryService service = service(record);

        QueryDefinitionResultDto result = service.retrieveStoredQuery("test::name", "1.0.0");
        assertEquals("test::name::id", result.getQualifiedName());
        assertEquals("1.0.0", result.getVersion());
        assertEquals("test", result.getType());
        assertEquals("SELECT es FROM EHR_STATUS es", result.getQueryText());
        assertEquals(
                record.getCreationDate().atZoneSameInstant(ZoneOffset.UTC),
                result.getSaved().toOffsetDateTime().atZoneSameInstant(ZoneOffset.UTC));
    }

    @Test
    void retrieveStoredQueryDoesNotExist() {

        StoredQueryService service = service();

        String message = assertThrows(
                        ObjectNotFoundException.class, () -> service.retrieveStoredQuery("test::cached", "1.4.2"))
                .getMessage();
        assertThat(message).isEqualTo("Could not retrieve stored query for qualified name: test::cached");
    }

    @Test
    void retrieveStoredQueryCached() {

        StoredQueryService service = service(new StoredQueryRecord(
                "test::cached", "id", "1.4.2", "SELECT es FROM EHR_STATUS es", "test", OffsetDateTime.now()));

        QueryDefinitionResultDto result = service.retrieveStoredQuery("test::cached", "1.4.2");
        QueryDefinitionResultDto result2 = service.retrieveStoredQuery("test::cached", "1.4.2");

        assertSame(result, result2, "Expected result to be cached");
        assertSame(
                result,
                cacheManager
                        .getCache(CacheProvider.STORED_QUERY_CACHE.name())
                        .get("test::cached/1.4.2")
                        .get());
    }

    @Test
    void retrieveStoredQueryPartial() {

        StoredQueryQualifiedName v05 = StoredQueryQualifiedName.create("test::name", SemVer.parse("0.5"));
        StoredQueryQualifiedName v050 = StoredQueryQualifiedName.create("test::name", SemVer.parse("0.5.0"));
        StoredQueryQualifiedName v051 = StoredQueryQualifiedName.create("test::name", SemVer.parse("0.5.1"));

        StoredQueryRecord record = new StoredQueryRecord(
                "test::crate",
                "id",
                v050.semVer().toVersionString(),
                "SELECT es FROM EHR_STATUS es",
                "test",
                OffsetDateTime.now());

        StoredQueryRecord record2 = new StoredQueryRecord(
                "test::crate",
                "id",
                v051.semVer().toVersionString(),
                "SELECT es FROM EHR_STATUS es",
                "test",
                OffsetDateTime.now());

        StoredQueryService service = service();

        QueryDefinitionResultDto result;

        // #1 create version 0.5.0

        // create and access using full version
        when(mockStoredQueryRepository.retrieveQualified(v050))
                .thenReturn(Optional.empty()) // #1 call nothing stored
                .thenReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record))); // #2 call find

        result = service.createStoredQuery("test::name", "0.5.0", "SELECT es FROM EHR_STATUS es");
        assertThat(result.getVersion()).isEqualTo("0.5.0");

        // Access using partial version
        when(mockStoredQueryRepository.retrieveQualified(v05))
                .thenReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record)));

        result = service.retrieveStoredQuery("test::name", "0.5");
        assertThat(result.getVersion()).isEqualTo("0.5.0");

        // #2 create version 0.5.1

        // create and access using full version
        when(mockStoredQueryRepository.retrieveQualified(v051))
                .thenReturn(Optional.empty()) // #1 call nothing stored
                .thenReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record2))); // #2 call find

        result = service.createStoredQuery("test::name", "0.5.1", "SELECT es FROM EHR_STATUS es");
        assertThat(result.getVersion()).isEqualTo("0.5.1");

        when(mockStoredQueryRepository.retrieveQualified(any()))
                .thenReturn(Optional.of(StoredQueryRepository.mapToQueryDefinitionDto(record2)));

        result = service.retrieveStoredQuery("test::name", "0.5");
        assertThat(result.getVersion()).isEqualTo("0.5.1");
    }

    @Test
    void retrieveStoredQueriesEmpty() {

        when(mockStoredQueryRepository.retrieveQualifiedList("test:query")).thenReturn(List.of());

        assertEquals(0, service().retrieveStoredQueries("test::query").size());
    }

    @Test
    void retrieveStoredQueries() {

        when(mockStoredQueryRepository.retrieveQualifiedList("test::query"))
                .thenReturn(List.of(new QueryDefinitionResultDto()));

        assertEquals(1, service().retrieveStoredQueries("test::query").size());
    }
}
