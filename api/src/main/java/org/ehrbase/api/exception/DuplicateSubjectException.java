/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.api.exception;

/**
 * Thrown when the subject of an EHR_STATUS is already used by a different EHR.
 * The subject id and namespace are kept as fields and not in the message, so the message can be logged without
 * exposing a patient identifier.
 */
public class DuplicateSubjectException extends StateConflictException {

    private final String subjectId;
    private final String subjectNamespace;

    public DuplicateSubjectException(String subjectId, String subjectNamespace) {
        super("Supplied subject is already used by a different EHR in the same namespace.");
        this.subjectId = subjectId;
        this.subjectNamespace = subjectNamespace;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getSubjectNamespace() {
        return subjectNamespace;
    }
}
