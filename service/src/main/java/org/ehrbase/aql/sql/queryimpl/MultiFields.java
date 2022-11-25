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
package org.ehrbase.aql.sql.queryimpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.jooq.Field;

public class MultiFields {

    private final List<QualifiedAqlField> fields = new ArrayList<>();
    private boolean useEntryTable = false;
    private String rootJsonKey;
    private final String templateId;
    private I_VariableDefinition variableDefinition; // the variable def for this list of qualified fields

    public MultiFields(I_VariableDefinition variableDefinition, List<QualifiedAqlField> fields, String templateId) {
        this.fields.addAll(fields);
        this.variableDefinition = variableDefinition;
        this.templateId = templateId;
    }

    public MultiFields(I_VariableDefinition variableDefinition, Field<?> field, String templateId) {
        this(variableDefinition, new QualifiedAqlField(field), templateId);
    }

    public static MultiFields asNull(
            I_VariableDefinition variableDefinition, String templateId, IQueryImpl.Clause clause) {
        String alias = variableDefinition.getAlias();

        if (clause.equals(IQueryImpl.Clause.WHERE)) alias = null;
        else {
            if (alias == null) alias = DefaultColumnId.value(variableDefinition);
        }

        Field<?> nullField = new NullField(variableDefinition, alias).instance();
        return new MultiFields(variableDefinition, nullField, templateId);
    }

    public MultiFields(I_VariableDefinition variableDefinition, QualifiedAqlField field, String templateId) {
        fields.add(field);
        this.variableDefinition = variableDefinition;
        this.templateId = templateId;
    }

    public void setUseEntryTable(boolean useEntryTable) {
        this.useEntryTable = useEntryTable;
    }

    public boolean isUseEntryTable() {
        return useEntryTable;
    }

    public int fieldsSize() {
        return fields.size();
    }

    public QualifiedAqlField getQualifiedField(int index) {
        return fields.get(index);
    }

    public Iterator<QualifiedAqlField> iterator() {
        return fields.iterator();
    }

    public QualifiedAqlField getLastQualifiedField() throws UnknownVariableException {
        if (fieldsSize() > 0) return fields.get(fieldsSize() - 1);
        else throw new UnknownVariableException(variableDefinition.getPath());
    }

    public QualifiedAqlField getQualifiedFieldOrLast(int index) throws UnknownVariableException {
        if (index >= fieldsSize()) return getLastQualifiedField();
        else return getQualifiedField(index);
    }

    public int size() {
        return fields.size();
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    public String getRootJsonKey() {
        return rootJsonKey;
    }

    public void setRootJsonKey(String rootJsonKey) {
        this.rootJsonKey = rootJsonKey;
    }

    public I_VariableDefinition getVariableDefinition() {
        return variableDefinition;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void replaceField(QualifiedAqlField originalField, Field newField) {
        int index = fields.indexOf(originalField);
        QualifiedAqlField originalAqlField = fields.get(index);
        QualifiedAqlField clonedQualifiedField = originalAqlField.duplicate();
        clonedQualifiedField.setField(newField);
        fields.set(fields.indexOf(originalField), clonedQualifiedField);
    }
}
