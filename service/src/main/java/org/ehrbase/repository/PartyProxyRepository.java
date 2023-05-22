/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.repository;

import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;
import static org.ehrbase.jooq.pg.Tables.USERS;

import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartyRelated;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.dao.access.jooq.party.PartyRefValue;
import org.ehrbase.dao.access.jooq.party.PersistedPartyIdentified;
import org.ehrbase.jooq.pg.enums.PartyType;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.ehrbase.jooq.pg.tables.records.UsersRecord;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.ehrbase.service.BaseServiceImp;
import org.ehrbase.service.PersistentCodePhrase;
import org.ehrbase.service.PersistentTermMapping;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB-Access to {@link org.ehrbase.jooq.pg.tables.PartyIdentified} and {@link org.ehrbase.jooq.pg.tables.Identifier}
 * @author Stefan Spiska
 */
@Repository
public class PartyProxyRepository {

    private final DSLContext context;

    private final TenantService tenantService;

    public PartyProxyRepository(DSLContext context, TenantService tenantService) {
        this.context = context;
        this.tenantService = tenantService;
    }

    /**
     * Find the party id of a ehrbase user corresponding to <code>username</code>
     * @param username
     * @return
     */
    public Optional<UUID> findInternalUserId(String username) {

        return context.select(USERS.PARTY_ID)
                .from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOptional(USERS.PARTY_ID);
    }

    /**
     * Create a {@link PartyIdentified} for a ehrbase user corresponding to <code>username</code>
     * @param username
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createInternalUser(String username) {

        DvIdentifier identifier = new DvIdentifier();

        identifier.setId(username);
        identifier.setIssuer(PersistedPartyIdentified.EHRBASE);
        identifier.setAssigner(PersistedPartyIdentified.EHRBASE);
        identifier.setType(PersistedPartyIdentified.SECURITY_USER_TYPE);

        PartyRef externalRef = new PartyRef(
                new GenericId(UuidGenerator.randomUUID().toString(), BaseServiceImp.DEMOGRAPHIC),
                "User",
                BaseServiceImp.PARTY);
        PartyIdentified partyIdentified =
                new PartyIdentified(externalRef, "EHRbase Internal " + username, List.of(identifier));

        UUID uuid = create(partyIdentified);

        UsersRecord usersRecord = context.newRecord(USERS);

        usersRecord.setPartyId(uuid);
        usersRecord.setUsername(username);
        usersRecord.setSysTenant(tenantService.getCurrentSysTenant());
        usersRecord.store();

        return uuid;
    }

    /**
     * Creates a new {@link PartyProxy} in the DB.
     * @param partyProxy
     * @return
     */
    @Transactional
    public UUID create(PartyProxy partyProxy) {

        PartyRefValue partyRefValue = new PartyRefValue(partyProxy).attributes();

        PartyIdentifiedRecord partyIdentifiedRecord = context.newRecord(PARTY_IDENTIFIED);

        partyIdentifiedRecord.setId(UuidGenerator.randomUUID());

        partyIdentifiedRecord.setPartyRefNamespace(partyRefValue.getNamespace());
        partyIdentifiedRecord.setPartyRefValue(partyRefValue.getValue());
        partyIdentifiedRecord.setPartyRefScheme(partyRefValue.getScheme());
        partyIdentifiedRecord.setPartyRefType(partyRefValue.getType());
        partyIdentifiedRecord.setObjectIdType(partyRefValue.getObjectIdType());
        partyIdentifiedRecord.setSysTenant(tenantService.getCurrentSysTenant());

        List<DvIdentifier> identifierList = Collections.emptyList();

        if (partyProxy instanceof PartyIdentified partyIdentified) {

            identifierList = partyIdentified.getIdentifiers();

            partyIdentifiedRecord.setPartyType(PartyType.party_identified);
            partyIdentifiedRecord.setName(partyIdentified.getName());

            if (partyIdentified instanceof PartyRelated partyRelated) {
                // Overwrite Type
                partyIdentifiedRecord.setPartyType(PartyType.party_related);
                partyIdentifiedRecord.setRelationship(relationshipAsRecord(partyRelated));
            }

        } else {
            partyIdentifiedRecord.setPartyType(PartyType.party_self);
        }

        partyIdentifiedRecord.store();

        RepositoryHelper.executeBulkInsert(
                context,
                identifierList.stream()
                        .map(i -> to(partyIdentifiedRecord.getId(), i))
                        .toList(),
                IDENTIFIER);

        return partyIdentifiedRecord.getId();
    }

