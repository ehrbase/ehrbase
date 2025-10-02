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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nedap.archie.rm.demographic.Actor;
import com.nedap.archie.rm.ehr.EhrAccess;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMAttributeInfo;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RmAttributeAliasTest {

    @Test
    void checkAliases() {
        Set<String> attributes = RmAttributeAlias.VALUES.stream()
                .map(RmAttributeAlias::attribute)
                .collect(Collectors.toSet());

        attributes.forEach(a -> assertThatThrownBy(() -> RmAttributeAlias.getAttribute(a))
                .withFailMessage(() -> "Alias name clashes with an existing attribute " + a)
                .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void checkAliasLength() {
        RmAttributeAlias.VALUES.forEach(aa -> Assertions.assertThat(aa.alias()).withFailMessage(()->"%s: Invalid alias length".formatted(aa)).hasSizeBetween(1, 4));
    }

    /**
     * Aliases for attributes within StructureRmType must not consist of more than 2 characters
     * (see DbToRmFormat::parse)
     */
    @Test
    void checkStructureAliasLength() {

        ArchieRMInfoLookup rmInfos = ArchieRMInfoLookup.getInstance();

        Arrays.stream(StructureRmType.values())
                .map(type -> rmInfos.getTypeInfo(type.type))
                .map(typeInfo -> {
                    Set<RMTypeInfo> ownTypeInfos = typeInfo.getAllParentClasses();
                    ownTypeInfos.add(typeInfo);
                    return ownTypeInfos;
                })
                .flatMap(ownTypeInfos -> rmInfos.getAllTypes().stream()
                        .filter(t -> Actor.class.getPackage() != t.getJavaClass().getPackage())
                        .filter(t -> t.getJavaClass() != EhrAccess.class)
                        .flatMap(t -> t.getAttributes().values().stream())
                        .filter(att -> !att.isComputed())
                        .filter(att -> ownTypeInfos.contains(rmInfos.getTypeInfo(att.getTypeInCollection())))
                        .map(RMAttributeInfo::getRmName)
                )
                .distinct()
                .map(RmAttributeAlias::getAlias)
                .forEach(a -> Assertions.assertThat(a).withFailMessage(()->"%s: Invalid alias length".formatted(a)).hasSizeBetween(1, 2));
    }

    @Test
    void rmToJsonPathParts() {
        assertThat(RmAttributeAlias.rmToJsonPathParts("archetype_details/template_id/value"))
                .isEqualTo(new String[] {"ad", "tm", "V"});
        assertThat(RmAttributeAlias.rmToJsonPathParts("subject/external_ref/id/value"))
                .isEqualTo(new String[] {"su", "er", "X", "V"});
    }

    @Test
    void aliasByAliasChar() {
        RmAttributeAlias.VALUES.stream().filter(a -> a.alias().length() == 1).forEach(a -> {
            //System.out.println("case '%s' -> \"%s\";".formatted(a.alias(), a.alias()));
            char aliasChar = a.alias().charAt(0);
            String resolvedAlias = RmAttributeAlias.aliasByAliasChar(aliasChar);
            assertThat(resolvedAlias).withFailMessage("alias mismatch for %s: %s vs. %s", a,
             a.alias(), resolvedAlias).isEqualTo(a.alias());
        });
    }
}
