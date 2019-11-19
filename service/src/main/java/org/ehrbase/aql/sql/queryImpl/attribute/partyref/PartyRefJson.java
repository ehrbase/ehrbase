/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
package org.ehrbase.aql.sql.queryImpl.attribute.partyref;

import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.EventContextAttribute;
import org.ehrbase.aql.sql.queryImpl.value_field.GenericJsonField;
import org.jooq.Field;
import org.jooq.TableField;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.tables.EventContext.EVENT_CONTEXT;

public class PartyRefJson extends PartyRefAttribute {

    public PartyRefJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        //query the json representation of EVENT_CONTEXT and cast the result as TEXT
        return new GenericJsonField(fieldContext, joinSetup).jsonField("PARTY_REF","ehr.js_canonical_party_ref",
                joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE),
                joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_TYPE),
                joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_SCHEME),
                joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_VALUE));
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
