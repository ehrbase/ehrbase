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

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.ehrbase.api.repository.KeyValuePair;
import org.ehrbase.api.repository.KeyValuePairRepository;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

@Component
public class KeyValueEntryRepositoryImpl implements KeyValuePairRepository {

    private static final org.jooq.Table<?> PLUGIN_CONFIG = table(name("ehr_system", "plugin_config"));

    private final DSLContext ctx;

    public KeyValueEntryRepositoryImpl(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<KeyValuePair> findAllBy(String context) {
        return ctx
                .select()
                .from(PLUGIN_CONFIG)
                .where(field(name("pluginid"), String.class).eq(context))
                .fetch()
                .stream()
                .map(this::toKvp)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<KeyValuePair> findBy(String context, String key) {
        Record rec = ctx.select()
                .from(PLUGIN_CONFIG)
                .where(field(name("pluginid"), String.class)
                        .eq(context)
                        .and(field(name("key"), String.class).eq(key)))
                .fetchOne();
        return Optional.ofNullable(rec).map(this::toKvp);
    }

    @Override
    public KeyValuePair save(KeyValuePair kve) {
        ctx.insertInto(PLUGIN_CONFIG)
                .set(field(name("id"), UUID.class), kve.getId())
                .set(field(name("pluginid"), String.class), kve.getContext())
                .set(field(name("key"), String.class), kve.getKey())
                .set(field(name("value"), String.class), kve.getValue())
                .execute();
        return kve;
    }

    @Override
    public Optional<KeyValuePair> findBy(UUID uid) {
        Record rec = ctx.select()
                .from(PLUGIN_CONFIG)
                .where(field(name("id"), UUID.class).eq(uid))
                .fetchOne();
        return Optional.ofNullable(rec).map(this::toKvp);
    }

    @Override
    public boolean deleteBy(UUID uid) {
        int res = ctx.deleteFrom(PLUGIN_CONFIG)
                .where(field(name("id"), UUID.class).eq(uid))
                .execute();
        return res > 0;
    }

    private KeyValuePair toKvp(Record rec) {
        return KeyValuePair.of(
                rec.get(field(name("id"), UUID.class)),
                rec.get(field(name("pluginid"), String.class)),
                rec.get(field(name("key"), String.class)),
                rec.get(field(name("value"), String.class)));
    }
}
