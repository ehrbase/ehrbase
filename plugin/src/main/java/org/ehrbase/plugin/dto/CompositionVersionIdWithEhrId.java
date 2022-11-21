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

import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.Objects;
import java.util.UUID;

/**
 * Wrapper for composition version {@link ObjectVersionId} ehrId {@link UUID}
 *
 * @author Stefan Spiska
 */
public class CompositionVersionIdWithEhrId {

    private final UUID ehrId;
    private final ObjectVersionId versionId;

    public CompositionVersionIdWithEhrId(ObjectVersionId versionId, UUID ehrId) {
        this.ehrId = ehrId;
        this.versionId = versionId;
    }

    public UUID getEhrId() {
        return ehrId;
    }

    public ObjectVersionId getVersionId() {
        return versionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositionVersionIdWithEhrId that = (CompositionVersionIdWithEhrId) o;
        return Objects.equals(ehrId, that.ehrId) && Objects.equals(versionId, that.versionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ehrId, versionId);
    }

    @Override
    public String toString() {
        return "CompositionVersionIdWithEhrId{" + "ehrId=" + ehrId + ", versionId=" + versionId + '}';
    }
}
