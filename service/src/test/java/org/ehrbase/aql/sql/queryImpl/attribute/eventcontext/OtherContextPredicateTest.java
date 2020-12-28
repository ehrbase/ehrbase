package org.ehrbase.aql.sql.queryImpl.attribute.eventcontext;

import static org.junit.Assert.*;

import org.junit.Test;

public class OtherContextPredicateTest {

  @Test
  public void testPath() {

    String queryPath = "context/other_context[at0001]";

    assertEquals("context/other_context", new OtherContextPredicate(queryPath).adjustForQuery());

    queryPath = "context/other_context[at0001]/items[at0003]/value/value";

    assertEquals(
        "context/other_context/items[at0003]/value/value",
        new OtherContextPredicate(queryPath).adjustForQuery());
  }
}
