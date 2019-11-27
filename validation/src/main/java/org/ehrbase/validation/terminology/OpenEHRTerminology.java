package org.ehrbase.validation.terminology;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * convenience to map an attribute to an openehr terminology group
 */
public class OpenEHRTerminology {

    private static final Map<String, String> groupMap =
            Collections.unmodifiableMap(
                    new HashMap<String, String>()
                    {{
                        //ORIGINAL_VERSION
                        put("lifecycle_state", "version lifecycle state");
                        //COMPOSITION
                        put("category", "composition category");
                        //EVENT_CONTEX
                        put("setting", "setting");
                        //ISM_TRANSITION
                        put("current_state", "instruction states");
                        put("transition", "instruction transitions");
                        //DV_MULTIMEDIA
                        put("media_type", "media type");
                        put("compression_algorithm", "compression algorithms");
                        put("integrity_check_algorithm", " integrity check");
                        //ELEMENT
                        put("null_flavour", " null flavours");
                        //PARTICIPATION
                        put("mode", "participation mode");
                        put("function", "participation functions"); //normally a DvText
                        //PARTY_RELATIONSHIP
                        put("relationship", "subject relationship");
                        //GENERIC
                        put("property", "property");
                        //INTERVAL_EVENT
                        put("math_function", "event math function");
                        //DV_ORDERED
                        put("normal_status", "normal statuses");
                        //TERM_MAPPING
                        put("purpose", "term mapping purpose");
                    }}
            );

    public static String fieldToGroup(String fieldName){
        return groupMap.get(fieldName);
    }
}
