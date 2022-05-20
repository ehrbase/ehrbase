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
package org.ehrbase.api.exception;

/**
 * Project-custom exception that allows outbound APIs to react on backend problems. Shall be thrown to invoke
 * status 409 "Conflict" or whatever is appropriate.
 * Request is in conflict with current state, e.g. referenced version of object is not the last version.
 */
public class StateConflictException extends RuntimeException {

    public StateConflictException(String message) {
        super(message);
    }

    public StateConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
