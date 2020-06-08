/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.response.ehrscape;


import com.nedap.archie.rm.composition.Composition;

import java.util.UUID;

public class CompositionDto {
    private final Composition composition;
    private final String templateId;
    private final UUID uuid;
    private final UUID ehrId;

    public CompositionDto(
            Composition composition,
            String templateId,
            UUID uuid,
            UUID ehrId
    ) {
        this.composition = composition;
        this.templateId = templateId;
        this.uuid = uuid;
        this.ehrId = ehrId;
    }

    public Composition getComposition() {
        return composition;
    }

    public String getTemplateId() {
        return templateId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getEhrId() { return ehrId; }
}
