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
 * status 412 "Precondition Failed" or whatever is appropriate.
 * To be thrown in all cases where part of the request leads to problems, like malformed queries or non-existent referenced objects.
 */
public class PreconditionFailedException extends RuntimeException {

    private final String currentVersionUid;
    private final String url;

    public PreconditionFailedException(String message) {
        super(message);

        this.currentVersionUid = null;
        this.url = null;
    }

    public PreconditionFailedException(String message, Throwable cause) {
        super(message, cause);

        this.currentVersionUid = null;
        this.url = null;
    }

    public PreconditionFailedException(String message, String currentVersionUid, String url) {
        super(message);

        this.currentVersionUid = currentVersionUid;
        this.url = url;
    }

    public String getCurrentVersionUid() {
        return currentVersionUid;
    }

    public String getUrl() {
        return url;
    }
}
