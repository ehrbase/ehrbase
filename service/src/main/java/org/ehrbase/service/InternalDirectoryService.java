/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.ehrbase.api.service.DirectoryService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.repository.ContributionRepository;

/**
 * @author Stefan Spiska
 */
public interface InternalDirectoryService extends DirectoryService {

    /**
     * Create a new folder for Ehr with id equal <code>ehrId</code>
     *
     * @param ehrId
     * @param folder
     * @param contributionId If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId        If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType)}
     * @return
     */
    Folder create(UUID ehrId, Folder folder, @Nullable UUID contributionId, @Nullable UUID auditId);

    /**
     * Update the folder for Ehr with id equal <code>ehrId</code>
     *
     * @param ehrId
     * @param folder
     * @param ifMatches      expected version before update for optimistic looking
     * @param contributionId If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId        If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType)}
     * @return
     */
    Folder update(
            UUID ehrId,
            Folder folder,
            ObjectVersionId ifMatches,
            @Nullable UUID contributionId,
            @Nullable UUID auditId);

    /**
     * delete the folder for Ehr with id equal <code>ehrId</code>
     *
     * @param ehrId
     * @param ifMatches      expected version before delete for optimistic looking
     * @param contributionId If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId        If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType)}
     */
    void delete(UUID ehrId, ObjectVersionId ifMatches, @Nullable UUID contributionId, @Nullable UUID auditId);

    List<ObjectVersionId> findForContribution(UUID ehrId, UUID contributionId);
}
