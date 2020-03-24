package org.ehrbase.aql.sql.queryImpl.attribute.ehr;

import org.junit.Test;

import static org.junit.Assert.*;

public class EhrResolverTest {

    @Test
    public void testIsEhrAttribute() {

        assertTrue(EhrResolver.isEhrAttribute("ehr_id"));
        assertTrue(EhrResolver.isEhrAttribute("ehr_status"));
        assertTrue(EhrResolver.isEhrAttribute("system_id"));
        assertTrue(EhrResolver.isEhrAttribute("time_created/value"));

        assertFalse(EhrResolver.isEhrAttribute("time_created/time"));
    }
}