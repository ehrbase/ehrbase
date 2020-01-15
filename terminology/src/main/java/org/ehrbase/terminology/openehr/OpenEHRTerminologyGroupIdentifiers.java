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
 * description: "Class TerminologyGroupIdentifiers"
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
 * List of identifiers for groups in the openEHR terminology.
 * 
 * @author Rong Chen
 */
public enum OpenEHRTerminologyGroupIdentifiers {

	AUDIT_CHANGE_TYPE("audit change type"),
	ATTESTATION_REASON("attestation reason"),
	COMPOSITION_CATEGORY("composition category"),
	EVENT_MATH_FUNCTION("event math function"),
	INSTRUCTION_STATES("instruction states"),
	INSTRUCTION_TRANSITIONS("instruction transitions"),
	NULL_FLAVOURS("null flavours"),
	PROPERTY("property"),
	PARTICIPATION_FUNCTION("participation function"),
	PARTICIPATION_MODE("participation mode"),
	SUBJECT_RELATIONSHIP("subject relationship"),
	SETTING("setting"),
	TERM_MAPPING_PURPOSE("term mapping purpose"),
	VERSION_LIFECYCLE_STATE("version lifecycle state");

	/**
	 * Private constructor
	 * 
	 * @param value
	 */
	private OpenEHRTerminologyGroupIdentifiers(String value) {
		this.value = value;
	}
	
	/**
	 * Validity function to test if an identifier is in the set 
	 * defined by this class.
	 * 
	 * @param value
	 * @return true if id valid
	 */
	public static boolean validTerminologyGroupId(String value) {
		for(OpenEHRTerminologyGroupIdentifiers id : values()) {
			if(id.value.equals(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets String representation of this identifier
	 * 
	 * @return the string value
	 */
	public String toString() {
		return value;
	}
	
	/**
	 * Gets the string value
	 * 
	 * @return value
	 */
	public String getValue() {
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
 *  The Original Code is OpenEHRTerminologyGroupIdentifiers.java
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