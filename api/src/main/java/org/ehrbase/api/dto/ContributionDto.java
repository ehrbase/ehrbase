/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.api.dto;

import com.nedap.archie.rm.generic.AuditDetails;

import java.util.Map;
import java.util.UUID;

public class ContributionDto {
    private final UUID uuid;
    private final Map<String, String> objectReferences;
    private final AuditDetails auditDetails;

    public ContributionDto(UUID uuid, Map<String, String> objectReferences, AuditDetails auditDetails) {
        this.uuid = uuid;
        this.objectReferences = objectReferences;
        this.auditDetails = auditDetails;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, String> getObjectReferences() {
        return objectReferences;
    }

    public AuditDetails getAuditDetails() {
        return auditDetails;
    }
}
