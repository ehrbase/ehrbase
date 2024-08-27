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
package org.ehrbase.service.maping;

import com.nedap.archie.rm.ehr.EhrStatus;
import org.ehrbase.api.dto.EhrStatusDto;

public class EhrStatusMapper {

    private EhrStatusMapper() {}

    /**
     *  Mapping of archi {@link EhrStatus} to {@link EhrStatusDto}.
     * @param ehrStatus archi {@link EhrStatus} to map
     * @return {@link EhrStatusDto}
     */
    public static EhrStatusDto toDto(EhrStatus ehrStatus) {
        return new EhrStatusDto(
                ehrStatus.getUid(),
                ehrStatus.getArchetypeNodeId(),
                ehrStatus.getName(),
                ehrStatus.getArchetypeDetails(),
                ehrStatus.getFeederAudit(),
                ehrStatus.getSubject(),
                ehrStatus.isQueryable(),
                ehrStatus.isModifiable(),
                ehrStatus.getOtherDetails());
    }

    /**
     * Mapping of {@link EhrStatusDto} to archi {@link EhrStatus}.
     * @param dto {@link EhrStatusDto} to map
     * @return {@link EhrStatus}
     */
    public static EhrStatus fromDto(EhrStatusDto dto) {
        return new EhrStatus(
                dto.uid(),
                dto.archetypeNodeId(),
                dto.name(),
                dto.archetypeDetails(),
                dto.feederAudit(),
                null,
                null,
                null,
                dto.subject(),
                dto.isQueryable(),
                dto.isModifiable(),
                dto.otherDetails());
    }
}
