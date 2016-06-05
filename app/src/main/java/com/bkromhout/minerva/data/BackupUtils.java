package com.bkromhout.minerva.data;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.enums.DBRestoreState;
import com.bkromhout.minerva.ui.SnackKiosk;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import timber.log.Timber;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class which helps with backing up and restoring data.
 */
public class BackupUtils {
    private static final String BACKUP_PATH = "/Minerva/";
    public static final String DB_BACKUP_EXT = ".minervaDB";
    private static final String SETTINGS_BACKUP_EXT = ".minervaSettings";
    private static final String RESTORE_NAME = "restore.realm";
    private static final String TEMP_NAME = "temp.realm";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss", Locale.US);

    private static volatile boolean isBackingUp = false;
    private static volatile DBRestoreState dbRestoreState = DBRestoreState.NOT;

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
     * Gets a list of currently backed up Realm files, ordered newest to oldest.
     * @return List of Realm database files.
     */
    @NonNull
    public static synchronized File[] getRestorableRealmFiles() {
        if (isBackingUp || dbRestoreState != DBRestoreState.NOT) return FileUtils.EMPTY_FILE_ARRAY;

        // Get the DB backups directory. If it doesn't exist or isn't a directory, return the empty list.
        File dbBackupDir = getExtDir(BACKUP_PATH);
        if (!dbBackupDir.isDirectory()) return FileUtils.EMPTY_FILE_ARRAY;

        // Get all files whose extensions match our DB backup extension. Sort so that the order is newest to oldest.
        File[] restorableDbFiles = dbBackupDir.listFiles((dir, filename) -> {
            return filename.endsWith(DB_BACKUP_EXT);
        });
        Arrays.sort(restorableDbFiles, NameFileComparator.NAME_INSENSITIVE_REVERSE);

        return restorableDbFiles;
    }

    /**
     * Copies one of the Realm files that is returned by {@link #getRestorableRealmFiles()} based on the given {@code
     * idx} to the app's data/files/ directory so that it will be restored the next time the app starts. Restarts the
     * app when finished.
     * @param activity    Activity to finish.
     * @param dbToRestore File which represents the database file to restore. It is assumed that this is one of the
     *                    files which would be returned by {@link #getRestorableRealmFiles()}.
     */
    public static synchronized void prepareToRestoreRealmFile(Activity activity, File dbToRestore) {
        if (isBackingUp || dbRestoreState != DBRestoreState.NOT) return;
        if (dbToRestore == null || !dbToRestore.isFile() || !dbToRestore.getName().endsWith(DB_BACKUP_EXT))
            throw new IllegalArgumentException("dbToRestore must be non-null and represent a valid existing file.");
        dbRestoreState = DBRestoreState.PREPARING;

        // Copy the specified file to our app's data/files/ directory with the name "restore.realm".
        try {
            FileUtils.copyFile(dbToRestore, new File(activity.getFilesDir(), RESTORE_NAME));
        } catch (IOException e) {
            Timber.e(e, "Failed to copy \"%s\" to data/files/ for restoration.", dbToRestore.getAbsolutePath());
            SnackKiosk.snack(R.string.sb_db_restore_fail, Snackbar.LENGTH_SHORT);
            return;
        }

        Minerva.rennervate(activity); // ;)
    }

