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
 * description: "Class SimpleTerminologyInterface"
 * keywords:    "terminology"
 *
 * author:      "Rong Chen <rong.acode@gmail.com>"
 * copyright:   "Copyright (c) 2007 Rong Chen"
 * license:     "See notice at bottom of class"
 *
 * file:        "$URL$"
 * revision:    "$LastChangedRevision$"
 * last_change: "$LastChangedDate$"
 */
package org.ehrbase.terminology.openehr.implementation;

import java.util.*;
import org.ehrbase.terminology.openehr.CodeSetAccess;
import org.ehrbase.terminology.openehr.OpenEHRCodeSetIdentifiers;
import org.ehrbase.terminology.openehr.TerminologyAccess;
import org.ehrbase.terminology.openehr.TerminologyInterface;


/**
 * A simple implementation of terminology service that provides
 * complete openEHR terminology and necessary code sets for the 
 * kernel to work properly
 * 
 * TODO: load complete external codesets 
 * 
 * @author rong.chen
 */
public class SimpleTerminologyInterface implements TerminologyInterface {
	
	public TerminologyAccess terminology(String name) {
		return terminologies.get(name);
	}

	public CodeSetAccess codeSet(String name) {
		return codeSets.get(name);
	}

	public CodeSetAccess codeSetForId(OpenEHRCodeSetIdentifiers id) {
		String name = codeSetInternalIdToExternalName.get(id.toString());
		if(name == null) {
			return null;
		}
		return codeSets.get(name);
	}

	public boolean hasTerminology(String name) {
		return terminologies.containsKey(name);
	}

	public boolean hasCodeSet(String name) {
		return codeSetInternalIdToExternalName.containsKey(name);
	}

	public List<String> terminologyIdentifiers() {
		return new ArrayList<String>(terminologies.keySet());
	}

	public List<String> codeSetIdentifiers() {
		return new ArrayList<String>(codeSets.keySet());
	}

	public Map<String, String> openehrCodeSets() {
		return Collections.unmodifiableMap(codeSetInternalIdToExternalName);
	}
	
	/*
	 * Creates a simpleTerminologyService
	 */
	protected SimpleTerminologyInterface(String language) {
		
		terminologies = new HashMap<>();
		codeSets = new HashMap<>();
		codeSetInternalIdToExternalName = new HashMap<>();

		
		try {			
			TerminologySource terminologySource = TerminologySourceFactory.getOpenEHRTerminology(language);
			loadTerminologies(terminologySource, language);
			loadCodeSets(terminologySource);
			
			terminologySource = TerminologySourceFactory.getExternalTerminologies(language);
			loadTerminologies(terminologySource, language);
			loadCodeSets(terminologySource);			
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void loadTerminologies(TerminologySource source, String language) {
		
		SimpleTerminologyAccess terminology = (SimpleTerminologyAccess)
				terminologies.get(TerminologyInterface.OPENEHR);
		if(terminology == null) {
			terminology = new SimpleTerminologyAccess(TerminologyInterface.OPENEHR);
		}
		
		List<Group> groups = source.getConceptGroups();
		for(Group group : groups) {
			Set<String> codes = new HashSet<>();
			Map<String, String> names = new HashMap<>();
			names.put(language, group.name);
			for(Concept concept : group.concepts) {
				codes.add(concept.id);
				terminology.addRubric(language, concept.id, concept.rubric);
			}
			// English name as group id
			terminology.addGroup(group.name, codes, names);
		}		
		terminologies.put(TerminologyInterface.OPENEHR, terminology);
	}
	
	private void loadCodeSets(TerminologySource source) {		
		
		for(CodeSet codeset : source.getCodeSets()) {
			SimpleCodeSetAccess codeSetAccess = new SimpleCodeSetAccess(codeset.externalId, new HashSet<>(codeset.codes));
			codeSets.put(codeset.externalId, codeSetAccess);
			codeSetInternalIdToExternalName.put(codeset.openehrId, codeset.externalId);
		}
	}
	
	/* static final field */
	
	/* code sets indexed by external codeset name */
	private Map<String, CodeSetAccess> codeSets;
	
	/* terminology indexed by name */
	private Map<String, TerminologyAccess> terminologies;
	
	/* mapping between external name and openEHR codeset id */ 
	private Map<String, String> codeSetInternalIdToExternalName;
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
 *  The Original Code is SimpleTerminologyInterface.java
 *
 *  The Initial Developer of the Original Code is Rong Chen.
 *  Portions created by the Initial Developer are Copyright (C) 2007
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