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
package org.ehrbase.dao.access.jooq;

import java.util.Objects;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.tables.AdminDeleteCompositionHistory;
import org.ehrbase.jooq.pg.tables.records.*;
import org.jooq.DSLContext;
import org.jooq.Result;

/**
 * Util class to offer reusable methods in the scope of the Admin API.
 */
public class AdminApiUtils {

    DSLContext ctx;

    public AdminApiUtils(DSLContext ctx) {
        this.ctx = Objects.requireNonNull(ctx);
    }

    /**
     * Deletes Composition and audit.
     * @param id Composition
     */
    private void internalDeleteComposition(UUID id) {
        // delete event_context and its participation first
        Routines.adminDeleteEventContextForCompo(ctx.configuration(), id);

        // deletion of composition itself
        Result<AdminDeleteCompositionRecord> delCompo = Routines.adminDeleteComposition(ctx.configuration(), id);
        // for each deleted compo delete auxiliary objects
        delCompo.forEach(del -> {
            // invoke deletion of audit
            deleteAudit(del.getAudit(), "Composition", false);
            // invoke deletion of attestation, if available
            if (del.getAttestation() != null) {
                Result<AdminDeleteAttestationRecord> delAttest =
                        Routines.adminDeleteAttestation(ctx.configuration(), del.getAttestation());
                delAttest.forEach(attest -> deleteAudit(attest.getAudit(), "Attestation", false));
            }

            // delete contribution
            deleteContribution(del.getContribution(), null, false);
        });
    }

    /**
     * Admin deletion of the given Composition.
     * @param id Composition
     */
    public void deleteComposition(UUID id) {
        // actual deletion of the composition
        internalDeleteComposition(id);

        // cleanup of composition auxiliary objects
        int res = ctx.selectQuery(new AdminDeleteCompositionHistory().call(id)).execute();
        if (res != 1) throw new InternalServerException("Admin deletion of Composition auxiliary objects failed!");
    }

    /**
     * Admin deletion of the given Audit
     * @param id Audit
     * @param context Object context to build error message, e.g. "Composition" for the audit of a Composition
     * @param resultCanBeEmpty Config parameter to disable check of result, in case the object is deleted already (for broader scopes, like EHR itself)
     */
    public void deleteAudit(UUID id, String context, Boolean resultCanBeEmpty) {
        Result<AdminDeleteAuditRecord> delAudit = Routines.adminDeleteAudit(ctx.configuration(), id);
        if (resultCanBeEmpty.equals(false) && delAudit.size() != 1)
            throw new InternalServerException("Admin deletion of " + context + " Audit failed!");
    }

    /**
     * Admin deletion of the given Contribution
     * @param id Contribution
     * @param audit Audit ID, optional
     * @param resultCanBeEmpty Config parameter to disable check of result, in case the object is deleted already (for broader scopes, like EHR itself)
     */
    public void deleteContribution(UUID id, UUID audit, Boolean resultCanBeEmpty) {
        // delete contribution
        Result<AdminDeleteContributionRecord> rec = Routines.adminDeleteContribution(ctx.configuration(), id);
        // and its audit (depending of how this is called, either get audit ID from parameter or from response)
        if (rec.isNotEmpty()) {
            rec.forEach(del -> deleteAudit(del.getAudit(), "Contribution", resultCanBeEmpty));
        } else if (audit != null) {
            deleteAudit(audit, "Contribution", resultCanBeEmpty);
        }
    }
}
