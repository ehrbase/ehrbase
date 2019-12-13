/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
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
package org.ehrbase.validation.terminology.validator;

import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;

public class DvMultimedia extends TerminologyCheck{

    public DvMultimedia() {
        this.RM_CLASS = com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia.class;
    }

    public static void check(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, String context, com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia dvMultimedia) throws Exception {
       check(terminologyInterface, codesetMapping, context, dvMultimedia, "en");
    }

    public static void check(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, String context, com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia dvMultimedia, String language) throws Exception {
        if (dvMultimedia.getIntegrityCheckAlgorithm() != null)
            validate(terminologyInterface, codesetMapping, "integrity_check_algorithm", dvMultimedia.getIntegrityCheckAlgorithm(), language);

        if (dvMultimedia.getCompressionAlgorithm() != null)
            validate(terminologyInterface, codesetMapping,"compression_algorithm", dvMultimedia.getCompressionAlgorithm(), language);

        if (dvMultimedia.getMediaType() != null)
            validate(terminologyInterface, codesetMapping,"media_type", dvMultimedia.getMediaType(), language);
    }
}
