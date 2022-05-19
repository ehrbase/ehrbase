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

import com.nedap.archie.rm.ehr.EhrStatus;
import java.util.Objects;
import java.util.UUID;

/**
 * Wrapper for an Ehr with <code>ehrId</code> {@link UUID} and <code>ehrStatus</code> {@link
 * EhrStatus}
 *
 * @author Stefan Spiska
 */
public class EhrStatusVersionRequestParameters {

    private final UUID ehrId;
    private final UUID ehrStatusId;
    private final int ehrStatusVersion;

    public EhrStatusVersionRequestParameters(UUID ehrId, UUID ehrStatusId, int ehrStatusVersion) {
        this.ehrStatusId = ehrStatusId;
        this.ehrStatusVersion = ehrStatusVersion;
        this.ehrId = ehrId;
    }

    public UUID getEhrId() {
        return ehrId;
    }

    public UUID getEhrStatusId() {
        return ehrStatusId;
    }

    public int getEhrStatusVersion() {
        return ehrStatusVersion;
    }

    @Override
    public String toString() {
        return "EhrStatusVersionRequestParameters{" + "ehrStatusId=" + ehrStatusId + ", version=" + ehrStatusVersion
                + ", ehrid=" + ehrId + '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EhrStatusVersionRequestParameters that = (EhrStatusVersionRequestParameters) o;
        return Objects.equals(ehrStatusId, that.ehrStatusId)
                && Objects.equals(ehrStatusVersion, that.ehrStatusVersion)
                && Objects.equals(ehrId, that.ehrId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ehrStatusId, ehrStatusVersion, ehrId);
    }
}
