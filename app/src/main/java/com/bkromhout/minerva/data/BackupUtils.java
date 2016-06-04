package com.bkromhout.minerva.data;

import android.os.Environment;
import android.support.design.widget.Snackbar;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.ui.SnackKiosk;
import io.realm.Realm;
import timber.log.Timber;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class which helps with backing up and restoring data.
 */
public class BackupUtils {
    private static final String DB_BACKUP_PATH = "/Minerva/DB_Backups/";
    private static final String SETTINGS_BACKUP_PATH = "/Minerva/Settings_Backups/";
    private static final String DB_BACKUP_EXT = ".minervaDB";
    private static final String SETTINGS_BACKUP_EXT = ".minervaSettings";
    private static final String RESTORE_NAME = "restore.realm";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);

    private static volatile boolean isBackingUp = false;
    private static volatile boolean isRestoring = false;

    /**
     * Get a File object representing a directory at {@code relPath}, where the base path is {@code
     * Environment.getExternalStorageDirectory()}.
     * @param relPath The path of the directory to get, relative to {@code Environment.getExternalStorageDirectory()}.
     * @return File object which represents the requested directory.
     */
    private static File getExtDir(String relPath) {
        File extDir = new File(Environment.getExternalStorageDirectory(), relPath);
        if (!(extDir.mkdirs() || extDir.isDirectory()))
            throw new IllegalStateException("Couldn't make \"" + extDir.getAbsolutePath() + "\"");
        return extDir;
    }

    /**
     * Backs up the Realm database file by using {@code Realm.writeCopyTo(File)}.
     */
    public static synchronized void backupRealmFile() {
        if (isBackingUp) return;
        isBackingUp = true;
        // Get the file to write the backup to.
        File backupFile = new File(getExtDir(DB_BACKUP_PATH), SDF.format(new Date()) + DB_BACKUP_EXT);
        // Back up the database, notifying the user is we succeed or fail.
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.writeCopyTo(backupFile);
            SnackKiosk.snack(R.string.sb_db_backup_success, Snackbar.LENGTH_SHORT);
        } catch (IOException e) {
            Timber.e(e, "Failed to back up Minerva's database to \"%s\".", backupFile.getAbsolutePath());
            SnackKiosk.snack(R.string.sb_db_backup_fail, Snackbar.LENGTH_SHORT);
        }
        isBackingUp = false;
    }

    /**
     * Gets a list of currently backed up Realm files.
     * @return List of Realm database files.
     */
    public static synchronized List<File> getRestorableRealmFiles() {
        if (isBackingUp || isRestoring) return null;
        // TODO.
        return new ArrayList<>();
    }

    /**
     * Copies one of the Realm files that is returned by {@link #getRestorableRealmFiles()} based on the given {@code
     * idx} to the app's data/files/ directory so that it will be restored the next time the app starts. Restarts the
     * app when finished.
     * @param idx Index of the file to restore in the list returned by {@link #getRestorableRealmFiles()}.
     */
    public static synchronized void prepareToRestoreRealmFile(int idx) {
        if (isBackingUp || isRestoring) return;
        isRestoring = true;
        // TODO.

    }

    /**
     * Checks the app's data/files/ directory to see if there's a Realm file waiting to be restored, and restores it if
     * there is.
     * <p>
     * This method assumes that Realm is completely closed.
     */
    public static synchronized void restoreRealmFileIfApplicable() {
        isRestoring = true;
        // TODO

        isRestoring = false;
    }
}
