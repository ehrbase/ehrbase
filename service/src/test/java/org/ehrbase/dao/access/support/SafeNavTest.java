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
package org.ehrbase.dao.access.support;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class SafeNavTest {
    public static class Dummy {
        private String a;
        private String b;

        private Dummy dummy;
    }

    @Test
    void safeNav1() {
        Dummy dummy = new Dummy();
        SafeNav<Dummy> safeDummy = SafeNav.of(dummy);
        Assert.assertNull(safeDummy.get(d -> d.a).get());
        Assert.assertNull(safeDummy.get(d -> d.b).get());
        Assert.assertNull(safeDummy.get(d -> d.dummy).get());

        Assert.assertNull(safeDummy.get(d -> d.dummy.a).get());
        Assert.assertNull(safeDummy.get(d -> d.dummy.b).get());
        Assert.assertNull(safeDummy.get(d -> d.dummy.dummy).get());

        Assert.assertNull(safeDummy.get(d -> d.dummy).get(d -> d.a).get());
        Assert.assertNull(safeDummy.get(d -> d.dummy).get(d -> d.b).get());
        Assert.assertNull(safeDummy.get(d -> d.dummy).get(d -> d.dummy).get());
    }

    @Test
    void safeNav2() {
        Dummy dummy = new Dummy();
        dummy.a = "MBA";

        SafeNav<Dummy> safeDummy = SafeNav.of(dummy);
        Assert.assertEquals("MBA", safeDummy.get(d -> d.a).get());

        Dummy dummy2 = new Dummy();
        dummy2.a = "VITA";

        dummy.dummy = dummy2;

        Assert.assertNotNull(safeDummy.get(d -> d.dummy).get());
        Assert.assertEquals("VITA", safeDummy.get(d -> d.dummy).get(d -> d.a).get());
    }

    @Test
    void safeNav3() {
        Dummy dummy = new Dummy();
        dummy.a = "MBA";
        SafeNav<Dummy> safeDummy = SafeNav.of(dummy);

        Dummy dummy2 = new Dummy();
        dummy2.a = "VITA";

        SafeNav<Dummy> safeDummy2 = SafeNav.of(dummy2);
        SafeNav<Dummy> use = safeDummy.use(safeDummy2).get((d1, d) -> {
            d.b = d1.a;
            return d;
        });

        Assert.assertEquals("MBA", use.get(d -> d.a).get());
        Assert.assertEquals("VITA", use.get(d -> d.b).get());
    }
}
