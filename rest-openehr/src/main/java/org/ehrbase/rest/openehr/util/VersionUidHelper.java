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

package org.ehrbase.rest.openehr.util;


import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUidHelper {

    public static final Pattern UUID_PATTERN = Pattern.compile("^([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})");
    public static final Pattern VERSION_PATTERN = Pattern.compile("::(\\d+)$");
    public static final Pattern SYSTEM_ID_PATTERN = Pattern.compile("::(\\w+.\\w+.\\w+)");
    public static final Pattern VERSION_UID_PATTERN =
            Pattern.compile(
                    UUID_PATTERN.toString() +
                            SYSTEM_ID_PATTERN.toString() +
                            VERSION_PATTERN.toString()
            );

    private UUID uuid;
    private String systemId;
    private int version;

    public VersionUidHelper(String versionUid) {
        if (!VERSION_UID_PATTERN.matcher(versionUid).matches()) {
            throw new IllegalArgumentException("Version uid " + versionUid + " is not a valid version_uid,");
        }
        this.uuid = extractUUID(versionUid);
        this.systemId = extractSystemId(versionUid);
        this.version = extractVersion(versionUid);
    }

    public static boolean isVersionUid(String testString) {
        return VERSION_UID_PATTERN.matcher(testString).matches();
    }

    public static boolean isUUID(String testString) {
        return UUID_PATTERN.matcher(testString).matches();
    }

    public static boolean isSystemId(String testString) {
        return SYSTEM_ID_PATTERN.matcher(testString).matches();
    }

    public static boolean isVersion(String testString) {
        return VERSION_PATTERN.matcher(testString).matches();
    }


    public static UUID extractUUID(String versionUid) {
        Matcher matcher = UUID_PATTERN.matcher(versionUid);
        if (matcher.find()) {
            return UUID.fromString(matcher.group(1));
        }
        return null;
    }


    public static String extractSystemId(String versionUid) {
        Matcher matcher = SYSTEM_ID_PATTERN.matcher(versionUid);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    public static int extractVersion(String versionUid) {
        Matcher matcher = VERSION_PATTERN.matcher(versionUid);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }

    public UUID getUuid() {
        return this.uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public String getSystemId() {
        return systemId;
    }
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }

}
