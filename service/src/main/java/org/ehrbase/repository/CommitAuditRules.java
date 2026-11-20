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

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.util.ArrayList;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.jspecify.annotations.Nullable;

/// Rules for audit values a client sends for a direct commit
public final class CommitAuditRules {

    /// The only lifecycle state a direct commit supports: complete.
    static final String COMPLETE = "532";

    /// The only change type a client can choose instead of the default: amendment, on an update.
    static final String AMENDMENT = "250";

    /// The headers can't carry a scheme for `committer.external_ref.id`.
    /// We use the one `PartyProxyRepository.fromUser` uses.
    static final String EXTERNAL_REF_ID_SCHEME = "DEMOGRAPHIC";

    private CommitAuditRules() {}

    /// Only complete (532) is supported, so a client never believes a draft was stored as such.
    public static void checkLifecycleState(@Nullable String code) {
        if (code != null && !code.equals(COMPLETE)) {
            throw new InvalidApiParameterException(
                    "lifecycle_state.code_string [%s] is not supported, only %s (complete)".formatted(code, COMPLETE));
        }
    }

    /// system_id isn't stored, so a value that differs from the server's is rejected rather than dropped.
    public static void checkSystemId(@Nullable String sent, String configured) {
        if (sent != null && !sent.equals(configured)) {
            throw new InvalidApiParameterException(
                    "system_id [%s] does not match the system id of this server [%s]".formatted(sent, configured));
        }
    }

    /// Returns the change type of the operation.
    /// The client may send that code, or amendment (250) on an update.
    public static DvCodedText changeType(@Nullable String code, ContributionChangeType defaultChangeType) {
        DvCodedText defaultCodedText = codedText(defaultChangeType);
        if (code == null || code.equals(defaultCodedText.getDefiningCode().getCodeString())) {
            return defaultCodedText;
        }
        if (code.equals(AMENDMENT) && defaultChangeType == ContributionChangeType.modification) {
            return codedText(ContributionChangeType.amendment);
        }
        throw new InvalidApiParameterException(
                "change_type.code_string [%s] is not allowed for a %s".formatted(code, defaultChangeType.getLiteral()));
    }

    /// Builds a PARTY_IDENTIFIED from the name and external_ref.
    /// Returns `null` when the client sent neither.
    public static @Nullable PartyIdentified committer(
            @Nullable String name,
            @Nullable String externalRefId,
            @Nullable String externalRefNamespace,
            @Nullable String externalRefType) {
        boolean hasExternalRef = externalRefId != null || externalRefNamespace != null || externalRefType != null;
        if (name == null && !hasExternalRef) {
            return null;
        }
        PartyRef externalRef = null;
        if (hasExternalRef) {
            if (externalRefId == null || externalRefNamespace == null || externalRefType == null) {
                throw new InvalidApiParameterException("committer.external_ref needs id, namespace and type together");
            }
            externalRef = new PartyRef(
                    new GenericId(externalRefId, EXTERNAL_REF_ID_SCHEME), externalRefNamespace, externalRefType);
            if (!externalRef.typeValidity()) {
                throw new InvalidApiParameterException(
                        "committer.external_ref.type [%s] is not a PARTY_REF type".formatted(externalRefType));
            }
            if (!externalRef.namespaceValid()) {
                throw new InvalidApiParameterException("committer.external_ref.namespace [%s] is not a valid namespace"
                        .formatted(externalRefNamespace));
            }
        }
        return new PartyIdentified(externalRef, name, new ArrayList<>());
    }

    /// The change type as DV_CODED_TEXT, the way `ContributionRepository` reads it back.
    private static DvCodedText codedText(ContributionChangeType changeType) {
        int code = ContributionService.ContributionChangeType.valueOf(
                        changeType.getLiteral().toUpperCase())
                .getCode();
        return new DvCodedText(
                changeType.getLiteral(), new CodePhrase(new TerminologyId("openehr"), Integer.toString(code)));
    }
}
