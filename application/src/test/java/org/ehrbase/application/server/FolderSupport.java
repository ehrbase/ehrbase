package org.ehrbase.application.server;

import java.net.URI;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;

import com.nedap.archie.rm.support.identification.ObjectVersionId;

public class FolderSupport {
	static String FOLDER_ID = "_FOLDER_ID_";
	static String FOLDER_NAME = "_FOLDER_NAME_";
	static String SUB_FOLDER = "_SUB_FOLDER_";
	static String FOLDER_ITEM = "_FOLDER_ITEM_";
	
	
	static String FOLDER_TEMPLATE =
	"""
	{
	  "_type": "FOLDER",
	  "uid": {
	    "_type": "HIER_OBJECT_ID",
	    "value": "_FOLDER_ID_"
	  },
	  "name": {
	    "_type": "DV_TEXT",
	    "value": "_FOLDER_NAME_"
	  },
	  "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1",
	  "folders": [
	    _SUB_FOLDER_
	  ],
	  "items": [
	    _FOLDER_ITEM_
	  ]
	}
	""";
	
	public static String render(UUID folderUUID, String folderName, String subFolder, String items) {
		return FOLDER_TEMPLATE
			.replace(FOLDER_ID, folderUUID.toString())
			.replace(FOLDER_NAME, folderName)
			.replace(SUB_FOLDER, subFolder)
			.replace(FOLDER_ITEM, items);
	}
	
    public static final String EHR_PATH = "rest/openehr/v1/ehr/";
    public static final String DIR_PATH = "/directory";
    private final OpenEhrClientConfig cfg;

    public FolderSupport(OpenEhrClientConfig cfg) {
    	this.cfg = cfg;
    }

    public UUID create(UUID ehrId, String folder) {
//    	http://localhost:8080/ehrbase/rest/openehr/v1/ehr/{{ehr_id}}/directory
    	
//        updatedVersion = defaultRestClient.httpPut(
//                defaultRestClient
//                        .getConfig()
//                        .getBaseUri()
//                        .resolve(EHR_PATH
//                                + ehrId.toString()
//                                + COMPOSITION_PATH
//                                + versionUid.getObjectId().getValue()),
//                composition,
//                versionUid);
    	
        var restClient = new DefaultRestClient(cfg) {
            ObjectVersionId doHttpPost(URI uri, String body) {
                HttpResponse response = internalPost(uri, null, body, ContentType.APPLICATION_JSON, ContentType.APPLICATION_JSON.getMimeType());
                Header eTag = response.getFirstHeader(HttpHeaders.ETAG);
                return new ObjectVersionId(StringUtils.unwrap(StringUtils.removeStart(eTag.getValue(), "W/"), '"'));
            }
        };    	
    	
        return UUID.fromString(restClient
                .doHttpPost(cfg.getBaseUri().resolve(EHR_PATH + ehrId.toString() + DIR_PATH), folder)
                .getObjectId()
                .getValue());
    }
}