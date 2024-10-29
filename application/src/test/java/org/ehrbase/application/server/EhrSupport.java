package org.ehrbase.application.server;

import java.util.UUID;

import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestEhrEndpoint;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

import com.nedap.archie.rm.ehr.EhrStatus;

public class EhrSupport {
	static String PARTY_REF_ID = "_REF_ID_";
	static String EHR_NS = "_EHR_NS_";
	
	static String EHR_TEMPLATE =
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
		String ehr = EHR_TEMPLATE
			.replace(PARTY_REF_ID, partyRefId.toString())
			.replace(EHR_NS, partyRefNs);
		
		EhrStatus ehrStatus = json.unmarshal(ehr, EhrStatus.class);
		return endpoint.createEhr(ehrStatus);
	}
}
