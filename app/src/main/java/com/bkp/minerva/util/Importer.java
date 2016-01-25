package com.bkp.minerva.util;

import android.util.Log;
import com.bkp.minerva.C;
import com.bkp.minerva.prefs.DefaultPrefs;
import com.bkp.minerva.rx.RxBookTransformer;
import com.bkp.minerva.rx.RxFileWalker;
import nl.siegmann.epublib.domain.Book;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.io.File;
import java.util.List;

/**
 * Handles importing files.
 */
public class Importer {
    /**
     * Various state the importer can be in.
     * <p>
     * Ready, Preparing, Running, Cancelling, Finishing.
     */
    private enum State {
        READY, PREP, RUNNING, CANCELLING, FINISHING
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
     * Subscription to an Rx flow which uses {@link RxFileWalker} to recursively walk down from a start directory in
     * order to find files with specific extensions.
     */
    private Subscription fileWalkerSubscription;
    /**
     * Subscription to an Rx flow which uses
     */
    private Subscription fileImporterSubscription;

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
     * Calling this when the importer isn't in a ready state will do nothing.
     */
    public void doFullImport() {
        // Don't do anything if we aren't in a ready state.
        if (currState != State.READY) return;

        // Kick off preparations; flow will continue from there.
        doFullImportPrep();
    }

    /**
     * Prepare for a full import.
     * <p>
     * Validate the current library directory, then try to recursively get all files with specific extensions within
     * that directory.
     * <p>
     * At the end of this method we pass off control to an Rx flow which uses {@link RxFileWalker} to get a list of
     * files; once that flow produces a list, it will call {@link #onGotFileList(List)}.
     */
    private void doFullImportPrep() {
        currState = State.PREP;

        // Get and check currently configured library directory.
        String libDirPath = DefaultPrefs.get().getLibDir(null);
        if ((currDir = tryResolveLibDir(libDirPath)) == null) {
            // We don't have a valid library directory.
            resetState();
            return;
        }

        // Get a list of files in the directory (and its subdirectories) which have certain extensions.
        // This will call through to onGotFileList() once it has the results.
        //List<File> files = Observable.create(new RxFileWalker(currDir, C.VALID_EXTS)) TODO remove?
        fileWalkerSubscription = Observable
                .create(new RxFileWalker(currDir, C.VALID_EXTS))
                .toList()
                .single()
                // TODO Not sure about the order of these 3 calls here...
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(this::onFileWalkerUnsubscribe)
                .subscribe(this::onGotFileList);
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
     * What to do when we unsubscribe from {@link #fileWalkerSubscription}.
     */
    private void onFileWalkerUnsubscribe() {
        // Just null the reference to it. TODO is this enough??
        fileWalkerSubscription = null;
    }

    /**
     * Called by {@link #fileWalkerSubscription} when it calls {@code onNext()}.
     * <p>
     * This is part of full import preparation.
     * @param files List of files with specific extensions found by {@link RxFileWalker}. These are the files we will
     *              try to import.
     */
    private void onGotFileList(List<File> files) {
        // Remove reference to subscription.
        fileWalkerSubscription.unsubscribe();

        // Check file list.
        if (files.isEmpty()) {
            // We don't have any files.
            resetState();
            return;
        }

        // Update state.
        numDone = 0;
        numTotal = files.size();

        // Start actual importing.
        doImportFiles(files);
    }

    /**
     * Take the list of files we want to import and actually import them.
     * <p>
     * This is the point where we transition from preparing to running. At the end of this method we pass off control to
     * an Rx flow. It will either call {@link #onAllFilesImported()} or {@link #onFileImporterError(Throwable)} when it
     * is finished.
     * @param files List of files to import.
     */
    private void doImportFiles(List<File> files) {
        // Change state to running.
        currState = State.RUNNING;

        // Do importer flow.
        fileImporterSubscription = Observable
                .from(files)
                .compose(new RxBookTransformer())
                .subscribeOn(Schedulers.io()) // Everything above runs on the io thread pool.
                .observeOn(AndroidSchedulers.mainThread()) // Switch to the main thread now!
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(this::onGotBookFromFile) // TODO might want to make this better for Realm somehow.
                .doOnUnsubscribe(this::onFileImporterUnsubscribe)
                .subscribe(this::onImportedBookFile, this::onFileImporterError, this::onAllFilesImported);
    }

    /**
     * What to do with a Book object we just got from a File.
     * @param book Newly-obtained Book object.
     */
    private void onGotBookFromFile(Book book) {
        // TODO!!
    }

    /**
     * What to do when we've finished importing and processing a book file.
     * <p>
     * This is called at the end of the import flow, it should be lightweight!!
     * @param book The Book we created from the file.
     */
    private void onImportedBookFile(Book book) {
        // TODO!!
    }

    /**
     * What to do if an error is thrown during import.
     * @param t Throwable.
     */
    private void onFileImporterError(Throwable t) {
        Log.e("Importer", "Error while importing and converting files.", t);
        cancelFullImport();
    }

    /**
     * What to do after we've finished importing all books.
     */
    private void onAllFilesImported() {
        // We've finished importing all books.
        // TODO
    }

    /**
     * What to do when we unsubscribe from {@link #fileImporterSubscription}.
     */
    private void onFileImporterUnsubscribe() {
        // Just null the reference to it. TODO is this enough??
        fileImporterSubscription = null;
    }

    /**
     * Cancels the currently running full import.
     * <p>
     * Note that, at this time, calling this method will not roll back any changes which have already been made to the
     * database.
     * <p>
     * If the importer isn't preparing or running, this does nothing.
     */
    public void cancelFullImport() {
        if (currState == State.READY || currState == State.CANCELLING || currState == State.FINISHING) return;

        resetState();

        // TODO!!

        // TODO make sure already applied changes are rolled back somehow??
    }

    /**
     * Resets the importer's state so that it is ready for the next call.
     * <p>
     * BE CAREFUL!
     */
    private void resetState() {
        currState = State.FINISHING;

        // Unsubscribe from the file walker subscription.
        if (fileWalkerSubscription != null) fileWalkerSubscription.unsubscribe();
        // Unsubscribe from the file walker subscription.
        if (fileImporterSubscription != null) fileImporterSubscription.unsubscribe();

        this.currDir = null;
        this.numDone = -1;
        this.numTotal = -1;

        this.currState = State.READY;
    }

    /**
     * Check whether the importer is currently importing from some folder.
     * @return True if the importer is currently not in the ready state, false otherwise.
     * @see State
     */
    public boolean isRunning() {
        return currState != State.READY;
    }

    /**
     * The path to the directory that the importer is currently importing from.
     * @return Current import path, or null if the importer either isn't currently running, or is still preparing.
     */
    public String getCurrDirPath() {
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
