/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.ArrayList;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CommitAuditRulesTest {

    private static final String SYSTEM_ID = "local.ehrbase.org";

    @Test
    void acceptsCompleteOrNoLifecycleState() {
        assertThatNoException().isThrownBy(() -> {
            CommitAuditRules.checkLifecycleState(null);
            CommitAuditRules.checkLifecycleState("532");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"553", "523", "complete"})
    void rejectsLifecycleStatesOtherThanComplete(String code) {
        assertThatThrownBy(() -> CommitAuditRules.checkLifecycleState(code))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void acceptsOwnOrNoSystemId() {
        assertThatNoException().isThrownBy(() -> {
            CommitAuditRules.checkSystemId(null, SYSTEM_ID);
            CommitAuditRules.checkSystemId(SYSTEM_ID, SYSTEM_ID);
        });
    }

    @Test
    void rejectsAnotherSystemId() {
        assertThatThrownBy(() -> CommitAuditRules.checkSystemId("other.system", SYSTEM_ID))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @ParameterizedTest
    @CsvSource(
            nullValues = "null",
            value = {
                "null, creation, 249, creation",
                "249, creation, 249, creation",
                "null, modification, 251, modification",
                "251, modification, 251, modification",
                "250, modification, 250, amendment",
                "null, deleted, 523, deleted",
                "523, deleted, 523, deleted"
            })
    void acceptsTheDefaultChangeTypeOrAmendmentOnUpdate(
            String sent, ContributionChangeType defaultChangeType, String expectedCode, String expectedValue) {
        var changeType = CommitAuditRules.changeType(sent, defaultChangeType);

        assertThat(changeType.getDefiningCode().getCodeString()).isEqualTo(expectedCode);
        assertThat(changeType.getValue()).isEqualTo(expectedValue);
    }

    @ParameterizedTest
    @CsvSource({
        "250, creation",
        "251, creation",
        "249, modification",
        "523, modification",
        "252, modification",
        "999, modification",
        "250, deleted"
    })
    void rejectsAnyOtherChangeType(String sent, ContributionChangeType defaultChangeType) {
        assertThatThrownBy(() -> CommitAuditRules.changeType(sent, defaultChangeType))
                .isInstanceOf(InvalidApiParameterException.class);
    }

    @Test
    void buildsNoCommitterWhenNoneWasSent() {
        assertThat(CommitAuditRules.committer(null, null, null, null)).isNull();
    }

    @Test
    void buildsACommitterFromANameAlone() {
        assertThat(CommitAuditRules.committer("Dr. Jane Doe", null, null, null))
                .isEqualTo(new PartyIdentified(null, "Dr. Jane Doe", new ArrayList<>()));
    }

    @Test
    void buildsACommitterWithAnExternalRef() {
        assertThat(CommitAuditRules.committer("Dr. Jane Doe", "BC81", "demographic", "PERSON"))
                .isEqualTo(new PartyIdentified(
                        new PartyRef(new GenericId("BC81", "DEMOGRAPHIC"), "demographic", "PERSON"),
                        "Dr. Jane Doe",
                        new ArrayList<>()));
    }

    @ParameterizedTest
    @CsvSource(
            nullValues = "null",
            value = {
                "BC81, null, null",
                "BC81, demographic, null",
                "null, demographic, PERSON",
                "BC81, demographic, DOCTOR",
                "BC81, 1demographic, PERSON"
            })
    void rejectsAnIncompleteOrInvalidExternalRef(String id, String namespace, String type) {
        assertThatThrownBy(() -> CommitAuditRules.committer("Dr. Jane Doe", id, namespace, type))
                .isInstanceOf(InvalidApiParameterException.class);
    }
}
