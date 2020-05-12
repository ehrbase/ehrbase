/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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

package org.ehrbase.ehr.encode;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDuration;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import com.nedap.archie.rm.generic.Participation;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.PartyRef;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.ehr.encode.wrappers.json.writer.*;
import org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw.ArrayListAdapter;
import org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw.LinkedTreeMapAdapter;

import java.util.ArrayList;

/**
 * Created by christian on 9/9/2016.
 */
public class EncodeUtilArchie {

    /**
     * utility to make sure writer adapter are set consistently
     *
     * @return GsonBuilder
     */
    public static GsonBuilder getGsonBuilderInstance() {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(DvDateTime.class, new DvDateTimeAdapter())
                .registerTypeAdapter(DvDate.class, new DvDateAdapter())
                .registerTypeAdapter(DvTime.class, new DvTimeAdapter())
                .registerTypeAdapter(DvDuration.class, new DvDurationAdapter())
                .registerTypeAdapter(DvText.class, new DvTextAdapter())
                .registerTypeAdapter(DvCodedText.class, new DvCodedTextAdapter())
                .registerTypeAdapter(CodePhrase.class, new CodePhraseAdapter())
                .registerTypeAdapter(Participation.class, new ParticipationAdapter())
                .registerTypeAdapter(PartyIdentified.class, new PartyIdentifiedAdapter())
                .registerTypeAdapter(PartyRef.class, new PartyRefAdapter());
        return builder;
    }

    public static GsonBuilder getGsonBuilderInstance(I_DvTypeAdapter.AdapterType dbjson2rawjson) {
        switch (dbjson2rawjson) {
            case DBJSON2RAWJSON:

                GsonBuilder builder = new GsonBuilder()
                        .registerTypeAdapter(LinkedTreeMap.class, new LinkedTreeMapAdapter())
                        .registerTypeAdapter(ArrayList.class, new ArrayListAdapter());
                return builder;
            default:
                throw new RuntimeException();
        }
    }
}
