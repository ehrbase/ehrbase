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
 * description: "Class OpenEHRCodeSetIdentifiers"
 * keywords:    "support"
 *
 * author:      "Rong Chen <rong.acode@gmail.com>"
 * copyright:   "Copyright (c) 2006 ACODE HB, Sweden"
 * license:     "See notice at bottom of class"
 *
 * file:        "$URL$"
 * revision:    "$LastChangedRevision$"
 * last_change: "$LastChangedDate$"
 */

package org.ehrbase.terminology.openehr;

/**
 * List of identifiers for code sets in the openEHR terminology.
 * 
 * @author Rong Chen
 */
public enum OpenEHRCodeSetIdentifiers {
	
	CHARACTER_SETS ("character sets"),
	COMPRESSION_ALGORITHMS ("compression algorithms"),
	COUNTRIES ("countries"),
	INTEGRITY_CHECK_ALGORITHMS ("integrity check algorithms"),
	LANGUAGES ("languages"),
	MEDIA_TYPES ("media types"),
	NORMAL_STATUSES ("normal statuses");
	
	/**
	 * Validity function to test if an identifier is in the set 
	 * defined by this class
	 * 
	 * @param id
	 * @return true if valid
	 */
	public static boolean validCodeSetId(String id) {
		for(OpenEHRCodeSetIdentifiers codeSetId : values()) {
			if(codeSetId.value.equals(id)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Private constructor
	 * 
	 * @param value
	 */
	private OpenEHRCodeSetIdentifiers(String value) {
		this.value = value;
	}
	
	/**
	 * Gets String representation of this identifier
	 * 
	 * @return the string value
	 */
	public String toString() {
		return value;
	}
	
	/* String value of the identifier */
	private final String value;
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
 *  The Original Code is OpenEHRCodeSetIdentifiers.java
 *
 *  The Initial Developer of the Original Code is Rong Chen.
 *  Portions created by the Initial Developer are Copyright (C) 2003-2006
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