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

import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.springframework.lang.NonNull;

public class SemVerUtil {

    /**
     * Based on a (potentially partial) version and the latest existing version that matches the pattern,
     * the subsequent version is generated.
     * Snapshot versions are retained.
     *
     * @param requestSemVer
     * @param dbSemVer
     * @return
     * @throws VersionConflictException if a release version already exists
     */
    public static @NonNull SemVer determineVersion(@NonNull SemVer requestSemVer, @NonNull SemVer dbSemVer)
            throws VersionConflictException {
        int major;
        int minor;
        int patch;

        if (requestSemVer.isNoVersion()) {
            major = incrementOrDefault(dbSemVer, SemVer::major, 1);
            minor = 0;
            patch = 0;

        } else if (!requestSemVer.isPartial()) {
            if (!dbSemVer.isNoVersion() && !requestSemVer.isPreRelease()) {
                throw new VersionConflictException("Release versions must not be replaced");
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
        if (semVer.isNoVersion()) {
            return fallback;
        } else {
            return func.applyAsInt(semVer) + 1;
        }
    }

    /**
     * Creates a regex pattern that matches all releases satisfying the given partial version
     *
     * @param partialVersion
     * @return
     * @throws IllegalArgumentException if a release or pre-release version is provided
     */
    public static String partialVersionPattern(SemVer partialVersion) throws IllegalArgumentException {
        if (!partialVersion.isPartial()) {
            throw new IllegalArgumentException("Only partial versions are supported");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("^");

        if (partialVersion.isNoVersion()) {
            sb.append("\\d+");
        } else {
            sb.append(partialVersion.toVersionString().replace(".", "\\."));
        }

        Stream.of(partialVersion.minor(), partialVersion.patch())
                .filter(Objects::isNull)
                .map(n -> "\\.\\d+")
                .forEach(sb::append);

        sb.append("$");

        return sb.toString();
    }
}
