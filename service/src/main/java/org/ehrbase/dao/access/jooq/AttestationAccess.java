/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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

import static org.ehrbase.jooq.pg.tables.Attestation.ATTESTATION;
import static org.ehrbase.jooq.pg.tables.AuditDetails.AUDIT_DETAILS;

import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvEHRURI;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.Attestation;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyProxy;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_AttestationAccess;
import org.ehrbase.dao.access.interfaces.I_AuditDetailsAccess;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.jooq.pg.tables.records.AttestationRecord;
import org.jooq.Result;

public class AttestationAccess extends DataAccess implements I_AttestationAccess {

    private AttestationRecord attestationRecord;
    private I_AuditDetailsAccess
            auditDetailsAccess; // attestation is subclass of audit_details, realized via association with one instance

    public AttestationAccess(I_DomainAccess domainAccess) {
        super(domainAccess);
    }

    @Override
    public I_AttestationAccess retrieveInstance(UUID attestationId) {
        try {
            this.attestationRecord =
                    getDataAccess().getContext().fetchOne(ATTESTATION, ATTESTATION.ID.eq(attestationId));
        } catch (Exception e) {
            throw new InternalServerException("fetching attestation failed", e);
        }

        auditDetailsAccess = new AuditDetailsAccess(getDataAccess(), this.attestationRecord.getSysTenant());
        try {
            auditDetailsAccess.setRecord(
                    getDataAccess().getContext().fetchOne(AUDIT_DETAILS, AUDIT_DETAILS.ID.eq(getAuditId())));
        } catch (Exception e) {
            throw new InternalServerException("fetching audit_details failed", e);
        }

        return null;
    }

    public static List<UUID> retrieveListOfAttestationsByRef(I_DomainAccess dataAccess, UUID attestationRef) {
        Result<AttestationRecord> result =
                dataAccess.getContext().fetch(ATTESTATION, ATTESTATION.REFERENCE.eq(attestationRef));
        if (result.isEmpty()) return Collections.emptyList();

        List<UUID> list = new ArrayList<>();
        for (AttestationRecord rec : result) {
            list.add(rec.getId());
        }
        return list;
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    @Override
    public UUID commit(Timestamp transactionTime) {
        return null; // FIXME VERSIONED_OBJECT_POC: to do
    }

    @Override
    public UUID commit() {
        return null; // FIXME VERSIONED_OBJECT_POC: to do
    }

    @Override
    public Boolean update(Timestamp transactionTime) {
        return null; // FIXME VERSIONED_OBJECT_POC: to do
    }

    @Override
    public Boolean update(Timestamp transactionTime, boolean force) {
        return null; // FIXME VERSIONED_OBJECT_POC: to do
    }

    @Override
    public Boolean update() {
        return null; // FIXME VERSIONED_OBJECT_POC: to do
    }

    @Override
    public Boolean update(Boolean force) {
        return null; // FIXME VERSIONED_OBJECT_POC: to do
    }

    @Override
    public Integer delete() {
        return null; // FIXME VERSIONED_OBJECT_POC: to do
    }

    private UUID getAuditId() {
        return this.attestationRecord.getHasAudit();
    }

    @Override
    public Attestation getAsAttestation() {
        AuditDetails audit = auditDetailsAccess.getAsAuditDetails(); // take most values from super class entry
        String systemId = audit.getSystemId();
        PartyProxy committer = audit.getCommitter();
        DvDateTime time = audit.getTimeCommitted();
        DvCodedText changeType = audit.getChangeType();
        DvText description = audit.getDescription();

        DvMultimedia attestedView = null; // FIXME VERSIONED_OBJECT_POC: implement retrieval from "attested_view" table
        String proof = attestationRecord.getProof();
        List<DvEHRURI> items =
                null; // FIXME VERSIONED_OBJECT_POC: implement?! seems to be completely unsupported right now
        DvText reason = new DvText(attestationRecord.getReason());
        boolean isPending = attestationRecord.getIsPending();

        return new Attestation(
                systemId, committer, time, changeType, description, attestedView, proof, items, reason, isPending);
    }
}
