package com.bkromhout.minerva.data;

import android.support.annotation.NonNull;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Helper class which handles logging functionality for the {@link Importer}.
 */
public class ImportLogger {
    /**
     * Represents either the current log (if we're logging), or the most recent log (if we aren't logging).
     */
    public static final int CURRENT_OR_LATEST_LOG = 0;

    /**
     * Implement to view past import logs and listen to running import logs.
     */
    public interface ImportLogListener {
        /**
         * Get an Observer whose {@code onNext()} method handles log string emissions.
         * <p>
         * It is expected that if {@code null} is passed to {@code onNext()}, the listener will clear whatever method it
         * is using to display the full log in preparation for receiving new content.
         * @return Full log observer.
         */
        @NonNull
        Observer<String> getFullLogObserver();

        /**
         * Get an Observer whose {@code onNext()} method handles error log string emissions.
         * <p>
         * It is expected that if {@code null} is passed to {@code onNext()}, the listener will clear whatever method it
         * is using to display the error log in preparation for receiving new content.
         * @return Error log observer.
         */
        @NonNull
        Observer<String> getErrorLogObserver();

        /**
         * Get whether or not the log should be switched automatically if a new import run starts.
         * @return True to auto-switch logs when a new import starts, false otherwise.
         */
        boolean shouldAutoSwitchWhenStarting();

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
     * Listener vars.
     */
    /**
     * Listener.
     */
    private ImportLogListener listener;
    /**
     * Listener's subscription to the log stream.
     */
    private Subscription listenerLogSub;
    /**
     * Listener's subscription to the error stream.
     */
    private Subscription listenerErrorSub;

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
        if (listener == null) return;
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
        if (listenerLogSub != null && !listenerLogSub.isUnsubscribed()) listenerLogSub.unsubscribe();
        if (listenerErrorSub != null && !listenerErrorSub.isUnsubscribed()) listenerErrorSub.unsubscribe();

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
            if (logs.size() == C.MAX_LOGS) realm.executeTransaction(tRealm -> logs.last().deleteFromRealm());
        }
    }

    /**
     * Persist the last success time, and notify the listener if attached.
     * @param endTime Time the last successful import ended.
     */
    private void updateLastSuccessTime(long endTime) {
        Minerva.getPrefs().putLastImportSuccessTime(endTime);
        if (listener != null) listener.setLatestSuccessfulRun(endTime);
    }

    /**
     * Publish a full log that we'd saved to the listener, if it's attached.
     * @param fullLog Full log as a string.
     */
    private void publishSavedFullLog(String fullLog) {
        if (listener == null) return;
        Observer<String> fullLogObserver = listener.getFullLogObserver();
        fullLogObserver.onNext(null);
        fullLogObserver.onNext(fullLog);
    }

    /**
     * Publish an error log that we'd saved to the listener, if it's attached.
     * @param errorLog Error log as a string.
     */
    private void publishSavedErrorsLog(String errorLog) {
        if (listener == null) return;
        Observer<String> errorLogObserver = listener.getErrorLogObserver();
        errorLogObserver.onNext(null);
        errorLogObserver.onNext(errorLog);
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

        // Switch the listener to the current log immediately, if it's attached and wishes for us to do so.
        if (listener != null && listener.shouldAutoSwitchWhenStarting()) switchLogs(CURRENT_OR_LATEST_LOG);
    }

    /**
     * Called by {@link Importer} when it has finished an import run so that we can save the current log.
     * @param wasSuccess Whether or not the import run was successful.
     */
    void finishCurrentLog(boolean wasSuccess) {
        throwIfNotLogging();

        // Complete our subjects.
        logSubject.onCompleted();
        errorSubject.onCompleted();

        // Get current time. Persist it if this was a successful finish.
        long endTime = Calendar.getInstance().getTimeInMillis();
        if (wasSuccess) updateLastSuccessTime(endTime);

        // Concatenate logs into strings, then destroy the subjects.
        String fullLog = DataUtils.rxToString(logSubject);
        String errorLog = DataUtils.rxToString(errorSubject);
        destroySubjects();

        // Save new log, then destroy the
        saveNewLog(endTime, fullLog, errorLog, wasSuccess);

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

    /**
     * Get whether or not there is a listener attached.
     * @return True if a listener is attached, otherwise false.
     */
    boolean isListenerAttached() {
        return listener != null;
    }

    /**
     * Attaches the given {@code listener} so that it can receive updates for any current logs which start as well as
     * view past logs.
     * <p>
     * Only one listener may be attached at a time.
     * @param listener Import log listener.
     */
    public final void startListening(@NonNull ImportLogListener listener) {
        if (this.listener != null) throw new IllegalStateException("There is already a listener attached.");
        this.listener = listener;

        // Push the latest data to the listener immediately.
        listener.setLatestSuccessfulRun(Minerva.getPrefs().getLastImportSuccessTime(-1));
        switchLogs(CURRENT_OR_LATEST_LOG);
    }

    /**
     * Detaches the currently attached listener.
     */
    public final void stopListening() {
        // Unsubscribe the listener's subscriptions, if they exist.
        unsubscribeListenerFromSubjects();
        listener = null;
    }

    /**
     * Returns a list of past logs which are available for viewing. If there is an import running, the current log will
     * be the first item. If there are no past logs, and there isn't an import running, the list will be empty.
     * @return List of logs which may be viewed currently, or empty if there are none.
     */
    public final List<String> getLogList() {
        ArrayList<String> logList = new ArrayList<>();

        // Add the current log if we're logging.
        if (isLogging) logList.add(C.getStr(R.string.log_label_current_import_uc));

        // Add all past logs.
        try (Realm realm = Realm.getDefaultInstance()) {
            // Get the list of past logs from Realm.
            RealmResults<RImportLog> logs = realm.where(RImportLog.class)
                                                 .findAllSorted("endTime", Sort.DESCENDING);

            // Create and add their labels.
            for (RImportLog log : logs)
                logList.add(C.getStr(R.string.log_label_from_uc, Util.getRelTimeString(log.endTime)));
        }

        return logList;
    }

    /**
     * Switches the log that is being shown in the listener to the one whose index is {@code whichLog}.
     * <p>
     * This is done by using the Observers obtained from the listener to clear and then send new content. A new log
     * label is also set.
     * <p>
     * If currently logging an import run, passing {@code 0} for {@code whichLog} will subscribe the listener to the
     * ongoing log.
     * @param whichLog The index of the log to switch to. If invalid, this method does nothing.
     */
    public final void switchLogs(int whichLog) {
        // Sanity checks.
        if (whichLog < 0 || whichLog >= C.MAX_LOGS || listener == null) return;

        // Special handling for when we're currently logging.
        if (isLogging) {
            if (whichLog == CURRENT_OR_LATEST_LOG) {
                // If 0 is passed, we should subscribe the listener to the currently ongoing log.
                subscribeListenerToSubjects();

                // Now have the listener clear its current contents.
                logSubject.onNext(null);
                errorSubject.onNext(null);

                // Then set the log label, and we're done.
                listener.setCurrLogLabel(C.getStr(R.string.log_label_current_import_lc));
                return;
            } else // If we're logging and didn't pass 0, we want a past log; translate to proper index now.
                whichLog--;
        }

        try (Realm realm = Realm.getDefaultInstance()) {
            // Get the list of past logs from Realm.
            RealmResults<RImportLog> logs = realm.where(RImportLog.class)
                                                 .findAllSorted("endTime", Sort.DESCENDING);
            // Make sure our index is valid. If it isn't, just do nothing.
            if (whichLog >= logs.size()) return;

            // Get the requested log.
            RImportLog log = logs.get(whichLog);
            // Make a log label for it.
            String label = C.getStr(R.string.log_label_from_lc, Util.getRelTimeString(log.endTime));

            // Make sure that the listener isn't subscribed currently.
            unsubscribeListenerFromSubjects();

            // Tell the listener the label, then publish the logs.
            listener.setCurrLogLabel(label);
            publishSavedFullLog(log.fullLog);
            publishSavedErrorsLog(log.errorLog);
        }
    }
}
