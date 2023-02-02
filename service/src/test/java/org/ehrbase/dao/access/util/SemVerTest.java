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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

class SemVerTest {

    @Test
    void parse() {
        {
            SemVer ver = SemVer.parse("");
            Assertions.assertThat(ver).isEqualTo(SemVer.NO_VERSION);
        }
        {
            SemVer ver = SemVer.parse("Latest");
            Assertions.assertThat(ver).isEqualTo(SemVer.NO_VERSION);
        }
        {
            SemVer ver = SemVer.parse("1");
            Assertions.assertThat(ver.major()).isEqualTo(1);
            Assertions.assertThat(ver.minor()).isNull();
            Assertions.assertThat(ver.patch()).isNull();
            Assertions.assertThat(ver.suffix()).isNull();
        }
        {
            SemVer ver = SemVer.parse("1.2");
            Assertions.assertThat(ver.major()).isEqualTo(1);
            Assertions.assertThat(ver.minor()).isEqualTo(2);
            Assertions.assertThat(ver.patch()).isNull();
            Assertions.assertThat(ver.suffix()).isNull();
        }
        {
            SemVer ver = SemVer.parse("1.29.3");
            Assertions.assertThat(ver.major()).isEqualTo(1);
            Assertions.assertThat(ver.minor()).isEqualTo(29);
            Assertions.assertThat(ver.patch()).isEqualTo(3);
            Assertions.assertThat(ver.suffix()).isNull();
        }
        {
            SemVer ver = SemVer.parse("1.2.3-SNAPSHOT");
            Assertions.assertThat(ver.major()).isEqualTo(1);
            Assertions.assertThat(ver.minor()).isEqualTo(2);
            Assertions.assertThat(ver.patch()).isEqualTo(3);
            Assertions.assertThat(ver.suffix()).isEqualTo("SNAPSHOT");
        }
        {
            SemVer ver = SemVer.parse("1.2.3-THE-SNAPSHOT.1.42");
            Assertions.assertThat(ver.major()).isEqualTo(1);
            Assertions.assertThat(ver.minor()).isEqualTo(2);
            Assertions.assertThat(ver.patch()).isEqualTo(3);
            Assertions.assertThat(ver.suffix()).isEqualTo("THE-SNAPSHOT.1.42");
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                ".",
                "1.",
                "1.2-SNAPSHOT",
                "1-SNAPSHOT",
                "-SNAPSHOT",
                "1.2.3.4",
                "1.2.3-SNAPSHOT.01",
                "1.0.0-alpha+001",
                "1.0.0+20130313144700",
                "1.0.0-beta+exp.sha.5114f85",
                "1.0.0+21AF26D3----117B344092BD"
            })
    void parseInvalid(String param) {
        assertThrows(InvalidVersionFormatException.class, () -> SemVer.parse(param));
    }

    @Test
    void toVersionString() {
        Assertions.assertThat(new SemVer(1, null, null, null).toVersionString()).isEqualTo("1");
        Assertions.assertThat(new SemVer(1, 2, null, null).toVersionString()).isEqualTo("1.2");
        Assertions.assertThat(new SemVer(1, 2, 3, null).toVersionString()).isEqualTo("1.2.3");
        Assertions.assertThat(new SemVer(1, 2, 3, "SNAPSHOT").toVersionString()).isEqualTo("1.2.3-SNAPSHOT");
    }
}
