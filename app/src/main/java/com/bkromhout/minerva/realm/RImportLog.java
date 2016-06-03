package com.bkromhout.minerva.realm;

import io.realm.RealmObject;

/**
 * Represents an import log in Realm.
 */
public class RImportLog extends RealmObject {
    /**
     * Time when the import ended in milliseconds.
     */
    public long endTime;
    /**
     * Full import log.
     */
    public String fullLog;
    /**
     * Errors-only import log.
     */
    public String errorLog;
    /**
     * Whether or not the import was a success.
     */
    public boolean wasSuccess;

    public RImportLog() {
    }

    /**
     * Create a new {@link RImportLog}.
     * @param endTime    Time when the import ended, in milliseconds.
     * @param fullLog    Full import log.
     * @param errorLog   Errors-only import log.
     * @param wasSuccess Whether or not the import was a success.
     */
    public RImportLog(long endTime, String fullLog, String errorLog, boolean wasSuccess) {
        this.endTime = endTime;
        this.fullLog = fullLog;
        this.errorLog = errorLog;
        this.wasSuccess = wasSuccess;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RImportLog that = (RImportLog) o;

        return endTime == that.endTime;
    }

    @Override
    public int hashCode() {
        return (int) (endTime ^ (endTime >>> 32));
    }
}
