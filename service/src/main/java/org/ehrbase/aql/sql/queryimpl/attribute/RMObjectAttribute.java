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
package org.ehrbase.aql.sql.queryimpl.attribute;

import org.ehrbase.aql.sql.binding.IJoinBinder;
import org.ehrbase.aql.sql.queryimpl.DefaultColumnId;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.attribute.composition.CompositionIdFieldSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.ehr.EhrSetup;
import org.jooq.Field;

@SuppressWarnings({"java:S3740", "java:S1452"})
public abstract class RMObjectAttribute implements IRMObjectAttribute, IJoinBinder {

    protected FilterSetup filterSetup = new FilterSetup();
    protected CompositionIdFieldSetup compositionIdFieldSetup = new CompositionIdFieldSetup();
    protected EhrSetup ehrSetup = new EhrSetup();

    protected final JoinSetup joinSetup;
    protected final FieldResolutionContext fieldContext;

    protected RMObjectAttribute(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        this.fieldContext = fieldContext;
        this.joinSetup = joinSetup;
    }

    protected Field<?> as(Field field) {
        if (fieldContext.isWithAlias()) return aliased(field);
        else {
            if (!fieldContext.getClause().equals(IQueryImpl.Clause.WHERE)) return defaultAliased(field);
            else return field;
        }
    }

    protected Field<?> aliased(Field field) {
        return field.as(effectiveAlias());
    }

    protected String effectiveAlias() {
        return (fieldContext.getVariableDefinition().getAlias() == null)
                ? "/" + fieldContext.getColumnAlias()
                : fieldContext.getVariableDefinition().getAlias();
    }

    protected Field<?> defaultAliased(Field field) {
        if (fieldContext.getClause().equals(IQueryImpl.Clause.WHERE)) return field;
        else return field.as(DefaultColumnId.value(fieldContext.getVariableDefinition()));
    }
}