    /**
     * Find the uuid a {@link PartyProxy} in the DB wich matches the given <code>partyProxy</code>
     * @param partyProxy
     * @return
     */
    public Optional<UUID> findMatching(PartyProxy partyProxy) {

        if (partyProxy instanceof PartySelf partySelf) {
            return findInDBSelf(partySelf);
        } else if (partyProxy instanceof PartyRelated partyRelated) {
            return findInDBPartyRelated(partyRelated);
        } else if (partyProxy instanceof PartyIdentified partyIdentified) {
            return findInDBIdentified(partyIdentified);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private DvCodedTextRecord relationshipAsRecord(PartyProxy partyProxy) {

        DvCodedText relationship = ((PartyRelated) partyProxy).getRelationship();

        return new DvCodedTextRecord(
                relationship.getValue(),
                new PersistentCodePhrase(relationship.getDefiningCode()).encode(),
                relationship.getFormatting(),
                new PersistentCodePhrase(relationship.getLanguage()).encode(),
                new PersistentCodePhrase(relationship.getEncoding()).encode(),
                new PersistentTermMapping().termMappingRepresentation(relationship.getMappings()));
    }

    private IdentifierRecord to(UUID partyId, DvIdentifier identifier) {

        if (identifier.getId() == null) {

            throw new IllegalArgumentException("DV_IDENTIFIER with null ID");
        }

        IdentifierRecord identifierRecord = context.newRecord(IDENTIFIER);

        identifierRecord.setParty(partyId);
        identifierRecord.setIdValue(identifier.getId());
        identifierRecord.setIssuer(identifier.getIssuer());
        identifierRecord.setAssigner(identifier.getAssigner());
        identifierRecord.setTypeName(identifier.getType());
        identifierRecord.setSysTenant(tenantService.getCurrentSysTenant());

        return identifierRecord;
    }

    private Optional<UUID> findInDBSelf(PartySelf partyProxy) {
        Optional<UUID> partySelfUUID = findInDB(partyProxy.getExternalRef());

        if (partySelfUUID.isEmpty() && partyProxy.getExternalRef() == null) { // find the generic PARTY_SELF in DB
            PartyIdentifiedRecord partyIdentifiedRecord = context.fetchAny(
                    PARTY_IDENTIFIED,
                    PARTY_IDENTIFIED
                            .PARTY_REF_VALUE
                            .isNull()
                            .and(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.isNull())
                            .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.isNull())
                            .and(PARTY_IDENTIFIED.PARTY_REF_TYPE.isNull())
                            .and(PARTY_IDENTIFIED.PARTY_TYPE.eq(PartyType.party_self)));

            partySelfUUID = Optional.ofNullable(partyIdentifiedRecord).map(PartyIdentifiedRecord::getId);
        }

        return partySelfUUID;
    }

    private Optional<UUID> findInDBPartyRelated(PartyRelated partyProxy) {
        Optional<UUID> uuid = findInDB(partyProxy.getExternalRef());

        // see https://www.postgresql.org/docs/11/rowtypes.html for syntax on accessing specific attributes in UDT
        if (uuid.isEmpty() && partyProxy.getExternalRef() == null) { // check for the same name and same relationship
            PartyIdentifiedRecord partyIdentifiedRecord = context.fetchAny(
                    PARTY_IDENTIFIED,
                    PARTY_IDENTIFIED
                            .PARTY_REF_VALUE
                            .isNull()
                            .and(PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.isNull())
                            .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.isNull())
                            .and(PARTY_IDENTIFIED.PARTY_REF_TYPE.isNull())
                            .and(PARTY_IDENTIFIED.NAME.eq(partyProxy.getName()))
                            .and(DSL.field("(" + PARTY_IDENTIFIED.RELATIONSHIP + ").value")
                                    .eq(relationshipAsRecord(partyProxy).getValue()))
                            .and(PARTY_IDENTIFIED.PARTY_TYPE.eq(PartyType.party_related)));

            if (partyIdentifiedRecord != null) {
                uuid = Optional.ofNullable(partyIdentifiedRecord.getId());
                // check for identifiers
                if (compare(partyIdentifiedRecord, partyProxy.getIdentifiers())) {
                    uuid = Optional.empty();
                }
            }
        }

        return uuid;
    }

