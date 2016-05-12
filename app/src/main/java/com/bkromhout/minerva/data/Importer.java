package com.bkromhout.minerva.data;

import android.support.annotation.NonNull;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.R;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.rx.RxFileWalker;
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
 * Handles full library imports.
 */
public class Importer {
    public static final int SET_PROGRESS_INDETERMINATE = -1;
    public static final int SET_PROGRESS_DETERMINATE_ZERO = -2;

    /**
     * Implemented by classes which wish to listen to events from the importer.
     */
    public interface ImportStateListener {
        /**
         * Set the max progress state.
         * <p>
         * Listeners should be prepared to accept {@link #SET_PROGRESS_INDETERMINATE} and {@link
         * #SET_PROGRESS_DETERMINATE_ZERO} and react accordingly.
         * @param maxProgress Max progress.
         */
        void setMaxProgress(int maxProgress);

        /**
         * Get an Observer whose {@code onNext()} method handles progress updates.
         * @return Progress observer.
         */
        @NonNull
        Observer<Integer> getProgressObserver();

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
     * Types of imports.
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
        resetState();
        // Get the ImportLogger now.
        logger = ImportLogger.get();
        // Create subject here, it should always exist.
        progressSubject = new SerializedSubject<>(BehaviorSubject.create());
    }

