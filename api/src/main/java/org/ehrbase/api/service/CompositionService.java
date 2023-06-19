/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.api.service;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.RevisionHistory;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;

public interface CompositionService extends BaseService, VersionedObjectService<Composition, UUID> {
    /**
     * @param compositionId The {@link UUID} of the composition to be returned.
     * @param ehrId The {@link UUID} of the ehr wich contains the composition
     * @param version The version to returned. If null return the latest
     * @return
     * @throws InternalServerException
     */
    Optional<Composition> retrieve(UUID ehrId, UUID compositionId, Integer version);

    static CompositionDto from(UUID ehrId, Composition composition) {

        return new CompositionDto(
                composition,
                composition.getArchetypeDetails().getTemplateId().getValue(),
                UUID.fromString(composition.getUid().getRoot().getValue()),
                ehrId);
    }

    /**
     * Public serializer entry point which will be called with composition dto fetched from database
     * and the desired target serialized string format. Will parse the composition dto into target
     * format either with a custom lambda expression for desired target format
     *
     * @param composition Composition dto from database
     * @param format Target format
     * @return Structured string with string of data and content format
     */
    StructuredString serialize(CompositionDto composition, CompositionFormat format);

    Integer getLastVersionNumber(UUID compositionId);

    /**
     * Helper function to read the template ID from given composition input in stated format.
     * @param content Composition input
     * @param format Composition format
     * @return The UUID or null when not available.
     */
    String getTemplateIdFromInputComposition(String content, CompositionFormat format);

    /**
     * Retrieves the template ID associated with a given composition ID.
     *
     * @param compositionId The UUID of the composition for which to retrieve the template ID
     * @return The template ID associated with the given composition ID
     */
    String retrieveTemplateId(UUID compositionId);

    /**
     * Gets the version of a composition that is closest in time before timestamp
     * @param compositionId UUID (versioned_object_id) of composition
     * @param timestamp Given time
     * @return Version closest in time before given timestamp, or `null` in case of error.
     */
    Integer getVersionByTimestamp(UUID compositionId, LocalDateTime timestamp);

    /**
     * Checks if given ID is a valid composition ID.
     * @param versionedObjectId ID to check
     * @return True if ID exists
     * @throws ObjectNotFoundException if ID does not exist
     */
    boolean exists(UUID versionedObjectId);

    /**
     * Checks if given composition ID is ID of a logically deleted composition.
     * @param versionedObjectId ID to check
     * @return True if deleted, false if not
     */
    boolean isDeleted(UUID versionedObjectId);

    /**
     * Admin method to delete a Composition from the DB. See EHRbase Admin API specification for details.
     * @param compositionId Composition to delete
     */
    void adminDelete(UUID compositionId);

    /**
     * Gets version container Composition associated with given EHR and Composition ID.
     * @param ehrUid Given EHR ID
     * @param composition Given Composition ID
     * @return Version container object
     */
    VersionedComposition getVersionedComposition(UUID ehrUid, UUID composition);

    /**
     * Gets revision history of given composition.
     *
     * @param composition Given composition.
     * @return Revision history
     */
    RevisionHistory getRevisionHistoryOfVersionedComposition(UUID ehrUid, UUID composition);

    /**
     * Gets Original Version container class representation of the given composition at given version.
     *
     * @param versionedObjectUid Given composition Uid.
     * @param version Given version number.
     * @return Original Version container class representation.
     */
    Optional<OriginalVersion<Composition>> getOriginalVersionComposition(
            UUID ehrUid, UUID versionedObjectUid, int version);

    Composition buildComposition(String content, CompositionFormat format, String templateId);

    UUID getEhrId(UUID compositionId);
}
