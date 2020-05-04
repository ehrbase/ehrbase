package org.ehrbase.aql.sql.binding;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class WhereTemporal {
    List<Object> whereItems;

    public WhereTemporal(List<Object> whereItems) {
        this.whereItems = whereItems;
    }

    public boolean containsTemporalItem(){
        for (Object item: whereItems){
            if (item instanceof String){ //ignore variable definition
                String testItem = (String)item;
                try {
                    if (testItem.startsWith("'") && testItem.endsWith("'"))
                        testItem = testItem.substring(1, testItem.length() - 1);
                    ZonedDateTime.parse(testItem);
                    return true;
                } catch (DateTimeParseException e){

                }

            }
        }
        return false;
    }
}
