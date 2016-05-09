package com.bkromhout.minerva.data;

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
import timber.log.Timber;

import java.io.File;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Handles full library imports.
 */
public class Importer {
    public static final int SET_PROGRESS_INDETERMINATE = -1;
    public static final int SET_PROGRESS_DETERMINATE_ZERO = -2;

    /**
     * Various state the importer can be in.
     */
    public enum State {
        READY, PREP, IMPORTING, SAVING, CANCELLING, FINISHING
    }

    /**
     * Types of imports.
     */
    private enum ImportType {
        FULL, REDO
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
     * What type of import is currently running.
     */
    private ImportType currType;
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
     * Total number of errors logged.
     */
    private int numErrors;
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
     * Subscription to an Rx flow which attempts to get a list of files for us to import.
     */
    private Subscription fileResolverSubscription;
    /**
     * Subscription to an Rx flow which uses
     */
    private Subscription fileImporterSubscription;
    /**
     * List of {@link RBook}s that have been created from book files.
     */
    private Queue<RBook> bookQueue;
    /**
     * Instance of Realm.
     */
    private Realm realm;

    // Variables below concern the listener.
    /**
     * Who is listening to our progress?
     */
    private ImportListener listener;
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
     * Get the instance of {@link Importer}.
     * @return Instance.
     */
    public static Importer get() {
        if (INSTANCE == null) INSTANCE = new Importer();
        return INSTANCE;
    }

    // No public initialization.
    private Importer() {
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
     * @param listener The {@link ImportListener} to register for this run, or null if no listener should be
     *                 registered. Note that if there is already a listener registered, this will be ignored.
     */
    public void doFullImport(ImportListener listener) {
        // Don't do anything if we aren't in a ready state.
        if (currState != State.READY) return;

        // Reset subjects and possibly register a listener.
        resetSubjects();
        if (listener != null) registerListener(listener);

        currType = ImportType.FULL;
        // Kick off preparations; flow will continue from there.
        doFullImportPrep();
    }

    /**
     * TODO Make re-import process use this instead!
     * <p>
     * Starts a re-import run. The importer will re-import files associated with the given {@code books} from the
     * configured library directory.
     * <p>
     * Calling this when the importer isn't in a ready state will do nothing.
     * @param listener The {@link ImportListener} to register for this run, or null if no listener should be
     *                 registered. Note that if there is already a listener registered, this will be ignored.
     * @param books    List of {@link RBook}s to re-import.
     */
    public void doReImport(ImportListener listener, List<RBook> books) {
        // Don't do anything if we aren't in a ready state.
        if (currState != State.READY) return;

        // Reset subjects and possibly register a listener.
        resetSubjects();
        if (listener != null) registerListener(listener);
        // TODO else, show a global snackbar saying re-import was started.

        currType = ImportType.REDO;
        // Kick off preparations; flow will continue from there.
        doReImportPrep(books);
    }

    /**
     * Prepare for import.
     */
    private void doCommonPrep() {
        currState = State.PREP;

        // Listener updates.
        publishLog(null);
        publishError(null);
        if (listener != null) listener.setRunning();
        publishLog(C.getStr(R.string.il_starting));
        progressSubject.onNext(-1);

        // Get and check currently configured library directory.
        String libDirPath = DefaultPrefs.get().getLibDir(null);
        if ((currDir = Util.tryResolveDir(libDirPath)) == null) {
            // We don't have a valid library directory.
            publishError(C.getStr(R.string.il_err_invalid_lib_dir));
            resetState();
        }
    }

    /**
     * Prepare for a full import.
     * <p>
     * Try to recursively get all files with specific extensions within the library directory.
     * <p>
     * At the end of this method we pass off control to an Rx flow which uses {@link RxFileWalker} to get a list of
     * files; once that flow produces a list, it will call {@link #onGotFileList(List)}.
     */
    private void doFullImportPrep() {
        doCommonPrep();

        publishLog(C.getStr(R.string.fil_finding_files));
        // Get a list of files in the directory (and its subdirectories) which have certain extensions.
        // This will call through to onGotFileList() once it has the results.
        fileResolverSubscription = Observable
                .create(new RxFileWalker(currDir, C.VALID_EXTENSIONS))
                .subscribeOn(Schedulers.io()) // Everything above runs on the io thread pool.
                .unsubscribeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> fileResolverSubscription = null)
                .toList()
                .single()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGotFileList);
    }

    /**
     * Prepare for a re-import.
     * <p>
     * Try to get files which are associated with the given {@code books} from the library directory.
     * <p>
     * We use an Rx flow to do that, and once that flow produces a list, it will call {@link #onGotFileList(List)}.
     * @param books List of {@link RBook}s to re-import.
     */
    private void doReImportPrep(List<RBook> books) {
        doCommonPrep();

        publishLog(C.getStr(R.string.ril_build_file_list));
        fileResolverSubscription = Observable
                .from(books)
                .map(book -> book.relPath)
                .map(relPath -> {
                    File file = Util.getFileFromRelPath(currDir, relPath);
                    if (file == null) publishError(C.getStr(R.string.ril_err_getting_file,
                            currDir.getAbsolutePath() + relPath));
                    return file;
                })
                .filter(file -> file != null)
                .toList()
                .single()
                .subscribe(this::onGotFileList, t -> {
                    String s = C.getStr(R.string.ril_err_getting_files);
                    Timber.e(t, s);
                    publishError("\n" + s + ":\n\"" + t.getMessage() + "\"\n");
                    cancelFullImport();
                });
    }

