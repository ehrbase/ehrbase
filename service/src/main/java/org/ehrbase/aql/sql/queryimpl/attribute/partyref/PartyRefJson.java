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
package org.ehrbase.aql.sql.queryimpl.attribute.partyref;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

import java.util.Optional;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;
import org.jooq.TableField;

public class PartyRefJson extends PartyRefAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    public PartyRefJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        // query the json representation of EVENT_CONTEXT and cast the result as TEXT
        if (jsonPath.isPresent()) {
            return new GenericJsonField(fieldContext, joinSetup)
                    .forJsonPath(jsonPath.get())
                    .partyRef(
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_VALUE),
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_SCHEME),
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE),
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_TYPE));
        } else
            return new GenericJsonField(fieldContext, joinSetup)
                    .partyRef(
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_VALUE),
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_SCHEME),
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE),
                            joinSetup.getPartyJoinRef().field(PARTY_IDENTIFIED.PARTY_REF_TYPE));
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    public PartyRefJson forJsonPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(jsonPath);
        return this;
    }
}
