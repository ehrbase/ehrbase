package org.ehrbase.aql.sql.queryimpl.attribute;

import org.junit.Test;

import static org.junit.Assert.*;

public class GenericJsonPathTest {

    @Test
    public void jqueryPath() {

        assertEquals("'{context,health_care_facility,external_ref,id}'", new GenericJsonPath("context/health_care_facility/external_ref/id").jqueryPath());
        assertEquals("'{setting,defining_code}'", new GenericJsonPath("setting/defining_code").jqueryPath());
        assertEquals("'{context,other_context,/items[openEHR-EHR-CLUSTER.composition_context_detail.v1],0}'", new GenericJsonPath("context/other_context/items[openEHR-EHR-CLUSTER.composition_context_detail.v1]").jqueryPath());
        assertEquals("'{context,other_context,/items[openEHR-EHR-CLUSTER.composition_context_detail.v1],0,/items[at0001],0}'", new GenericJsonPath("context/other_context/items[openEHR-EHR-CLUSTER.composition_context_detail.v1]/items[at0001]").jqueryPath());
        assertEquals("'{context,other_context,/items[openEHR-EHR-CLUSTER.composition_context_detail.v1],0,/items[at0001],0,/value,value}'", new GenericJsonPath("context/other_context/items[openEHR-EHR-CLUSTER.composition_context_detail.v1]/items[at0001]/value/value").jqueryPath());
    }

    @Test
    public void jqueryPathOtherDetails() {

        assertEquals("'{other_details,/items[at0111],0,/value}'", new GenericJsonPath("other_details/items[at0111]/value").jqueryPath());
        assertEquals("'{other_details,/items[at0111],0,/name,0}'", new GenericJsonPath("other_details/items[at0111]/name").jqueryPath());
        assertEquals("'{other_details,/items[at0111],0,/value,value}'", new GenericJsonPath("other_details/items[at0111]/value/value").jqueryPath());
        assertEquals("'{other_details,/items[at0111],0,/name,0,value}'", new GenericJsonPath("other_details/items[at0111]/name/value").jqueryPath());

    }
}