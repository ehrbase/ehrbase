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

import static org.ehrbase.jooq.pg.Tables.COMMITTER;
import static org.ehrbase.jooq.pg.Tables.USERS;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.ehrbase.jooq.pg.tables.records.CommitterRecord;
import org.ehrbase.jooq.pg.tables.records.UsersRecord;
import org.ehrbase.openehr.dbformat.VersionedObjectDataStructure;
import org.ehrbase.openehr.dbformat.json.RmDbJson;
import org.ehrbase.service.UserService.UserAndCommitterId;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
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
    public Optional<UserAndCommitterId> findInternalUserAndCommitterId(String username) {

        return context.select(USERS.ID, USERS.COMMITTER_ID)
                .from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOptional()
                .map(r -> new UserAndCommitterId(r.value1(), r.value2()));
    }

    /**
     * Create a {@link PartyIdentified} for a ehrbase user corresponding to <code>username</code>
     *
     * @param username
     * @return
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UserAndCommitterId createInternalUser(String username) {

        UUID uuid = UuidGenerator.randomUUID();

        UsersRecord usersRecord = context.newRecord(USERS);

        usersRecord.setId(uuid);
        usersRecord.setUsername(username);
        usersRecord.setCommitterId(findOrCreateCommitter(partyIdentifiedForUser(uuid, username)));
        usersRecord.store();

        return new UserAndCommitterId(uuid, usersRecord.getCommitterId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID findOrCreateCommitter(PartyProxy party) {

        String dbJson = VersionedObjectDataStructure.applyRmAliases(RmDbJson.MARSHAL_OM.valueToTree(party))
                .toString();

        return context.select(COMMITTER.ID)
                .from(COMMITTER)
                // SQLDataType.CLOB is resolved to PostgresDataType.TEXT, which is necessary to enable index usage
                .where(COMMITTER
                        .DATA
                        .cast(SQLDataType.CLOB)
                        .eq(DSL.inline(dbJson).cast(JSONB.class).cast(SQLDataType.CLOB)))
                // The migration did not necessarily eliminate all duplicates, so choose the first matching one
                .limit(1)
                .fetchOptional(Record1::value1)
                .orElseGet(() -> {
                    CommitterRecord committerRecord = context.newRecord(COMMITTER);
                    committerRecord.setId(UuidGenerator.randomUUID());
                    committerRecord.setData(JSONB.valueOf(dbJson));
                    committerRecord.store();

                    return committerRecord.getId();
                });
    }

    @Nonnull
    private static PartyIdentified partyIdentifiedForUser(UUID userId, String username) {
        DvIdentifier identifier = new DvIdentifier();

        identifier.setId(username);
        identifier.setIssuer(EHRBASE);
        identifier.setAssigner(EHRBASE);
        identifier.setType(SECURITY_USER_TYPE);

        PartyRef externalRef = new PartyRef(new GenericId(userId.toString(), "DEMOGRAPHIC"), "User", "PARTY");
        PartyIdentified partyIdentified =
                new PartyIdentified(externalRef, "EHRbase Internal " + username, List.of(identifier));

        return partyIdentified;
    }
}
