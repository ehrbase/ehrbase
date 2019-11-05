package org.ehrbase.dao.access.util;

import com.nedap.archie.rm.directory.Folder;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;

public class FolderUtils {

    public static void updateFolder(Folder update, I_FolderAccess target) {

        if (update.getNameAsString() != null) {
            target.setFolderName(update.getNameAsString());
        }
        if (update.getDetails() != null) {
            target.setDetails(update.getDetails());
        }
        if (update.getArchetypeNodeId() != null) {
            target.setFolderNArchetypeNodeId(update.getArchetypeNodeId());
        }
    }
}
