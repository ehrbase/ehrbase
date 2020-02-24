/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.validation.constraints.wrappers;

import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import org.apache.commons.lang3.StringUtils;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CDVORDINAL;
import org.openehr.schemas.v1.DVORDINAL;

import java.util.Map;

/**
 * Validate a DvOrdinal
 *
 * @see DvOrdinal
 *
 * Created by christian on 7/24/2016.
 */
public class CDvOrdinal extends CConstraint implements I_CArchetypeConstraintValidate {

    CDvOrdinal(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException {

        if (!(aValue instanceof DvOrdinal))
            ValidationException.raise(path, "INTERNAL: argument is not a DvOrdinal", "DV_ORDINAL_01");

        DvOrdinal dvOrdinal = (DvOrdinal) aValue;
        CDVORDINAL cdvordinal = (CDVORDINAL) archetypeconstraint;

        if (dvOrdinal.getValue() == null)
            ValidationException.raise(path, "DvOrdinal requires a non null value", "DV_ORDINAL_02");

        if (dvOrdinal.getSymbol() == null)
            ValidationException.raise(path, "DvOrdinal requires a non null symbol", "DV_ORDINAL_03");

        match_loop:
        {
            if (cdvordinal.sizeOfListArray() > 0) {
                for (DVORDINAL ordinal : cdvordinal.getListArray()) {
                    if (ordinal.getValue() == dvOrdinal.getValue()) {
                        //check symbol
                        if (StringUtils.isNotEmpty(ordinal.getSymbol().getValue()) && !(ordinal.getSymbol().getValue().equals(dvOrdinal.getSymbol().getValue())))
                            continue;
                        String codeString = dvOrdinal.getSymbol().getDefiningCode().getCodeString();
                        String terminology = dvOrdinal.getSymbol().getDefiningCode().getTerminologyId().getValue();

                        if (!(StringUtils.isNotEmpty(codeString) && ordinal.getSymbol().getDefiningCode().getCodeString().equals(codeString))
                                &&
                                (StringUtils.isNotEmpty(terminology) && ordinal.getSymbol().getDefiningCode().getTerminologyId().getValue().equals(terminology)))
                            continue;
                        break match_loop;
                    }
                }
                ValidationException.raise(path, "DvOrdinal does not match any valid value, ordinal value:" + dvOrdinal.getValue() + ", code:'" + dvOrdinal.getSymbol() + "'", "DV_ORDINAL_01");
            }
        }
    }
}
