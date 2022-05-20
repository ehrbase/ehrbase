/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.service;

/**
 * Status service to get information about the running EHRbase instance
 */
public interface StatusService extends BaseService {

    /**
     * Returns information on the current operating system this EHRbase instance is running on.
     * The resulting info string contains the name, the architecture and the version of the
     * operating system, e.g. "Mac OS X x86_64 10.15.7"
     *
     * @return OS information
     */
    String getOperatingSystemInformation();

    /**
     * Returns information on the current Java Virtual Machine that is running this EHRbase
     * instance. Provide information on the JVM vendor and the full java runtime version, e.g.
     * "Eclipse OpenJ9 11.0.9+11".
     *
     * @return JVM Version string
     */
    String getJavaVMInformation();

    /**
     * Returns information on the current connected Database instance version. This contains the
     * major version and additionally also the operating system it is running on, e.g. useful
     * if the database runs on a remote server or inside a docker container.
     *
     * @return Database information
     */
    String getDatabaseInformation();

    /**
     * Returns current version of EHRbase build that is running.
     *
     * @return Current EHRbase version
     */
    String getEhrbaseVersion();

    /**
     * Returns current version of archie which has been used to build the running EHRbase instance.
     *
     * @return Current used Archie version
     */
    String getArchieVersion();

    /**
     * Returns the current version of openEHR_SDK which has been used to build the running EHRbase instance.
     *
     * @return Current used openEHR_SDK version
     */
    String getOpenEHR_SDK_Version();
}
