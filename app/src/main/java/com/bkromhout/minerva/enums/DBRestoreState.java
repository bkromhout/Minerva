package com.bkromhout.minerva.enums;

/**
 * Used by {@link com.bkromhout.minerva.data.BackupUtils}.
 * <p>
 * The states are moved through from top to bottom, though a couple might be skipped depending on whether or not we run
 * into problems.
 */
public enum DBRestoreState {
    /**
     * Not restoring currently.
     */
    NOT,
    /**
     * Preparing to restore. This involves copying a backed up file to internal storage and restarting the app.
     */
    PREPARING,
    /**
     * Starting the actual restore process. This is the state entered when the app first starts and tries to begin
     * restoring the DB file copied during {@link #PREPARING}.
     * <p>
     * The next state might be {@link #TEMP_CREATED} if there is currently a DB file present which we need to make a
     * temporary copy of, or {@link #RENAMED} if we just need to rename the (aforementioned) previously-copied one.
     */
    STARTING,
    /**
     * State entered after the current DB file has been copied to a temporary file, but before we try to delete the
     * current DB file and other supporting files.
     */
    TEMP_CREATED,
    /**
     * State entered after we've deleted the current Realm files, but before we've renamed the "restore.realm" file.
     */
    CURR_DELETED,
    /**
     * State entered after we've renamed the "restore.realm" file.
     */
    RENAMED,
    /**
     * State which may be entered if we run into problems at some point after entering {@link #STARTING}.
     * <p>
     * Just before this state is entered, we make a note of the current state so that we know what to do to roll back.
     */
    ROLLBACK,
    /**
     * State entered when we know that our restore was successful. In this stage we delete the temporary copy of the DB
     * which we made at the start of the process, and then perform a number of validations to ensure that our app's
     * other data is in sync with the state of the Realm.
     */
    COMPLETING
}
