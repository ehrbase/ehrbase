package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.VariableDefinition;

import java.util.List;

/**
 * check if a where variable represents a temporal object. This is used to apply proper type casting and
 * relevant operator using EPOCH_OFFSET instead of string value when dealing with date/time comparison in
 * json structure
 */
public class WhereTemporal {
    List<Object> whereItems;

    public WhereTemporal(List<Object> whereItems) {
        this.whereItems = whereItems;
    }

    public boolean containsTemporalItem(VariableDefinition variableDefinition){

        //get the index of variable definition in item list
        Integer pos = whereItems.indexOf(variableDefinition);

        for (Object item: whereItems.subList(pos, whereItems.size())){

            if (item instanceof String){ //ignore variable definition
                if (new DateTimes((String)item).isDateTimeZoned())
                    return true;
            }
        }
        return false;
    }
}
