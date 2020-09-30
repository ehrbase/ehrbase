package org.ehrbase.dao.access.jooq;

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.jooq.pg.tables.AdminDeleteAudit;
import org.ehrbase.jooq.pg.tables.AdminDeleteCompositionHistory;
import org.ehrbase.jooq.pg.tables.records.AdminDeleteCompositionRecord;
import org.ehrbase.jooq.pg.tables.records.AdminGetChildCompositionsRecord;
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
        Result<AdminDeleteCompositionRecord> delCompo = Routines.adminDeleteComposition(ctx.configuration(), id);
        // for each deleted compo delete auxiliary objects
        delCompo.forEach(del -> {
            int resp = ctx.selectQuery(new AdminDeleteAudit().call(del.getAudit())).execute();
            if (resp != 1)
                throw new InternalServerException("Admin deletion of Composition Audit failed!");
            // TODO-314: more?
        });
    }

    /**
     * Admin deletion of the given Composition.
     * @param id Composition
     */
    public void deleteComposition(UUID id) {
        // TODO-314: call admin_get_child_compositions and invoke the following for them first, incl. checking for their children recursively
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
}
