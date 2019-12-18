/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 *
 * This file is part of Project EHRbase
 *
 * Original Copyright: see below
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
/*
 * component:   "openEHR Reference Implementation"
 * description: "Class TerminologyAccess"
 * keywords:    "support"
 *
 * author:      "Rong Chen <rong@acode.se>"
 * support:     "Acode HB <support@acode.se>"
 * copyright:   "Copyright (c) 2004 Acode HB, Sweden"
 * license:     "See notice at bottom of class"
 *
 * file:        "$URL: http://svn.openehr.org/ref_impl_java/BRANCHES/Release-1.0/libraries/src/java/org/openehr/rm/support/terminology/TerminologyAccess.java $"
 * revision:    "$LastChangedRevision: 2 $"
 * last_change: "$LastChangedDate: 2005-10-12 22:20:08 +0100 (Wed, 12 Oct 2005) $"
 */
package org.ehrbase.terminology.openehr;


import com.nedap.archie.rm.datatypes.CodePhrase;

import java.util.Set;

/**
 * Defines an interface to access a terminology
 *
 * @author Rong Chen
 * @version 1.0
 */
public interface TerminologyAccess {

    /**
     * Returns identification of this terminology
     *
     * @return ID not null or empty
     */
    String id();

    /**
     * Returns all codes known in this terminology
     *
     * @return Set of DvCodePhrase
     */
    Set<CodePhrase> allCodes();

    /**
     * Returns all codes under grouper groupID of this terminology
     *
     * @param groupID
     * @return Set of CodePhrase for given group ID, empty set
     *         returned if not found
     * @throws IllegalArgumentException if groupID null or empty
     */
    Set<CodePhrase> codesForGroupId(String groupID);
    
    /**
     * Returns true if the given code is known in the specified group
     * 
     * @param groupId
     * @param code
     * @return true if code exists
     */
    boolean hasCodeForGroupId(String groupId, CodePhrase code);

    /**
     * Return all codes under grouper whose name of given
     * name and language from this terminology.
     *
     * @param name
     * @param language
     * @return Set of CodePhrase for given group name,
     *         empty set returned if not found
     * @throws IllegalArgumentException if name,language null or empty
     */
    Set<CodePhrase> codesForGroupName(String name, String language);

    /**
     * Returns all rubric of given code and language
     *
     * @param code
     * @param language
     * @return rubric of given code and language or null if not found
     * @throws IllegalArgumentException if code,language null or empty
     */
    String rubricForCode(String code, String language);

}
/*
 *  ***** BEGIN LICENSE BLOCK *****
 *  Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  1.1 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  The Original Code is TerminologyAccess.java
 *
 *  The Initial Developer of the Original Code is Rong Chen.
 *  Portions created by the Initial Developer are Copyright (C) 2003-2004
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 *  ***** END LICENSE BLOCK *****
 */