package org.ehrbase.aql.sql;

import org.junit.Test;

import java.util.LinkedHashMap;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class VariableMultiMapTest {

    private String[][] variableSet = {
            {"LabordatenID", "/context/other_context[at0001]/items[at0002]/value/value"},
            {"PatientID","/ehr_status/subject/external_ref/id/value"},
            {"FallID","/items[at0001]/value/value"},
            {"Befund","/items[at0001]/value/value"}
    };

    @Test
    public void testAqlResult(){
        AqlResult aqlResult = new AqlResult(null, null);

        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (String[] entry: variableSet){
            linkedHashMap.put(entry[0], entry[1]);
        }

        aqlResult.setVariables(linkedHashMap);

        assertEquals(4,aqlResult.getVariables().size() );

        //check for a column
        assertTrue(aqlResult.variablesContains("FallID"));

        //reverse lookup
        assertTrue(aqlResult.variablesContains("/items[at0001]/value/value"));
    }
}
