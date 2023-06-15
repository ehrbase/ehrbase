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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;

/**
 * GSON adapter for DvDateTime
 * Required since JSON does not support natively a DateTime data type
 */
public abstract class DvTypeAdapter<T> extends TypeAdapter<T> implements I_DvTypeAdapter {

    protected AdapterType adapterType;

    protected static final String VALUE = "value";
    protected static final String EPOCH_OFFSET = "epoch_offset";

    public DvTypeAdapter(AdapterType adapterType) {
        super();
        this.adapterType = adapterType;
    }

    public DvTypeAdapter() {
        super();
        this.adapterType = AdapterType.PG_JSONB;
    }

    //	@Override
    public T read(JsonReader arg0) throws IOException {
        return null;
    }

    //	@Override
    public void write(JsonWriter writer, T dvalue) throws IOException {}
}
