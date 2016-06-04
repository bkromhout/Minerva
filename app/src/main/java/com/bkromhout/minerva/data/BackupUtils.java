package com.bkromhout.minerva.data;

import android.os.Environment;
import android.support.design.widget.Snackbar;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.ui.SnackKiosk;
import io.realm.Realm;
import org.apache.commons.io.FileUtils;
import timber.log.Timber;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class which helps with backing up and restoring data.
 */
public class BackupUtils {
    private static final String BACKUP_PATH = "/Minerva/";
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
        File backupFile = new File(getExtDir(BACKUP_PATH), SDF.format(new Date()) + DB_BACKUP_EXT);
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
    public static synchronized File[] getRestorableRealmFiles() {
        if (isBackingUp || isRestoring) return null;

        // Get the DB backups directory. If it doesn't exist, return the empty list.
        File dbBackupDir = getExtDir(BACKUP_PATH);
        if (!dbBackupDir.exists()) return new File[] {};

        // Return all files whose extensions match our DB backup extension.
        return dbBackupDir.listFiles((dir, filename) -> {
            return filename.endsWith(DB_BACKUP_EXT);
        });
    }

    /**
     * Copies one of the Realm files that is returned by {@link #getRestorableRealmFiles()} based on the given {@code
     * idx} to the app's data/files/ directory so that it will be restored the next time the app starts. Restarts the
     * app when finished.
     * @param dbToRestore File which represents the database file to restore. It is assumed that this is one of the
     *                    files which would be returned by {@link #getRestorableRealmFiles()}.
     */
    public static synchronized void prepareToRestoreRealmFile(File dbToRestore) {
        if (isBackingUp || isRestoring) return;
        if (dbToRestore == null || !dbToRestore.isFile() || !dbToRestore.getName().endsWith(DB_BACKUP_EXT))
            throw new IllegalArgumentException("dbToRestore must be non-null and represent a valid existing file.");
        isRestoring = true;

        // Copy the specified file to our app's data/files/ directory with the name "restore.realm".
        try {
            FileUtils.copyFile(dbToRestore, new File(Minerva.get().getFilesDir(), RESTORE_NAME));
        } catch (IOException e) {
            Timber.e(e, "Failed to copy \"%s\" to data/files/ for restoration.", dbToRestore.getAbsolutePath());
            return;
        }

        Minerva.rennervate(); // ;)
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
