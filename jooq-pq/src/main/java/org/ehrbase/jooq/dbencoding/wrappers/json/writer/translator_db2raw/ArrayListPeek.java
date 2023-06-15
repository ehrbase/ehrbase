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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

import com.google.gson.internal.LinkedTreeMap;
import java.util.ArrayList;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;

public class ArrayListPeek {

    ArrayList arrayList;

    public ArrayListPeek(ArrayList arrayList) {
        this.arrayList = arrayList;
    }

    public String findClass() {

        String classInArray = null;

        for (Object map : arrayList) {

            if (map instanceof LinkedTreeMap) {
                if (((LinkedTreeMap) map).containsKey(CompositionSerializer.TAG_CLASS)) {
                    //                                writeNameAsValue(writer, (ArrayList) value);
                    Object classValue = (((LinkedTreeMap) map).get(CompositionSerializer.TAG_CLASS));
                    if (classValue instanceof String) classInArray = (String) classValue;
                    else if (classValue instanceof ArrayList) classInArray = (String) ((ArrayList) classValue).get(0);
                    else throw new IllegalArgumentException("Could not handle class tag in array");
                }
            }
        }

        return classInArray;
    }
}
