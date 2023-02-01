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
package org.ehrbase.dao.access.util;

import java.util.function.ToIntFunction;
import org.springframework.lang.NonNull;

public class SemVerUtil {

    /**
     * From the
     * @param requestSemVer
     * @param dbSemVer
     * @return
     */
    public static @NonNull SemVer determineVersion(SemVer requestSemVer, SemVer dbSemVer) {
        int major;
        int minor;
        int patch;

        if (requestSemVer.isNoVersion()) {
            major = incrementOrDefault(dbSemVer, SemVer::major, 1);
            minor = 0;
            patch = 0;

        } else if (!requestSemVer.isPartial()) {
            if (dbSemVer != null && !requestSemVer.isPreRelease()) {
                throw new IllegalArgumentException("Release versions must not be replaced");
            }
            return requestSemVer;

        } else if (requestSemVer.minor() == null) {
            major = requestSemVer.major();
            minor = incrementOrDefault(dbSemVer, SemVer::minor, 0);
            patch = 0;

        } else { // dbSemVer.patch() == null
            major = requestSemVer.major();
            minor = requestSemVer.minor();
            patch = incrementOrDefault(dbSemVer, SemVer::patch, 0);
        }
        return new SemVer(major, minor, patch, null);
    }

    private static int incrementOrDefault(SemVer semVer, ToIntFunction<SemVer> func, int fallback) {
        if (semVer == null) {
            return fallback;
        } else {
            return func.applyAsInt(semVer) + 1;
        }
    }
}
