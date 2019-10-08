/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.dao.access.interfaces;

import org.ehrbase.dao.access.jooq.PartyIdentifiedAccess;
import com.nedap.archie.rm.generic.PartyIdentified;
import org.jooq.Record;

/**
 * Created by christian on 11/9/2016.
 */
public interface I_PartyIdentifiedPerformerAccess extends I_PartyIdentifiedAccess {

    static PartyIdentified retrievePartyIdentified(Record record) {
        //get the composer attributes
        String name = (String) record.getValue(I_CompositionAccess.F_PERFORMER_NAME);
        String refScheme = (String) record.getValue(I_CompositionAccess.F_PERFORMER_REF_SCHEME);
        String refNamespace = (String) record.getValue(I_CompositionAccess.F_PERFORMER_REF_NAMESPACE);
        String refValue = (String) record.getValue(I_CompositionAccess.F_PERFORMER_REF_VALUE);
        String refType = (String) record.getValue(I_CompositionAccess.F_PERFORMER_REF_TYPE);

        return PartyIdentifiedAccess.retrievePartyIdentified(name, refScheme, refNamespace, refValue, refType);
    }
}
