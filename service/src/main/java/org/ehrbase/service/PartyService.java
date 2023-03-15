/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import com.nedap.archie.rm.generic.PartyProxy;
import java.util.UUID;

/**
 * @author Stefan Spiska
 */
public interface PartyService {

    /**
     * Trys to find a Party matching <code>partyProxy</code> otherwise creates it.
     *
     * @param partyProxy
     * @return {@link UUID} of the corresponding DB object.
     */
    UUID findOrCreateParty(PartyProxy partyProxy);
}
