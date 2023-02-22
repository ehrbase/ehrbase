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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

public record SemVer(Integer major, Integer minor, Integer patch, String suffix) {

    private static String capturing(String content) {
        return "(" + content + ")";
    }

    private static String nonCapturing(String content) {
        return "(?:" + content + ")";
    }

    private static String optionalNonCapturing(String content) {
        return nonCapturing(content) + '?';
    }

    /**
     * partial, release and pre-release versions are permitted
     */
    public static final Pattern SEMVER_REGEX;

    static {
        String dot = "\\.";
        String versionPart = "0|[1-9]\\d*";
        String versionGroup = capturing(versionPart);

        String preReleaseIdentifier = nonCapturing(versionPart + "|\\d*[a-zA-Z-][0-9a-zA-Z-]*");
        String dotSeparatedPreReleaseIdentifiers =
                preReleaseIdentifier + nonCapturing(dot + preReleaseIdentifier) + '*';

        SEMVER_REGEX = Pattern.compile(
                // major
                versionGroup
                        + optionalNonCapturing(dot
                                // minor
                                + versionGroup
                                + optionalNonCapturing(dot
                                        // patch
                                        + versionGroup
                                        // dot-separated pre-release identifiers:
                                        // numeric identifier prohibits leading 0
                                        + optionalNonCapturing('-' + capturing(dotSeparatedPreReleaseIdentifiers)))));
    }

    public static final SemVer NO_VERSION = new SemVer(null, null, null, null);

    public static @NonNull SemVer parse(String semVerStr) throws InvalidVersionFormatException {
        if (StringUtils.isBlank(semVerStr) || "LATEST".equalsIgnoreCase(semVerStr)) {
            return NO_VERSION;
        }

        Matcher matcher = SEMVER_REGEX.matcher(semVerStr);
        if (!matcher.matches()) {
            throw new InvalidVersionFormatException(semVerStr);
        }

        return new SemVer(
                integerFromGroup(matcher, 1),
                integerFromGroup(matcher, 2),
                integerFromGroup(matcher, 3),
                group(matcher, 4).orElse(null));
    }

    public String toVersionString() {
        if (isNoVersion()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(major);
        if (minor != null) {
            sb.append('.').append(minor);
            if (patch != null) {
                sb.append('.').append(patch);
                if (suffix != null) {
                    sb.append('-').append(suffix);
                }
            }
        }
        return sb.toString();
    }

    public boolean isNoVersion() {
        return major == null;
    }

    public boolean isPartial() {
        return patch == null;
    }

    public boolean isRelease() {
        return patch != null && suffix == null;
    }

    public boolean isPreRelease() {
        return suffix != null;
    }

    private static Optional<String> group(Matcher matcher, int groupNr) {
        return Optional.of(matcher).filter(m -> m.groupCount() >= groupNr).map(m -> m.group(groupNr));
    }

    private static Integer integerFromGroup(Matcher matcher, int groupNr) {
        return group(matcher, groupNr).map(Integer::parseInt).orElse(null);
    }

    @Override
    public String toString() {
        return toVersionString();
    }
}
