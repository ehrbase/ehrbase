package org.ehrbase.aql.sql.queryImpl;

import org.junit.Test;

import static org.junit.Assert.*;

public class VariableAqlPathTest {

    @Test
    public void getSuffix() {

        String aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value";

        assertEquals("value", new VariableAqlPath(aPath).getSuffix());

    }

    @Test
    public void getInfix() {

        String aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value";

        assertEquals("data[at0001]/events[at0002]/data[at0003]/items[at0007]", new VariableAqlPath(aPath).getInfix());
    }

    @Test
    public void testIsPartialAqlDataValuePath(){
        String aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value";

        assertTrue(new VariableAqlPath(aPath).isPartialAqlDataValuePath());

        aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/value/magnitude";

        assertFalse(new VariableAqlPath(aPath).isPartialAqlDataValuePath());

        aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/name";

        assertTrue(new VariableAqlPath(aPath).isPartialAqlDataValuePath());

        aPath = "data[at0001]/events[at0002]/data[at0003]/items[at0007]/time";

        assertTrue(new VariableAqlPath(aPath).isPartialAqlDataValuePath());

    }
}