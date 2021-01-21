package org.ehrbase.aql.sql.queryimpl;

import java.io.Serializable;

public class ItemInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String itemType;
    private final String itemCategory;

    public ItemInfo(String itemType, String itemCategory) {
        this.itemType = itemType;
        this.itemCategory = itemCategory;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemCategory() {
        return itemCategory;
    }
}
