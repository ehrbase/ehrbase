/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.joda.time.DateTime;

/**
 * utility to build dummy or null EventContext
 */
public class EventContextFactory {

    public EventContext makeDummy() {
        PartyRef partyRef = new PartyRef(new GenericId("123456-123", "EHRBASE-SCHEME"), "DEMOGRAPHIC", "PARTY");
        PartyIdentified healthcareFacility = new PartyIdentified(partyRef, "FACILITY", null);
        DateTime timenow = DateTime.now();
        DvCodedText concept = new DvCodedText("Other Care", new CodePhrase(new TerminologyId("openehr"), "238"));
        return new EventContext(
                healthcareFacility, new DvDateTime(timenow.toString()), null, null, "TEST LAB", concept, null);
    }

    public EventContext makeNull() {
        PartyRef partyRef = new PartyRef(new HierObjectId("ref"), "null", "null");
        PartyIdentified healthcareFacility = new PartyIdentified(partyRef, "null", null);
        DvCodedText concept = new DvCodedText("Other Care", new CodePhrase(new TerminologyId("openehr"), "238"));
        return new EventContext(
                healthcareFacility, new DvDateTime(new DateTime(0L).toString()), null, null, null, concept, null);
    }
}
