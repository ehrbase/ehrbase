/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.test.assertions;

import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import java.util.UUID;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class EhrStatusAssert extends AbstractAssert<EhrStatusAssert, EhrStatus> {

    protected EhrStatusAssert(EhrStatus actual) {
        super(actual, EhrStatusAssert.class);
    }

    public static EhrStatusAssert assertThat(EhrStatus actual) {
        return new EhrStatusAssert(actual);
    }

    public EhrStatusAssert isEqualToIgnoreId(EhrStatus expected) {

        Assertions.assertThat(actual.getArchetypeNodeId())
                .describedAs("archetype_node_id")
                .isEqualTo(expected.getArchetypeNodeId());
        Assertions.assertThat(actual.getName()).describedAs("name").isEqualTo(expected.getName());
        Assertions.assertThat(actual.getSubject())
                .describedAs("subject")
                .isInstanceOf(PartySelf.class)
                .isEqualTo(expected.getSubject());
        Assertions.assertThat(actual.isQueryable()).describedAs("is_queryable").isEqualTo(expected.isQueryable());
        Assertions.assertThat(actual.isModifiable())
                .describedAs("is_modifiable")
                .isEqualTo(expected.isModifiable());
        Assertions.assertThat(actual.getOtherDetails())
                .describedAs("other_details")
                .isEqualTo(expected.getOtherDetails());
        Assertions.assertThat(actual.getArchetypeDetails())
                .describedAs("archetype_details")
                .isEqualTo(expected.getArchetypeDetails());
        Assertions.assertThat(actual.getFeederAudit())
                .describedAs("feeder_audit")
                .isEqualTo(expected.getFeederAudit());
        Assertions.assertThat(actual.getLinks()).describedAs("links").isNull();
        Assertions.assertThat(actual.getParent()).describedAs("parent").isEqualTo(expected.getParent());
        return this.myself;
    }

    public EhrStatusAssert hasIdRoot() {

        Assertions.assertThat(actual.getUid().getRoot())
                .describedAs("uid::root")
                .isNotNull();
        return this.myself;
    }

    public EhrStatusAssert hasIdRootValue(UUID id) {

        hasIdRoot();
        Assertions.assertThat(actual.getUid().getRoot().getValue())
                .describedAs("uid::root")
                .isEqualTo(id.toString());
        return this.myself;
    }

    public EhrStatusAssert hasIdExtension(String idExtension) {

        Assertions.assertThat(actual.getUid().getExtension())
                .describedAs("uid::extension")
                .isEqualTo(idExtension);
        return this.myself;
    }
}
