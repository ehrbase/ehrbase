/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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
package org.ehrbase.dao.access.jooq;

import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.apache.catalina.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_PartyIdentifiedAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.IdentifierRecord;
import org.ehrbase.jooq.pg.tables.records.PartyIdentifiedRecord;
import org.jooq.DSLContext;
import org.jooq.Result;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.ehrbase.jooq.pg.Tables.IDENTIFIER;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

/**
 * Created by Christian Chevalley on 4/10/2015.
 */
public class PartyIdentifiedAccess extends DataAccess implements I_PartyIdentifiedAccess {

    private static Logger log = LogManager.getLogger(PartyIdentifiedAccess.class);
    private PartyIdentifiedRecord partyIdentifiedRecord;
    private Map<String, IdentifierRecord> identifiers;

    public PartyIdentifiedAccess(DSLContext context, ServerConfig serverConfig, String partyName) {
        super(context, null, null, serverConfig);
        partyIdentifiedRecord = context.newRecord(PARTY_IDENTIFIED);
        partyIdentifiedRecord.setName(partyName);
    }

    /**
     * Internal constructor to get empty access
     */
    private PartyIdentifiedAccess(DSLContext context, ServerConfig serverConfig) {
        super(context, null, null, serverConfig);
    }

    public static I_PartyIdentifiedAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        DSLContext context = domainAccess.getContext();
        PartyIdentifiedRecord record = context.fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(id));

        if (record == null)
            return null;

        PartyIdentifiedAccess partyIdentifiedAccess = new PartyIdentifiedAccess(context, domainAccess.getServerConfig());
        partyIdentifiedAccess.partyIdentifiedRecord = record;

        Result<IdentifierRecord> identifierRecords = context.fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(partyIdentifiedAccess.partyIdentifiedRecord.getId()));

        for (IdentifierRecord identifierRecord : identifierRecords) {
            if (partyIdentifiedAccess.identifiers == null) {
                partyIdentifiedAccess.identifiers = new HashMap<>();
            }

            partyIdentifiedAccess.identifiers.put(makeMapKey(identifierRecord.getIdValue(), identifierRecord.getIssuer()), identifierRecord);
        }

        return partyIdentifiedAccess;
    }

    public static UUID retrievePartyIdByPartyName(I_DomainAccess domainAccess, String partyName) {
        if (domainAccess.getContext().fetchExists(PARTY_IDENTIFIED, PARTY_IDENTIFIED.NAME.eq(partyName))) {
            UUID uuid = domainAccess.getContext().fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.NAME.eq(partyName)).getId();
            return uuid;
        }

        return null;
    }

    private static String makeMapKey(String s1, String s2) {
        return s1 + ":" + s2;
    }

    public static UUID getOrCreateParty(I_DomainAccess domainAccess, String name, String idCode, String issuer, String assigner, String typeName) {
        DSLContext context1 = domainAccess.getContext();
        //check if it exists first with idCode and issuer
        if (context1.fetchExists(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(idCode).and(IDENTIFIER.ISSUER.eq(issuer))))
            return context1.fetchOne(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(idCode).and(IDENTIFIER.ISSUER.eq(issuer))).getParty();

        //check if a party exists with the same name, if found, the identifier is just added to the list
        if (context1.fetchExists(PARTY_IDENTIFIED, PARTY_IDENTIFIED.NAME.eq(name))) {
            UUID partyIdentifiedUuid = context1.fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.NAME.eq(name)).getId();
            //add identifier to the list
            if (idCode != null && issuer != null)
                context1.insertInto(IDENTIFIER, IDENTIFIER.PARTY, IDENTIFIER.ID_VALUE, IDENTIFIER.ISSUER, IDENTIFIER.ASSIGNER, IDENTIFIER.TYPE_NAME)
                        .values(partyIdentifiedUuid, idCode, issuer, assigner, typeName)
                        .execute();
            return partyIdentifiedUuid;
        } else {
            //storeComposition a new party identified
            UUID partyIdentifiedUuid = context1
                    .insertInto(PARTY_IDENTIFIED, PARTY_IDENTIFIED.NAME)
                    .values(name)
                    .returning(PARTY_IDENTIFIED.ID)
                    .fetchOne().getId();
            //and storeComposition the identifier
            if (idCode != null && issuer != null)
                context1.insertInto(IDENTIFIER, IDENTIFIER.PARTY, IDENTIFIER.ID_VALUE, IDENTIFIER.ISSUER, IDENTIFIER.ASSIGNER, IDENTIFIER.TYPE_NAME)
                        .values(partyIdentifiedUuid, idCode, issuer, assigner, typeName)
                        .execute();
            return partyIdentifiedUuid;
        }
    }

    public static UUID findIdentifiedParty(DSLContext context, List<DvIdentifier> identifierList) {

        if (identifierList == null)
            return null;

        for (DvIdentifier identifier : identifierList) {
            if (context.fetchExists(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(identifier.getId()).and(IDENTIFIER.ISSUER.eq(identifier.getIssuer()))))
                return context.fetchOne(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(identifier.getId()).and(IDENTIFIER.ISSUER.eq(identifier.getIssuer()))).getParty();
        }

        return null;
    }

    public static UUID findReferencedParty(DSLContext context, PartyRef partyRef) {

        if (partyRef == null)
            return null;

        Object ref = partyRef.getId();

        if (ref instanceof GenericId) {

            GenericId genericID = (GenericId) ref;

            if (context.fetchExists(PARTY_IDENTIFIED,
                    PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.eq(partyRef.getNamespace())
//                            .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.eq(genericID.getScheme()))
                            .and(PARTY_IDENTIFIED.PARTY_REF_VALUE.eq(genericID.getValue())))) {

                return context.fetchAny(PARTY_IDENTIFIED,
                        PARTY_IDENTIFIED.PARTY_REF_NAMESPACE.eq(partyRef.getNamespace())
//                                .and(PARTY_IDENTIFIED.PARTY_REF_SCHEME.eq(genericID.getScheme()))
                                .and(PARTY_IDENTIFIED.PARTY_REF_VALUE.eq(genericID.getValue()))).getId();
            }

//                return context.fetchOne(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(identifier.getId()).and(IDENTIFIER.ISSUER.eq(identifier.getIssuer()))).getParty();
        }

        return null;
    }

    public static UUID getOrCreateParty(I_DomainAccess domainAccess, PartyIdentified partyIdentified) {
        DSLContext context1 = domainAccess.getContext();
        //check if it exists first with idCode and issuer
        //check with external ref if any

        UUID identifiedParty = null;

        PartyRef externalRef = partyIdentified.getExternalRef();
        List<DvIdentifier> identifierList = partyIdentified.getIdentifiers();
        GenericId genericID = null;

        if (externalRef != null) {
            Object ref = externalRef.getId();

            if (ref instanceof GenericId) {
                genericID = (GenericId) ref;
            } else if (ref instanceof HierObjectId)
                genericID = null;
            else
                log.warn("Passed partyIdentified does not contain a GenericID in external ref:" + partyIdentified.toString());
        }

        if (externalRef != null) {
            identifiedParty = findReferencedParty(domainAccess.getContext(), externalRef);
        } else {

            if (identifierList != null && !identifierList.isEmpty())
                identifiedParty = findIdentifiedParty(domainAccess.getContext(), identifierList);
        }
        if (identifiedParty != null)
            return identifiedParty;

        //store a new party identified
        UUID partyIdentifiedUuid = context1
                .insertInto(PARTY_IDENTIFIED,
                        PARTY_IDENTIFIED.NAME,
                        PARTY_IDENTIFIED.PARTY_REF_NAMESPACE,
                        PARTY_IDENTIFIED.PARTY_REF_VALUE,
                        PARTY_IDENTIFIED.PARTY_REF_SCHEME,
                        PARTY_IDENTIFIED.PARTY_REF_TYPE)
                .values(partyIdentified.getName(),
                        externalRef != null ? externalRef.getNamespace() : null,
                        genericID != null ? genericID.getValue() : null,
                        genericID != null ? genericID.getScheme() : null,
                        externalRef != null ? externalRef.getType() : null)
                .returning(PARTY_IDENTIFIED.ID)
                .fetchOne().getId();
        //and store the identifier if any
        if (identifierList != null) {
            for (DvIdentifier identifier : identifierList) {
                if (identifier.getId() != null && identifier.getIssuer() != null)
                    context1.insertInto(IDENTIFIER, IDENTIFIER.PARTY, IDENTIFIER.ID_VALUE, IDENTIFIER.ISSUER, IDENTIFIER.ASSIGNER, IDENTIFIER.TYPE_NAME)
                            .values(partyIdentifiedUuid, identifier.getId(), identifier.getIssuer(), identifier.getAssigner(), identifier.getType())
                            .execute();
            }
        }
        return partyIdentifiedUuid;
    }

    public static PartyIdentified retrievePartyIdentified(I_DomainAccess domainAccess, UUID id) {
        PartyRef partyRef = null;
        if (!(domainAccess.getContext().fetchExists(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(id))))
            return null;

        //rebuild an identified party
        List<DvIdentifier> identifierList = new ArrayList<>();

        domainAccess.getContext().fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(id)).forEach(record -> {
            DvIdentifier identifier = new DvIdentifier();
            identifier.setIssuer(record.getIssuer());
            identifier.setAssigner(record.getAssigner());
            identifier.setId(record.getIdValue());
            identifier.setType(record.getTypeName());
            identifierList.add(identifier);
        });

        PartyIdentifiedRecord identifiedRecord = domainAccess.getContext().fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(id));

        if (identifiedRecord.getPartyRefType() != null) {
            if (identifiedRecord.getPartyRefValue() != null && identifiedRecord.getPartyRefScheme() != null) {
                GenericId genericID = new GenericId(identifiedRecord.getPartyRefValue(), identifiedRecord.getPartyRefScheme());
                partyRef = new PartyRef(genericID, identifiedRecord.getPartyRefNamespace(), identifiedRecord.getPartyRefType());
            } else {
                ObjectId objectID = new HierObjectId("ref");
                partyRef = new PartyRef(objectID, identifiedRecord.getPartyRefNamespace(), identifiedRecord.getPartyRefType());
            }
        }

        PartyIdentified partyIdentified = new PartyIdentified(partyRef,
                identifiedRecord.getName(),
                identifierList.isEmpty() ? null : identifierList);

        return partyIdentified;
    }

    public static PartyIdentified retrievePartyIdentified(String name, String refScheme, String refNamespace, String refValue, String refType) {
        PartyRef partyRef = null;

        //rebuild an identified party
        List<DvIdentifier> identifierList = new ArrayList<>();

//        domainAccess.getContext().fetch(IDENTIFIER, IDENTIFIER.PARTY.eq(id)).forEach(record -> {
//            DvIdentifier identifier = new DvIdentifier(record.getIssuer(), record.getAssigner(), record.getIdValue(), record.getTypeName());
//            identifierList.add(identifier);
//        });

//        PartyIdentifiedRecord identifiedRecord = domainAccess.getContext().fetchOne(PARTY_IDENTIFIED, PARTY_IDENTIFIED.ID.eq(id));

        if (refType != null) {
            if (refValue != null && refScheme != null) {
                GenericId genericID = new GenericId(refValue, refScheme);
                partyRef = new PartyRef(genericID, refNamespace, refType);
            } else {
                ObjectId objectID = new HierObjectId("ref");
                partyRef = new PartyRef(objectID, refNamespace, refType);
            }
        }

        if (name == null && partyRef == null)
            return null;

        PartyIdentified partyIdentified = new PartyIdentified(partyRef,
                name,
                identifierList.isEmpty() ? null : identifierList);

        return partyIdentified;
    }

    // TODO not used at all. will it get used with ehr status?
    @Override
    public UUID commit(Timestamp transactionTime) {
        partyIdentifiedRecord.store();

        if (identifiers != null) {
            for (IdentifierRecord identifierRecord : identifiers.values()) {
                identifierRecord.setParty(partyIdentifiedRecord.getId());
                getContext().insertInto(IDENTIFIER, IDENTIFIER.PARTY, IDENTIFIER.ID_VALUE, IDENTIFIER.ISSUER, IDENTIFIER.ASSIGNER, IDENTIFIER.TYPE_NAME)
                        .values(identifierRecord.getParty(), identifierRecord.getIdValue(), identifierRecord.getIssuer(), identifierRecord.getAssigner(), identifierRecord.getTypeName())
                        .execute();
                log.debug("Create identifier for party:" + identifierRecord.getParty());
            }
        }

        log.debug("created party:" + partyIdentifiedRecord.getId());

        return partyIdentifiedRecord.getId();
    }

    // TODO not used at all. will it get used with ehr status?
    @Override
    public UUID commit() {
        return commit(Timestamp.valueOf(LocalDateTime.now()));
    }

    // TODO not used at all. will it get used with ehr status?
    @Override
    public Boolean update(Timestamp transactionTime) {

        int count = 0;

        if (partyIdentifiedRecord.changed()) {
            count += partyIdentifiedRecord.update();
        }

        for (IdentifierRecord identifierRecord : identifiers.values()) {
            if (getContext().fetchExists(IDENTIFIER, IDENTIFIER.ID_VALUE.eq(identifierRecord.getIdValue()).and(IDENTIFIER.ISSUER.eq(identifierRecord.getIssuer())))) {
                //updateComposition this record
                count += getContext().update(IDENTIFIER)
                        .set(IDENTIFIER.ID_VALUE, identifierRecord.getIdValue())
                        .set(IDENTIFIER.ASSIGNER, identifierRecord.getAssigner())
                        .set(IDENTIFIER.ISSUER, identifierRecord.getIssuer())
                        .where(IDENTIFIER.ID_VALUE.eq(identifierRecord.getIdValue()).and(IDENTIFIER.ISSUER.eq(identifierRecord.getIssuer())))
                        .execute();
            } else //add it
                count += getContext().insertInto(IDENTIFIER, IDENTIFIER.PARTY, IDENTIFIER.ID_VALUE, IDENTIFIER.ISSUER, IDENTIFIER.ASSIGNER, IDENTIFIER.TYPE_NAME)
                        .values(partyIdentifiedRecord.getId(), identifierRecord.getIdValue(), identifierRecord.getIssuer(), identifierRecord.getAssigner(), identifierRecord.getTypeName())
                        .execute();
        }


        return count > 0;
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        throw new InternalServerException("INTERNAL: Invalid update call, this signature is not supported in PartyIdentifiedAccess");
    }

    // TODO not used at all. will it get used with ehr status?
    @Override
    public Boolean update() {
        return update(Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update(Boolean force) {
        throw new InternalServerException("INTERNAL: Invalid update call, this signature is not supported in PartyIdentifiedAccess");
    }

    // TODO not used at all. will it get used with ehr status?
    @Override
    public Integer delete() {
        int count = 0;
        //delete corresponding identifiers
        if (identifiers != null) {
            for (IdentifierRecord identifierRecord : identifiers.values()) {
                count += getContext().delete(IDENTIFIER).where(IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId())).execute();
            }
        }
        count += partyIdentifiedRecord.delete();
        return count;
    }

    @Override
    public Integer addIdentifier(String value, String issuer, String assigner, String type) {

        IdentifierRecord identifierRecord = getContext().newRecord(IDENTIFIER);
        identifierRecord.setIdValue(value);
        identifierRecord.setIssuer(issuer);
        identifierRecord.setAssigner(assigner);
        identifierRecord.setTypeName(type);

        if (identifiers == null) {
            identifiers = new HashMap<>();
        }
        identifiers.put(makeMapKey(value, issuer), identifierRecord);

        return identifiers.size();
    }

    @Override
    public Integer deleteIdentifier(String idCode, String issuer) {

        String key = makeMapKey(idCode, issuer);
        identifiers.remove(key);

        return getContext().delete(IDENTIFIER).where(IDENTIFIER.PARTY.eq(partyIdentifiedRecord.getId())
                .and(IDENTIFIER.ID_VALUE.eq(idCode))
                .and(IDENTIFIER.ISSUER.eq(issuer))).execute();
    }

    @Override
    public String getPartyName() {
        return partyIdentifiedRecord.getName();
    }

    @Override
    public void setPartyName(String name) {
        partyIdentifiedRecord.setName(name);
    }

    @Override
    public String getPartyRefValue() {
        return partyIdentifiedRecord.getPartyRefValue();
    }

    @Override
    public String getPartyRefNamespace() {
        return partyIdentifiedRecord.getPartyRefNamespace();
    }

    @Override
    public String[] getIdentifiersKeySet() {
        return identifiers.keySet().toArray(new String[identifiers.size()]);
    }

    @Override
    public UUID getId() {
        return partyIdentifiedRecord.getId();
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
