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
 * description: "Class TerminologyInterface"
 * keywords:    "support"
 *
 * author:      "Rong Chen <rong@acode.se>"
 * support:     "Acode HB <support@acode.se>"
 * copyright:   "Copyright (c) 2004 Acode HB, Sweden"
 * license:     "See notice at bottom of class"
 *
 * file:        "$URL: http://svn.openehr.org/ref_impl_java/TRUNK/libraries/src/java/org/openehr/rm/support/terminology/TerminologyService.java $"
 * revision:    "$LastChangedRevision: 2 $"
 * last_change: "$LastChangedDate: 2005-10-12 22:20:08 +0100 (Wed, 12 Oct 2005) $"
 */
package org.ehrbase.terminology.openehr;

import java.util.*;

/**
 * Defines an object providing proxy access to a terminology service
 *
 * @author Rong Chen 
 * @version 1.0
 */
public interface TerminologyInterface {

    /**
     * Returns a TerminologyAccess of given name
     * 
     * @param name not empty and known to this service
     * @return terminology
     * @throws IllegalArgumentException if name null, empty
     *  or unknown to this terminology service
     */
    TerminologyAccess terminology(String name);

    /**
     * Return an interface to the code_set identified by the 
     * *external* identifier name
     * 
     * @param name not empty and known to this service
     * @return codeSet
     * @throws IllegalArgumentException if name is null, empty
     *  or unknown to this terminology service
     */
    CodeSetAccess codeSet(String name);
    
    /**
     * Return an interface to the code_set identified internally 
     * in openEHR by id.
     * 
     * @param id not empty and known to this service
     * @return codeSet
     * @throws IllegalArgumentException if id is null, empty
     *  or unknown to this terminology service
     */
    CodeSetAccess codeSetForId(OpenEHRCodeSetIdentifiers id);

    /**
     * Returns true if terminology of given name known by this service
     *
     * @param name not empty
     * @return true if has given terminology
     * @throws IllegalArgumentException if name is null or empty
     */
    boolean hasTerminology(String name);

    /**
     * Returns true if code set of given name known by this service
     *
     * @param name not empty
     * @return true if has given codeset
     * @throws IllegalArgumentException if name is null or empty
     */
    boolean hasCodeSet(String name);
    
    /**
     * Gets all terminology identifiers known in the terminology service.
     * 
     * @return all terminology identifiers
     */
    List<String> terminologyIdentifiers();

    /**
     * Gets all code set identifiers known in the terminology service
     * 
     * @return all code identifiers
     */
    List<String> codeSetIdentifiers();
    
    /**
     * Gets all code sets identifiers for which there is an internal 
     * openEHR name
     * 
     * @return as a Hash of ids keyed by internal name
     */
    Map<String, String> openehrCodeSets();
    
    /* static fields */
    public static final String OPENEHR = "openehr";
    public static final String NULL_FLAVOURS = "null flavours";
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
 *  The Original Code is TerminologyInterface.java
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