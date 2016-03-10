package com.bkromhout.minerva.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.bkromhout.minerva.Minerva;

/**
 * Preferences class for {@link com.bkromhout.minerva.fragments.RecentFragment}.
 */
public class RecentPrefs {
    // File name.
    private static final String FILE_NAME = "recent";
    // Key Strings.
    private static final String CARD_TYPE = "CARD_TYPE";

    /**
     * Static instance.
     */
    private static RecentPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences prefs;

    // No public construction allowed.
    private RecentPrefs() {
        // Get shared preferences.
        this.prefs = Minerva.getAppCtx().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get an instance.
     * @return {@link RecentPrefs}.
     */
    public static RecentPrefs get() {
        if (INSTANCE == null) INSTANCE = new RecentPrefs();
        return INSTANCE;
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
}