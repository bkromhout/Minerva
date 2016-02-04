package com.bkp.minerva.util;

import android.util.Log;
import com.bkp.minerva.C;
import com.bkp.minerva.Minerva;
import com.bkp.minerva.prefs.DefaultPrefs;
import com.bkp.minerva.realm.RBook;
import com.bkp.minerva.rx.RxFileWalker;
import com.bkp.minerva.rx.RxSuperBookFromFile;
import io.realm.Realm;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Handles full library imports.
 */
public class FullImporter {
    /**
     * Various state the importer can be in.
     * <p>
     * Ready, Preparing, Running, Cancelling, Finishing.
     */
    private enum State {
        READY, PREP, IMPORTING, SAVING, CANCELLING, FINISHING
    }

    /**
     * Instance of FullImporter.
     */
    private static FullImporter INSTANCE;

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
     * List of {@link RBook}s that have been created from book files.
     */
    private Queue<RBook> bookQueue;
    /**
     * Realm for accessing... Be sure to close when finished!!
     */
    private Realm realm;

    /**
     * Get an instance of {@link FullImporter}.
     * @return Instance.
     */
    public static FullImporter get() {
        if (INSTANCE == null) INSTANCE = new FullImporter();
        return INSTANCE;
    }

    // Empty constructor, this class gets static init only.
    private FullImporter() {
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
        fileWalkerSubscription = Observable
                .create(new RxFileWalker(currDir, C.VALID_EXTS))
                .toList()
                .single()
                // TODO Not sure about the order of these 3 calls here...
                .subscribeOn(Schedulers.io()) // Everything above runs on the io thread pool.
                .observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> fileWalkerSubscription = null)
                .subscribe(this::onGotFileList);
    }

    /**
     * Called by {@link #fileWalkerSubscription} when it calls {@code onNext()}.
     * <p>
     * This is part of full import preparation.
     * @param files List of files with specific extensions found by {@link RxFileWalker}. These are the files we will
     *              try to import.
     */
    private void onGotFileList(List<File> files) {
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }
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
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }
        // Change state to running.
        currState = State.IMPORTING;

        // Do importer flow.
        fileImporterSubscription = Observable
                .from(files)
                .compose(new RxSuperBookFromFile(currDir.getAbsolutePath())) // Create a SuperBook from the file.
                .subscribeOn(Schedulers.io()) // Everything above runs on the io thread pool.
                .map(RBook::new) // Create an RBook from the SuperBook.
                .subscribeOn(Schedulers.computation()) // Everything above runs on the computation thread pool.
                .observeOn(AndroidSchedulers.mainThread()) // Switch to the main thread now!
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> fileImporterSubscription = null)
                .subscribe(this::onImportedBookFile, this::onFileImporterError, this::onAllFilesImported);
    }

    /**
     * What to do when we've finished importing and processing a book file.
     * <p>
     * This is called at the end of the import flow, it should be lightweight!!
     * @param rBook The {@link RBook} we created using info from the file.
     */
    private void onImportedBookFile(RBook rBook) {
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }

        // Add RBook to queue.
        bookQueue.add(rBook);

        // TODO emit title/filename
    }

    /**
     * What to do if an error is thrown during import.
     * @param t Throwable.
     */
    private void onFileImporterError(Throwable t) {
        Log.e("FullImporter", "Error while importing and converting files.", t);
        cancelFullImport();
    }

    /**
     * What to do after we've finished importing all books.
     */
    private void onAllFilesImported() {
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }

        // TODO make sure that the cancel dialog on the dialog goes away at this point!

        // We've finished importing all books, now we'll persist them to Realm.
        currState = State.SAVING;
        Realm realm = Realm.getInstance(Minerva.getAppCtx());
        realm.executeTransaction(bgRealm -> bgRealm.copyToRealmOrUpdate(bookQueue),
                new Realm.Transaction.Callback() {
                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        fullImportFinished();
                    }

                    @Override
                    public void onError(Exception e) {
                        super.onError(e);
                        Log.e("FullImporter", "Realm Error", e);
                        resetState();
                    }
                });
    }

    /**
     * Called when the full import has finished naturally.
     */
    private void fullImportFinished() {
        resetState();
    }

    /**
     * Cancels the currently running full import.
     * <p>
     * Note that if the importer has already entered the saving state, or if the importer isn't preparing or running,
     * this does nothing.
     */
    public void cancelFullImport() {
        if (isIdleOrTryingToBe() || currState == State.SAVING) return;
        resetState();

        // TODO dismiss dialog?? Or no??
    }

    /**
     * Resets the importer's state so that it is ready for the next call.
     * <p>
     * BE CAREFUL!
     */
    private void resetState() {
        // Start resetting, unless we already are resetting.
        if (currState != State.FINISHING && currState != State.READY) currState = State.FINISHING;
        else return;

        // Unsubscribe from the file walker subscription.
        if (fileWalkerSubscription != null) fileWalkerSubscription.unsubscribe();
        // Unsubscribe from the file importer subscription.
        if (fileImporterSubscription != null) fileImporterSubscription.unsubscribe();

        // Reset vars.
        this.currDir = null;
        this.numDone = -1;
        this.numTotal = -1;
        this.bookQueue = new LinkedList<>();

        // Close Realm.
        if (realm != null) realm.close();
        realm = null;

        this.currState = State.READY;
    }

    /**
     * Checks the state to determine if the importer is currently idle or is trying to get to an idle state.
     * @return True if state is ready, cancelling, or finishing; otherwise false.
     */
    private boolean isIdleOrTryingToBe() {
        return currState == State.READY || currState == State.CANCELLING || currState == State.FINISHING;
    }

    /**
     * Check whether the importer is currently in the ready state (doing absolutely nothing).
     * @return True if the importer is currently in the ready state, false otherwise.
     * @see State
     */
    public boolean isReady() {
        return currState == State.READY;
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
