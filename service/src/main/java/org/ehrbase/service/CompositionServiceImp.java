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

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.VersionedComposition;
import com.nedap.archie.rm.generic.Attestation;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.dao.access.interfaces.I_AttestationAccess;
import org.ehrbase.dao.access.interfaces.I_CompoXrefAccess;
import org.ehrbase.dao.access.interfaces.I_CompositionAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess.ContributionChangeType;
import org.ehrbase.dao.access.interfaces.I_EntryAccess;
import org.ehrbase.dao.access.jooq.AttestationAccess;
import org.ehrbase.dao.access.jooq.CompoXRefAccess;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.StructuredStringFormat;
import org.ehrbase.serialisation.flatencoding.FlatFormat;
import org.ehrbase.serialisation.flatencoding.FlatJasonProvider;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.serialisation.xmlencoding.CanonicalXML;
import org.ehrbase.validation.ConstraintViolationException;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.ehrbase.webtemplate.templateprovider.TemplateProvider;
import org.jooq.DSLContext;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional()
public class CompositionServiceImp extends BaseServiceImp implements CompositionService {

  public static final String DESCRIPTION = "description";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ValidationService validationService;
  private final KnowledgeCacheService knowledgeCacheService;
  private final EhrService ehrService;
  private boolean supportCompositionXRef = false;

  @Autowired
  public CompositionServiceImp(
      KnowledgeCacheService knowledgeCacheService,
      ValidationService validationService,
      EhrService ehrService,
      DSLContext context,
      ServerConfig serverConfig) {

    super(knowledgeCacheService, context, serverConfig);
    this.validationService = validationService;
    this.ehrService = ehrService;
    this.knowledgeCacheService = knowledgeCacheService;
  }

