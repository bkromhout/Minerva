package com.bkp.minerva.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.bkp.minerva.Minerva;

/**
 * Preferences class for {@link com.bkp.minerva.MainActivity}.
 */
public class MainPrefs {
    // File name.
    private static final String FILE_NAME = "main";
    // Key strings.
    private final static String CURR_FRAG = "CURR_FRAG";
    private final static String CURR_LIST_SEL = "CURR_LIST_UNIQUE_SEL";

    /**
     * Static instance.
     */
    private static MainPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences mPrefs;

    // No public construction allowed.
    private MainPrefs() {
        // Get shared preferences.
        this.mPrefs = Minerva.getAppCtx().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get an instance.
     * @return {@link MainPrefs}.
     */
    public static MainPrefs get() {
        if (INSTANCE == null) INSTANCE = new MainPrefs();
        return INSTANCE;
    }

    /**
     * Get the int which specifies the fragment that is currently being shown in {@link com.bkp.minerva.MainActivity}.
     * @param defValue The default value to return if nothing is set.
     * @return Frag int.
     */
    public int getCurrFrag(int defValue) {
        return mPrefs.getInt(CURR_FRAG, defValue);
    }

    /**
     * Put the int which specifies the fragment that is currently being shown in {@link com.bkp.minerva.MainActivity}.
     * @param currFrag Frag int.
     */
    public void putCurrFrag(int currFrag) {
        mPrefs.edit().putInt(CURR_FRAG, currFrag).apply();
    }

    /**
     * Get the a string which uniquely identifies the list that was last shown in the list fragment.
     * @param defValue The default value to return if nothing is set.
     * @return The list selector string.
     */
    public String getCurrListSel(String defValue) {
        return mPrefs.getString(CURR_LIST_SEL, defValue);
    }

    /**
     * Put the a string which uniquely identifies the list that was last shown in the list fragment.
     * @param currListSel Unique list selector string.
     */
    public void putCurrListSel(String currListSel) {
        mPrefs.edit().putString(CURR_LIST_SEL, currListSel).apply();
    }
}
