package com.bkromhout.minerva.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class which helps with backing up and restoring data.
 */
public class BackupUtils {
    /**
     * Backs up the Realm database file by using {@code Realm.writeCopyTo(File)}.
     */
    public static void backupRealmFile() {
        // TODO.
    }

    /**
     * Gets a list of currently backed up Realm files.
     * @return List of Realm database files.
     */
    public static List<File> getRestorableRealmFiles() {
        // TODO.
        return new ArrayList<>();
    }

    /**
     * Restores one of the Realm files that is returned by {@link #getRestorableRealmFiles()} based on the given {@code
     * idx}.
     * @param idx Index of the file to restore in the list returned by {@link #getRestorableRealmFiles()}.
     */
    public static void restoreRealmFile(int idx) {
        // TODO.
    }
}
