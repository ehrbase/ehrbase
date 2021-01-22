package org.ehrbase.aql.sql.binding;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

/**
 * do some smart guess to identify if a string is an ISO8601 data/time with timezone
 */
public class DateTimes {
    String testItem;

    public DateTimes(String toCheck) {
        this.testItem = toCheck;
    }

    public boolean isDateTimeZoned(){
        try {
            if (testItem.startsWith("'") && testItem.endsWith("'"))
                testItem = testItem.substring(1, testItem.length() - 1);
            ZonedDateTime.parse(testItem);
            return true;
        } catch (DateTimeParseException e){
            return false;
        }
    }
}
