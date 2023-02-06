/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/*
 * Copyright (c) 2022. vitasystems GmbH and Hannover Medical School.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

class SemVerUtilTest {

    @Test
    void determineVersionFromRelease() {
        SemVer req = SemVer.parse("1.2.3");
        assertThat(SemVerUtil.determineVersion(req, SemVer.NO_VERSION)).isEqualTo(SemVer.parse("1.2.3"));
        assertThatExceptionOfType(VersionConflictException.class)
                .isThrownBy(() -> SemVerUtil.determineVersion(req, req));
    }

    @Test
    void determineVersionSnapshot() {
        SemVer req = SemVer.parse("1.2.3-SNAPSHOT");
        assertThat(SemVerUtil.determineVersion(req, SemVer.NO_VERSION)).isEqualTo(SemVer.parse("1.2.3-SNAPSHOT"));
        assertThat(SemVerUtil.determineVersion(req, SemVer.parse("1.2.3-SNAPSHOT")))
                .isEqualTo(SemVer.parse("1.2.3-SNAPSHOT"));
    }

    @Test
    void determineVersionAuto() {
        assertThat(SemVerUtil.determineVersion(SemVer.NO_VERSION, SemVer.NO_VERSION))
                .isEqualTo(SemVer.parse("1.0.0"));
        assertThat(SemVerUtil.determineVersion(SemVer.NO_VERSION, SemVer.parse("41.2.3")))
                .isEqualTo(SemVer.parse("42.0.0"));
    }

    @Test
    void determineVersionFromPartialMajor() {
        SemVer req = SemVer.parse("42");
        assertThat(SemVerUtil.determineVersion(req, SemVer.NO_VERSION)).isEqualTo(SemVer.parse("42.0.0"));
        assertThat(SemVerUtil.determineVersion(req, SemVer.parse("42.4.5"))).isEqualTo(SemVer.parse("42.5.0"));
    }

    @Test
    void determineVersionFromPartialMinor() {
        SemVer req = SemVer.parse("3.42");
        assertThat(SemVerUtil.determineVersion(req, SemVer.NO_VERSION)).isEqualTo(SemVer.parse("3.42.0"));
        assertThat(SemVerUtil.determineVersion(req, SemVer.parse("3.42.5"))).isEqualTo(SemVer.parse("3.42.6"));
    }

    @Test
    void partialVersionPattern() {
        assertPartialVersionPatternMatches("", true, "1.1.1", "1.2.3", "987.654.321");
        assertPartialVersionPatternMatches("", false, "", "1", "1.1", "1.1.1-SNAPSHOT");

        assertPartialVersionPatternMatches("1", true, "1.1.1", "1.2.3", "1.654.321");
        assertPartialVersionPatternMatches("1", false, "", "1", "1.1", "1.1.1-SNAPSHOT", "2.1.1");

        assertPartialVersionPatternMatches("11.2", true, "11.2.1", "11.2.3", "11.2.321");
        assertPartialVersionPatternMatches("11.2", false, "", "1", "1.1", "1.1.1-SNAPSHOT", "2.1.1", "12.2.3");

        assertThatThrownBy(() -> SemVerUtil.partialVersionPattern(SemVer.parse("1.2.3")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVerUtil.partialVersionPattern(SemVer.parse("1.2.3-SNAPSHOT")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertPartialVersionPatternMatches(String partialVersion, boolean mustMatch, String... values) {
        SemVer semVer = SemVer.parse(partialVersion);
        Pattern pattern = Pattern.compile(SemVerUtil.partialVersionPattern(semVer));
        Arrays.stream(values).forEach(v -> assertThat(pattern.matcher(v).matches())
                .as(() -> partialVersion + " must " + (mustMatch ? "" : " not") + " match " + v)
                .isEqualTo(mustMatch));
    }
}
