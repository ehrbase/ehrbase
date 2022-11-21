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
import java.util.Objects;
import java.util.UUID;

/**
 * Wrapper for {@link com.nedap.archie.rm.composition.Composition} with ehrId {@link UUID}
 *
 * @author Stefan Spiska
 */
public class CompositionWithEhrId {

    private final Composition composition;
    private final UUID ehrId;

    public CompositionWithEhrId(Composition composition, UUID ehrId) {
        this.composition = composition;
        this.ehrId = ehrId;
    }

    public Composition getComposition() {
        return composition;
    }

    public UUID getEhrId() {
        return ehrId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositionWithEhrId that = (CompositionWithEhrId) o;
        return Objects.equals(composition, that.composition) && Objects.equals(ehrId, that.ehrId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composition, ehrId);
    }

    @Override
    public String toString() {
        return "CompositionMergeInput{" + "composition=" + composition + ", ehrId=" + ehrId + '}';
    }
}