    /**
     * Checks the app's data/files/ directory to see if there's a Realm file waiting to be restored, and restores it if
     * there is.
     * <p>
     * This method assumes that Realm is completely closed, and that one of either {@link #rollBackFromDBRestore()} or
     * {@link #removeTempRealmFile()} will be called soon after this completes in order to complete the restoring
     * process.
     */
    public static synchronized void restoreRealmFileIfApplicable() {
        if (dbRestoreState != DBRestoreState.NOT) return;
        Minerva minerva = Minerva.get();
        File filesDir = minerva.getFilesDir();

        // Check to see if there's a "restore.realm" file. If there isn't, then we're done here.
        File restoreDb = new File(filesDir, RESTORE_NAME);
        if (!restoreDb.isFile()) return;
        dbRestoreState = DBRestoreState.STARTING;

        // Handle current Realm files.
        RealmConfiguration config = new RealmConfiguration.Builder(minerva).name(Minerva.REALM_FILE_NAME).build();
        File currDb = new File(config.getPath());
        if (currDb.exists()) {
            try {
                // Make a temporary copy of the current Realm database file.
                FileUtils.copyFile(currDb, minerva.openFileOutput(TEMP_NAME, Context.MODE_PRIVATE));
                dbRestoreState = DBRestoreState.TEMP_CREATED;
            } catch (IOException e) {
                Timber.e(e, "Problem while making temporary copy of current Realm DB file.");
                rollBackFromDBRestore();
                return;
            }

            // Have Realm delete its stuff.
            if (!Realm.deleteRealm(config)) {
                Timber.e("Problem while having Realm delete its current files.");
                rollBackFromDBRestore();
                return;
            } else dbRestoreState = DBRestoreState.CURR_DELETED;
        }

        // Rename the "restore.realm" file to "minerva.realm".
        if (!restoreDb.renameTo(new File(filesDir, Minerva.REALM_FILE_NAME))) {
            Timber.e("Problem while renaming restore.realm to minerva.realm.");
            rollBackFromDBRestore();
        } else dbRestoreState = DBRestoreState.RENAMED;
    }

    /**
     * Rolls back the efforts made to restore a previous database.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static synchronized void rollBackFromDBRestore() {
        if (isBackingUp || dbRestoreState == DBRestoreState.NOT) return;
        DBRestoreState currState = dbRestoreState;
        dbRestoreState = DBRestoreState.ROLLBACK;

        Minerva minerva = Minerva.get();
        File filesDir = minerva.getFilesDir();
        RealmConfiguration config = new RealmConfiguration.Builder(minerva).name(Minerva.REALM_FILE_NAME).build();
        File currDb = new File(config.getPath());
        File restoreDb = new File(filesDir, RESTORE_NAME);
        File tempDb = new File(filesDir, TEMP_NAME);

        // Do rollback actions.
        switch (currState) {
            case RENAMED:
                // We physically restored the DB file successfully, but Realm wasn't happy with it. Delete it, and
                // any files Realm may have made, then proceed onto the next cases to put the temporary copy back.
                Realm.deleteRealm(config);
            case CURR_DELETED:
                // We just had issues renaming "restore.realm" to "minerva.realm", but were able to delete the Realm
                // files. Proceed onto the next case.
            case TEMP_CREATED:
                // We created the temporary file successfully, so check to see if we actually deleted the DB file it
                // was copied from.
                if (!currDb.exists()) {
                    // If we deleted it, copy it back as "minerva.realm". We would try and rename it, but it's
                    // possible we had issues with renaming, so we copy it instead.
                    try {
                        FileUtils.copyFile(tempDb, currDb);
                    } catch (IOException e) {
                        // We'd better hope this doesn't happen, or the user is not going to be happy.
                        e.printStackTrace();
                    }
                }
            case STARTING:
                // Remove the temporary DB copy, if it exists.
                if (tempDb.exists()) tempDb.delete();
                // Remove the "restore.realm" file, if it exists.
                if (restoreDb.exists()) restoreDb.delete();
        }

        // Notify user of failure to restore database.
        SnackKiosk.snack(R.string.sb_db_restore_fail, Snackbar.LENGTH_SHORT);
        dbRestoreState = DBRestoreState.NOT;
    }

    /**
     * Removes the temporary copy of the previously-present database created during the restore process.
     */
    public static synchronized void removeTempRealmFile() {
        if (isBackingUp || dbRestoreState == DBRestoreState.NOT) return;
        dbRestoreState = DBRestoreState.COMPLETING;

        // Delete the temporary copy of the DB we created earlier.
        File tempDb = new File(Minerva.get().getFilesDir(), TEMP_NAME);
        if (tempDb.exists()) //noinspection ResultOfMethodCallIgnored
            tempDb.delete();

        // Notify user of successful database restoration.
        SnackKiosk.snack(R.string.sb_db_restore_success, Snackbar.LENGTH_SHORT);
        dbRestoreState = DBRestoreState.NOT;
    }
}
