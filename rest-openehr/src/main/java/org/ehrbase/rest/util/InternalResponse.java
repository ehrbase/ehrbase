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
package org.ehrbase.rest.util;

import org.springframework.http.HttpHeaders;

/**
 * Wrapper class to allow internal creation of response data classes with access to their headers, necessary to build different responses based on the response data.
 *
 * @param <T>
 */
public class InternalResponse<
        T /*implements ResponseData*/> { // TODO might needs to be changed to one more general layer, e.g. ResponseData
    // class
    T responseData;
    HttpHeaders headers;

    public InternalResponse(T responseData, HttpHeaders headers) {
        this.responseData = responseData;
        this.headers = headers;
    }

    public T getResponseData() {
        return responseData;
    }

    public void setResponseData(T responseData) {
        this.responseData = responseData;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }
}
