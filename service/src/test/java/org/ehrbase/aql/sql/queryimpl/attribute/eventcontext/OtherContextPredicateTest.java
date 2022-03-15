package org.ehrbase.aql.sql.queryimpl.attribute.eventcontext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtherContextPredicateTest {

    @Test
    void testPath() {
        String queryPath = "context/other_context";
        assertEquals("context/other_context", new OtherContextPredicate(queryPath).adjustForQuery());

        queryPath = "context/other_context[at0001]";
        assertEquals("context/other_context", new OtherContextPredicate(queryPath).adjustForQuery());

        queryPath = "context/other_context[at0001]/items[at0003]/value/value";
        assertEquals("context/other_context/items[at0003]/value/value", new OtherContextPredicate(queryPath).adjustForQuery());
    }
}