/*
 * Copyright (c) 2019 Vitasystems GmbH,
 * Jake Smolka (Hannover Medical School),
 * Luis Marco-Ruiz (Hannover Medical School),
 * Stefan Spiska (Vitasystems GmbH).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.service;

import com.nedap.archie.rm.composition.Composition;
import org.apache.catalina.Server;
import org.ehrbase.api.definitions.CompositionFormat;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.definitions.StructuredString;
import org.ehrbase.api.definitions.StructuredStringFormat;
import org.ehrbase.api.dto.CompositionDto;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.dao.access.interfaces.I_CompoXrefAccess;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_EntryAccess;
import org.ehrbase.dao.access.jooq.CompoXRefAccess;
import org.ehrbase.serialisation.CanonicalJson;
import org.ehrbase.serialisation.CanonicalXML;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional()
public class CompositionServiceImp extends BaseService implements CompositionService {

    public static final String DESCRIPTION = "description";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private boolean supportCompositionXRef = false;
    private final ValidationService validationService;
    private final EhrService ehrService;

    @Autowired
    public CompositionServiceImp(KnowledgeCacheService knowledgeCacheService, ValidationService validationService, EhrService ehrService, DSLContext context, ServerConfig serverConfig) {

        super(knowledgeCacheService, context, serverConfig);
        this.validationService = validationService;
        this.ehrService = ehrService;
    }

    @Override
    public Optional<CompositionDto> retrieve(UUID compositionId, Integer version) throws InternalServerException {

        final I_CompositionAccess compositionAccess;
        if (version != null) {
            compositionAccess = I_CompositionAccess.retrieveCompositionVersion(getDataAccess(), compositionId, version);
        } else {    // default to latest version
            compositionAccess = I_CompositionAccess.retrieveCompositionVersion(getDataAccess(), compositionId, getLastVersionNumber(compositionId));
        }
        return getCompositionDto(compositionAccess);
    }

    // TODO: untested because not needed, yet
    @Override
    public Optional<CompositionDto> retrieveByTimestamp(UUID compositionId, LocalDateTime timestamp) {
        I_CompositionAccess compositionAccess;
        try {
            compositionAccess = I_CompositionAccess.retrieveInstanceByTimestamp(getDataAccess(), compositionId, Timestamp.valueOf(timestamp));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }

        return getCompositionDto(compositionAccess);
    }

    // Helper function to create returnable DTO
    private Optional<CompositionDto> getCompositionDto(I_CompositionAccess compositionAccess) {
        if (compositionAccess == null) {
            return Optional.empty();
        }
        final UUID ehrId = compositionAccess.getEhrid();
        //There is only one EntryAccess per compositionAccess
        return compositionAccess.getContent().stream().findAny().map(i -> new CompositionDto(i.getComposition(), i.getTemplateId(), i.getCompositionId(), ehrId));
    }

    /**
     * Public serializer entry point which will be called with
     * composition dto fetched from database and the
     * desired target serialized string format.
     * Will parse the composition dto into target format either
     * with a custom lambda expression for desired target format
     *
     * @param composition Composition dto from database
     * @param format      Target format
     * @return Structured string with string of data and content format
     */
    @Override
    public StructuredString serialize(CompositionDto composition, CompositionFormat format) {
        final StructuredString compositionString;
        switch (format) {
            case XML:
                compositionString = new StructuredString(new CanonicalXML().marshal(composition.getComposition(), false), StructuredStringFormat.XML);
                break;
            case JSON:
                compositionString = new StructuredString(new CanonicalJson().marshal(composition.getComposition()), StructuredStringFormat.JSON);
                break;

            default:
                throw new UnexpectedSwitchCaseException(format);
        }
        return compositionString;
    }

    @Override
    public UUID create(UUID ehrId, String content, CompositionFormat format) {

        final Composition composition = buildComposition(content, format);

        return internalCreate(ehrId, composition, null);
    }

    @Override
    public UUID create(UUID ehrId, String content, CompositionFormat format, String templateId, UUID linkUid) {
        return create(ehrId, content, format);
    }

    @Override
    public UUID create(UUID ehrId, Composition composition, UUID contributionId) {
        return internalCreate(ehrId, composition, contributionId);
    }

    /**
     * Creation of a new composition. With optional custom contribution, or one will be created.
     *
     * @param ehrId          ID of EHR
     * @param composition    RMObject instance of the given Composition to be created
     * @param contributionId NULL if is not needed, or ID of given custom contribution
     * @return ID of created composition
     * @throws InternalServerException when creation failed
     */
    private UUID internalCreate(UUID ehrId, Composition composition, UUID contributionId) {
        //pre-step: validate
        try {
            validationService.check(composition.getArchetypeDetails().getTemplateId().getValue(), composition);
        } catch (Exception e) {
            throw new InternalServerException(e);
        }

        //pre-step: check for valid ehrId
        if (ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        // actual creation
        final UUID compositionId;
        try {
            I_CompositionAccess compositionAccess = I_CompositionAccess.getNewInstance(getDataAccess(), composition, ehrId);
            I_EntryAccess entryAccess = I_EntryAccess.getNewInstance(getDataAccess(), composition.getArchetypeDetails().getTemplateId().getValue(), 0, compositionAccess.getId(), composition);
            compositionAccess.addContent(entryAccess);
            if (contributionId != null) {   // in case of custom contribution, set it and invoke commit that allows custom contributions
                compositionAccess.setContributionId(contributionId);
                compositionId = compositionAccess.commitWithCustomContribution(getUserUuid(), getSystemUuid(), DESCRIPTION);
            } else {    // else, invoke commit that ad hoc creates a new contribution for the composition
                compositionId = compositionAccess.commit(getUserUuid(), getSystemUuid(), DESCRIPTION);
            }
        } catch (Exception e) {
            throw new InternalServerException(e);
        }
        return compositionId;
    }

    private Composition buildComposition(String content, CompositionFormat format) {
        final Composition composition;
        switch (format) {
            case XML:
                composition = new CanonicalXML().unmarshal(content, Composition.class);
                break;
            case JSON:
                composition = new CanonicalJson().unmarshal(content, Composition.class);
                break;
            default:
                throw new UnexpectedSwitchCaseException(format);
        }
        return composition;
    }

    @Override
    public String update(UUID compositionId, CompositionFormat format, String content) {
        return update(compositionId, format, content, null);
    }

    @Override
    public String update(UUID compositionId, CompositionFormat format, String content, String templateId) {

        Composition composition = buildComposition(content, format);

        // call internalUpdate with null as contributionId to create a new ad-hoc contribution
        return internalUpdate(compositionId, composition, null);
    }

    @Override
    public String update(UUID compositionId, Composition composition, UUID contributionId) {
        // call internalUpdate with given contributionId to use it as contribution
        return internalUpdate(compositionId, composition, contributionId);
    }

    /**
     * Update of an existing composition. With optional custom contribution, or existing one will be updated.
     *
     * @param compositionId  ID of existing composition
     * @param composition    RMObject instance of the given Composition which represents the new version
     * @param contributionId NULL if is not needed, or ID of given custom contribution
     * @return Version UID pointing to updated composition
     */
    private String internalUpdate(UUID compositionId, Composition composition, UUID contributionId) {
        Boolean result;
        try {
            I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
            if (compositionAccess == null) {
                throw new ObjectNotFoundException(I_CompositionAccess.class.getName(), "Could not find composition: " + compositionId);
            }

            //validate RM composition
            validationService.check(composition.getArchetypeDetails().getTemplateId().getValue(), composition);

            // to keep reference to entry to update: pull entry out of composition access and replace composition content with input, then write back to the original access
            List<I_EntryAccess> contentList = compositionAccess.getContent();
            contentList.get(0).setCompositionData(composition);
            compositionAccess.setContent(contentList);
            if (contributionId != null) {   // if custom contribution should be set
                compositionAccess.setContributionId(contributionId);
                result = compositionAccess.updateWithCustomContribution(getUserUuid(), getSystemUuid(), I_ConceptAccess.ContributionChangeType.MODIFICATION, null);
            } else {    // else existing one will be updated
                result = compositionAccess.update(getUserUuid(), getSystemUuid(), null, I_ConceptAccess.ContributionChangeType.MODIFICATION, DESCRIPTION);
            }

        } catch (ObjectNotFoundException e) {   //otherwise ObjectNotFound exceptions would always get sucked up by the catch below
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(e);
        }

        if (!result) {
            throw new InternalServerException("Update failed on composition:" + compositionId);
        }
        return compositionId + "::" + getLastVersionNumber(compositionId);
    }

    @Override
    public LocalDateTime delete(UUID compositionId) {
        return internalDelete(compositionId, null);
    }

    @Override
    public LocalDateTime delete(UUID compositionId, UUID contributionId) {
        return internalDelete(compositionId, contributionId);
    }

    /**
     * Deletion of an existing composition. With optional custom contribution, or existing one will be updated.
     *
     * @param compositionId  ID of existing composition
     * @param contributionId NULL if is not needed, or ID of given custom contribution
     * @return Time of deletion, if successful
     */
    private LocalDateTime internalDelete(UUID compositionId, UUID contributionId) {
        I_CompositionAccess compositionAccess = null;
        try {
            compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
        } catch (Exception e) {
            throw new ObjectNotFoundException(I_CompositionAccess.class.getName(), "Error while retrieving composition", e);
        }
        if (compositionAccess == null) {
            throw new ObjectNotFoundException(I_CompositionAccess.class.getName(), "Could not find composition:" + compositionId);
        }

        Integer result = 0;
        if (contributionId != null) {   // if custom contribution should be set
            compositionAccess.setContributionId(contributionId);
            try {
                result = compositionAccess.deleteWithCustomContribution(getUserUuid(), getSystemUuid(), DESCRIPTION);
            } catch (Exception e) {
                throw new InternalServerException(e);
            }
        } else {    // if not continue with standard delete
            try {
                result = compositionAccess.delete(getUserUuid(), getSystemUuid(), DESCRIPTION);
            } catch (Exception e) {
                throw new InternalServerException(e);
            }
        }
        if (result <= 0)
            throw new InternalServerException("Delete failed on composition:" + compositionAccess.getId());

        return compositionAccess.getTimeCommitted().toLocalDateTime();
    }


    @Override
    public Integer getLastVersionNumber(UUID compositionId) throws InternalServerException {
        try {
            return I_CompositionAccess.getLastVersionNumber(getDataAccess(), compositionId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }
    }

    @Override
    public Integer getVersionByTimestamp(UUID compositionId, LocalDateTime timestamp) {
        int version;
        try {
            version = I_CompositionAccess.getVersionFromTimeStamp(getDataAccess(), compositionId, Timestamp.valueOf(timestamp));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
        if (version <= 0) {
            return null;
        } else {
            return version;
        }
    }

    private void linkComposition(UUID master, UUID child) {
        if (!supportCompositionXRef)
            return;
        if (master == null || child == null)
            return;
        I_CompoXrefAccess compoXrefAccess = new CompoXRefAccess(getDataAccess());
        compoXrefAccess.setLink(master, child);
    }


    /**
     * Internal helper funcition to read UID from given composition input in stated format.
     *
     * @param content Composition input
     * @param format  Composition format
     * @return
     */
    public String getUidFromInputComposition(String content, CompositionFormat format) throws IllegalArgumentException, InternalServerException, UnexpectedSwitchCaseException {

        Composition composition = buildComposition(content, format);
        if (composition.getUid() == null) {
            return null;
        } else {
            return composition.getUid().toString();
        }

    }
}

