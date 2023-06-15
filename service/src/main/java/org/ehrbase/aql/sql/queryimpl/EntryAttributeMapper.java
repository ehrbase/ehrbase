/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.queryimpl.attribute.GenericJsonPath.OTHER_CONTEXT;
import static org.ehrbase.aql.sql.queryimpl.attribute.GenericJsonPath.OTHER_DETAILS;
import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_UID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.jooq.dbencoding.attributes.EntryAttributes;
import org.ehrbase.jooq.dbencoding.attributes.LocatableAttributes;

/**
 * Map a datavalue UML expression from an ARCHETYPED structure into its RM/JSON representation
 * see http://www.openehr.org/releases/trunk/UML/ for field identification
 * Created by christian on 5/11/2016.
 */
@SuppressWarnings({"java:S3776", "java:S3740"})
public class EntryAttributeMapper {

    public static final String ISM_TRANSITION = "ism_transition";
    public static final String NAME = "name";
    public static final String TIME = "time";
    public static final String ORIGIN = "origin";
    public static final String TIMING = "timing";
    public static final String OTHER_PARTICIPATIONS = "other_participations";
    public static final String SLASH_VALUE = "/value";
    public static final String VALUE = "value";
    public static final String DEFINING_CODE = "defining_code";
    public static final String SLASH = "/";
    public static final String COMMA = ",";
    public static final String LOWER = "lower";
    public static final String UPPER = "upper";
    public static final String INTERVAL = "interval";
    public static final String MAPPINGS = "mappings";
    public static final String FEEDER_AUDIT = "feeder_audit";
    public static final String CONTEXT = "context";

    private EntryAttributeMapper() {}

    private static Integer firstOccurence(int offset, List<String> list) {
        for (int i = offset; i < list.size(); i++) {
            if (list.get(i).equals(EntryAttributeMapper.VALUE)) return i;
        }
        return null;
    }

    /**
     * do a simple toCamelCase translation... and prefix all with /value :-)
     *
     * @param attribute
     * @return
     */
    public static String map(String attribute) {
        boolean inOtherParticipations = false;

        if (attribute.equals(OTHER_CONTEXT)) return OTHER_CONTEXT; // conventionally

        List<String> fields = new ArrayList<>(Arrays.asList(attribute.split(SLASH)));
        if (!fields.get(0).equals(OTHER_CONTEXT)) fields.remove(0);

        int floor = 1;

        if (fields.isEmpty())
            return null; // this happens when a non specified value is queried f.e. the whole json body

        // deals with the tricky ones first...
        if (fields.get(0).equals(OTHER_PARTICIPATIONS)) {
            inOtherParticipations = true; // to avoid adding an index to access name array struct
        } else if (fields.get(0).equals(NAME)) {
            fields.add(
                    1, "0"); // name is now formatted as /name -> array of values! Required to deal with cluster items
        } else if (fields.size() >= 2 && fields.get(1).equals(MAPPINGS)) {
            fields.add(2, "0"); // mappings is now formatted as /mappings -> array of values!
        } else if (fields.get(0).equals(TIME)
                || fields.get(0).equals(ORIGIN)
                || fields.get(0).equals(TIMING)) {
            if (fields.size() > 1 && fields.get(1).equals(VALUE)) {
                fields.add(VALUE); // time is formatted with 2 values: string value and epoch_offset
                fields.set(1, SLASH_VALUE);
            } else {
                fields.add(1, SLASH_VALUE);
            }
        } else if (LocatableAttributes.isLocatableAttribute("/" + fields.get(0))) {
            fields = setLocatableField(fields);
        } else if (EntryAttributes.isEntryAttribute("/" + fields.get(0))) {
            fields = setEntryAttributeField(fields);
        } else { // this deals with the "/value,value"
            Integer match = firstOccurence(0, fields);

            if (match != null) { // deals with "/value/value"
                if (match != 0) {
                    // deals with name/value (name value is contained into a list conventionally)
                    if (match > 1
                            && fields.get(match - 1)
                                    .matches("name|time|current_state|transition|careflow_step|reason|terminology_id"))
                        fields.set(match, VALUE);
                    else
                        // usual /value
                        fields.set(match, SLASH_VALUE);

                } else if (match + 1 < fields.size() - 1) {
                    Integer first = firstOccurence(match + 1, fields);
                    if (first != null && first == match + 1) fields.set(match + 1, SLASH_VALUE);
                }
            }
        }

        // prefix the first element
        fields.set(0, SLASH + fields.get(0));

        // deals with the remainder of the array
        for (int i = floor; i < fields.size(); i++) {

            if (fields.get(i).equalsIgnoreCase("NAME") && !inOtherParticipations) {
                // whenever the canonical json for name is queried
                fields.set(i, "/name,0");
            } else fields.set(i, fields.get(i));
        }

        return StringUtils.join(fields, COMMA);
    }

    private static List<String> setLocatableField(List<String> fields) {
        if (("/" + fields.get(0)).equals(TAG_UID)) {
            fields.add(1, SLASH_VALUE);
        }

        boolean inItemStruct = false;
        boolean inNameAttribute = false;

        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).contains("[") && !fields.get(i).startsWith(OTHER_DETAILS)) {
                fields.set(i, "/" + fields.get(i));
                inItemStruct = true;
            }
            if (fields.get(i).equals(NAME)) {
                inNameAttribute = true;
            }
            if (fields.get(i).equals(VALUE) && inItemStruct) {
                fields.set(i, (inNameAttribute ? "" : "/") + fields.get(i));
                if (inNameAttribute) inNameAttribute = false;
                inItemStruct = false;
            }
        }

        return fields;
    }

    private static List<String> setEntryAttributeField(List<String> fields) {
        return fields;
    }
}
