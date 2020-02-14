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

public class EventContextFactory {

    public EventContext makeDummy() {
        PartyRef partyRef = new PartyRef(new GenericId("123456-123", "EHRBASE-SCHEME"), "DEMOGRAPHIC", "PARTY");
        PartyIdentified healthcareFacility = new PartyIdentified(partyRef, "FACILITY", null);
        DateTime timenow = DateTime.now();
        DvCodedText concept = new DvCodedText("Other Care", new CodePhrase(new TerminologyId("openehr"), "238"));
        return new EventContext(healthcareFacility, new DvDateTime(timenow.toString()), null, null, "TEST LAB", concept, null);
    }

    public EventContext makeNull() {
        PartyRef partyRef = new PartyRef(new HierObjectId("ref"), "null", "null");
        PartyIdentified healthcareFacility = new PartyIdentified(partyRef, "null", null);
        DvCodedText concept = new DvCodedText("Other Care", new CodePhrase(new TerminologyId("openehr"), "238"));
        return new EventContext(healthcareFacility, new DvDateTime(new DateTime(0L).toString()), null, null, null, concept, null);
    }
}
