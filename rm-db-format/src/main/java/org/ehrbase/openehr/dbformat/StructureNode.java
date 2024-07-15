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
package org.ehrbase.openehr.dbformat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StructureNode {

    private int num = -1;
    private int numCap = -1;
    private String rmEntity;
    private String archetypeNodeId;
    private String entityName;
    private StructureIndex entityIdx;

    private final List<StructureNode> children = new ArrayList<>();
    private ObjectNode jsonNode;

    private StructureRmType structureRmType;

    private StructureNode contentItem;
    private int parentNum = -1;

    public StructureNode() {
        this.entityIdx = StructureIndex.of();
    }

    public StructureNode(StructureNode parent) {
        parent.children.add(this);
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getRmEntity() {
        return rmEntity;
    }

    public void setRmEntity(String rmEntity) {
        this.rmEntity = rmEntity;
    }

    public void setStructureRmType(StructureRmType structureRmType) {
        this.structureRmType = structureRmType;
        setRmEntity(structureRmType.name());
    }

    public StructureRmType getStructureRmType() {
        return structureRmType;
    }

    public String getArchetypeNodeId() {
        return archetypeNodeId;
    }

    public void setArchetypeNodeId(String archetypeNodeId) {
        this.archetypeNodeId = archetypeNodeId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public StructureIndex getEntityIdx() {
        return entityIdx;
    }

    public List<StructureNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void setEntityIdx(StructureIndex entityIdx) {
        this.entityIdx = entityIdx;
    }

    public ObjectNode getJsonNode() {
        return jsonNode;
    }

    public void setJsonNode(ObjectNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    @Override
    public String toString() {
        return "StructureNode{" + "num="
                + num + ", rmEntity='"
                + rmEntity + '\'' + ", entityConcept='"
                + archetypeNodeId + '\'' + ", entityName='"
                + entityName + '\'' + ", entityIdx="
                + entityIdx + ", children="
                + children + ", jsonNode="
                + jsonNode + ", structureRmType="
                + structureRmType + '}';
    }

    public StructureNode getContentItem() {
        return contentItem;
    }

    public void setContentItem(StructureNode contentItem) {
        this.contentItem = contentItem;
    }

    public int getParentNum() {
        return parentNum;
    }

    public void setParentNum(final int parentNum) {
        this.parentNum = parentNum;
    }

    /**
     * numCap is calculated at first access
     * @return max num of node and its descendents
     */
    public int getNumCap() {
        if (numCap == -1) {
            numCap = children.stream().mapToInt(StructureNode::getNumCap).max().orElse(num);
        }
        return numCap;
    }
}