  @Override
  public Optional<CompositionDto> create(
      UUID ehrId, Composition objData, UUID systemId, UUID committerId, String description) {

    UUID compositionId = internalCreate(ehrId, objData, systemId, committerId, description, null);
    return getCompositionDto(I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId));
  }

  @Override
  public Optional<CompositionDto> create(UUID ehrId, Composition objData, UUID contribution) {
    UUID compositionId = internalCreate(ehrId, objData, null, null, null, contribution);
    return getCompositionDto(I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId));
  }

  @Override
  public Optional<CompositionDto> create(UUID ehrId, Composition objData) {
    return create(ehrId, objData, getSystemUuid(), getUserUuid(), null);
  }

  /**
   * Creation of a new composition. With optional custom contribution, or one will be created.
   *
   * @param ehrId          ID of EHR
   * @param composition    RMObject instance of the given Composition to be created
   * @param systemId       Audit system; or NULL if contribution is given
   * @param committerId    Audit committer; or NULL if contribution is given
   * @param description    (Optional) Audit description; or NULL if contribution is given
   * @param contributionId NULL if is not needed, or ID of given custom contribution
   * @return ID of created composition
   * @throws InternalServerException when creation failed
   */
  private UUID internalCreate(
      UUID ehrId,
      Composition composition,
      UUID systemId,
      UUID committerId,
      String description,
      UUID contributionId) {
    // pre-step: validate
    try {
      validationService.check(composition);

    } catch (org.ehrbase.validation.ValidationException e) {
      throw new UnprocessableEntityException(e.getMessage());
    } catch (Exception e) {
      // rethrow if this class, but wrap all others in InternalServerException
      if (e.getClass().equals(UnprocessableEntityException.class)) {
        throw (UnprocessableEntityException) e;
      }
      if (e.getClass().equals(IllegalArgumentException.class)) {
        throw new ValidationException(e);
      } else if (e.getClass()
          .equals(org.ehrbase.validation.ValidationException.class)) {
        throw new ValidationException(e);
      } else {
        throw new InternalServerException(e);
      }
    }

    // pre-step: check for valid ehrId
    if (ehrService.hasEhr(ehrId).equals(Boolean.FALSE)) {
      throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
    }

    // actual creation
    final UUID compositionId;
    try {
      var compositionAccess =
          I_CompositionAccess.getNewInstance(getDataAccess(), composition, ehrId);
      var entryAccess =
          I_EntryAccess.getNewInstance(
              getDataAccess(),
              Objects.requireNonNull(composition.getArchetypeDetails().getTemplateId()).getValue(),
              0,
              compositionAccess.getId(),
              composition);
      compositionAccess.addContent(entryAccess);
      if (contributionId
          != null) { // in case of custom contribution, set it and invoke commit that allows custom
        // contributions
        compositionAccess.setContributionId(contributionId);
        compositionId = compositionAccess.commit(LocalDateTime.now(), contributionId);
      } else { // else, invoke commit that ad hoc creates a new contribution for the composition
        if (committerId == null || systemId == null) { // mandatory fields
          throw new InternalServerException(
              "Error on internal contribution handling for composition creation.");
        }
        compositionId =
            compositionAccess.commit(LocalDateTime.now(), committerId, systemId, description);
      }
    } catch (Exception e) {
      if (e instanceof IllegalArgumentException) {
        throw new IllegalArgumentException(e);
      } else {
        throw new InternalServerException(e);
      }
    }
    return compositionId;
  }

  @Override
  public Optional<CompositionDto> update(
      UUID ehrId,
      ObjectVersionId targetObjId,
      Composition objData,
      UUID systemId,
      UUID committerId,
      String description) {

    var compoId =
        internalUpdate(
            UUID.fromString(targetObjId.getObjectId().getValue()),
            objData,
            systemId,
            committerId,
            description,
            null);
    return getCompositionDto(
        I_CompositionAccess.retrieveInstance(
            getDataAccess(), UUID.fromString(compoId.getObjectId().getValue())));
  }

  @Override
  public Optional<CompositionDto> update(
      UUID ehrId, ObjectVersionId targetObjId, Composition objData, UUID contribution) {

    var compoId =
        internalUpdate(
            UUID.fromString(targetObjId.getObjectId().getValue()),
            objData,
            null,
            null,
            null,
            contribution);
    return getCompositionDto(
        I_CompositionAccess.retrieveInstance(
            getDataAccess(), UUID.fromString(compoId.getObjectId().getValue())));
  }

  @Override
  public Optional<CompositionDto> update(
      UUID ehrId, ObjectVersionId targetObjId, Composition objData) {
    return update(ehrId, targetObjId, objData, getSystemUuid(), getUserUuid(), null);
  }

  /**
   * Update of an existing composition. With optional custom contribution, or existing one will be
   * updated.
   *
   * @param compositionId  ID of existing composition
   * @param composition    RMObject instance of the given Composition which represents the new
   *                       version
   * @param systemId       Audit system; or NULL if contribution is given
   * @param committerId    Audit committer; or NULL if contribution is given
   * @param description    (Optional) Audit description; or NULL if contribution is given
   * @param contributionId NULL if new one should be created; or ID of given custom contribution
   * @return Version UID pointing to updated composition
   */
  private ObjectVersionId internalUpdate(
      UUID compositionId,
      Composition composition,
      UUID systemId,
      UUID committerId,
      String description,
      UUID contributionId) {
    boolean result;
    try {
      var compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
      if (compositionAccess == null) {
        throw new ObjectNotFoundException(
            I_CompositionAccess.class.getName(), "Could not find composition: " + compositionId);
      }

      // validate RM composition
      validationService.check(composition);

      // Check if template ID is not the same in existing and given data -> error
      String existingTemplateId = compositionAccess.getContent().get(0).getTemplateId();
      String inputTemplateId = composition.getArchetypeDetails().getTemplateId().getValue();
      if (!existingTemplateId.equals(inputTemplateId)) {
        // check if base template ID doesn't match  (template ID schema: "$NAME.$LANG.v$VER")
        if (!existingTemplateId.split("\\.")[0].equals(inputTemplateId.split("\\.")[0])) {
          throw new InvalidApiParameterException(
              "Can't update composition to have different template.");
        }
        // if base matches, check if given template ID is just a new version of the correct template
        int existingTemplateIdVersion = Integer.parseInt(existingTemplateId.split("\\.v")[1]);
        int inputTemplateIdVersion =
            Integer.parseInt(inputTemplateId.substring(inputTemplateId.lastIndexOf("\\.v") + 1));
        if (inputTemplateIdVersion < existingTemplateIdVersion) {
          throw new InvalidApiParameterException(
              "Can't update composition with wrong template version bump.");
        }
      }

      // to keep reference to entry to update: pull entry out of composition access and replace
      // composition content with input, then write back to the original access
      List<I_EntryAccess> contentList = compositionAccess.getContent();
      contentList.get(0).setCompositionData(composition);
      compositionAccess.setContent(contentList);
      compositionAccess.setComposition(composition);
      if (contributionId != null) { // if custom contribution should be set
        compositionAccess.setContributionId(contributionId);
        result = compositionAccess.update(LocalDateTime.now(), contributionId);
      } else { // else existing one will be updated
        if (committerId == null || systemId == null) {
          throw new InternalServerException(
              "Failed to update composition, missing mandatory audit meta data.");
        }
        result =
            compositionAccess.update(
                LocalDateTime.now(),
                committerId,
                systemId,
                description,
                ContributionChangeType.MODIFICATION);
      }

    } catch (ObjectNotFoundException
        | InvalidApiParameterException
        e) { // otherwise exceptions would always get sucked up by the catch below
      throw e;
    } catch (Exception e) {
      throw new InternalServerException(e);
    }

    if (!result) {
      throw new InternalServerException("Update failed on composition:" + compositionId);
    }
    return new ObjectVersionId(
        compositionId.toString(),
        this.getServerConfig().getNodename(),
        getLastVersionNumber(compositionId).toString());
  }

  @Override
  public boolean delete(
      UUID ehrId,
      ObjectVersionId targetObjId,
      UUID systemId,
      UUID committerId,
      String description) {
    return internalDelete(
        UUID.fromString(targetObjId.getObjectId().getValue()),
        systemId,
        committerId,
        description,
        null);
  }

  @Override
  public boolean delete(UUID ehrId, ObjectVersionId targetObjId, UUID contribution) {
    return internalDelete(
        UUID.fromString(targetObjId.getObjectId().getValue()), null, null, null, contribution);
  }

  @Override
  public boolean delete(UUID ehrId, ObjectVersionId targetObjId) {
    return delete(ehrId, targetObjId, getSystemUuid(), getUserUuid(), null);
  }

  /**
   * Deletion of an existing composition. With optional custom contribution, or existing one will be
   * updated.
   *
   * @param compositionId  ID of existing composition
   * @param systemId       Audit system; or NULL if contribution is given
   * @param committerId    Audit committer; or NULL if contribution is given
   * @param description    (Optional) Audit description; or NULL if contribution is given
   * @param contributionId NULL if is not needed, or ID of given custom contribution
   * @return Time of deletion, if successful
   */
  private boolean internalDelete(
      UUID compositionId,
      UUID systemId,
      UUID committerId,
      String description,
      UUID contributionId) {
    I_CompositionAccess compositionAccess;
    try {
      compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
    } catch (Exception e) {
      throw new ObjectNotFoundException(
          I_CompositionAccess.class.getName(), "Error while retrieving composition", e);
    }
    if (compositionAccess == null) {
      throw new ObjectNotFoundException(
          I_CompositionAccess.class.getName(), "Could not find composition:" + compositionId);
    }

    int result;
    if (contributionId != null) { // if custom contribution should be set
      compositionAccess.setContributionId(contributionId);
      try {
        result = compositionAccess.delete(LocalDateTime.now(), contributionId);
      } catch (Exception e) {
        throw new InternalServerException(e);
      }
    } else { // if not continue with standard delete
      try {
        if (committerId == null || systemId == null) {
          throw new InternalServerException(
              "Failed to update composition, missing mandatory audit meta data.");
        }
        result = compositionAccess.delete(LocalDateTime.now(), committerId, systemId, description);
      } catch (Exception e) {
        throw new InternalServerException(e);
      }
    }
    if (result <= 0) {
      throw new InternalServerException(
          "Delete failed on composition:" + compositionAccess.getId());
    } else {
      return true;
    }
  }

  @Override
  public Optional<CompositionDto> retrieve(UUID compositionId, Integer version)
      throws InternalServerException {

    final I_CompositionAccess compositionAccess;
    if (version != null) {
      compositionAccess =
          I_CompositionAccess.retrieveCompositionVersion(getDataAccess(), compositionId, version);
    } else { // default to latest version
      compositionAccess =
          I_CompositionAccess.retrieveCompositionVersion(
              getDataAccess(), compositionId, getLastVersionNumber(compositionId));
    }
    return getCompositionDto(compositionAccess);
  }

  // TODO: untested because not needed, yet
  @Override
  public Optional<CompositionDto> retrieveByTimestamp(UUID compositionId, LocalDateTime timestamp) {
    I_CompositionAccess compositionAccess;
    try {
      compositionAccess =
          I_CompositionAccess.retrieveInstanceByTimestamp(
              getDataAccess(), compositionId, Timestamp.valueOf(timestamp));
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
    // There is only one EntryAccess per compositionAccess
    return compositionAccess.getContent().stream()
        .findAny()
        .map(
            i ->
                new CompositionDto(
                    i.getComposition(), i.getTemplateId(), i.getCompositionId(), ehrId));
  }

  /**
   * Public serializer entry point which will be called with composition dto fetched from database
   * and the desired target serialized string format. Will parse the composition dto into target
   * format either with a custom lambda expression for desired target format
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
        compositionString =
            new StructuredString(
                new CanonicalXML().marshal(composition.getComposition(), false),
                StructuredStringFormat.XML);
        break;
      case JSON:
        compositionString =
            new StructuredString(
                new CanonicalJson().marshal(composition.getComposition()),
                StructuredStringFormat.JSON);
        break;
      case FLAT:
        compositionString =
            new StructuredString(
                new FlatJasonProvider(
                    new TemplateProvider() {
                      @Override
                      public Optional<OPERATIONALTEMPLATE> find(String s) {
                        return knowledgeCacheService.retrieveOperationalTemplate(s);
                      }

                      @Override
                      public Optional<WebTemplate> buildIntrospect(String templateId) {
                        return Optional.ofNullable(
                            knowledgeCacheService.getQueryOptMetaData(templateId));
                      }
                    })
                    .buildFlatJson(FlatFormat.SIM_SDT, composition.getTemplateId())
                    .marshal(composition.getComposition()),
                StructuredStringFormat.JSON);
        break;
      case STRUCTURED:
        compositionString =
            new StructuredString(
                new FlatJasonProvider(
                    new TemplateProvider() {
                      @Override
                      public Optional<OPERATIONALTEMPLATE> find(String s) {
                        return knowledgeCacheService.retrieveOperationalTemplate(s);
                      }

                      @Override
                      public Optional<WebTemplate> buildIntrospect(String templateId) {
                        return Optional.ofNullable(
                            knowledgeCacheService.getQueryOptMetaData(templateId));
                      }
                    })
                    .buildFlatJson(FlatFormat.STRUCTURED, composition.getTemplateId())
                    .marshal(composition.getComposition()),
                StructuredStringFormat.JSON);
        break;
      default:
        throw new UnexpectedSwitchCaseException(format);
    }
    return compositionString;
  }

  public Composition buildComposition(String content, CompositionFormat format, String templateId) {
    final Composition composition;
    switch (format) {
      case XML:
        composition = new CanonicalXML().unmarshal(content, Composition.class);
        break;
      case JSON:
        composition = new CanonicalJson().unmarshal(content, Composition.class);
        break;
      case FLAT:
        composition =
            new FlatJasonProvider(
                new TemplateProvider() {
                  @Override
                  public Optional<OPERATIONALTEMPLATE> find(String s) {
                    return knowledgeCacheService.retrieveOperationalTemplate(s);
                  }

                  @Override
                  public Optional<WebTemplate> buildIntrospect(String templateId) {
                    return Optional.ofNullable(
                        knowledgeCacheService.getQueryOptMetaData(templateId));
                  }
                })
                .buildFlatJson(FlatFormat.SIM_SDT, templateId)
                .unmarshal(content);
        break;
      case STRUCTURED:
        composition =
            new FlatJasonProvider(
                new TemplateProvider() {
                  @Override
                  public Optional<OPERATIONALTEMPLATE> find(String s) {
                    return knowledgeCacheService.retrieveOperationalTemplate(s);
                  }

                  @Override
                  public Optional<WebTemplate> buildIntrospect(String templateId) {
                    return Optional.ofNullable(
                        knowledgeCacheService.getQueryOptMetaData(templateId));
                  }
                })
                .buildFlatJson(FlatFormat.STRUCTURED, templateId)
                .unmarshal(content);
        break;
      default:
        throw new UnexpectedSwitchCaseException(format);
    }
    return composition;
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
      version =
          I_CompositionAccess.getVersionFromTimeStamp(
              getDataAccess(), compositionId, Timestamp.valueOf(timestamp));
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw e;
    }
    if (version <= 0) {
      throw new InternalServerException("Invalid version number calculated.");
    } else {
      return version;
    }
  }

  private void linkComposition(UUID master, UUID child) {
    if (!supportCompositionXRef) {
      return;
    }
    if (master == null || child == null) {
      return;
    }
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
  @Override
  public String getUidFromInputComposition(String content, CompositionFormat format)
      throws IllegalArgumentException, InternalServerException, UnexpectedSwitchCaseException {

    Composition composition = buildComposition(content, format, null);
    if (composition.getUid() == null) {
      return null;
    } else {
      return composition.getUid().toString();
    }
  }

  @Override
  public String getTemplateIdFromInputComposition(String content, CompositionFormat format) {
    Composition composition = buildComposition(content, format, null);
    if (composition.getArchetypeDetails() == null
        || composition.getArchetypeDetails().getTemplateId() == null) {
      return null;
    } else {
      return composition.getArchetypeDetails().getTemplateId().getValue();
    }
  }

  @Override
  public boolean exists(UUID versionedObjectId) {
    return I_CompositionAccess.exists(this.getDataAccess(), versionedObjectId);
  }

  @Override
  public boolean isDeleted(UUID versionedObjectId) {
    return I_CompositionAccess.isDeleted(this.getDataAccess(), versionedObjectId);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Override
  public void adminDelete(UUID compositionId) {
    I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(),
        compositionId);
    if (compositionAccess != null) {
      compositionAccess.adminDelete();
    }
  }

  @Override
  public VersionedComposition getVersionedComposition(UUID ehrId, UUID composition) {
    Optional<CompositionDto> dto = retrieve(composition, 1);

    VersionedComposition compo = new VersionedComposition();
    if (dto.isPresent()) {
      compo.setUid(new HierObjectId(dto.get().getUuid().toString()));
      compo.setOwnerId(
          new ObjectRef<>(new HierObjectId(dto.get().getEhrId().toString()), "local", "ehr"));

      Map<Integer, I_CompositionAccess> compos =
          I_CompositionAccess.getVersionMapOfComposition(getDataAccess(), composition);
      if (compos.containsKey(1)) {
        compo.setTimeCreated(
            new DvDateTime(
                OffsetDateTime.of(
                    compos.get(1).getSysTransaction().toLocalDateTime(),
                    OffsetDateTime.now().getOffset())));
      } else {
        throw new InternalServerException("Inconsistent composition data, no version 1 available");
      }
    }

    return compo;
  }

  @Override
  public RevisionHistory getRevisionHistoryOfVersionedComposition(UUID composition) {
    // get number of versions
    int versions = getLastVersionNumber(composition);
    // fetch each version and add to revision history
    RevisionHistory revisionHistory = new RevisionHistory();
    for (int i = 1; i <= versions; i++) {
      Optional<OriginalVersion<Composition>> compoVersion =
          getOriginalVersionComposition(composition, i);
      compoVersion.ifPresent(
          compositionOriginalVersion ->
              revisionHistory.addItem(
                  revisionHistoryItemFromComposition(compositionOriginalVersion)));
    }

    if (revisionHistory.getItems().isEmpty()) {
      throw new InternalServerException(
          "Problem creating RevisionHistory"); // never should be empty; not valid
    }
    return revisionHistory;
  }

  private RevisionHistoryItem revisionHistoryItemFromComposition(
      OriginalVersion<Composition> composition) {

    ObjectVersionId objectVersionId = composition.getUid();

    // Note: is List but only has more than one item when there are contributions regarding this
    // object of change type attestation
    List<AuditDetails> auditDetailsList = new ArrayList<>();
    // retrieving the audits
    auditDetailsList.add(composition.getCommitAudit());

    // add retrieval of attestations, if there are any
    if (composition.getAttestations() != null) {
      for (Attestation a : composition.getAttestations()) {
        AuditDetails newAudit =
            new AuditDetails(
                a.getSystemId(),
                a.getCommitter(),
                a.getTimeCommitted(),
                a.getChangeType(),
                a.getDescription());
        auditDetailsList.add(newAudit);
      }
    }

    return new RevisionHistoryItem(objectVersionId, auditDetailsList);
  }

  @Override
  public Optional<OriginalVersion<Composition>> getOriginalVersionComposition(
      UUID versionedObjectUid, int version) {
    // check for valid version parameter
    if ((version == 0)
        || I_CompositionAccess.getLastVersionNumber(getDataAccess(), versionedObjectUid)
        < version) {
      throw new ObjectNotFoundException(
          "versioned_composition", "No VERSIONED_COMPOSITION with given version: " + version);
    }

    // retrieve requested object
    I_CompositionAccess compositionAccess =
        I_CompositionAccess.retrieveCompositionVersion(
            getDataAccess(), versionedObjectUid, version);
    if (compositionAccess == null) {
      return Optional.empty();
    }

    // create data for output, i.e. fields of the OriginalVersion<Composition>
    ObjectVersionId versionId =
        new ObjectVersionId(
            versionedObjectUid + "::" + getServerConfig().getNodename() + "::" + version);
    DvCodedText lifecycleState =
        new DvCodedText(
            "complete",
            new CodePhrase(
                "532")); // TODO: once lifecycle state is supported, get it here dynamically
    AuditDetails commitAudit = compositionAccess.getAuditDetailsAccess().getAsAuditDetails();
    ObjectRef<HierObjectId> contribution =
        new ObjectRef<>(
            new HierObjectId(compositionAccess.getContributionId().toString()),
            "openehr",
            "contribution");
    List<UUID> attestationIdList =
        I_AttestationAccess.retrieveListOfAttestationsByRef(
            getDataAccess(), compositionAccess.getAttestationRef());
    List<Attestation> attestations =
        null; // as default, gets content if available in the following lines
    if (!attestationIdList.isEmpty()) {
      attestations = new ArrayList<>();
      for (UUID id : attestationIdList) {
        I_AttestationAccess a = new AttestationAccess(getDataAccess()).retrieveInstance(id);
        attestations.add(a.getAsAttestation());
      }
    }

    ObjectVersionId precedingVersionId = null;
    // check if there is a preceding version and set it, if available
    if (version > 1) {
      // in the current scope version is an int and therefore: preceding = current - 1
      precedingVersionId =
          new ObjectVersionId(
              versionedObjectUid + "::" + getServerConfig().getNodename() + "::" + (version - 1));
    }

    Optional<CompositionDto> compositionDto = retrieve(versionedObjectUid, version);
    Composition composition = null;
    if (compositionDto.isPresent()) {
      composition = compositionDto.get().getComposition();
    }

    OriginalVersion<Composition> versionComposition =
        new OriginalVersion<>(
            versionId,
            precedingVersionId,
            composition,
            lifecycleState,
            commitAudit,
            contribution,
            null,
            null,
            attestations);

    return Optional.of(versionComposition);
  }
}
