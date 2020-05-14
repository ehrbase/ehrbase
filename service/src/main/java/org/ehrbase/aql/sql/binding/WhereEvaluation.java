package org.ehrbase.aql.sql.binding;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WhereEvaluation {

    private String[] operatorRequiringSQL = {"EXISTS"};

    List<Object> whereItems;

    public WhereEvaluation(List<Object> whereItems) {
        this.whereItems = whereItems;
    }

    public boolean requiresSQL(){

        List<String> operators = Arrays.asList(operatorRequiringSQL);

        for (Object item: whereItems){
            if (item instanceof String){ //ignore variable definition
                String testItem = ((String)item).toUpperCase();
                if (operators.contains(testItem))
                    return true;

            }
        }
        return false;
    }
}
