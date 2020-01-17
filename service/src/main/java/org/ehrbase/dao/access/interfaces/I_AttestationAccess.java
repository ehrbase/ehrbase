package org.ehrbase.dao.access.interfaces;

import com.nedap.archie.rm.generic.Attestation;
import org.ehrbase.dao.access.jooq.AttestationAccess;

import java.util.List;
import java.util.UUID;

public interface I_AttestationAccess extends I_SimpleCRUD<I_AttestationAccess, UUID> {

    /**
     * Retrieve runtime instance of given attestation from DB
     * @param attestationId Given attestation ID
     * @return Attestation access object
     */
    I_AttestationAccess retrieveInstance(UUID attestationId);

    /**
     * Retrieve list of attestation IDs by the reference given by a version object
     * @param domainAccess      General data access
     * @param attestationRef    ID of reference from DB to find associated attestations
     * @return List of ID of attestations referenced by the given reference ID
     */
    static List<UUID> retrieveListOfAttestationsByRef(I_DomainAccess domainAccess, UUID attestationRef) {
        return AttestationAccess.retrieveListOfAttestationsByRef(domainAccess, attestationRef);
    }

    Attestation getAsAttestation();
}
