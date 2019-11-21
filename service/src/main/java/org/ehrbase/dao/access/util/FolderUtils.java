package org.ehrbase.dao.access.util;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.directory.Folder;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.serialisation.RawJson;
import org.postgresql.util.PGobject;

public class FolderUtils {

    private FolderUtils(){}

    /**
     * Checks all fields that can be updated from update Folder object and sets
     * the contents to the target folderAccess.
     *
     * @param update - Folder update from request input
     * @param target - folderAccess to set new values tos
     */
    public static void updateFolder(Folder update, I_FolderAccess target) {

        if (update.getNameAsString() != null) {
            target.setFolderName(update.getNameAsString());
        }
        if (update.getDetails() != null) {
            target.setFolderDetails(update.getDetails());
        }
        if (update.getArchetypeNodeId() != null) {
            target.setFolderNArchetypeNodeId(update.getArchetypeNodeId());
        }
        if (update.getItems() != null) {
            target.getItems().clear();
            target.getItems().addAll(update.getItems());
        }
    }

    public static ItemStructure parseFromPGobject(PGobject databaseObject) {
        if (databaseObject == null) {
            return null;
        }
        String value = databaseObject.getValue();
        return new RawJson().unmarshal(value, ItemStructure.class);
    }
}
