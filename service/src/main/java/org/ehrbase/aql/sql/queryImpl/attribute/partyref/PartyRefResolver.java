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

import org.ehrbase.aql.sql.queryImpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.jooq.Field;

import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

public class PartyRefResolver extends AttributeResolver
{

    public PartyRefResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path){

        switch (path){
            case "name/value":
            case "name":
                return new SimplePartyRefAttribute(fieldResolutionContext, joinSetup).forTableField(PARTY_IDENTIFIED.NAME).sqlField();
            case "external_ref/type":
                return new SimplePartyRefAttribute(fieldResolutionContext, joinSetup).forTableField(PARTY_IDENTIFIED.PARTY_REF_TYPE).sqlField();
            case "external_ref/namespace":
                return new SimplePartyRefAttribute(fieldResolutionContext, joinSetup).forTableField(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE).sqlField();
            case "external_ref/scheme":
                return new SimplePartyRefAttribute(fieldResolutionContext, joinSetup).forTableField(PARTY_IDENTIFIED.PARTY_REF_SCHEME).sqlField();
            case "external_ref/id":
                return new SimplePartyRefAttribute(fieldResolutionContext, joinSetup).forTableField(PARTY_IDENTIFIED.PARTY_REF_VALUE).sqlField();
            case "external_ref":
                return new PartyRefJson(fieldResolutionContext, joinSetup).sqlField();

        }
        throw new IllegalArgumentException("Unresolved party_identified external_ref attribute path:"+path);
    }
}
