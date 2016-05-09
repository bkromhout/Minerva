package com.bkromhout.minerva.data;

import android.support.annotation.NonNull;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.prefs.DefaultPrefs;
import com.bkromhout.minerva.realm.RImportLog;
import com.bkromhout.minerva.util.Util;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import java.util.Calendar;

/**
 * Helper class which handles logging functionality for the {@link Importer}.
 */
public class ImportLogger {
    /**
     * Implement to view past import logs and listen to running import logs.
     */
    public interface ImportLogListener {
        /**
         * Get an Observer whose {@code onNext()} method handles log string emissions.
         * @return Full log observer.
         */
        @NonNull
        Observer<String> getFullLogObserver();

        /**
         * Get an Observer whose {@code onNext()} method handles error log string emissions.
         * @return Error log observer.
         */
        @NonNull
        Observer<String> getErrorLogObserver();

        /**
         * Inform the listener of the last time an import completed successfully.
         * @param time Most recent time an import run completed successfully in milliseconds.
         */
        void setLatestSuccessfulRun(long time);

        /**
         * Inform the listener of the label it should use for the log currently published.
         * @param logLabel New log label to use.
         */
        void setCurrLogLabel(String logLabel);
    }

    /**
     * Instance of ImportLogger.
     */
    private static ImportLogger INSTANCE;

    /*
     * Common vars.
     */
    /**
     * Listener.
     */
    private ImportLogListener listener;

    /*
     * Current import run vars.
     */
    /**
     * Whether or not we're currently logging for an import run.
     */
    private boolean isLogging = false;
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
     * Current number of errors logged.
     */
    private int currNumErrors;

    /*
     * Subscriber vars.
     */
    /**
     * Listener's subscription to the log stream.
     */
    private Subscription listenerLogSub;
    /**
     * Listener's subscription to the error stream.
     */
    private Subscription listenerErrorSub;

    /**
     * Get the instance of {@link ImportLogger}.
     * @return Instance.
     */
    public static ImportLogger get() {
        if (INSTANCE == null) INSTANCE = new ImportLogger();
        return INSTANCE;
    }

    // No public initialization.
    private ImportLogger() {
    }

    /**
     * Throws an IllegalStateException if {@link #isLogging} is {@code false}.
     */
    private void throwIfNotLogging() {
        if (!isLogging) throw new IllegalStateException("Must call prepareNewLog() first.");
    }

    /**
     * Create new subjects.
     */
    private void createSubjects() {
        if (logSubject == null) logSubject = new SerializedSubject<>(ReplaySubject.create());
        if (errorSubject == null) errorSubject = new SerializedSubject<>(ReplaySubject.create());
    }

    /**
     * Unsubscribe, complete, and nullify subjects.
     */
    private void destroySubjects() {
        unsubscribeListenerFromSubjects();

        // Complete subjects, if they exist.
        if (logSubject != null) logSubject.onCompleted();
        if (errorSubject != null) errorSubject.onCompleted();

        // Null them.
        logSubject = null;
        errorSubject = null;
    }

    /**
     * Subscribe listener to subjects.
     */
    private void subscribeListenerToSubjects() {
        // Subscribe to full log.
        if (logSubject != null && listenerLogSub == null)
            listenerLogSub = logSubject.onBackpressureBuffer()
                                       .observeOn(AndroidSchedulers.mainThread())
                                       .subscribe(listener.getFullLogObserver());

        // Subscribe to error log.
        if (errorSubject != null && listenerErrorSub == null)
            listenerErrorSub = errorSubject.onBackpressureBuffer()
                                           .observeOn(AndroidSchedulers.mainThread())
                                           .subscribe(listener.getErrorLogObserver());
    }

    /**
     * Unsubscribe listener from subjects.
     */
    private void unsubscribeListenerFromSubjects() {
        // Unsubscribe listener from old subjects.
        if (listenerLogSub != null) listenerLogSub.unsubscribe();
        if (listenerErrorSub != null) listenerErrorSub.unsubscribe();

        // Null them.
        listenerLogSub = null;
        listenerErrorSub = null;
    }