    /**
     * Called by {@link #fileResolverSubscription} when it calls {@code onNext()}.
     * <p>
     * This is part of full import preparation.
     * @param files List of files with specific extensions found by {@link RxFileWalker}. These are the files we will
     *              try to import.
     */
    private void onGotFileList(List<File> files) {
        publishLog(C.getStr(R.string.il_done));
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }
        // Remove reference to subscription.
        fileResolverSubscription.unsubscribe();

        // Check file list.
        if (files.isEmpty()) {
            // We don't have any files.
            publishLog(C.getStr(R.string.il_err_no_files));
            resetState();
            return;
        }

        // Update state.
        numDone = 0;
        numTotal = files.size();

        // Update listener.
        publishLog(C.getStr(R.string.il_found_files, numTotal));
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
        publishLog(C.getStr(R.string.il_reading_files));

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
            return Util.readEpubFile(file, relPath);
        } catch (IllegalArgumentException e) {
            publishError(C.getStr(R.string.il_err_processing_file, e.getMessage()));
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
        publishLog(C.getStr(R.string.il_read_file, rBook.relPath));
        progressSubject.onNext(numDone++);
    }

    /**
     * What to do if an error is thrown during import.
     * @param t Throwable.
     */
    private void onFileImporterError(Throwable t) {
        String s = C.getStr(R.string.il_err_generic);
        Timber.e(t, s);
        publishError("\n" + s + ":\n\"" + t.getMessage() + "\"\n");
        cancelFullImport();
    }

    /**
     * What to do after we've finished importing all books.
     */
    private void onAllFilesImported() {
        publishLog(C.getStr(R.string.il_all_files_read));
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }

        // Cancelling isn't allowed from this point until we're done persisting data to Realm.
        currState = State.SAVING;
        if (listener != null) listener.setSaving();
        publishLog(C.getStr(R.string.il_saving_files));
        progressSubject.onNext(-1);

        // We've finished importing all books, now we'll persist them to Realm.
        realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                bgRealm -> {
                    for (RBook book : bookQueue) {
                        // Try to find existing RBook before adding a new one.
                        RBook existingBook = bgRealm.where(RBook.class)
                                                    .equalTo("relPath", book.relPath)
                                                    .findFirst();

                        // If we have an existing RBook for this file, just update the fields which we read from the
                        // file. If we don't have one, create one.
                        if (existingBook != null) existingBook.updateFromOtherRBook(bgRealm, book);
                        else bgRealm.copyToRealmOrUpdate(book);
                    }
                },
                this::importFinished,
                error -> {
                    String s = C.getStr(R.string.il_err_realm);
                    Timber.e(error, s);
                    publishError("\n" + s + "\n");
                    unsafeCancelFullImport();
                });
    }

    /**
     * Called when the full import has finished successfully.
     */
    private void importFinished() {
        // TODO global snackbar or something.
        publishLog(C.getStr(R.string.il_done));
        // Save current time to prefs to indicate a full import completed, then tell listener we finished.
        DefaultPrefs.get().putLastImportSuccessTime(Calendar.getInstance().getTimeInMillis());
        if (numErrors > 0) publishLog(C.getStr(R.string.il_finished_with_errors, numErrors));
        else publishLog(C.getStr(R.string.il_finished));
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
        // TODO global snackbar or something.
        if (listener != null) listener.setCancelling();
        publishLog(C.getStr(R.string.il_cancelling));
        resetState();
        publishLog(C.getStr(R.string.il_done));
        if (listener != null) listener.setCancelled();
    }

    /**
     * Publish a log line to the {@link #logSubject}.
     * @param logStr String to log.
     */
    private void publishLog(String logStr) {
        logSubject.onNext(logStr);
    }

    /**
     * Publish a log line to the {@link #errorSubject} and the {@link #logSubject}.
     * @param errStr String to log.
     */
    private void publishError(String errStr) {
        if (errStr != null) {
            publishLog(errStr);
            numErrors++;
        }
        errorSubject.onNext(errStr);
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
        if (fileResolverSubscription != null) fileResolverSubscription.unsubscribe();
        // Unsubscribe from the file importer subscription.
        if (fileImporterSubscription != null) fileImporterSubscription.unsubscribe();

        // Reset vars.
        this.currDir = null;
        this.numDone = 0;
        this.numTotal = 0;
        this.numErrors = 0;
        this.bookQueue = new LinkedList<>();

        // Close Realm.
        if (realm != null) realm.close();
        realm = null;

        this.currType = null;
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
    public void registerListener(ImportListener listener) {
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
     * Implemented by classes which wish to listen to events from the importer.
     */
    public interface ImportListener {
        /**
         * Set the max progress state.
         * @param maxProgress Max progress.
         */
        void setMaxProgress(int maxProgress);

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
