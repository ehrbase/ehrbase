/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.configuration.config.validation;

import com.nedap.archie.rm.datavalues.DvCodedText;
import java.util.Collections;
import java.util.List;
import org.ehrbase.openehr.sdk.util.functional.Try;
import org.ehrbase.openehr.sdk.validation.ConstraintViolation;
import org.ehrbase.openehr.sdk.validation.ConstraintViolationException;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;

public class NopExternalTerminologyValidation implements ExternalTerminologyValidation {

    private final ConstraintViolation err;

    NopExternalTerminologyValidation(String errorMessage) {
        this.err = new ConstraintViolation(errorMessage);
    }

    public Try<Boolean, ConstraintViolationException> validate(TerminologyParam param) {
        return Try.failure(new ConstraintViolationException(List.of(err)));
    }

    public boolean supports(TerminologyParam param) {
        return false;
    }

    public List<DvCodedText> expand(TerminologyParam param) {
        return Collections.emptyList();
    }
}
