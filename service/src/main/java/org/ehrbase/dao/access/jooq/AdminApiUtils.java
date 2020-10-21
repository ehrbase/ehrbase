package org.ehrbase.dao.access.jooq;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.tables.AdminDeleteCompositionHistory;
import org.ehrbase.jooq.pg.tables.AdminDeleteFolderHistory;
import org.ehrbase.jooq.pg.tables.AdminDeleteFolderObjRefHistory;
import org.ehrbase.jooq.pg.tables.AdminDeleteParty;
import org.ehrbase.jooq.pg.tables.records.*;
import org.jooq.DSLContext;
import org.jooq.Result;

import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

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
        Result<AdminDeleteEventContextForCompoRecord> parties = Routines.adminDeleteEventContextForCompo(ctx.configuration(), id);
        // delete linked party, if not referenced somewhere else
        parties.forEach(party -> ctx.selectQuery(new AdminDeleteParty().call(party.getParty())).execute());

        // deletion of composition itself
        Result<AdminDeleteCompositionRecord> delCompo = Routines.adminDeleteComposition(ctx.configuration(), id);
        // for each deleted compo delete auxiliary objects
        delCompo.forEach(del -> {
            // invoke deletion of audit
            deleteAudit(del.getAudit(), "Composition", false);
            // invoke deletion of attestation, if available
            if (del.getAttestation() != null) {
                Result<AdminDeleteAttestationRecord> delAttest = Routines.adminDeleteAttestation(ctx.configuration(), del.getAttestation());
                delAttest.forEach(attest -> deleteAudit(attest.getAudit(), "Attestation", false));
            }
            // delete linked party, if not referenced somewhere else (logic inside DB function)
            ctx.selectQuery(new AdminDeleteParty().call(del.getParty())).execute();

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
        if (res != 1)
            throw new InternalServerException("Admin deletion of Composition auxiliary objects failed!");
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
        // delete linked party, if not referenced somewhere else
        delAudit.forEach(audit -> ctx.selectQuery(new AdminDeleteParty().call(audit.getParty())).execute());

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

    /**
     * Admin deletion of the given Folder
     * @param id Folder
     * @param deleteContributions Option to en- or disable the deletion of all linked contributions. Disable when calling in context of cascading high level object like EHR.
     */
    public void deleteFolder(UUID id, Boolean deleteContributions) {
        Result<AdminDeleteFolderRecord> records = Routines.adminDeleteFolder(ctx.configuration(), id);

        // folders are used to clean folder history tables later
        HashSet<UUID> folders = new HashSet<>();
        // contributions are used to clean object_ref_history table later
        HashSet<UUID> contribs = new HashSet<>();

        // initially add this scope's root folder ID
        folders.add(id);

        // add all other IDs to their sets, if available
        records.forEach(rec -> {
            folders.add(rec.getChild());
            contribs.add(rec.getContribution());
        });

        // invoke both *_HISTORY cleaning functions
        folders.forEach(folder -> ctx.selectQuery(new AdminDeleteFolderHistory().call(folder)).execute());
        contribs.forEach(contrib -> ctx.selectQuery(new AdminDeleteFolderObjRefHistory().call(contrib)).execute());

        // invoke contribution deletion - if set to true
        if (deleteContributions.equals(true))
            contribs.forEach(contrib -> deleteContribution(contrib, null, false));
    }
}
