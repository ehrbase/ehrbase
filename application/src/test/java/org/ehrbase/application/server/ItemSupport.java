package org.ehrbase.application.server;

import org.hibernate.validator.constraints.UUID;

public class ItemSupport {
	static String ITEM_ID = "_ITEM_ID_";
	static String ITEM_NS = "_ITEM_NS_";
	
	static String ITEM_TEMPLATE =
	"""
    {
      "id": {
        "_type": "HIER_OBJECT_ID",
        "value": "_ITEM_ID_"
      },
      "namespace": "ITEM_NS",
      "type": "VERSIONED_COMPOSITION"
    }
	""";
	
	public static String create(UUID itemUUID, String namespace) {
		return ITEM_TEMPLATE
			.replace(ITEM_ID, itemUUID.toString())
			.replace(ITEM_NS, namespace);
	}
	
	public static String create(String itemUUID, String namespace) {
		return ITEM_TEMPLATE
			.replace(ITEM_ID, itemUUID)
			.replace(ITEM_NS, namespace);
	}
}
