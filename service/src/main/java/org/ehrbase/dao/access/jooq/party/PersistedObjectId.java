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

import com.nedap.archie.rm.support.identification.*;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.openehr.sdk.util.SnakeCase;

/**
 * handles id (of type OBJECT_ID) in  PartyRef
 */
public class PersistedObjectId {

    /**
     * returns the specific ObjectId corresponding to the specialization
     * The specialization is provided by the object_id_type Enum value stored in DB (party_identified)
     * @param identifiedRecord
     * @return
     */
    public ObjectId fromDB(PartyIdentifiedRecord identifiedRecord) {

        ObjectId objectId = null;

        switch (identifiedRecord.getObjectIdType()) {
            case generic_id:
                objectId = new GenericId(identifiedRecord.getPartyRefValue(), identifiedRecord.getPartyRefScheme());
                break;
            case hier_object_id:
                objectId = new HierObjectId(identifiedRecord.getPartyRefValue());
                break;
            case object_version_id:
                objectId = new ObjectVersionId(identifiedRecord.getPartyRefValue());
                break;
        }

        return objectId;
    }

    /**
     * convert an objectId specialized class name in its snake case equivalent for storage
     * @param externalRef
     * @return
     */
    public String objectIdClassSnakeCase(PartyRef externalRef) {

        ObjectId objectId = externalRef.getId();

        String objectIdType = null;

        if (objectId != null) {
            objectIdType = new SnakeCase(objectId.getClass().getSimpleName()).camelToSnake();
        }

        return objectIdType;
    }
}
