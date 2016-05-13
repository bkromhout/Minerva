package com.bkromhout.minerva.data;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.rx.RxFileWalker;
import com.bkromhout.minerva.ui.SnackKiosk;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;
import timber.log.Timber;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Handles all importing.
 */
public class Importer {
    /**
     * Change the progress UI to an indeterminate state.
     */
    public static final int SET_PROGRESS_INDETERMINATE = -1;
    /**
     * Clear the progress UI to a determinate, empty state.
     */
    public static final int SET_PROGRESS_DETERMINATE_ZERO = -2;

    /**
     * Implemented by classes which wish to listen to events from the importer.
     */
    public interface ImportStateListener {
        /**
         * Get an Observer whose {@code onNext()} method handles progress updates.
         * @return Progress observer.
         */
        @NonNull
        Observer<Integer> getProgressObserver();

        /**
         * Called when listener needs to change some meta-state of their progress UI.
         * <p>
         * Treat all positive numbers as values to use for maximum progress.
         * <p>
         * Also be prepared to accept {@link #SET_PROGRESS_INDETERMINATE} and {@link #SET_PROGRESS_DETERMINATE_ZERO},
         * both of which are negative values, and react accordingly based on those constants' docs.
         * @param maxProgress Max progress, or special constant.
         */
        void onProgressFlag(int maxProgress);

        /**
         * Set the number of queued import runs.
         * @param numQueued Number of import runs in the queue.
         */
        void setNumQueued(int numQueued);

        /**
         * Called when the importer's state has changed.
         * @param newState New state of the importer.
         */
        void onImportStateChanged(State newState);
    }

    /**
     * Various state the importer can be in.
     */
    public enum State {
        READY, PREP, IMPORTING, SAVING, CANCELLING, FINISHING
    }

    /**
     * Types of import runs.
     */
    private enum ImportType {
        FULL, REDO
    }

    /**
     * Instance of Importer.
     */
    private static Importer INSTANCE;

    /*
     * State vars.
     */
    /**
     * The current state of the importer.
     */
    private State currState;
    /**
     * The {@link ImportRun}s which are currently waiting to be executed.
     */
    private volatile Queue<ImportRun> queuedRuns;
    /**
     * The {@link ImportRun} that is currently being executed.
     */
    private ImportRun currRun = null;
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

    /*
     * Import process vars.
     */
    private ImportLogger logger;
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

    /*
     * Listener vars.
     */
    /**
     * Who is listening to our progress?
     */
    private ImportStateListener listener;
    /**
     * BehaviorSubject wrapped in a SerializedSubject which holds the latest progress.
     */
    private Subject<Integer, Integer> progressSubject;
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
        // Init vars
        this.logger = ImportLogger.get();
        this.currDir = null;
        this.numDone = 0;
        this.numTotal = 0;
        this.currRun = null;
        this.bookQueue = new LinkedList<>();
        this.queuedRuns = new LinkedList<>();

