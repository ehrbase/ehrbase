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
package org.ehrbase.openehr.aqlengine.testdata;

import com.nedap.archie.rm.ehr.EhrStatus;
import java.util.UUID;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

public class EhrSupport {
    private static final String PARTY_REF_ID = "_REF_ID_";
    private static final String EHR_NS = "_EHR_NS_";

	private static final String EHR_TEMPLATE =
            """
	{
	  "_type": "EHR_STATUS",
	  "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
	  "name": {
	    "value": "EHR Status"
	  },
	  "subject": {
	    "external_ref": {
	      "_type": "PARTY_REF",
	      "id": {
	        "_type": "GENERIC_ID",
	        "value": "_REF_ID_",
	        "scheme": "id_scheme"
	      },
	      "namespace": "_EHR_NS_",
	      "type": "PERSON"
	    }
	  },
	  "is_modifiable": true,
	  "is_queryable": true
	}
	""";

    private final CanonicalJson json = new CanonicalJson();
    private final DefaultRestEhrEndpoint endpoint;

    public EhrSupport(OpenEhrClientConfig cfg) {
        endpoint = new DefaultRestEhrEndpoint(new DefaultRestClient(cfg));
    }

    public UUID create(UUID partyRefId, String partyRefNs) {
        String ehr = EHR_TEMPLATE.replace(PARTY_REF_ID, partyRefId.toString()).replace(EHR_NS, partyRefNs);

        EhrStatus ehrStatus = json.unmarshal(ehr, EhrStatus.class);
        return endpoint.createEhr(ehrStatus);
    }
}
