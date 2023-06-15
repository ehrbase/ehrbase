/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.rmobject;

import com.nedap.archie.rm.archetyped.FeederAudit;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.attributes.FeederAuditAttributes;

/**
 * Encode/decode a FeederAudit object as a json structure.
 * Should be used to support FeederAudit at DB level (f.e. Composition Entry)
 */
public class FeederAuditEncoding extends RMObjectEncoding {

    public String toDB(FeederAudit feederAudit) {
        Map<String, Object> objectMap = new FeederAuditAttributes(feederAudit).toMap();
        return super.toDB(objectMap);
    }

    public FeederAudit fromDB(String dbJonRepresentation) {
        return (FeederAudit) super.fromDB(FeederAudit.class, dbJonRepresentation);
    }
}
