/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.util;

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartyRelated;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.util.Objects;
import org.ehrbase.api.exception.InternalServerException;

/**
 * Utility class for PartyProxy and its concrete implementations.
 */
public class PartyUtils {

    private PartyUtils() {}

    public static boolean isEmpty(PartyProxy partyProxy) {
        if (partyProxy == null) {
            return true;
        }

        if (isPartySelf(partyProxy)) {
            return isEmpty((PartySelf) partyProxy);
        } else if (isPartyIdentified(partyProxy)) {
            return isEmpty((PartyIdentified) partyProxy);
        } else if (isPartyRelated(partyProxy)) {
            return isEmpty((PartyRelated) partyProxy);
        } else {
            throw new InternalServerException(
                    "Unhandled Party type detected:" + partyProxy.getClass().getSimpleName());
        }
    }

    public static boolean isEmpty(PartyIdentified partyIdentified) {
        if (partyIdentified == null) {
            return true;
        }
        return partyIdentified.getName() == null
                && partyIdentified.getIdentifiers().isEmpty()
                && (partyIdentified.getExternalRef() == null || isEmpty(partyIdentified.getExternalRef()));
    }

    public static boolean isEmpty(PartySelf partySelf) {
        if (partySelf == null) {
            return true;
        }
        return partySelf.getExternalRef() == null || isEmpty(partySelf.getExternalRef());
    }

    public static boolean isEmpty(PartyRef partyRef) {
        if (partyRef == null) {
            return true;
        }
        return partyRef.getId() == null && partyRef.getNamespace() == null && partyRef.getType() == null;
    }

    public static boolean isEmpty(PartyRelated partyRelated) {
        if (partyRelated == null) {
            return true;
        }
        return partyRelated.getName() == null
                && partyRelated.getIdentifiers().isEmpty()
                && partyRelated.getRelationship() == null
                && (partyRelated.getExternalRef() == null || isEmpty(partyRelated.getExternalRef()));
    }

    public static boolean isPartySelf(PartyProxy partyProxy) {
        return Objects.equals(partyProxy.getClass().getSimpleName(), "PartySelf");
    }

    public static boolean isPartyIdentified(PartyProxy partyProxy) {
        return Objects.equals(partyProxy.getClass().getSimpleName(), "PartyIdentified");
    }

    public static boolean isPartyRelated(PartyProxy partyProxy) {
        return Objects.equals(partyProxy.getClass().getSimpleName(), "PartyRelated");
    }
}
