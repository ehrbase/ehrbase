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
package org.ehrbase.util;

import com.nedap.archie.rm.changecontrol.OriginalVersion;

public class OriginalVersionUtil {

    private OriginalVersionUtil() {}

    /**
     * Creates copy of the given <code>inputVersion</code> with given <code>OutputType</code>.
     *
     * @param inputVersion to create a copy of
     * @param outputData   used as the output {@link OriginalVersion#getData()}
     * @return {@link OriginalVersion} with <code>outputData</code>
     * @param <I> of the input {@link OriginalVersion#getData()}
     * @param <O> for the output  {@link OriginalVersion#getData()}
     */
    public static <I, O> OriginalVersion<O> originalVersionCopyWithData(OriginalVersion<I> inputVersion, O outputData) {
        return new OriginalVersion<>(
                inputVersion.getUid(),
                inputVersion.getPrecedingVersionUid(),
                outputData,
                inputVersion.getLifecycleState(),
                inputVersion.getCommitAudit(),
                inputVersion.getContribution(),
                inputVersion.getSignature(),
                inputVersion.getOtherInputVersionUids(),
                inputVersion.getAttestations());
    }
}
