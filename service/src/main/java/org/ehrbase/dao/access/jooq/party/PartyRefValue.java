/*
 * Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.ehrbase.jooq.pg.enums.PartyRefIdType;

/**
 * handles party_ref attribute of a party proxy
 */
class PartyRefValue {

    private final PartyProxy partyProxy;

    private String namespace = null;
    private String value = null;
    private String scheme = null;
    private String type = null;
    private PartyRefIdType objectIdType = PartyRefIdType.undefined;

    PartyRefValue(PartyProxy partyProxy) {
        this.partyProxy = partyProxy;
    }

    /**
     * extract the attributes of party_ref
     * @return
     */
    PartyRefValue attributes(){

        if (partyProxy.getExternalRef() == null) //PartySelf f.e.
            return this;

        PartyRef partyRef = partyProxy.getExternalRef();

        namespace = partyRef != null ? partyRef.getNamespace() : null;
        ObjectId objectId = partyRef.getId();
        value = objectId != null ? objectId.getValue() : null;
        if (objectId != null && objectId instanceof GenericId)
            scheme = ((GenericId)objectId).getScheme();
        type = partyRef != null ? partyRef.getType() : null;
        objectIdType = partyRef != null ? PartyRefIdType.valueOf(new PersistedObjectId().objectIdClassSnakeCase(partyRef)) : PartyRefIdType.undefined;

        return this;
    }

    String getNamespace() {
        return namespace;
    }

    String getValue() {
        return value;
    }

    String getScheme() {
        return scheme;
    }

    String getType() {
        return type;
    }

    PartyRefIdType getObjectIdType() {
        return objectIdType;
    }
}
