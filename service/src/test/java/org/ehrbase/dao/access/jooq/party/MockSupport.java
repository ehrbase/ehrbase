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
package org.ehrbase.dao.access.jooq.party;

import java.util.stream.Collector;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.jooq.pg.tables.Identifier;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectWhereStep;
import org.mockito.Mockito;

public class MockSupport {

    public static I_DomainAccess prepareDomainAccessMock(Object fetchResult) {
        //    List<UUID> uuids = DataGenerator.anyUUIDs(4);
        //    List<PartyIdentifiedRecord> records = DataGenerator.anyPartyIdentifiedRecordWith(uuids);
        //
        //    List<IdentifierRecord> idRec = DataGenerator.anyIdentifierRecordWith(uuids);

        Result result = Mockito.mock(Result.class);
        Mockito.when(result.collect(Mockito.isA(Collector.class))).thenReturn(fetchResult);

        SelectConditionStep cond = Mockito.mock(SelectConditionStep.class);
        Mockito.when(cond.fetch()).thenReturn(result);

        SelectWhereStep where = Mockito.mock(SelectWhereStep.class);
        Mockito.when(where.where(Mockito.isA(Condition.class))).thenReturn(cond);

        DSLContext ctx = Mockito.mock(DSLContext.class);
        Mockito.when(ctx.selectFrom(Mockito.isA(Identifier.class))).thenReturn(where);

        I_DomainAccess domAccess = Mockito.mock(I_DomainAccess.class);
        Mockito.when(domAccess.getContext()).thenReturn(ctx);

        //    PartyIdentifiers pIds = new PartyIdentifiers(domAccess);
        //    List<Pair<PartyIdentifiedRecord,List<DvIdentifier>>> retrieveMultiple = pIds.retrieveMultiple(records);
        //
        //
        //    retrieveMultiple.forEach(p -> {
        //      Assert.assertTrue(p.getRight().stream().map(e ->
        // e.getId()).collect(Collectors.toList()).contains(p.getLeft().getId().toString()));
        //    });

        return domAccess;
    }
}
