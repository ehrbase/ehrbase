/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.plugin.dto;

import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Wrapper to Identifier a composition by <code>compositionId</code> {@link UUID} , in version
 * <code>version</code>, null means latest, containing ehr with ehrId {@link UUID}
 *
 * @author Stefan Spiska
 */
public class CompositionIdWithVersionAndEhrId {

    private final UUID ehrId;
    private final UUID compositionId;
    private final Integer version;

    public CompositionIdWithVersionAndEhrId(UUID ehrId, UUID compositionId, @Nullable Integer version) {
        this.ehrId = ehrId;
        this.compositionId = compositionId;
        this.version = version;
    }

    public UUID getEhrId() {
        return ehrId;
    }

    public UUID getCompositionId() {
        return compositionId;
    }

    public Integer getVersion() {
        return version;
    }

    public boolean isLatestVersion() {
        return version == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositionIdWithVersionAndEhrId that = (CompositionIdWithVersionAndEhrId) o;
        return Objects.equals(ehrId, that.ehrId)
                && Objects.equals(compositionId, that.compositionId)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ehrId, compositionId, version);
    }
}
