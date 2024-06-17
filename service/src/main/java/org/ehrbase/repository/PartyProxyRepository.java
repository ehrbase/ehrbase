/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.repository;

import static org.ehrbase.jooq.pg.Tables.USERS;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.jooq.pg.tables.records.UsersRecord;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PartyProxyRepository {

    public static final String SECURITY_USER_TYPE = "EHRbase Security Authentication User";
    public static final String EHRBASE = "EHRbase";

    private final DSLContext context;

    public PartyProxyRepository(DSLContext context) {
        this.context = context;
    }

    /**
     * Find the party id of a ehrbase user corresponding to <code>username</code>
     *
     * @param username
     * @return
     */
    public Optional<UUID> findInternalUserId(String username) {

        return context.select(USERS.ID)
                .from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOptional()
                .map(Record1::value1);
    }

    /**
     * Create a {@link PartyIdentified} for a ehrbase user corresponding to <code>username</code>
     *
     * @param username
     * @return
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID createInternalUser(String username) {

        UUID uuid = UuidGenerator.randomUUID();

        UsersRecord usersRecord = context.newRecord(USERS);

        usersRecord.setId(uuid);
        usersRecord.setUsername(username);
        usersRecord.store();

        return uuid;
    }

    public PartyProxy fromUser(UUID userId) {

        String username = context.select(USERS.USERNAME)
                .from(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOptional()
                .map(Record1::value1)
                .orElseThrow();

        DvIdentifier identifier = new DvIdentifier();

        identifier.setId(username);
        identifier.setIssuer(EHRBASE);
        identifier.setAssigner(EHRBASE);
        identifier.setType(SECURITY_USER_TYPE);

        PartyRef externalRef = new PartyRef(new GenericId(userId.toString(), "DEMOGRAPHIC"), "User", "PARTY");

        return new PartyIdentified(externalRef, "EHRbase Internal " + username, List.of(identifier));
    }
}
