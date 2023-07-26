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
package org.ehrbase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nedap.archie.json.JacksonUtil;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.Version;
import com.nedap.archie.rm.generic.AuditDetails;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

/**
 * Helper class to collect helper methods for contribution processing.
 */
public class ContributionServiceHelper {

    /**
     * splits contribution string content into its versions list & audit part
     * @param content Payload serialized input
     * @param format Format of given input
     * @return Map split at first level of input, so access to the version list and audit is directly possible
     */
    public static Map<String, Object> splitContent(String content, CompositionFormat format) {
        switch (format) {
            case JSON:
                return new CanonicalJson().unmarshalToMap(content);
            case XML:
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
    }

    /**
     * returns a list of RM VERSIONs extracted from given serialization
     * @param listVersions List of still serialized version objects
     * @param format Format of the serialization
     * @return List of deserialized version objects
     * @throws IllegalArgumentException when processing of given input fails
     */
    public static List<Version> extractVersionObjects(ArrayList listVersions, CompositionFormat format) {
        List<Version> versionsList = new LinkedList<>();

        switch (format) {
            case JSON:
                for (Object version : listVersions) {
                    try {
                        // TODO CONTRIBUTION: round trip ((string->)object->string->object) really necessary?
                        String json = JacksonUtil.getObjectMapper().writeValueAsString(version);
                        RMObject versionRmObject = new CanonicalJson().unmarshal(json, RMObject.class);
                        if (versionRmObject instanceof Version) {
                            versionsList.add((Version) versionRmObject);
                        } else {
                            throw new IllegalArgumentException(
                                    "Wrong input. At least one VERSION in this contribution is invalid.");
                        }
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException(
                                "Error while processing given json input: " + e.getMessage());
                    }
                }
                break;
            case XML:
            default:
                throw new UnexpectedSwitchCaseException(format);
        }

        return versionsList;
    }

    /** TODO CONTRIBUTION: isn't this in its current form independent of the format? the map should be <string, object> without JSON specifics. only the problematic round trip conversation depends of a format, but that could be fix.
     * unmarshaller that creates an RMObject from a Map's content
     * @param content Map instance containing data for a RMObject (i.e. pseudo marshalled)
     * @param format Format of the origin payload // TODO technically given content doesn't contain any (real) marshalled, for instance, json parts anymore
     * @return RM Object representation fitting the given content
     * @throws IllegalArgumentException when processing of given input fails
     */
    public static RMObject unmarshalMapContentToRmObject(LinkedHashMap content, CompositionFormat format) {
        switch (format) {
            case JSON:
                String json = null;
                try { // TODO CONTRIBUTION: round trip ((string->)object->string->object) really necessary?
                    json = JacksonUtil.getObjectMapper().writeValueAsString(content);
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException("Error while processing given json input: " + e.getMessage());
                }
                return new CanonicalJson().unmarshal(json, RMObject.class);
            case XML:
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
    }

    /**
     * Convenience helper that combines some methods from above and prepares direct access to the list of contained versions
     * @param content Plain string content
     * @param format Format of content
     * @return List of version objects extracted from content
     */
    public static List<Version> parseVersions(String content, CompositionFormat format) {
        // extract both per standard parts of the content: data block containing versions & audit
        Map<String, Object> splitContent = splitContent(content, format);

        // process versions: unmarshal to some iterable object & create RM objects out of input
        List<Version> versions;

        Object versionsContent = splitContent.get("versions");
        if (versionsContent instanceof List) {
            versions = extractVersionObjects((ArrayList) versionsContent, format);
        } else {
            throw new IllegalArgumentException("Can't process input, possible malformed version payload");
        }
        return versions;
    }

    /**
     * Helper that parses the AuditDetails from the contribution input.
     * @param content Plain string content
     * @param format Format of content
     * @return AuditDetails object
     */
    public static AuditDetails parseAuditDetails(String content, CompositionFormat format) {
        // extract both per standard parts of the content: data block containing versions & audit
        Map<String, Object> splitContent = splitContent(content, format);

        Object auditContent = splitContent.get("audit");
        AuditDetails auditResult;

        switch (format) {
            case JSON:
                try {
                    String json = JacksonUtil.getObjectMapper().writeValueAsString(auditContent);
                    auditResult = new CanonicalJson().unmarshal(json, AuditDetails.class);
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException("Error while processing given json input: " + e.getMessage());
                }
                break;
            case XML:
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
        return auditResult;
    }
}
