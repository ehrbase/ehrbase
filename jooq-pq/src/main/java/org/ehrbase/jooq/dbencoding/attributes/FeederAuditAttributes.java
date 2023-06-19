/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.attributes;

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_CLASS;

import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.PathMap;
import org.ehrbase.jooq.dbencoding.RmObjectEncoding;
import org.ehrbase.jooq.dbencoding.SimpleClassName;

/**
 * populate the attributes for RM FeederAudit
 */
public class FeederAuditAttributes {

    private final FeederAudit feederAudit;

    public FeederAuditAttributes(FeederAudit feederAudit) {
        this.feederAudit = feederAudit;
    }

    /**
     * encode the attributes lower snake case to comply with UML conventions and make is queryable
     * @return
     */
    public Map<String, Object> toMap() {
        Map<String, Object> valuemap = PathMap.getInstance();

        valuemap.put(TAG_CLASS, new SimpleClassName(feederAudit).toString());

        if (feederAudit.getOriginatingSystemItemIds() != null
                && !feederAudit.getOriginatingSystemItemIds().isEmpty()) {
            valuemap.put("originating_system_item_ids", encodeDvIdentifiers(feederAudit.getOriginatingSystemItemIds()));
        }

        valuemap.put(
                "feeder_system_audit", new FeederAuditDetailsAttributes(feederAudit.getFeederSystemAudit()).toMap());

        if (feederAudit.getFeederSystemItemIds() != null
                && !feederAudit.getFeederSystemItemIds().isEmpty()) {
            valuemap.put("feeder_system_item_ids", encodeDvIdentifiers(feederAudit.getFeederSystemItemIds()));
        }

        if (feederAudit.getOriginalContent() != null)
            valuemap.put("original_content", new RmObjectEncoding(feederAudit.getOriginalContent()).toMap());

        valuemap.put(
                "originating_system_audit",
                new FeederAuditDetailsAttributes(feederAudit.getOriginatingSystemAudit()).toMap());

        return valuemap;
    }

    List<Map<String, Object>> encodeDvIdentifiers(List<DvIdentifier> dvIdentifiers) {
        List<Map<String, Object>> idList = new ArrayList<>();
        for (DvIdentifier dvIdentifier : dvIdentifiers) idList.add(new RmObjectEncoding(dvIdentifier).toMap());

        return idList;
    }
}
