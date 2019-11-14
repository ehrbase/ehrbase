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

package org.ehrbase.dao.access.jooq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_ContainmentAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.ehr.encode.ItemStack;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.CONTAINMENT;

/**
 * Created by christian on 6/1/2016.
 */
public class ContainmentAccess extends DataAccess implements I_ContainmentAccess {

    private static final Logger log = LogManager.getLogger(ContainmentAccess.class);

    private UUID entryId;
    private UUID compositionId;

    private Map<String, String> ltree;

    public ContainmentAccess(DataAccess dataAccess, UUID entryId, String archetypeId, Map<String, String> ltreeMap, boolean debug) {
        super(dataAccess.getContext(), null, null, dataAccess.getServerConfig());
        ltree = new HashMap<>();
        this.entryId = entryId;

        //initial label and path
        String rootArchetype = ItemStack.normalizeLabel(archetypeId);
        ltree.put(rootArchetype, "/composition[" + archetypeId + "]");

        for (Map.Entry entry : ltreeMap.entrySet()) {
            String label = rootArchetype + "." + entry.getKey().toString();
            String path = entry.getValue().toString();
            ltree.put(label, path);
            if (debug)
                log.debug("LABEL:" + label + "|PATH:" + path);
        }
    }

    /**
     * @throws IllegalArgumentException when committing was aborted
     */
    @Override
    public UUID commit(Timestamp transactionTime) {
        return commit();
    }

    /**
     * @throws IllegalArgumentException when committing was aborted
     */
    @Override
    public UUID commit() {
        commitContainments();
        return null;
    }

    /**
     * @throws IllegalArgumentException when committing was aborted
     */
    @Override
    public Boolean update(Timestamp transactionTime) {
        return update();
    }

    /**
     * @throws IllegalArgumentException when committing was aborted
     */
    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        return update();
    }

    /**
     * @throws IllegalArgumentException when committing was aborted
     */
    @Override
    public Boolean update() {
        commitContainments();
        return true;
    }

    /**
     * @throws IllegalArgumentException when committing was aborted
     */
    @Override
    public Boolean update(Boolean force)  {
        return update();
    }

    /**
     * Executes the committing of the containments
     *
     * @throws IllegalArgumentException when containment label tree is not initialized
     */
    //TODO: changes this to work on template instead and avoid the loop for writing!
    private void commitContainments() {
        if (ltree == null) {
            throw new IllegalArgumentException("Containment label tree is not initialized, aborting");
        }
        //if entries exists already for this entry delete them
        if (getContext().fetchExists(CONTAINMENT, CONTAINMENT.COMP_ID.eq(compositionId))) {
            getContext().delete(CONTAINMENT).where(CONTAINMENT.COMP_ID.eq(compositionId)).execute();
        }

        //insert the new containment for this composition
        for (Map.Entry entry : ltree.entrySet()) {
            getContext().insertInto(CONTAINMENT, CONTAINMENT.COMP_ID, CONTAINMENT.LABEL, CONTAINMENT.PATH)
                    .values(DSL.val(compositionId), DSL.field(DSL.val(entry.getKey().toString()) + "::ltree"), DSL.val(entry.getValue().toString()))
                    .execute();
        }
    }

    /**
     * @throws InternalServerException because inherited interface function isn't implemented in this class
     * @deprecated
     */
    @Deprecated
    @Override
    public Integer delete() {
        throw new InternalServerException("INTERNAL: Invalid delete call, this signature is not supported in ContainmentAccess");
    }

    @Override
    public void setCompositionId(UUID compositionId) {
        this.compositionId = compositionId;
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
