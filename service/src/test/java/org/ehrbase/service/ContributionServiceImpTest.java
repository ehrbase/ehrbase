/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.service;

import static org.junit.jupiter.api.Assertions.*;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import java.util.Arrays;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.junit.jupiter.api.Test;

class ContributionServiceImpTest {

    @Test
    void isModifiableCheckNeeded() {
        // simple cases
        checkIsModifiableCheckNeeded(false);

        checkIsModifiableCheckNeeded(true, delete());
        checkIsModifiableCheckNeeded(true, composition());
        checkIsModifiableCheckNeeded(true, directory());
        checkIsModifiableCheckNeeded(true, delete(), composition(), directory());

        checkIsModifiableCheckNeeded(false, status(true));
        checkIsModifiableCheckNeeded(false, status(false));

        // status-modifiable=true
        checkIsModifiableCheckNeeded(false, status(true), delete(), composition(), directory());
        checkIsModifiableCheckNeeded(true, delete(), status(true), composition(), directory());

        // status-modifiable=false
        checkIsModifiableCheckNeeded(null, status(false), delete(), composition(), directory());
        checkIsModifiableCheckNeeded(null, delete(), composition(), status(false), directory());
        checkIsModifiableCheckNeeded(true, delete(), composition(), directory(), status(false));

        // status-modifiable=mixed
        checkIsModifiableCheckNeeded(false, status(true), composition(), status(false));
        checkIsModifiableCheckNeeded(null, status(false), composition(), status(true));
        checkIsModifiableCheckNeeded(null, status(true), composition(), status(false), composition());
        checkIsModifiableCheckNeeded(null, status(false), composition(), status(true), composition());
        checkIsModifiableCheckNeeded(false, status(false), status(true), composition());
        checkIsModifiableCheckNeeded(null, status(true), status(false), composition());
    }

    @SafeVarargs
    private static void checkIsModifiableCheckNeeded(Boolean needed, OriginalVersion<? extends RMObject>... versions) {
        ContributionCreateDto contribution = new ContributionCreateDto();
        contribution.getVersions().addAll(Arrays.asList(versions));

        if (needed == null) {
            assertThrows(
                    StateConflictException.class, () -> ContributionServiceImp.isModifiableCheckNeeded(contribution));
        } else {
            assertEquals(needed, ContributionServiceImp.isModifiableCheckNeeded(contribution));
        }
    }

    private static OriginalVersion<? extends RMObject> delete() {
        return new OriginalVersion<Composition>();
    }

    private static OriginalVersion<Composition> composition() {
        OriginalVersion<Composition> ov = new OriginalVersion<>();
        ov.setData(new Composition());
        return ov;
    }

    private static OriginalVersion<Folder> directory() {
        OriginalVersion<Folder> ov = new OriginalVersion<>();
        ov.setData(new Folder());
        return ov;
    }

    private static OriginalVersion<EhrStatus> status(boolean modifiable) {
        OriginalVersion<EhrStatus> ov = new OriginalVersion<>();
        EhrStatus ehrStatus = new EhrStatus();
        ehrStatus.setModifiable(modifiable);
        ov.setData(ehrStatus);
        return ov;
    }
}
