package org.ehrbase.aql.sql.postprocessing;

import org.ehrbase.aql.sql.binding.JsonbBlockDef;

public class ResultBlock {

    JsonbBlockDef jsonbBlockDef;


    public ResultBlock(JsonbBlockDef jsonbBlockDef) {
        this.jsonbBlockDef = jsonbBlockDef;
    }

    public boolean isCanonical(){

        if (jsonbBlockDef.getPath() == null)
            return false;

        return jsonbBlockDef.getPath().endsWith("composer/identifiers")||
                    jsonbBlockDef.getPath().endsWith("health_care_facility/identifiers");
    }
}
