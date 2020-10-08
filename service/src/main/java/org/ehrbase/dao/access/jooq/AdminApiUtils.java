package org.ehrbase.dao.access.jooq;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.tables.AdminDeleteCompositionHistory;
import org.ehrbase.jooq.pg.tables.AdminDeleteParty;
import org.ehrbase.jooq.pg.tables.records.*;
import org.jooq.DSLContext;
import org.jooq.Result;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AdminApiUtils {

    DSLContext ctx;

    public AdminApiUtils(DSLContext ctx) {
        this.ctx = Objects.requireNonNull(ctx);
    }

    // TODO: Test when the compo_xref feature comes available.
    /**
     * Recursively makes a list of all children for a given Composition (for Compositions that have others linked).
     * @param id Composition
     * @param childrenIds List of children.
     */
    private void getAndAddChildren(UUID id, List<UUID> childrenIds) {
        Result<AdminGetChildCompositionsRecord> children = Routines.adminGetChildCompositions(ctx.configuration(), id);
        if (children.isNotEmpty()) {
            children.forEach(child -> {
                // recursive call
                getAndAddChildren(id, childrenIds);
                childrenIds.add(child.getComposition());
            });
        }
    }

    /**
     * Deletes Composition and audit.
     * @param id Composition
     */
    private void internalDeleteComposition(UUID id) {
        // delete event_context and its participation first
        Result<AdminDeleteEventContextForCompoRecord> parties = Routines.adminDeleteEventContextForCompo(ctx.configuration(), id);
        // delete linked party, if not referenced somewhere else
        parties.forEach(party -> ctx.selectQuery(new AdminDeleteParty().call(party.getParty())).execute());

        Result<AdminDeleteCompositionRecord> delCompo = Routines.adminDeleteComposition(ctx.configuration(), id);
        // for each deleted compo delete auxiliary objects
        delCompo.forEach(del -> {
            deleteAudit(del.getAudit(), "Composition");
            // invoke deletion of attestation, if available
            if (del.getAttestation() != null) {
                Result<AdminDeleteAttestationRecord> delAttest = Routines.adminDeleteAttestation(ctx.configuration(), del.getAttestation());
                delAttest.forEach(attest -> deleteAudit(attest.getAudit(), "Attestation"));
            }
            // delete linked party, if not referenced somewhere else
            ctx.selectQuery(new AdminDeleteParty().call(del.getParty())).execute();
            // TODO-314: more?
        });
    }

    /**
     * Admin deletion of the given Composition.
     * @param id Composition
     */
    public void deleteComposition(UUID id) {
        // first handle possible children of the given composition
        List<UUID> childrenIds = new LinkedList<>();
        getAndAddChildren(id, childrenIds);
        childrenIds.forEach(this::internalDeleteComposition);

        // actual deletion of the composition
        internalDeleteComposition(id);

        // cleanup of composition auxiliary objects
        int res = ctx.selectQuery(new AdminDeleteCompositionHistory().call(id)).execute();
        if (res != 1)
            throw new InternalServerException("Admin deletion of Composition auxiliary objects failed!");

        // TODO-314: handle own contributions, so this can be used from deleteEHR and deleteCompo
    }

    /**
     * Admin deletion of the given Audit
     * @param id Audit
     * @param context Object context to build error message, e.g. "Composition" for the audit of a Composition
     */
    public void deleteAudit(UUID id, String context) {
        Result<AdminDeleteAuditRecord> delAudit = Routines.adminDeleteAudit(ctx.configuration(), id);
        if (delAudit.size() != 1)
            throw new InternalServerException("Admin deletion of " + context + " Audit failed!");
        // delete linked party, if not referenced somewhere else
        delAudit.forEach(audit -> ctx.selectQuery(new AdminDeleteParty().call(audit.getParty())).execute());

    }
}
