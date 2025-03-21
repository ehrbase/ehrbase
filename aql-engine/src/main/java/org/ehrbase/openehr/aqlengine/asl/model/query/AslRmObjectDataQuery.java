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
package org.ehrbase.openehr.aqlengine.asl.model.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.ehrbase.openehr.aqlengine.asl.meta.AslTypeOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslDescendantCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField.FieldSource;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.jooq.JSONB;

/**
 * <pre>
 *   select
 * 	  jsonb_object_agg(
 * 	( sub_string(d2."entity_idx" FROM char_length(c2."entity_idx") + 1)
 * 	), "data"
 * 	) as "data"
 *     from "ehr"."comp_one" d2
 * 	  where
 *       c2."ehr_id" = "d2"."ehr_id"
 *       and c2."VO_ID" = "d2"."VO_ID"
 *       and c2."num" <= "d2"."num"
 *       and c2."num_cap" >= "d2"."num"
 * 	  group by "d2"."VO_ID"
 * 	 </pre>
 *
 * @see AslDescendantCondition
 */
public final class AslRmObjectDataQuery extends AslDataQuery {
    private final AslField field;

    public AslRmObjectDataQuery(String alias, AslTypeOrigin origin, AslStructureQuery base, AslQuery baseProvider) {
        super(alias, origin, base, baseProvider);
        this.field = new AslColumnField(JSONB.class, "data", FieldSource.withOwner(this), false);
    }

    @Override
    public Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering() {
        return Collections.emptyMap();
    }

    @Override
    public List<AslField> getSelect() {
        return List.of(field);
    }
}
