/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School).

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
package org.ehrbase.dao.access.interfaces;

import org.ehrbase.dao.access.jooq.ContextAccess;
import com.nedap.archie.rm.composition.EventContext;
import org.ehrbase.api.exception.InternalServerException;
import org.jooq.Result;

import java.sql.Timestamp;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;

/**
 * Event Context access layer
 * ETHERCIS Project
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_ContextAccess extends I_SimpleCRUD<I_ContextAccess, UUID> {

    /**
     * get a new access layer instance to the table
     *
     * @param domain       SQL context
     * @param eventContext an {@link EventContext} instance
     * @return an <b>uncommitted</b> interface to the access layer
     * @see EventContext
     */
    static I_ContextAccess getInstance(I_DomainAccess domain, EventContext eventContext) {
        return new ContextAccess(domain.getContext(), domain.getServerConfig(), eventContext);
    }

    /**
     * retrieve an Event Context access layer instance from the DB
     *
     * @param domainAccess SQL context
     * @param id           the event context id
     * @return an interface to the access layer
     */
    static I_ContextAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        return ContextAccess.retrieveInstance(domainAccess, id);
    }

    static I_ContextAccess retrieveInstance(I_DomainAccess domainAccess, Result<?> records) {
        return ContextAccess.retrieveInstance(domainAccess, records);
    }

    /**
     * quick delete...
     *
     * @param domainAccess SQL context
     * @param id           event context id
     * @return 1 on success, 0 otherwise
     */
    static Integer delete(I_DomainAccess domainAccess, UUID id) {
        return domainAccess.getContext().delete(EVENT_CONTEXT).where(EVENT_CONTEXT.ID.eq(id)).execute();
    }

    /**
     * Retrieves an EventContext for a specific historical time.
     * @param domainAccess Access object
     * @param id ID of composition the context is connected to
     * @param transactionTime Historical time of the context
     * @return New {@link EventContext} object matching the given time or null if not available.
     * @throws InternalServerException on failure of decoding DvText or DvDateTime
     */
    static EventContext retrieveHistoricalEventContext(I_DomainAccess domainAccess, UUID id, Timestamp transactionTime) {
        return ContextAccess.retrieveHistoricalEventContext(domainAccess, id, transactionTime);
    }

    /**
     * Creates an EventContext object from already set record data of an already existing ContextAccess instance.
     * @return {@link EventContext} object representing this instance's data
     * @throws InternalServerException on failure of decoding DvText or DvDateTime
     */
    EventContext mapRmEventContext();

    /**
     * Retrieves otherContext from event context record, which is represented as json blob in the DB.
     * @return Json representation of otherContext
     */
    String getOtherContextJson();

    void setCompositionId(UUID compositionId);

    UUID getId();
}
