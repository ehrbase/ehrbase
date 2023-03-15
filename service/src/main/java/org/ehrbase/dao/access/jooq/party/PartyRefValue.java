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
package org.ehrbase.dao.access.jooq.party;

import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.Optional;
import org.ehrbase.jooq.pg.enums.PartyRefIdType;

/**
 * handles party_ref attribute of a party proxy
 */
public class PartyRefValue {

    private final PartyProxy partyProxy;

    private String namespace = null;
    private String value = null;
    private String scheme = null;
    private String type = null;
    private PartyRefIdType objectIdType = PartyRefIdType.undefined;

    public PartyRefValue(PartyProxy partyProxy) {
        this.partyProxy = partyProxy;
    }

    /**
     * extract the attributes of party_ref
     *
     * @return
     */
    public PartyRefValue attributes() {

        if (partyProxy.getExternalRef() != null) { // PartySelf f.e.

            PartyRef partyRef = partyProxy.getExternalRef();

            namespace = partyRef.getNamespace();
            ObjectId objectId = partyRef.getId();
            value = Optional.ofNullable(objectId).map(ObjectId::getValue).orElse(null);
            if (objectId instanceof GenericId genericId) {
                scheme = (genericId).getScheme();
            }
            type = partyRef.getType();
            objectIdType = PartyRefIdType.valueOf(new PersistedObjectId().objectIdClassSnakeCase(partyRef));
        }

        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getValue() {
        return value;
    }

    public String getScheme() {
        return scheme;
    }

    public String getType() {
        return type;
    }

    public PartyRefIdType getObjectIdType() {
        return objectIdType;
    }
}
