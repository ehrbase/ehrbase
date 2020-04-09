/*
 * Copyright (c) 2020 Vitasystems GmbH, Hannover Medical School, and Luis Marco-Ruiz (Hannover Medical School).
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
package org.ehrbase.dao.access.interfaces;

import org.ehrbase.api.definitions.FhirTsProps;
import org.ehrbase.api.service.TerminologyServer;
import org.ehrbase.service.FhirTerminologyServerR4AdaptorImpl;

import com.nedap.archie.rm.datavalues.DvCodedText;

/*
 * Copyright (c) 2020 Vitasystems GmbH, Hannover Medical School, and Luis Marco-Ruiz (Hannover Medical School).
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
/***
 * @Created by Luis Marco-Ruiz on Mar 6, 2020
 *
 * @param <DvCodedText> concept type
 * @param <ID>          id type
 * @param <ID> generic type for parameters that are custom to each operation implementation.
 */
public interface I_OpenehrTerminologyServer <ID, U> extends TerminologyServer<DvCodedText, ID, U> {

	/**
	 * Create new instance of the external terminology server adaptor.
	 * 
	 * @param <DvCodedText>
	 * @param <ID>
	 * @param props         Configuration properties for the external terminology
	 *                      server adaptor.
	 * @return
	 * @throws Exception
	 */
	static  <ID, U> I_OpenehrTerminologyServer <ID, U> getInstance(final FhirTsProps props,
			final String adapterId) throws Exception {
		if (TerminologyServer.TerminologyAdapter.isAdapterSupported(adapterId)) {
			throw new Exception("Terminology adapter not supported exception: " + adapterId);
		}
		// Cast is correct because of the fixed parameterization of generics in
		// FhirTerminologyServerAdaptorImpl
		@SuppressWarnings("unchecked")
		I_OpenehrTerminologyServer <ID, U>  result =  (I_OpenehrTerminologyServer <ID, U> ) FhirTerminologyServerR4AdaptorImpl
				.getInstance(props);
		return  (I_OpenehrTerminologyServer<ID, U>) result;
	}
}
