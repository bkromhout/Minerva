package com.bkromhout.minerva.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.bkromhout.minerva.Minerva;

/**
 * Preferences related to the database. This class is a bit more special than the other *Prefs classes since it keeps
 * its data in memory for faster access.
 */
public class DBPrefs {
    // File name.
    private static final String FILE_NAME = "db";
    // Key Strings.
    private final static String NEXT_RBOOK_UID = "NEXT_RBOOK_UID";
    private final static String NEXT_RBOOKLISTITEM_UID = "NEXT_RBOOKLISTITEM_UID";

    /**
     * Static instance.
     */
    private static DBPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences prefs;

    // In-memory values for faster access!
    private static long nextRBookUid;
    private static long nextRBookListItemUid;

    private DBPrefs() {
        // Get shared preferences.
        this.prefs = Minerva.getAppCtx().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        // Make sure that we have the first UID set up.
        if (!prefs.contains(NEXT_RBOOK_UID)) prefs.edit().putLong(NEXT_RBOOK_UID, 0).apply();
        if (!prefs.contains(NEXT_RBOOKLISTITEM_UID)) prefs.edit().putLong(NEXT_RBOOKLISTITEM_UID, 0).apply();
        // Put values in memory.
        nextRBookUid = prefs.getLong(NEXT_RBOOK_UID, 0);
        nextRBookListItemUid = prefs.getLong(NEXT_RBOOKLISTITEM_UID, 0);
    }

    /**
     * Get an instance.
     * @return {@link DBPrefs}.
     */
    public static DBPrefs get() {
        if (INSTANCE == null) INSTANCE = new DBPrefs();
        return INSTANCE;
    }

    /**
     * Get the next long value to use for creating an {@link com.bkromhout.minerva.realm.RBook}.
     * @return A long value.
     */
    public long getNextRBookUid() {
        long ret = nextRBookUid;
        prefs.edit().putLong(NEXT_RBOOK_UID, nextRBookUid++).apply();
        return ret;
    }

    /**
     * Get the next long value to use for creating an {@link com.bkromhout.minerva.realm.RBookListItem}.
     * @return A long value.
     */
    public long getNextRBookListItemUid() {
        long ret = nextRBookListItemUid;
        prefs.edit().putLong(NEXT_RBOOKLISTITEM_UID, nextRBookListItemUid++).apply();
        return ret;
    }
}
