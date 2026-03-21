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
package org.ehrbase.repository.composition;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Metadata from {@code ehr_system.composition} used to reconstruct a full RM Composition.
 * Separated from clinical data which lives in {@code ehr_data} tables.
 */
public record CompositionMetadata(
        UUID id,
        UUID ehrId,
        UUID templateId,
        String archetypeId,
        String templateName,
        String composerName,
        @Nullable String composerId,
        @Nullable String language,
        @Nullable String territory,
        @Nullable String categoryCode,
        @Nullable String feederAudit,
        @Nullable String participations,
        int sysVersion,
        @Nullable UUID contributionId,
        String changeType,
        OffsetDateTime committedAt,
        String committerName,
        @Nullable String committerId,
        short sysTenant) {}
