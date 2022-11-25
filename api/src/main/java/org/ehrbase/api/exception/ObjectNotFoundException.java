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
package org.ehrbase.api.exception;

/**
 * Project-custom exception that allows outbound APIs to react on backend problems. Shall be thrown to invoke
 * status 404 "Not found" or whatever is appropriate.
 * The specific resource (EHR, composition, ...) is not available.
 *
 * @author Jake Smolka
 * @since 1.0
 */
public class ObjectNotFoundException extends RuntimeException {

    private final String type;

    public ObjectNotFoundException(String type, String message) {
        super(message);
        this.type = type;
    }

    public ObjectNotFoundException(String type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