    /**
     * Check the current list of logs, and if we're at the limit defined by {@link C#MAX_LOGS}, remove the earliest
     * one.
     */
    private void makeRoomForNewLogIfNeeded() {
        try (Realm realm = Realm.getDefaultInstance()) {
            // Get logs.
            RealmResults<RImportLog> logs = realm.where(RImportLog.class)
                                                 .findAllSorted("endTime", Sort.DESCENDING);

            // Remove earliest log if we're at the limit.
            realm.executeTransaction(tRealm -> {
                if (logs.size() == C.MAX_LOGS) logs.last().deleteFromRealm();
            });
        }
    }

    /**
     * Persist the last success time, and notify the listener if attached.
     * @param endTime Time the last successful import ended.
     */
    private void updateLastSuccessTime(long endTime) {
        DefaultPrefs.get().putLastImportSuccessTime(endTime);
        if (listener != null) listener.setLatestSuccessfulRun(endTime);
    }

    /**
     * Save a new log using the current import run information.
     * @param endTime    Time the import run ended.
     * @param fullLog    Full log string.
     * @param errorLog   Error log string.
     * @param wasSuccess Whether or not the import run was successful.
     */
    private void saveNewLog(long endTime, String fullLog, String errorLog, boolean wasSuccess) {
        // Create and persist new log object.
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(tRealm ->
                    tRealm.copyToRealm(new RImportLog(endTime, fullLog, errorLog, wasSuccess)));
        }
    }

    /**
     * Called by {@link Importer} to indicate that it is about to start an import run, and that we need to be prepared
     * to start logging.
     */
    void prepareNewLog() {
        // Make sure we aren't already logging.
        if (isLogging) throw new IllegalStateException("Already logging.");
        isLogging = true;

        // Create subjects, and subscribe listener to them if it's attached.
        createSubjects();
        subscribeListenerToSubjects();

        // Set the current number of errors to 0.
        currNumErrors = 0;

        // Make room for a new log if necessary.
        makeRoomForNewLogIfNeeded();

        // TODO
    }

    /**
     * Called by {@link Importer} when it has finished an import run so that we can save the current log.
     * @param wasSuccess Whether or not the import run was successful.
     */
    void finishCurrentLog(boolean wasSuccess) {
        throwIfNotLogging();

        // Get current time. Persist it if this was a successful finish.
        long endTime = Calendar.getInstance().getTimeInMillis();
        if (wasSuccess) updateLastSuccessTime(endTime);

        // Concatenate logs into strings.
        String fullLog = Util.rxToString(logSubject);
        String errorLog = Util.rxToString(errorSubject);

        // Save new log.
        saveNewLog(endTime, fullLog, errorLog, wasSuccess);

        // Destroy subjects.
        destroySubjects();

        // TODO Switch listener to use latest log strings.

        // Set isLogging to false.
        isLogging = false;
    }

    /**
     * Publish a normal log line for the current import run.
     * @param logStr String to log.
     */
    void log(String logStr) {
        throwIfNotLogging();
        logSubject.onNext(logStr);
    }

    /**
     * Publish an error log line for the current import run.
     * @param errStr String to log.
     */
    void error(String errStr) {
        throwIfNotLogging();
        if (errStr != null) {
            log(errStr);
            currNumErrors++;
        }
        errorSubject.onNext(errStr);
    }

    /**
     * Get the number of errors which have been logged so far in this run.
     * @return Number of errors logged this run.
     */
    int getCurrNumErrors() {
        throwIfNotLogging();
        return currNumErrors;
    }

    public final void startListening(ImportLogListener listener) {
        // TODO
    }

    public final void stopListening() {
        // TODO
    }

    /**
     * Check whether there is currently an import run being logged.
     * @return True if an import run is being logged, otherwise false.
     */
    public final boolean isLogging() {
        return isLogging;
    }
}
