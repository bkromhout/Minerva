package com.bkp.minerva.util;

import com.bkp.minerva.C;
import com.bkp.minerva.prefs.DefaultPrefs;
import com.bkp.minerva.rx.RxFileWalker;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.io.File;
import java.util.List;

/**
 * Handles importing files.
 */
public class Importer {
    /**
     * Help track the state of the importer.
     */
    private enum State {
        READY, PREP, RUNNING, CANCELLING
    }

    /**
     * Instance of Importer.
     */
    private static Importer INSTANCE;

    /**
     * The current state of the importer.
     */
    private State currState;
    /**
     * What directory the importer is currently importing from.
     */
    private File currDir;
    /**
     * How many files in the current run have been processed.
     */
    private long numDone;
    /**
     * Total number of files in the current run.
     */
    private long numTotal;

    /**
     * Get an instance of {@link Importer}.
     * @return Instance.
     */
    public static Importer get() {
        if (INSTANCE == null) INSTANCE = new Importer();
        return INSTANCE;
    }

    // Empty constructor, static init only.
    private Importer() {
        resetState();
    }

    /**
     * Starts a full import run. The importer will import files from the configured library directory.
     * <p>
     * Calling this when the importer is already running will do nothing.
     */
    public void doFullImport() {
        currState = State.PREP;

        // Get and check currently configured library directory.
        String libDirPath = DefaultPrefs.get().getLibDir(null);
        if ((currDir = tryResolveLibDir(libDirPath)) == null) {
            // We don't have a valid library directory.
            resetState();
            return;
        }

        // Get a list of files in the directory (and its subdirectories) which have certain extensions.
        List<File> files = Observable.create(new RxFileWalker(currDir, C.VALID_EXTS))
                                     .toList()
                                     .subscribeOn(Schedulers.io())
                                     .observeOn(AndroidSchedulers.mainThread())
                                     .toBlocking()
                                     .single();

        // TODO something with the list of files!
    }

    /**
     * Try to resolve the given path to a File object representing a valid, readable director.
     * @param libDirPath The path to the directory.
     * @return The File object for the directory, or null if we had issues.
     */
    private File tryResolveLibDir(String libDirPath) {
        if (libDirPath == null || libDirPath.isEmpty()) return null;
        File dir = new File(libDirPath);
        return (dir.exists() && dir.isDirectory() && dir.canRead()) ? dir : null;
    }

    /**
     * Cancels the currently running full import.
     * <p>
     * If the importer isn't running, this does nothing.
     */
    public void cancelFullImport() {
        if (currState == State.READY || currState == State.CANCELLING) return;

        // TODO!!
    }

    /**
     * Resets the importer's state so that it is ready for the next call.
     * <p>
     * BE CAREFUL!
     */
    private void resetState() {
        currState = State.CANCELLING;
        this.currDir = null;
        this.numDone = -1;
        this.numTotal = -1;
        this.currState = State.READY;
    }

    /**
     * Check whether the importer is currently importing from some folder.
     * @return True if the importer is currently preparing or running, false if the importer isn't running.
     */
    public boolean isRunning() {
        return currState != State.READY;
    }

    /**
     * The path to the directory that the importer is currently importing from.
     * @return Current import path, or null if the importer either isn't currently running, or is still preparing.
     */
    public String getCurrDir() {
        return currDir == null ? null : currDir.getAbsolutePath();
    }

    // TODO add some sort of observable which caches and emits the file names as they are processed.

    /**
     * The number of files processed so far in the current import run.
     * @return Number of files processed, or -1 if the importer either isn't currently running, or is still preparing.
     */
    public long getNumDone() {
        return numDone;
    }

    /**
     * The total number of files in the current import run.
     * @return Total number of files, or -1 if the importer either isn't currently running, or is still preparing.
     */
    public long getNumTotal() {
        return numTotal;
    }
}
