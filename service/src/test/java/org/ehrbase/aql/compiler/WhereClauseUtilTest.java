package org.ehrbase.aql.compiler;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class WhereClauseUtilTest {

    @Test
    public void testIsBalancedBlocks() {
        List<Object> expression =
                Arrays.asList(("a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude,>,38," +
                        "AND,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C'," +
                        "AND,e::ehr_id/value,IN,(,'849bf097-bd16-44fc-a394-10676284a012',,,'34b2e263-00eb-40b8-88f1-823c87096457',)," +
                        "OR,(,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C'," +
                            "AND, a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',)").split(","));

        assertTrue(new WhereClauseUtil(expression).isBalancedBlocks());

        expression =
                Arrays.asList(("a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude,>,38," +
                        "AND,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C'," +
                        "AND,e::ehr_id/value,IN,'849bf097-bd16-44fc-a394-10676284a012',,,'34b2e263-00eb-40b8-88f1-823c87096457',)," +
                        "OR,(,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C'," +
                        "AND, a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',)").split(","));

        assertFalse(new WhereClauseUtil(expression).isBalancedBlocks());

        expression =
                Arrays.asList(("a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude,>,38," +
                        "AND,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C'," +
                        "AND,e::ehr_id/value,IN,(,'849bf097-bd16-44fc-a394-10676284a012',,,'34b2e263-00eb-40b8-88f1-823c87096457'," +
                        "OR,(,a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C'," +
                        "AND, a_a::data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/units,=,'°C',)").split(","));

        assertFalse(new WhereClauseUtil(expression).isBalancedBlocks());
    }
}