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
package org.ehrbase.jooq.dbencoding.attributes;

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.*;

import com.nedap.archie.rm.composition.Action;
import com.nedap.archie.rm.composition.InstructionDetails;
import com.nedap.archie.rm.composition.IsmTransition;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.ItemStack;
import org.ehrbase.jooq.dbencoding.RmObjectEncoding;

/**
 * populate the attributes for RM Action
 */
public class ActionAttributes extends CareEntryAttributes {

    public ActionAttributes(CompositionSerializer compositionSerializer, ItemStack itemStack, Map<String, Object> map) {
        super(compositionSerializer, itemStack, map);
    }

    public Map<String, Object> toMap(Action action) {

        if (action.getTime() != null && !action.getTime().equals(new DvDateTime())) {
            map = toMap(TAG_TIME, action.getTime(), action.getName());
        }

        if (action.getInstructionDetails() != null) {
            InstructionDetails instructionDetails = action.getInstructionDetails();
            map.put(TAG_INSTRUCTION_DETAILS, new RmObjectEncoding(instructionDetails).toMap());
        }

        if (action.getIsmTransition() != null) {
            IsmTransition ismTransition = action.getIsmTransition();
            if (ismTransition != null) {
                map.put(TAG_ISM_TRANSITION, new RmObjectEncoding(ismTransition).toMap());
            }
        }

        map = super.toMap(action);

        return map;
    }
}
