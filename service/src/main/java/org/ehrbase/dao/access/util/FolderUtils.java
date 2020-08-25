package org.ehrbase.dao.access.util;

import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.interfaces.I_FolderAccess;
import org.ehrbase.serialisation.dbencoding.RawJson;
import org.jooq.JSONB;
import org.postgresql.util.PGobject;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FolderUtils {

    private FolderUtils() {}

    public static I_FolderAccess getPath(I_FolderAccess folderAccess, int currentIndex, String[] path) {

        // End of recursion since we have reached the end of the path
        if (currentIndex >= path.length) {
            return folderAccess;
        }

        String searchFolderName = path[currentIndex];

        // Throw not found on empty list of folders
        if (folderAccess.getSubfoldersList() == null || folderAccess.getSubfoldersList().isEmpty()) {
            throw new ObjectNotFoundException("DIRECTORY", "Folder at path '" + searchFolderName + "' could not be found.");
        }

        Optional<I_FolderAccess> foundFolder = folderAccess
                .getSubfoldersList()
                .values()
                .stream()
                .filter(subFolderAccess -> searchFolderName.equals(subFolderAccess.getFolderName()))
                .findFirst();

        // Throw not found if folder could not be found
        if (foundFolder.isEmpty()) {
            throw new ObjectNotFoundException("DIRECTORY", "Folder at path '" + searchFolderName + "' could not be found." );
        }

        // Call recursive search for next path element
        return getPath(foundFolder.get(), currentIndex + 1, path);
    }

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

    /**
     * Checks each sub folder level for conflicts. For this purpose for each sub folder level there will be a set
     * created that contains all names of the siblings as values. If at least one value could not be inserted it will
     * be identified as duplicate and will throw an IllegalArgumentException that results to a 400 error on the
     * controller layer.
     *
     * @param folder - Folder to check sub folders for
     */
    public static void checkSiblingNameConflicts(Folder folder) {

        if (folder.getFolders() != null && !folder.getFolders().isEmpty()) {
            Set<String> folderNames = new HashSet<>();

            folder.getFolders().forEach(subFolder -> {

                // A new entry in the set results to false if there is already a duplicate element existing
                if (!folderNames.add(subFolder.getNameAsString())) {
                    throw new IllegalArgumentException("Duplicate folder name " + subFolder.getNameAsString());
                } else {
                    // Check sub folder hierarchies as well for duplicates
                    checkSiblingNameConflicts(subFolder);
                }
            });
        }
    }

    // TODO: Remove after Archie fixes handling of ObjectVersionIds
    public static UUID extractUuidFromObjectVersionId(ObjectVersionId folderId) {

        String value = folderId.getValue();
        if (value == null) {
            return null;
        }
        int index = value.indexOf("::");
        if (index < 0) {
            return UUID.fromString(value);
        }
        return UUID.fromString(value.substring(0, index));
    }

    // TODO: Remove after Archie fixes handling of ObjectVersionIds
    public static Integer extractVersionNumberFromObjectVersionId(ObjectVersionId objectVersionId) {

        String value = objectVersionId.getValue();
        if (value == null) {
            return null;
        }
        int index = value.lastIndexOf("::");
        if (index < 0 || index == value.indexOf("::")) {
            // No or only one occurrence of :: found
            return null;
        } return Integer.parseInt(value.substring(index + 2));
    }

    public static ItemStructure parseFromJSONB(JSONB dbObject) {
        if (dbObject == null) {
            return null;
        }
        String value = dbObject.data();
        return new RawJson().unmarshal(value, ItemStructure.class);
    }

    public static ItemStructure parseFromPGobject(PGobject databaseObject) {
        if (databaseObject == null) {
            return null;
        }
        String value = databaseObject.getValue();
        return new RawJson().unmarshal(value, ItemStructure.class);
    }
}
