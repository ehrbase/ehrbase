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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.SystemRecord;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.SYSTEM;

/**
 * Created by Christian Chevalley on 4/20/2015.
 */
public class SystemAccess extends DataAccess implements I_SystemAccess {


    private static final Logger log = LogManager.getLogger(SystemAccess.class);
    private SystemRecord systemRecord;

    public SystemAccess(I_DomainAccess domainAccess) {
        super(domainAccess);

    }

    public SystemAccess(I_DomainAccess domainAccess, String description, String settings) {
        super(domainAccess);

        systemRecord = domainAccess.getContext().newRecord(SYSTEM);
        systemRecord.setDescription(description);
        systemRecord.setSettings(settings);
    }



    public static UUID createOrRetrieveLocalSystem(I_DomainAccess domainAccess) {
        DSLContext context1 = domainAccess.getContext();

        String settings = domainAccess.getServerConfig().getNodename();

        //try to retrieveInstanceByNamedSubject the corresponding entry in the system table
        Result<Record1<UUID>> uuids = context1.select(SYSTEM.ID).from(SYSTEM).where(SYSTEM.SETTINGS.equal(settings)).fetch();

        if (uuids.isEmpty()) { //storeComposition a new default entry

            Record result = context1.insertInto(SYSTEM, SYSTEM.DESCRIPTION, SYSTEM.SETTINGS).values("DEFAULT RUNNING SYSTEM", settings).returning(SYSTEM.ID).fetchOne();

            if (result == null)
                return null;

            return ((SystemRecord) result).getId();
        }

        return (UUID) uuids.get(0).getValue(0);
    }

    /**
     * @throws IllegalArgumentException if couldn't retrieve instance with given settings
     */
    public static UUID retrieveInstanceId(I_DomainAccess domainAccess, String settings) {
        UUID uuid;

        try {
            uuid = Optional.ofNullable(domainAccess.getContext().fetchOne(SYSTEM, SYSTEM.SETTINGS.eq(settings))).map(SystemRecord::getId).orElse(null);

            if (uuid == null) {
                log.warn("Could not retrieveInstanceByNamedSubject system for settings:" + settings);
                return null;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not getNewFolderAccessInstance settings:" + settings + " Exception:" + e);
        }

        return uuid;
    }

    @Override
    public UUID commit(Timestamp transactionTime) {
        systemRecord.store();
        return systemRecord.getId();
    }

    @Override
    public UUID commit() {
        return commit(new Timestamp(DateTime.now().getMillis()));
    }

    @Override
    public Boolean update(Timestamp transactionTime) {

        if (systemRecord.changed()) {
            return systemRecord.update() > 0;
        }

        return false;
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        return update(transactionTime);
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update() {
        throw new InternalServerException("INTERNAL: this update signature is not valid");
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Boolean update(Boolean force) {
        throw new InternalServerException("INTERNAL: this update signature is not valid");
    }

    @Override
    public Integer delete() {
        return systemRecord.delete();
    }

    public static I_SystemAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        SystemAccess systemAccess = new SystemAccess(domainAccess);

        systemAccess.systemRecord = domainAccess.getContext().fetchOne(SYSTEM, SYSTEM.ID.eq(id));

        return systemAccess;
    }

    @Override
    public UUID getId() {
        return systemRecord.getId();
    }

    @Override
    public String getSettings() {
        return systemRecord.getSettings();
    }

    @Override
    public String getDescription() {
        return systemRecord.getDescription();
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
