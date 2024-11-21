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

import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.net.URI;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClientConfig;
import org.ehrbase.openehr.sdk.client.openehrclient.defaultrestclient.DefaultRestClient;

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
        var restClient = new DefaultRestClient(cfg) {
            ObjectVersionId doHttpPost(URI uri, String body) {
                HttpResponse response = internalPost(
                        uri, null, body, ContentType.APPLICATION_JSON, ContentType.APPLICATION_JSON.getMimeType());
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
