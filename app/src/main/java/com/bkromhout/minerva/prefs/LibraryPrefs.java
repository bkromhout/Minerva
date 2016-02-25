package com.bkromhout.minerva.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.bkromhout.minerva.Minerva;

/**
 * Preferences class for {@link com.bkromhout.minerva.fragments.LibraryFragment}.
 */
public class LibraryPrefs {
    // File name.
    private static final String FILE_NAME = "library";
    // Key strings.
    private static final String SORT_TYPE = "SORT_TYPE";
    private static final String SORT_DIR = "SORT_DIRECTION";
    private static final String CARD_TYPE = "CARD_TYPE";

    /**
     * Static instance.
     */
    private static LibraryPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences prefs;

    // No public construction allowed.
    private LibraryPrefs() {
        // Get shared preferences.
        this.prefs = Minerva.getAppCtx().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get an instance.
     * @return {@link LibraryPrefs}.
     */
    public static LibraryPrefs get() {
        if (INSTANCE == null) INSTANCE = new LibraryPrefs();
        return INSTANCE;
    }

    /**
     * Get sort type string.
     * @param defValue The default value to return if nothing is set.
     * @return Sort type string.
     */
    public String getSortType(String defValue) {
        return prefs.getString(SORT_TYPE, defValue);
    }

    /**
     * Put sort type string.
     * @param sortType Sort type string.
     */
    public void putSortType(String sortType) {
        prefs.edit().putString(SORT_TYPE, sortType).apply();
    }

    /**
     * Get sort direction string.
     * @param defValue The default value to return if nothing is set.
     * @return Sort direction string.
     */
    public String getSortDir(String defValue) {
        return prefs.getString(SORT_DIR, defValue);
    }

    /**
     * Put sort direction string.
     * @param sortDir Sort direction string.
     */
    public void putSortDir(String sortDir) {
        prefs.edit().putString(SORT_DIR, sortDir).apply();
    }

    /**
     * Get card type string.
     * @param defValue The default value to return if nothing is set.
     * @return Card type string.
     */
    public String getCardType(String defValue) {
        return prefs.getString(CARD_TYPE, defValue);
    }

    /**
     * Put card type string.
     * @param cardType Card type string.
     */
    public void putCardType(String cardType) {
        prefs.edit().putString(CARD_TYPE, cardType).apply();
    }

    /**
     * Put all library view options in one call.
     * @param sortType Sort type string.
     * @param sortDir  Sort direction string.
     * @param cardType Card type string.
     */
    public void putLibraryViewOpts(String sortType, String sortDir, String cardType) {
        prefs.edit()
             .putString(SORT_TYPE, sortType)
             .putString(SORT_DIR, sortDir)
             .putString(CARD_TYPE, cardType)
             .apply();
    }
}