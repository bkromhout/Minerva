package com.bkromhout.minerva.data;

import android.util.Log;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.rx.RxFileWalker;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import java.io.File;
import java.util.Calendar;
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
    private int numDone;
    /**
     * Total number of files in the current run.
     */
    private int numTotal;
    /**
     * ReplaySubject wrapped in a SerializedSubject which holds log lines.
     */
    private Subject<String, String> logSubject;
    /**
     * ReplaySubject wrapped in a SerializedSubject which holds log lines which describe errors (these lines are also
     * printed to the regular log stream)
     */
    private Subject<String, String> errorSubject;
    /**
     * BehaviorSubject wrapped in a SerializedSubject which holds the latest progress.
     */
    private Subject<Integer, Integer> progressSubject;
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

    // Variables below concern the listener.
    /**
     * Who is listening to our progress?
     */
    private IFullImportListener listener;
    /**
     * Listener's subscription to the log stream.
     */
    private Subscription listenerLogSub;
    /**
     * Listener's subscription to the error stream.
     */
    private Subscription listenerErrorSub;
    /**
     * Listener's subscription to the progress stream.
     */
    private Subscription listenerProgressSub;

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
        // Create subjects here, they should always exist.
        logSubject = new SerializedSubject<>(ReplaySubject.create());
        errorSubject = new SerializedSubject<>(ReplaySubject.create());
        progressSubject = new SerializedSubject<>(BehaviorSubject.create());
    }

    /**
     * Starts a full import run. The importer will import files from the configured library directory.
     * <p>
     * Calling this when the importer isn't in a ready state will do nothing.
     * @param listener The {@link IFullImportListener} to register for this run, or null if no listener should be
     *                 registered. Note that if there is already a listener registered, this will be ignored.
     */
    public void doFullImport(IFullImportListener listener) {
        // Don't do anything if we aren't in a ready state.
        if (currState != State.READY) return;

        // Reset subjects and possible register a listener.
        resetSubjects();
        if (listener != null) registerListener(listener);

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

        // Listener updates.
        logSubject.onNext(null);
        errorSubject.onNext(null);
        if (listener != null) listener.setRunning();
        logSubject.onNext(C.getStr(R.string.fil_starting));
        progressSubject.onNext(-1);

        // Get and check currently configured library directory.
        String libDirPath = DefaultPrefs.get().getLibDir(null);
        if ((currDir = Util.tryResolveDir(libDirPath)) == null) {
            // We don't have a valid library directory.
            logSubject.onNext(C.getStr(R.string.fil_err_invalid_lib_dir));
            errorSubject.onNext(C.getStr(R.string.fil_err_invalid_lib_dir));
            resetState();
            return;
        }

        logSubject.onNext(C.getStr(R.string.fil_finding_files));
        // Get a list of files in the directory (and its subdirectories) which have certain extensions.
        // This will call through to onGotFileList() once it has the results.
        fileWalkerSubscription = Observable
                .create(new RxFileWalker(currDir, C.VALID_EXTS))
                .subscribeOn(Schedulers.io()) // Everything above runs on the io thread pool.
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> fileWalkerSubscription = null)
                .toList()
                .single()
                .observeOn(AndroidSchedulers.mainThread())
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
        logSubject.onNext(C.getStr(R.string.fil_done));
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
            logSubject.onNext(C.getStr(R.string.fil_err_no_files));
            resetState();
            return;
        }

        // Update state.
        numDone = 0;
        numTotal = files.size();

        // Update listener.
        logSubject.onNext(C.getStr(R.string.fil_found_files, numTotal));
        if (listener != null) listener.setMaxProgress(numTotal);
        progressSubject.onNext(numDone);

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
        logSubject.onNext(C.getStr(R.string.fil_reading_files));

        // Do importer flow.
        fileImporterSubscription = Observable
                .from(files)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> fileImporterSubscription = null)
                .map(this::convertFileToSuperBook) // Create a SuperBook from the file.
                .filter(sb -> sb != null)
                .map(RBook::new) // Create an RBook from the SuperBook.
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onImportedBookFile, this::onFileImporterError, this::onAllFilesImported);
    }

    /**
     * Convert the given file to a {@link SuperBook}.
     * @param file File.
     * @return New SuperBook, or null if there were issues.
     */
    private SuperBook convertFileToSuperBook(File file) {
        String relPath = file.getAbsolutePath().replace(currDir.getAbsolutePath(), "");
        try {
            return new SuperBook(Util.readEpubFile(file), relPath);
        } catch (IllegalArgumentException e) {
            logSubject.onNext(C.getStr(R.string.fil_err_processing_file, e.getMessage()));
            errorSubject.onNext(C.getStr(R.string.fil_err_processing_file, e.getMessage()));
            return null;
        }
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

        // Add RBook to queue and emit file path.
        bookQueue.add(rBook);
        logSubject.onNext(C.getStr(R.string.fil_read_file, rBook.getRelPath()));
        progressSubject.onNext(numDone++);
    }

    /**
     * What to do if an error is thrown during import.
     * @param t Throwable.
     */
    private void onFileImporterError(Throwable t) {
        String s = C.getStr(R.string.fil_err_generic);
        Log.e("FullImporter", s, t);
        logSubject.onNext("\n" + s + ":\n\"" + t.getMessage() + "\"\n");
        errorSubject.onNext("\n" + s + ":\n\"" + t.getMessage() + "\"\n");
        cancelFullImport();
    }

    /**
     * What to do after we've finished importing all books.
     */
    private void onAllFilesImported() {
        logSubject.onNext(C.getStr(R.string.fil_all_files_read));
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }

        // Cancelling isn't allowed from this point until we're done persisting data to Realm.
        currState = State.SAVING;
        if (listener != null) listener.setSaving();
        logSubject.onNext(C.getStr(R.string.fil_saving_files));
        progressSubject.onNext(-1);

        // We've finished importing all books, now we'll persist them to Realm.
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                bgRealm -> {
                    for (RBook book : bookQueue) {
                        // Try to find existing RBook before adding a new one.
                        RBook existingBook = bgRealm.where(RBook.class)
                                                    .equalTo("relPath", book.getRelPath())
                                                    .findFirst();

                        // If we have an existing RBook for this file, just update the fields which we read from the
                        // file. If we don't have one, create one.
                        if (existingBook != null) existingBook.updateFromOtherRBook(bgRealm, book);
                        else bgRealm.copyToRealmOrUpdate(book);
                    }
                },
                () -> {
                    logSubject.onNext(C.getStr(R.string.fil_done));
                    fullImportFinished();
                },
                error -> {
                    String s = C.getStr(R.string.fil_err_realm);
                    Log.e("FullImporter", s, error);
                    logSubject.onNext("\n" + s + "\n");
                    errorSubject.onNext("\n" + s + "\n");
                    unsafeCancelFullImport();
                });
    }

    /**
     * Called when the full import has finished successfully.
     */
    private void fullImportFinished() {
        // Save current time to prefs to indicate a full import completed, then tell listener we finished.
        DefaultPrefs.get().putLastFullImportTime(Calendar.getInstance().getTimeInMillis());
        logSubject.onNext(C.getStr(R.string.fil_finished));
        resetState();
        if (listener != null) listener.setReady();
    }

    /**
     * Cancels the currently running full import.
     * <p>
     * Note that if the importer has already entered the saving state, or if the importer isn't preparing or running,
     * this does nothing.
     */
    public void cancelFullImport() {
        if (isIdleOrTryingToBe() || currState == State.SAVING) return;
        unsafeCancelFullImport();
    }

    /**
     * Cancels the full import, unconditionally.
     * <p>
     * Internal methods should still prefer calling {@link #cancelFullImport()} to this method, unless the checks on
     * that method prohibit a cancel where it is required.
     */
    private void unsafeCancelFullImport() {
        if (listener != null) listener.setCancelling();
        logSubject.onNext(C.getStr(R.string.fil_cancelling));
        resetState();
        logSubject.onNext(C.getStr(R.string.fil_done));
        if (listener != null) listener.setCancelled();
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
        this.numDone = 0;
        this.numTotal = 0;
        this.bookQueue = new LinkedList<>();

        // Close Realm.
        if (realm != null) realm.close();
        realm = null;

        this.currState = State.READY;
    }

    /**
     * Recreate subjects and subscribe the listener to the new ones.
     */
    private void resetSubjects() {
        // Unsubscribe listener from old subjects.
        if (listenerLogSub != null) listenerLogSub.unsubscribe();
        if (listenerErrorSub != null) listenerErrorSub.unsubscribe();
        if (listenerProgressSub != null) listenerProgressSub.unsubscribe();

        // Complete subjects, if they exist.
        if (logSubject != null) logSubject.onCompleted();
        if (errorSubject != null) errorSubject.onCompleted();
        if (progressSubject != null) progressSubject.onCompleted();

        // Create new subjects.
        logSubject = new SerializedSubject<>(ReplaySubject.create());
        errorSubject = new SerializedSubject<>(ReplaySubject.create());
        progressSubject = new SerializedSubject<>(BehaviorSubject.create());

        // Subscribe listener to the new subjects.
        if (listener != null) {
            listenerLogSub = listener.subscribeToLogStream(logSubject);
            listenerErrorSub = listener.subscribeToErrorStream(errorSubject);
            listenerProgressSub = listener.subscribeToProgressStream(progressSubject);
        }
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
     * Set the listener. (The listener will be caught up if need be)
     * <p>
     * If there is already a listener registered, this will do nothing.
     * @param listener Listener.
     */
    public void registerListener(IFullImportListener listener) {
        if (listener == null || this.listener != null) return;
        this.listener = listener;

        // Catch the listener up to our current state.
        switch (currState) {
            case READY:
                listener.setReady();
                break;
            case PREP:
            case IMPORTING:
                listener.setRunning();
                break;
            case SAVING:
                listener.setSaving();
                break;
            case CANCELLING:
            case FINISHING:
                listener.setCancelling();
                break;
        }
        listener.setCurrImportDir(currDir == null ? null : currDir.getAbsolutePath());
        listener.setMaxProgress(numTotal);
        listenerProgressSub = listener.subscribeToProgressStream(progressSubject);
        listenerLogSub = listener.subscribeToLogStream(logSubject);
        listenerErrorSub = listener.subscribeToErrorStream(errorSubject);
    }

    /**
     * Unregister the listener.
     */
    public void unregisterListener() {
        if (listenerLogSub != null) listenerLogSub.unsubscribe();
        if (listenerErrorSub != null) listenerErrorSub.unsubscribe();
        if (listenerProgressSub != null) listenerProgressSub.unsubscribe();
        listener = null;
        // If the importer is in a ready state, we should go ahead and reset the subjects too.
        if (isReady()) resetSubjects();
    }

    /**
     * Implemented by classes which wish to listen to events from the full importer.
     */
    public interface IFullImportListener {
        /**
         * Set the max progress state.
         * @param maxProgress Max progress.
         */
        void setMaxProgress(int maxProgress);

        /**
         * Set the directory we're currently importing from.
         * @param importDir Import directory.
         */
        void setCurrImportDir(String importDir);

        /**
         * Have the listener subscribe to the log stream and return their subscription.
         * <p>
         * The log stream will emit all past and future log strings when subscribed to. A null emitted indicates that
         * the stream has been reset.
         * <p>
         * Note: If the implementer subscribes but doesn't return the subscription, memory leaks will very likely
         * occur.
         * @param logSubject The log stream.
         * @return Listener's subscription to the log stream, or null if the listener didn't subscribe.
         */
        Subscription subscribeToLogStream(final Subject<String, String> logSubject);

        /**
         * Have the listener subscribe to the error stream and return their subscription. The error stream is a subset
         * of the log stream, it only emits log strings which describe errors.
         * <p>
         * The error stream will emit all past and future error log strings when subscribed to. A null emitted indicates
         * that the stream has been reset.
         * <p>
         * Note: If the implementer subscribes but doesn't return the subscription, memory leaks will very likely
         * occur.
         * @param errorSubject The error stream.
         * @return Listener's subscription to the error stream, or null if the listener didn't subscribe.
         */
        Subscription subscribeToErrorStream(final Subject<String, String> errorSubject);

        /**
         * Have the listener subscribe to the progress stream and return their subscription.
         * <p>
         * The progress stream will return the last and future values when subscribed to. Any value emitted which is
         * negative should be interpreted as indeterminate, and any values greater than the max should be interpreted as
         * the max.
         * <p>
         * Note: If the implementer subscribes but doesn't return the subscription, memory leaks will very likely
         * occur.
         * @param progressSubject The progress stream.
         * @return Listener's subscription to the progress stream, or null if the listener didn't subscribe.
         */
        Subscription subscribeToProgressStream(final Subject<Integer, Integer> progressSubject);

        /**
         * Called when the importer has finished and is in the ready state.
         */
        void setReady();

        /**
         * Called when the importer starts running.
         */
        void setRunning();

        /**
         * Called when the importer transitions from the running to saving state. Implementers should note that trying
         * to cancel in the saving state is not allowed, nothing will happen.
         */
        void setSaving();

        /**
         * Called when the importer starts trying to cancel.
         */
        void setCancelling();

        /**
         * Called when the importer has finished cancelling.
         */
        void setCancelled();
    }
}
