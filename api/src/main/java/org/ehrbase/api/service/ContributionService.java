/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.api.service;

import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.generic.AuditDetails;
import java.util.UUID;
import java.util.stream.Stream;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.ContributionDto;

/**
 * Interface for contribution service roughly based on openEHR SM "I_EHR_CONTRIBUTION Interface",
 * see: https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_contribution_interface
 */
public interface ContributionService {

    enum ContributionChangeType {
        CREATION(249),
        AMENDMENT(250),
        MODIFICATION(251),
        SYNTHESIS(252),
        UNKNOWN(253),
        DELETED(523);
        final int code;

        ContributionChangeType(int code) {
            this.code = code;
        }

        public static ContributionChangeType fromAuditDetails(AuditDetails commitAudit) {
            DvCodedText changeType = commitAudit.getChangeType();

            if (!"openehr"
                    .equals(changeType.getDefiningCode().getTerminologyId().getValue())) {
                throw new ValidationException("Unsupported change type terminology: %s"
                        .formatted(
                                changeType.getDefiningCode().getTerminologyId().getValue()));
            }

            ContributionChangeType byCode =
                    getByCode(changeType.getDefiningCode().getCodeString());
            if (byCode.name().equalsIgnoreCase(changeType.getValue())) {
                return byCode;
            } else {
                throw new ValidationException("Inconsistent change type: %s for code %s"
                        .formatted(
                                changeType.getValue(),
                                changeType.getDefiningCode().getCodeString()));
            }
        }

        private static ContributionChangeType getByCode(String codeString) {

            int code;
            try {
                code = Integer.parseInt(codeString);
            } catch (NumberFormatException e) {
                throw new ValidationException("Unknown change type code %s".formatted(codeString));
            }

            return Stream.of(ContributionChangeType.values())
                    .filter(t -> t.code == code)
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Unknown change type code %s".formatted(codeString)));
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Return the Contribution with given id in given EHR.
     *
     * @param ehrId          ID of EHR
     * @param contributionId ID of contribution
     */
    ContributionDto getContribution(UUID ehrId, UUID contributionId);

    /**
     * Commit a CONTRIBUTION containing any number of serialized VERSION<Type> objects.
     *
     * @param ehrId   ID of EHR
     * @param content serialized content, containing version objects and audit object in given format
     * @return ID of successfully committed contribution
     * @throws IllegalArgumentException when input can't be processed
     * @throws InternalServerException  when DB is inconsistent
     */
    UUID commitContribution(UUID ehrId, String content);

    /**
     * Admin method to delete a Contribution from the DB. See EHRbase Admin API specification for details.
     *
     * @param ehrId
     * @param contributionId Contribution to delete
     */
    void adminDelete(UUID ehrId, UUID contributionId);
}
