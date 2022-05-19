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
package org.ehrbase.dao.access.jooq;

import static org.ehrbase.jooq.pg.Tables.SYSTEM;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.interfaces.I_SystemAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.SystemRecord;
import org.joda.time.DateTime;

/**
 * Created by Christian Chevalley on 4/20/2015.
 */
public class SystemAccess extends DataAccess implements I_SystemAccess {

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
        String settings = domainAccess.getServerConfig().getNodename();

        // try to retrieve and return if successful, otherwise create
        UUID res = retrieveInstanceId(domainAccess, settings);
        if (res == null) {
            return new SystemAccess(domainAccess, "DEFAULT RUNNING SYSTEM", settings).commit();
        } else return res;
    }

    public static UUID createOrRetrieveInstanceId(I_DomainAccess domainAccess, String description, String settings) {
        // try to retrieve and return if successful, otherwise create
        UUID res = retrieveInstanceId(domainAccess, settings);
        if (res == null) {
            if (description == null) description = "default";
            return new SystemAccess(domainAccess, description, settings).commit();
        } else return res;
    }

    /**
     * @throws IllegalArgumentException if couldn't retrieve instance with given settings
     */
    public static UUID retrieveInstanceId(I_DomainAccess domainAccess, String settings) {
        UUID uuid;

        try {
            uuid = Optional.ofNullable(domainAccess.getContext().fetchAny(SYSTEM, SYSTEM.SETTINGS.eq(settings)))
                    .map(SystemRecord::getId)
                    .orElse(null);

            if (uuid == null) {
                return null;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not getNewFolderAccessInstance settings:" + settings + " Exception:" + e);
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
