package org.ehrbase.aql.containment;

import org.ehrbase.aql.TestAqlBase;
import org.junit.Test;

import static org.junit.Assert.*;

public class OptJsonPathTest extends TestAqlBase {

    @Test
    public void testRepresent()  {
        OptJsonPath optJsonPath = new OptJsonPath(knowledge);

        String json = optJsonPath.representAsString("LabResults1");

        assertNotNull(json);

        assertNotNull(optJsonPath.jsonPathEval(json, "$"));

    }


}