    private boolean compare(PartyIdentifiedRecord partyIdentifiedRecord, List<DvIdentifier> identifiers) {
        List<DvIdentifier> identifiersFromDB =
                context.fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId())).stream()
                        .map(idConvert::apply)
                        .toList();

        return compare(identifiersFromDB, identifiers);
    }

    private static final Function<IdentifierRecord, DvIdentifier> idConvert = identifierRecord -> {
        DvIdentifier identifier = new DvIdentifier();
        identifier.setIssuer(identifierRecord.getIssuer());
        identifier.setAssigner(identifierRecord.getAssigner());
        identifier.setId(identifierRecord.getIdValue());
        identifier.setType(identifierRecord.getTypeName());
        return identifier;
    };

    private boolean compare(List<DvIdentifier> identifiersFromDB, List<DvIdentifier> identifiers) {

        if (identifiersFromDB == null && identifiers == null) {
            return true;
        }

        if (identifiersFromDB == null || identifiers == null) {
            return false;
        }

        if (identifiersFromDB.size() != identifiers.size()) {
            return false;
        }

        List<DvIdentifier> filteredList = identifiersFromDB.stream()
                .filter(identifier -> identifiers.stream()
                        .anyMatch(identifier1 -> Objects.equals(identifier1.getType(), identifier.getType())
                                && Objects.equals(identifier1.getId(), identifier.getId())
                                && Objects.equals(identifier1.getAssigner(), identifier.getAssigner())
                                && Objects.equals(identifier1.getIssuer(), identifier.getIssuer())))
                .toList();

        return filteredList.size() == identifiersFromDB.size();
    }

    private Optional<UUID> findInDBIdentified(PartyIdentified partyProxy) {
        Optional<UUID> uuid = findInDB(partyProxy.getExternalRef());

        // check that name matches the one already stored in DB, otherwise throw an exception (conflicting
        // identification)
        if (uuid.isPresent()) {
            PartyIdentifiedRecord partyIdentifiedRecord =
                    context.fetchAny(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(uuid.get()));
            if (partyIdentifiedRecord == null) {
                throw new InternalServerException("Inconsistent PartyIdentified UUID:" + uuid);
            }
            if (!partyIdentifiedRecord.getName().equals(partyProxy.getName())) {
                throw new IllegalArgumentException("Conflicting identification, existing name was:"
                        + partyIdentifiedRecord.get(PARTY_IDENTIFIED.NAME)
                        + ", but found passed name:"
                        + partyProxy.getName());
            }

            // check for identifiers
            if (compare(partyIdentifiedRecord, partyProxy.getIdentifiers())) {
                uuid = Optional.empty();
            }
        }

        return uuid;
    }

    private Optional<UUID> findInDB(PartyRef partyRef) {

        if (partyRef == null) {
            return Optional.empty();
        }

        Object ref = partyRef.getId();
        PartyIdentifiedRecord partyIdentifiedRecord;

        if (ref instanceof ObjectVersionId || ref instanceof HierObjectId) {

            ObjectId objectId = (ObjectId) ref;

            partyIdentifiedRecord = context.fetchAny(
                    PARTY_IDENTIFIED,
                    PARTY_IDENTIFIED
                            .PARTY_REF_NAMESPACE
                            .eq(partyRef.getNamespace())
                            .and(PARTY_IDENTIFIED.PARTY_REF_VALUE.eq(objectId.getValue())));

        } else if (ref instanceof GenericId genericId) {

            partyIdentifiedRecord = context.fetchAny(
                    PARTY_IDENTIFIED,
                    PARTY_IDENTIFIED
                            .PARTY_REF_NAMESPACE
                            .eq(partyRef.getNamespace())
                            .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.eq(genericId.getScheme()))
                            .and(PARTY_IDENTIFIED.PARTY_REF_VALUE.eq(genericId.getValue())));
        } else {
            throw new IllegalStateException(
                    "Unsupported PartyRef identification:" + ref.getClass().getSimpleName());
        }

        return Optional.ofNullable(partyIdentifiedRecord).map(PartyIdentifiedRecord::getId);
    }
}