    /**
     * Starts a full import run. The importer will import files from the configured library directory.
     * <p>
     * Calling this when the importer isn't in a ready state will do nothing.
     * @param listener The {@link ImportStateListener} to register for this run, or null if no listener should be
     *                 registered. Note that if there is already a listener registered, this will be ignored.
     */
    public void doFullImport(ImportStateListener listener) {
        // Don't do anything if we aren't in a ready state.
        if (currState != State.READY) return;

        // Reset subjects and possibly register a listener.
        resetProgressSubject();
        if (listener != null) startListening(listener);

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
     * @param listener The {@link ImportStateListener} to register for this run, or null if no listener should be
     *                 registered. Note that if there is already a listener registered, this will be ignored.
     * @param books    List of {@link RBook}s to re-import.
     */
    public void doReImport(ImportStateListener listener, List<RBook> books) {
        // Don't do anything if we aren't in a ready state.
        if (currState != State.READY) return;

        // Reset subjects and possibly register a listener.
        resetProgressSubject();
        if (listener != null) startListening(listener);
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

        // Inform the logger we're about to start an import run. Then log the type of import.
        logger.prepareNewLog();

        // Listener updates.
        if (listener != null) listener.onImportStateChanged(State.PREP);
        logger.log(C.getStr(currType == ImportType.FULL ? R.string.fil_starting : R.string.ril_starting));
        progressSubject.onNext(SET_PROGRESS_INDETERMINATE);

        // Get and check currently configured library directory.
        String libDirPath = DefaultPrefs.get().getLibDir(null);
        if ((currDir = Util.tryResolveDir(libDirPath)) == null) {
            // We don't have a valid library directory.
            logger.error(C.getStr(R.string.il_err_invalid_lib_dir));
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
     * Try to get files which are associated with the given {@code books} from the library directory.
     * <p>
     * We use an Rx flow to do that, and once that flow produces a list, it will call {@link #onGotFileList(List)}.
     * @param books List of {@link RBook}s to re-import.
     */
    private void doReImportPrep(List<RBook> books) {
        doCommonPrep();

        logger.log(C.getStr(R.string.ril_build_file_list));
        fileResolverSubscription = Observable
                .from(books)
                .map(book -> book.relPath)
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
        logger.log(C.getStr(R.string.il_done));
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
            logger.log(C.getStr(R.string.il_err_no_files));
            resetState();
            return;
        }

        // Update state.
        numDone = 0;
        numTotal = files.size();

        // Update listener.
        logger.log(C.getStr(R.string.il_found_files, numTotal));
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
        if (isIdleOrTryingToBe()) {
            resetState();
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
        cancelFullImport();
    }

    /**
     * What to do after we've finished importing all books.
     */
    private void onAllFilesImported() {
        logger.log(C.getStr(R.string.il_all_files_read));
        // Check if we should stop.
        if (isIdleOrTryingToBe()) {
            resetState();
            return;
        }

        // Cancelling isn't allowed from this point until we're done persisting data to Realm.
        currState = State.SAVING;
        if (listener != null) listener.onImportStateChanged(State.SAVING);
        logger.log(C.getStr(R.string.il_saving_files));
        progressSubject.onNext(SET_PROGRESS_INDETERMINATE);

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
                    unsafeCancelFullImport();
                });
    }

    /**
     * Called when the full import has finished successfully.
     */
    private void importFinished() {
        // TODO global snackbar or something.
        logger.log(C.getStr(R.string.il_done));

        // Final log message depends on the number of errors that occurred.
        int numErrors = logger.getCurrNumErrors();
        if (numErrors > 0) logger.log(C.getStr(R.string.il_finished_with_errors, numErrors));
        else logger.log(C.getStr(R.string.il_finished));

        resetState();
        progressSubject.onNext(SET_PROGRESS_DETERMINATE_ZERO);
        logger.finishCurrentLog(numErrors == 0);
        if (listener != null) listener.onImportStateChanged(State.READY);
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
        if (listener != null) listener.onImportStateChanged(State.CANCELLING);
        logger.log(C.getStr(R.string.il_cancelling));
        resetState();
        logger.log(C.getStr(R.string.il_done));

        progressSubject.onNext(SET_PROGRESS_DETERMINATE_ZERO);
        logger.finishCurrentLog(false);
        if (listener != null) listener.onImportStateChanged(State.READY);
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
        this.bookQueue = new LinkedList<>();

        // Close Realm.
        if (realm != null) realm.close();
        realm = null;

        this.currType = null;
        this.currState = State.READY;
    }

    /**
     * Recreate subject and subscribe the listener to the new one.
     */
    private void resetProgressSubject() {
        // Unsubscribe listener from old subject.
        if (listenerProgressSub != null) listenerProgressSub.unsubscribe();

        // Complete subject, if it exists.
        if (progressSubject != null) progressSubject.onCompleted();

        // Create new subject.
        progressSubject = new SerializedSubject<>(BehaviorSubject.create());

        // Subscribe listener to the new subject.
        if (listener != null) subscribeListenerToProgressSubject();
    }

    /**
     * Checks the state to determine if the importer is currently idle or is trying to get to an idle state.
     * @return True if state is ready, cancelling, or finishing; otherwise false.
     */
    private boolean isIdleOrTryingToBe() {
        return currState == State.READY || currState == State.CANCELLING || currState == State.FINISHING;
    }

    /**
     * Subscribe listener to progress subject.
     */
    private void subscribeListenerToProgressSubject() {
        if (progressSubject != null && listenerProgressSub == null)
            listenerProgressSub = progressSubject.observeOn(AndroidSchedulers.mainThread())
                                                 .subscribe(listener.getProgressObserver());
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
     * Set the listener. (The listener will be caught up if need be)
     * <p>
     * If there is already a listener registered, this will do nothing.
     * @param listener Listener.
     */
    public final void startListening(ImportStateListener listener) {
        if (listener == null || this.listener != null) return;
        this.listener = listener;

        // Catch the listener up to our current state.
        listener.onImportStateChanged(currState);
        listener.setMaxProgress(numTotal);
        subscribeListenerToProgressSubject();
    }

    /**
     * Unregister the listener.
     */
    public final void stopListening() {
        if (listenerProgressSub != null) listenerProgressSub.unsubscribe();
        listener = null;
        // If the importer is in a ready state, we should go ahead and reset the subject too.
        if (isReady()) resetProgressSubject();
    }
}
