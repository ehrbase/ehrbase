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

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.Objects;
import java.util.UUID;

/**
 * Wrapper for {@link com.nedap.archie.rm.composition.Composition} with ehrId {@link UUID} and
 * previous version {@link ObjectVersionId}
 *
 * @author Stefan Spiska
 */
public class CompositionWithEhrIdAndPreviousVersion extends CompositionWithEhrId {

    private final ObjectVersionId previousVersion;

    public CompositionWithEhrIdAndPreviousVersion(
            Composition composition, ObjectVersionId previousVersion, UUID ehrId) {
        super(composition, ehrId);
        this.previousVersion = previousVersion;
    }

    public ObjectVersionId getPreviousVersion() {
        return previousVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CompositionWithEhrIdAndPreviousVersion that = (CompositionWithEhrIdAndPreviousVersion) o;
        return Objects.equals(previousVersion, that.previousVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), previousVersion);
    }

    @Override
    public String toString() {
        return "CompositionWithEhrIdAndPreviousVersion{"
                + "previousVersion="
                + previousVersion
                + "} "
                + super.toString();
    }
}
