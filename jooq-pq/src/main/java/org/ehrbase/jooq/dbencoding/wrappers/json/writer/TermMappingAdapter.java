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

import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.datavalues.TermMapping;
import java.io.IOException;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Created by christian on 4/3/2017.
 */
public class TermMappingAdapter extends DvTypeAdapter<TermMapping> {

    private final DvCodedTextAdapter codedTextAdapter;
    private final CodePhraseAdapter codePhraseAdapter;

    public TermMappingAdapter(AdapterType adapterType) {
        super(adapterType);
        codedTextAdapter = new DvCodedTextAdapter(adapterType);
        codePhraseAdapter = new CodePhraseAdapter(adapterType);
    }

    public void write(JsonWriter writer, List<TermMapping> termMappings) throws IOException {

        if (CollectionUtils.isNotEmpty(termMappings)) {
            writer.name("mappings");
            writer.beginArray(); // [
            for (TermMapping termMapping : termMappings) {
                write(writer, termMapping);
            }
            writer.endArray(); //
        }
    }

    @Override
    public void write(JsonWriter writer, TermMapping termMapping) throws IOException {
        writer.beginObject(); // {
        writer.name("_type").value("TERM_MAPPING");
        writer.name("match").value(Character.toString(termMapping.getMatch()));
        writer.name("purpose");
        codedTextAdapter.write(writer, termMapping.getPurpose());
        writer.name("target");
        codePhraseAdapter.write(writer, termMapping.getTarget());
        writer.endObject(); // }
    }
}
