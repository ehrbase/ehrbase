/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.pg;

import org.ehrbase.jooq.pg.udt.CodePhrase;
import org.ehrbase.jooq.pg.udt.DvCodedText;

/**
 * Convenience access to all UDTs in ehr.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class UDTs {

    /**
     * The type <code>ehr.code_phrase</code>
     */
    public static final CodePhrase CODE_PHRASE = org.ehrbase.jooq.pg.udt.CodePhrase.CODE_PHRASE;

    /**
     * The type <code>ehr.dv_coded_text</code>
     */
    public static final DvCodedText DV_CODED_TEXT = org.ehrbase.jooq.pg.udt.DvCodedText.DV_CODED_TEXT;
}
