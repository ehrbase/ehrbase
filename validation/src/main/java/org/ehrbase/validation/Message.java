/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.validation;

/**
 * Encode a validation exception message
 * The exception message consists of:
 * - a code
 * - the AQL path of the failing item
 * - a description
 */
public class Message {

    /**
     * encode an exception message
     * @param path AQL path of the item
     * @param message exception message
     * @param code code to ease identifying the issue
     * @return the encoded message
     */
    public String encode(String path, String message, String code){
        return (code +(path.isEmpty() ? "-" :"-Validation error at "+path+":"))+message+".\n";
    }
}
