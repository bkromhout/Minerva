package com.bkromhout.minerva.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;

/**
 * Preferences class for general/miscellaneous preferences.
 */
public class DefaultPrefs {
    // Key strings.
    private final static String CURR_FRAG = "CURR_FRAG";
    private final static String CURR_LIST_SEL = "CURR_LIST_UNIQUE_SEL";

    public final static String LIB_DIR = Minerva.getAppCtx().getString(R.string.key_lib_dir);
    private final static String LIB_AUTO_IMPORT = Minerva.getAppCtx().getString(R.string.key_auto_import);
    private final static String LAST_IMPORT_SUCCESS_TIME = "LAST_IMPORT_SUCCESS_TIME";
    private final static String FIRST_IMPORT_TRIGGERED = "FIRST_IMPORT_TRIGGERED";

    /**
     * Static instance.
     */
    private static DefaultPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences prefs;

    // No public construction allowed.
    private DefaultPrefs() {
        // Get shared preferences.
        this.prefs = PreferenceManager.getDefaultSharedPreferences(Minerva.getAppCtx());

    }

    /**
     * Get an instance.
     * @return {@link DefaultPrefs}.
     */
    public static DefaultPrefs get() {
        if (INSTANCE == null) INSTANCE = new DefaultPrefs();
        return INSTANCE;
    }

    /**
     * Get the int which specifies the fragment that is currently being shown in {@link
     * com.bkromhout.minerva.MainActivity}.
     * @param defValue The default value to return if nothing is set.
     * @return Frag int.
     */
    public int getCurrFrag(int defValue) {
        return prefs.getInt(CURR_FRAG, defValue);
    }

    /**
     * Put the int which specifies the fragment that is currently being shown in {@link
     * com.bkromhout.minerva.MainActivity}.
     * @param currFrag Frag int.
     */
    public void putCurrFrag(int currFrag) {
        prefs.edit().putInt(CURR_FRAG, currFrag).apply();
    }

    /**
     * Get the a string which uniquely identifies the list that was last shown in the list fragment.
     * @param defValue The default value to return if nothing is set.
     * @return The list selector string.
     */
    public String getCurrListSel(String defValue) {
        return prefs.getString(CURR_LIST_SEL, defValue);
    }

    /**
     * Put the a string which uniquely identifies the list that was last shown in the list fragment.
     * @param currListSel Unique list selector string.
     */
    public void putCurrListSel(String currListSel) {
        prefs.edit().putString(CURR_LIST_SEL, currListSel).apply();
    }

    /**
     * Check whether or not the first time import has been triggered yet.
     * @return True if the first time import has been triggered, otherwise false.
     */
    public boolean hasFirstImportBeenTriggered() {
        return prefs.getBoolean(FIRST_IMPORT_TRIGGERED, false);
    }

    /**
     * Record that the first time import has been triggered.
     */
    public void setFirstImportTriggered() {
        prefs.edit().putBoolean(FIRST_IMPORT_TRIGGERED, true).apply();
    }

    /**
     * Get the library directory path.
     * @param defValue The default value to return if nothing is set.
     * @return Library directory path.
     */
    public String getLibDir(String defValue) {
        return prefs.getString(LIB_DIR, defValue);
    }

    /**
     * Put the library directory.
     * @param libDir Library directory path.
     */
    public void putLibDir(String libDir) {
        prefs.edit().putString(LIB_DIR, libDir).apply();
    }

    /**
     * Get the boolean telling us whether or not auto-import is turned on.
     * @param defValue The default value to return if nothing is set.
     * @return Auto-import boolean.
     */
    public boolean getLibAutoImport(boolean defValue) {
        return prefs.getBoolean(LIB_AUTO_IMPORT, defValue);
    }

    /**
     * Put the library auto-import boolean.
     * @param libAutoImport Auto-import boolean.
     */
    public void putLibAutoImport(boolean libAutoImport) {
        prefs.edit().putBoolean(LIB_AUTO_IMPORT, libAutoImport).apply();
    }

    /**
     * Get the last time an import run completed successfully.
     * @param defValue The default value to return if nothing is set.
     * @return Last import success time.
     */
    public long getLastImportSuccessTime(long defValue) {
        return prefs.getLong(LAST_IMPORT_SUCCESS_TIME, defValue);
    }

    /**
     * Set the last time an import run completed successfully.
     * @param lastImportSuccessTime Last import success time.
     */
    public void putLastImportSuccessTime(long lastImportSuccessTime) {
        prefs.edit().putLong(LAST_IMPORT_SUCCESS_TIME, lastImportSuccessTime).apply();
    }
}
