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
package org.ehrbase.repository;

import com.nedap.archie.rm.archetyped.Locatable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.dbformat.StructureIndex;
import org.ehrbase.openehr.dbformat.StructureNode;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.dbformat.VersionedObjectDataStructure;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectDataRecordPrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectDataTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectVersionRecordPrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectVersionTablePrototype;
import org.jooq.DSLContext;
import org.jooq.JSONB;

/**
 * Holds the database records representing one openEHR VERSION.
 *
 * @param versionRecord the version record
 * @param dataRecords a single-use stream of data records (created lazily)
 */
public record VersionDataDbRecord(
        ObjectVersionRecordPrototype versionRecord, Supplier<Stream<ObjectDataRecordPrototype>> dataRecords) {

    public static VersionDataDbRecord toRecords(
            UUID ehrId,
            Locatable versionDataObject,
            UUID contributionId,
            UUID auditId,
            OffsetDateTime now,
            DSLContext context) {
        var roots = VersionedObjectDataStructure.createDataStructure(versionDataObject);

        UUID voId = UUID.fromString(versionDataObject.getUid().getRoot().getValue());

        ObjectVersionRecordPrototype versionRecord = VersionDataDbRecord.buildVersionRecord(
                context,
                ehrId,
                voId,
                AbstractVersionedObjectRepository.extractVersion(versionDataObject.getUid()),
                contributionId,
                auditId,
                now);

        Supplier<Stream<ObjectDataRecordPrototype>> dataRecords =
                VersionDataDbRecord.dataRecordsBuilder(voId, roots, context);

        return new VersionDataDbRecord(versionRecord, dataRecords);
    }

    private static ObjectVersionRecordPrototype buildVersionRecord(
            DSLContext context,
            UUID ehrId,
            UUID voId,
            int sysVersion,
            UUID contributionId,
            UUID auditId,
            OffsetDateTime now) {
        ObjectVersionRecordPrototype objectDataRecord = context.newRecord(ObjectVersionTablePrototype.INSTANCE);

        // system columns
        objectDataRecord.setEhrId(ehrId);
        objectDataRecord.setVoId(voId);
        objectDataRecord.setSysVersion(sysVersion);
        objectDataRecord.setSysPeriodLower(now);
        objectDataRecord.setAuditId(auditId);
        objectDataRecord.setContributionId(contributionId);

        return objectDataRecord;
    }

    private static Supplier<Stream<ObjectDataRecordPrototype>> dataRecordsBuilder(
            UUID voId, Collection<StructureNode> nodeList, DSLContext context) {
        return () -> nodeList.stream()
                .filter(r -> r.getStructureRmType().isStructureEntry())
                .map(n -> buildDataRecord(voId, n, context));
    }

    private static ObjectDataRecordPrototype buildDataRecord(UUID voId, StructureNode node, DSLContext context) {

        ObjectDataRecordPrototype rec = context.newRecord(ObjectDataTablePrototype.INSTANCE);

        rec.setNum(node.getNum());

        rec.setCitemNum(Optional.of(node)
                .map(StructureNode::getContentItem)
                .map(StructureNode::getNum)
                .orElse(null));
        rec.setParentNum(node.getParentNum());
        rec.setNumCap(node.getNumCap());
        rec.setRmEntity(StructureRmType.byTypeName(node.getRmEntity())
                .orElseThrow(() -> new InternalServerException("No alias for %s".formatted(node.getRmEntity())))
                .getAlias());
        rec.setEntityConcept(AslRmTypeAndConcept.toEntityConcept(node.getArchetypeNodeId()));
        rec.setEntityName(node.getEntityName());

        StructureIndex index = node.getEntityIdx();
        rec.setEntityAttribute(index.printLastAttribute());
        rec.setEntityPath(index.printIndexString(false, false));
        rec.setEntityPathCap(index.printIndexString(true, false));
        rec.setEntityIdx(index.printIndexString(false, true));
        rec.setEntityIdxCap(index.printIndexString(true, true));
        rec.setEntityIdxLen(index.length());

        rec.setData(JSONB.valueOf(
                VersionedObjectDataStructure.applyRmAliases(node.getJsonNode()).toString()));

        // system columns
        rec.setVoId(voId);

        return rec;
    }
}
