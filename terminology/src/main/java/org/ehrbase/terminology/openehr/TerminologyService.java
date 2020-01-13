/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
package org.ehrbase.terminology.openehr;

import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;
import org.ehrbase.terminology.openehr.implementation.LocalizedTerminologies;

import java.util.Map;


/**
 * @link https://specifications.openehr.org/releases/RM/latest/support.html#_terminology_package
 */
public interface TerminologyService {

    /**
     * Return an interface to the terminology named name.
     * @param name
     * @return
     */
    TerminologyAccess terminology(String name);

    /**
     * Return an interface to the terminology named name for a specific language.
     * @param name
     * @return
     */
    TerminologyAccess terminology(String name, String language);

    /**
     * Return an interface to the code_set identified by the external identifier name (e.g. ISO_639-1).
     * @param name
     * @return
     */
    CodeSetAccess codeSet(String name);
    CodeSetAccess codeSet(String name, String language);

    /**
     * Return an interface to the code_set identified internally in openEHR by id.
     * @param name
     * @return
     */
    CodeSetAccess codeSetForId(String name);
    CodeSetAccess codeSetForId(String name, String language);

    /**
     * True if terminology named name known by this service.
     * @param name
     * @return
     */
    Boolean hasTerminology(String name);
    Boolean hasTerminology(String name, String language);

    /**
     * True if code_set linked to internal name (e.g. languages ) is available.
     * @param name
     * @return
     */
    Boolean hasCodeSet(String name);
    Boolean hasCodeSet(String name, String language);

    /**
     * Set of all terminology identifiers known in the terminology service.
     * @return
     */
    String[] terminologyIdentifiers();
    String[] terminologyIdentifiers(String language);

    /**
     * Set of all code set identifiers known in the terminology service.
     * @return
     */
    Map<String, String> openehrCodeSets();
    Map<String, String> openehrCodeSets(String language);

    /**
     * Set of all code sets identifiers for which there is an internal openEHR name.
     * @return Map of ids keyed by internal name.
     */
    String[] codeSetIdentifiers();
    String[] codeSetIdentifiers(String language);

    /**
     * retrieve the rubric literal for an openehr code and language
     * @param code
     * @param language
     * @return
     */
    String getLabelForCode(String code, String language);

    /**
     * return the mapping between RM attribute and entries in openehr terminology
     * @return
     */
    AttributeCodesetMapping codesetMapping();

    /**
     * returns the set of openehr localized terminologies as defined in their respective XML sources
     * @return
     */
    LocalizedTerminologies localizedTerminologies();
}