        // Set our state to READY.
        currState = State.READY;
    }

    /**
     * Create new progress subject.
     */
    private void createProgressSubject() {
        if (progressSubject == null) progressSubject = new SerializedSubject<>(BehaviorSubject.create());
    }

    /**
     * Unsubscribe, complete, and nullify progress subject.
     */
    private void destroyProgressSubject() {
        unsubscribeListenerFromProgressSubject();

        // Complete subject, if it exists.
        if (progressSubject != null) progressSubject.onCompleted();

        // Null it.
        progressSubject = null;
    }

    /**
     * Subscribe listener to progress subject.
     */
    private void subscribeListenerToProgressSubject() {
        // Subscribe to progress updates.
        if (progressSubject != null && listenerProgressSub == null)
            listenerProgressSub = progressSubject.observeOn(AndroidSchedulers.mainThread())
                                                 .subscribe(listener.getProgressObserver());
    }

    /**
     * Unsubscribe listener from progress subject.
     */
    private void unsubscribeListenerFromProgressSubject() {
        // Unsubscribe listener from old subject.
        if (listenerProgressSub != null && !listenerProgressSub.isUnsubscribed()) listenerProgressSub.unsubscribe();

        // Null it.
        listenerProgressSub = null;
    }

    /**
     * Checks the state to determine if the importer is currently idle or is trying to get to an idle state.
     * @return True if state is ready, cancelling, or finishing; otherwise false.
     */
    private boolean isReadyOrTryingToBe() {
        return currState == State.READY || currState == State.CANCELLING || currState == State.FINISHING;
    }

    /**
     * Publishes a new {@link State} to the listener, if it's attached.
     * @param newState State to publish.
     */
    private void publishStateUpdate(State newState) {
        if (listener != null) listener.onImportStateChanged(newState);
    }

    /**
     * Queue a new import run. If no import runs are currently queued/running, the new one will be started immediately.
     * @param newRun New import run.
     */
    private void queueNewRun(ImportRun newRun) {
        queuedRuns.add(newRun);
        int numBefore = queuedRuns.size();
        startNextRun();
        // Only update the listener if we know startNextRun() didn't do it.
        if (queuedRuns.size() == numBefore && listener != null) listener.setNumQueued(numBefore);
    }

    /**
     * Attempts to start executing the next {@link ImportRun} in {@link #queuedRuns}.
     */
    private void startNextRun() {
        // Don't do anything if we're still executing some other import run.
        if (currRun != null) return;

        // Try to get and start the next import run.
        currRun = queuedRuns.poll();
        if (currRun != null) {
            if (listener != null) listener.setNumQueued(queuedRuns.size());
            doCommonPrep();
        }
    }

    /**
     * Prepare for import.
     */
    private void doCommonPrep() {
        currState = State.PREP;

        // Figure out what type of import run this is, then tell the logger we're about to start a new run.
        logger.prepareNewLog();
        logger.log(C.getStr(currRun.type == ImportType.FULL ? R.string.fil_starting : R.string.ril_starting));

        // Create progress subject and update listener
        createProgressSubject();
        subscribeListenerToProgressSubject();
        publishStateUpdate(State.PREP);
        if (listener != null) listener.onProgressFlag(SET_PROGRESS_INDETERMINATE);

        // Get and check currently configured library directory.
        String libDirPath = DefaultPrefs.get().getLibDir(null);
        if ((currDir = Util.tryResolveDir(libDirPath)) == null) {
            // We don't have a valid library directory.
            logger.error(C.getStr(R.string.il_err_invalid_lib_dir));
            doTeardownThenStartNextRun(true);
        }

        // Depending on the type of run this is, do a different sort of preparation.
        if (currRun.type == ImportType.FULL) doFullImportPrep();
        else doReImportPrep(currRun.reImportRelPaths);
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
        logger.log(C.getStr(R.string.fil_finding_files));
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
     * Try to get files which are at the given {@code relPaths} in the library directory.
     * <p>
     * We use an Rx flow to do that, and once that flow produces a list, it will call {@link #onGotFileList(List)}.
     * @param relPaths List of relative paths pulled from {@link RBook}s we wish to re-import.
     */
    private void doReImportPrep(List<String> relPaths) {
        logger.log(C.getStr(R.string.ril_build_file_list));
        fileResolverSubscription = Observable
                .from(relPaths)
                .map(relPath -> {
                    File file = Util.getFileFromRelPath(currDir, relPath);
                    if (file == null) logger.error(C.getStr(R.string.ril_err_getting_file,
                            currDir.getAbsolutePath() + relPath));
                    return file;
                })
                .filter(file -> file != null)
                .toList()
                .single()
                .subscribe(this::onGotFileList, t -> {
                    String s = C.getStr(R.string.ril_err_getting_files);
                    Timber.e(t, s);
                    logger.error("\n" + s + ":\n\"" + t.getMessage() + "\"\n");
                    cancelImportRun();
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
        logger.log(C.getStr(R.string.il_done));
        // Check if we should stop.
        if (isReadyOrTryingToBe()) {
            doTeardownThenStartNextRun(true);
            return;
        }
        // Remove reference to subscription.
        fileResolverSubscription.unsubscribe();

        // Check file list.
        if (files.isEmpty()) {
            // We don't have any files.
            logger.log(C.getStr(R.string.il_err_no_files));
            doTeardownThenStartNextRun(false);
            return;
        }

        // Update state.
        numDone = 0;
        numTotal = files.size();

        // Update listener.
        logger.log(C.getStr(R.string.il_found_files, numTotal));
        if (listener != null) listener.onProgressFlag(numTotal);
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
        if (isReadyOrTryingToBe()) {
            doTeardownThenStartNextRun(true);
            return;
        }

        // Change state to running.
        currState = State.IMPORTING;
        logger.log(C.getStr(R.string.il_reading_files));

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
            logger.error(C.getStr(R.string.il_err_processing_file, e.getMessage()));
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
        if (isReadyOrTryingToBe()) {
            doTeardownThenStartNextRun(true);
            return;
        }

        // Add RBook to queue and emit file path.
        bookQueue.add(rBook);
        logger.log(C.getStr(R.string.il_read_file, rBook.relPath));
        progressSubject.onNext(numDone++);
    }

    /**
     * What to do if an error is thrown during import.
     * @param t Throwable.
     */
    private void onFileImporterError(Throwable t) {
        String s = C.getStr(R.string.il_err_generic);
        Timber.e(t, s);
        logger.error("\n" + s + ":\n\"" + t.getMessage() + "\"\n");
        cancelImportRun();
    }

    /**
     * What to do after we've finished importing all books.
     */
    private void onAllFilesImported() {
        logger.log(C.getStr(R.string.il_all_files_read));
        // Check if we should stop.
        if (isReadyOrTryingToBe()) {
            doTeardownThenStartNextRun(true);
            return;
        }

        // Cancelling isn't allowed from this point until we're done persisting data to Realm.
        currState = State.SAVING;
        publishStateUpdate(State.SAVING);
        logger.log(C.getStr(R.string.il_saving_files));
        if (listener != null) listener.onProgressFlag(SET_PROGRESS_INDETERMINATE);

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
                    logger.error("\n" + s + "\n");
                    _cancelImportRun();
                });
    }

    /**
     * Called when an import run has finished successfully.
     */
    private void importFinished() {
        logger.log(C.getStr(R.string.il_done));
        doTeardownThenStartNextRun(false);
    }

    /**
     * Cancels the current import run, unconditionally. Will then try and start the next queued run.
     * <p>
     * Internal methods should still prefer calling {@link #cancelImportRun()} to this method, unless the checks on that
     * method prohibit a cancel where it is required.
     */
    private void _cancelImportRun() {
        publishStateUpdate(State.CANCELLING);
        doTeardownThenStartNextRun(true);
    }

    /**
     * Sends a Snackbar using {@link SnackKiosk} with the results of the import, unless a listener is attached and
     * subscribed to the logs, in which case we assume they already know.
     * @param wasCancelled Whether or not the import was cancelled.
     * @param numErrors    How many errors occurred during the run.
     */
    private void sendFinishedSnackbar(boolean wasCancelled, int numErrors) {
        StringBuilder builder = new StringBuilder();

        // Which type?
        String part = C.getStr(currRun.type == ImportType.FULL ? R.string.sb_fil : R.string.sb_ril);
        builder.append(part);

        // Finished, or cancelled?
        part = C.getStr(wasCancelled ? R.string.sb_result_cancelled : R.string.sb_result_finished);
        builder.append(part);

        // Processing result.
        if (numTotal == 0 && numErrors == 0) part = C.getStr(R.string.sb_il_results_zero);
        else if (numTotal == 0) part = C.getQStr(R.plurals.sb_il_just_error_results, numErrors, numErrors);
        else part = C.getQStr(R.plurals.sb_il_results, numDone, numDone, numTotal);
        builder.append(part);

        // Errors result (if errors weren't our sole processing result).
        if (numTotal != 0 && numErrors != 0) {
            part = C.getQStr(R.plurals.sb_il_and_error_results, numErrors, numErrors);
            builder.append(part);
        }

        // Number of imports queued.
        int numQueued = queuedRuns.size();
        if (numQueued != 0) {
            part = C.getQStr(R.plurals.sb_il_more_queued, numQueued, numQueued);
            builder.append(part);
        }

        // Append a period too.
        builder.append(".");

        // Only send if listener isn't subscribed to the current import log right now.
        if (!logger.isCurrentRunObserved()) SnackKiosk.snack(builder.toString(), R.string.sb_il_action_details,
                R.id.sb_action_open_import_activity, Snackbar.LENGTH_LONG);
    }

    /**
     * Takes care of resetting the importer's state, finishing the log, and updating the listener. Once that's done,
     * attempts to start executing the next {@link ImportRun} in {@link #queuedRuns}.
     * @param wasCancelled Whether or not the run was cancelled.
     */
    private void doTeardownThenStartNextRun(boolean wasCancelled) {
        // Start resetting, unless we already are resetting.
        if (currState != State.FINISHING && currState != State.READY) currState = State.FINISHING;
        else return;

        // Unsubscribe from internal-use subs if they're still active.
        if (fileResolverSubscription != null) fileResolverSubscription.unsubscribe();
        if (fileImporterSubscription != null) fileImporterSubscription.unsubscribe();

        // Close Realm if need be.
        if (realm != null) {
            realm.close();
            realm = null;
        }

        // Try to have the listener clear its progress UI, then destroy the progress subject.
        if (listener != null) listener.onProgressFlag(SET_PROGRESS_DETERMINATE_ZERO);
        destroyProgressSubject();

        // Do a bit more logging.
        boolean wasSuccess;
        int numErrors = logger.getCurrNumErrors();
        if (wasCancelled) {
            // Final log message notes that the import was cancelled.
            logger.log(C.getStr(R.string.il_cancelled));
            wasSuccess = false;
        } else {
            // We currently still consider a full import successful even if it had errors.
            wasSuccess = numErrors == 0 || currRun.type == ImportType.FULL;

            // Final log message depends on the number of errors that occurred.
            if (numErrors > 0) logger.log(C.getStr(R.string.il_finished_with_errors, numErrors));
            else logger.log(C.getStr(R.string.il_finished));
        }

        // Send Snackbar at this point informing the user of results.
        sendFinishedSnackbar(wasCancelled, numErrors);

        // Reset vars.
        currDir = null;
        numDone = 0;
        numTotal = 0;
        currRun = null;
        bookQueue = new LinkedList<>();

        // Close the log, then inform the listener that we're ready again.
        logger.finishCurrentLog(wasSuccess);
        this.currState = State.READY;
        publishStateUpdate(State.READY);

        // Try to start the next queued run.
        startNextRun();
    }

    /**
     * Set the listener. (The listener will be caught up if need be)
     * <p>
     * Only one listener may be attached at a time.
     * @param listener Import state listener.
     */
    public final void startListening(@NonNull ImportStateListener listener) {
        if (this.listener != null) throw new IllegalStateException("There is already a listener attached.");
        this.listener = listener;

        // Catch the listener up to our current state.
        listener.onImportStateChanged(currState);
        listener.setNumQueued(queuedRuns.size());
        listener.onProgressFlag(numTotal);
        subscribeListenerToProgressSubject();
    }


    /**
     * Unregister the listener.
     */
    public final void stopListening() {
        // Unsubscribe the listener's subscription, if it exists.
        unsubscribeListenerFromProgressSubject();
        listener = null;
    }

    /**
     * Check whether the importer is currently in the ready state (doing absolutely nothing).
     * @return True if the importer is currently in the ready state, false otherwise.
     * @see State
     */
    public final boolean isReady() {
        return currState == State.READY;
    }

    /**
     * Queues a full import run. The importer will import files from the configured library directory.
     * <p>
     * Calling this when the importer isn't in a ready state will do nothing.
     */
    public final void queueFullImport() {
        queueNewRun(new ImportRun(ImportType.FULL, null));
        // Show snackbar if we had to queue the import, or if we didn't but our listener isn't attached.
        if (!queuedRuns.isEmpty()) SnackKiosk.snack(R.string.sb_fil_queued, Snackbar.LENGTH_SHORT);
        else if (listener == null) SnackKiosk.snack(R.string.sb_fil_started, Snackbar.LENGTH_SHORT);
    }

    /**
     * Queues a re-import run. The importer will re-import files associated with the given {@code books} from the
     * configured library directory.
     * <p>
     * Calling this when the importer isn't in a ready state will do nothing.
     * @param books List of {@link RBook}s to re-import.
     */
    public final void queueReImport(List<RBook> books) {
        queueNewRun(new ImportRun(ImportType.REDO, books));

        // Show a snackbar saying either that the re-import was started or queued.
        SnackKiosk.snack(queuedRuns.isEmpty() ? R.string.sb_ril_started : R.string.sb_ril_queued,
                Snackbar.LENGTH_SHORT);
    }

    /**
     * Cancels the current import run. Note that this will automatically cause the next queued run to be started.
     * <p>
     * If the importer has already entered the saving state, or if the importer isn't preparing or running, this does
     * nothing.
     */
    public final void cancelImportRun() {
        if (isReadyOrTryingToBe() || currState == State.SAVING) return;
        _cancelImportRun();
    }

    /**
     * Helper class to hold information about an import run.
     */
    private static class ImportRun {
        /**
         * The type of import run this is.
         */
        public final ImportType type;
        /**
         * If {@link #type} is {@link ImportType#REDO}, this will be a list of relative file paths which we need to
         * re-import from the library directory.
         */
        public final List<String> reImportRelPaths;

        private ImportRun(ImportType type, List<RBook> reImportRelPaths) {
            this.type = type;
            this.reImportRelPaths = reImportRelPaths == null ? null : rBooksToRelPaths(reImportRelPaths);
        }

        /**
         * Extracts relative paths from a list of {@link RBook}s.
         * @param books List of {@link RBook}s.
         * @return List of relative paths.
         */
        private List<String> rBooksToRelPaths(List<RBook> books) {
            return Observable.from(books)
                             .map(book -> book.relPath)
                             .toList()
                             .toBlocking()
                             .single();
        }
    }
}
