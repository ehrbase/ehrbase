package org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.Test;

import static org.junit.Assert.*;

public class GenericRmTypeTest {

    @Test
    public void testAddSpecializedType(){
        String dvIntervalExample =
                "{\n" +
                "  \"lower\": {\n" +
                "    \"value\": \"2019-11-22T00:00+01:00\",\n" +
                "    \"epoch_offset\": 1.5743772E9\n" +
                "  },\n" +
                "  \"upper\": {\n" +
                "    \"value\": \"2020-12-22T00:00+01:00\",\n" +
                "    \"epoch_offset\": 1.5743772E9\n" +
                "  },\n" +
                "  \"lowerUnbounded\": false,\n" +
                "  \"upperUnbounded\": false,\n" +
                "  \"lowerIncluded\": true,\n" +
                "  \"upperIncluded\": true,\n" +
                "  \"_type\": \"DV_INTERVAL\\u003cDV_DATE_TIME\\u003e\"\n" +
                "}";

        LinkedTreeMap linkedTreeMap = new GsonBuilder().create().fromJson(dvIntervalExample, LinkedTreeMap.class);

        LinkedTreeMap specializedMap = new GenericRmType((String)linkedTreeMap.get("_type")).inferSpecialization(linkedTreeMap);

        assertEquals(((LinkedTreeMap)specializedMap.get("lower")).get("_type"), "DV_DATE_TIME");
        assertEquals(((LinkedTreeMap)specializedMap.get("upper")).get("_type"), "DV_DATE_TIME");
    }

}