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
package org.ehrbase.service.contribution;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;

/**
 * Wrapper class that contains a {@link ContributionCreateDto} with an optional associated <code>DTO</code> object
 * per {@link ContributionCreateDto#getVersions()} entry. This allows us to use for some <code>RMObject</code>s a
 * custom <code>DTO</code> implementation that can be utilized to apply additional properties or validations.
 */
public class ContributionWrapper {

    private ContributionCreateDto contributionCreateDto;

    private Map<OriginalVersion<? extends RMObject>, Object> versionsToDtos;

    public ContributionWrapper(ContributionCreateDto contributionCreateDto) {
        this.contributionCreateDto = contributionCreateDto;
        versionsToDtos = HashMap.newHashMap(contributionCreateDto.getVersions().size());
    }

    public ContributionCreateDto getContributionCreateDto() {
        return contributionCreateDto;
    }

    void registerDtoForVersion(OriginalVersion<? extends RMObject> version, Object dto) {
        if (!contributionCreateDto.getVersions().contains(version)) {
            throw new InternalServerException(
                    "Can not register a dto for a [OriginalVersion<%s>] because it does not exist"
                            .formatted(version.getPrecedingVersionUid()));
        }
        versionsToDtos.put(version, dto);
    }

    public void forEachVersion(BiConsumer<OriginalVersion<? extends RMObject>, Object> consumer) {
        contributionCreateDto.getVersions().forEach(version -> {
            Object dto = versionsToDtos.get(version);
            consumer.accept(version, dto);
        });
    }
}
