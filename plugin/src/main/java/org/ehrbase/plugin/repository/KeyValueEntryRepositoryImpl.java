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
package org.ehrbase.plugin.repository;

import static org.ehrbase.jooq.pg.tables.Plugin.PLUGIN;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ehrbase.api.repository.KeyValuePair;
import org.ehrbase.api.repository.KeyValuePairRepository;
import org.ehrbase.jooq.pg.tables.records.PluginRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class KeyValueEntryRepositoryImpl implements KeyValuePairRepository {
    private final DSLContext ctx;

    public KeyValueEntryRepositoryImpl(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<KeyValuePair> findAllBy(String context) {
        return ctx.fetchStream(PLUGIN, PLUGIN.PLUGINID.eq(context))
                .map(rec -> toKvp.apply(rec))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<KeyValuePair> findBy(String context, String key) {
        return ctx.fetchOptional(PLUGIN, PLUGIN.PLUGINID.eq(context).and(PLUGIN.KEY.eq(key)))
                .map(rec -> toKvp.apply(rec));
    }

    @Override
    public KeyValuePair save(KeyValuePair kve) {
        PluginRecord rec = ctx.newRecord(PLUGIN);
        rec.setId(kve.getId());
        rec.setPluginid(kve.getContext());
        rec.setKey(kve.getKey());
        rec.setValue(kve.getValue());

        rec.insert();
        return kve;
    }

    @Override
    public Optional<KeyValuePair> findBy(UUID uid) {
        return ctx.fetchOptional(PLUGIN, PLUGIN.ID.eq(uid)).map(rec -> toKvp.apply(rec));
    }

    @Override
    public boolean deleteBy(UUID uid) {
        int res = ctx.delete(PLUGIN).where(PLUGIN.ID.eq(uid)).execute();
        return res > 0;
    }

    private Function<PluginRecord, KeyValuePair> toKvp =
            rec -> KeyValuePair.of(rec.getId(), rec.getPluginid(), rec.getKey(), rec.getValue());
}
