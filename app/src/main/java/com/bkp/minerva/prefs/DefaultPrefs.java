package com.bkp.minerva.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.bkp.minerva.Minerva;

/**
 * Preferences class for {@link com.bkp.minerva.MainActivity}.
 */
public class DefaultPrefs {
    // Key strings.
    private final static String CURR_FRAG = "CURR_FRAG";
    private final static String CURR_LIST_SEL = "CURR_LIST_UNIQUE_SEL";

    private final static String LIB_DIR = "LIB_DIR_";
    private final static String LIB_AUTO_IMPORT = "LIB_AUTO_IMPORT_";

    /**
     * Static instance.
     */
    private static DefaultPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences mPrefs;
    /**
     * Which library is currently being used.
     * <p>
     * TODO later we'll manipulate this.
     */
    private static String currLibNum = "1";

    // No public construction allowed.
    private DefaultPrefs() {
        // Get shared preferences.
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(Minerva.getAppCtx());
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

    /**
     * Get the library directory path.
     * @param defValue The default value to return if nothing is set.
     * @return Library directory path.
     */
    public String getLibDir(String defValue) {
        return mPrefs.getString(LIB_DIR + currLibNum, defValue);
    }

    /**
     * Put the library directory.
     * @param libDir Library directory path.
     */
    public void putLibDir(String libDir) {
        mPrefs.edit().putString(LIB_DIR + currLibNum, libDir).apply();
    }

    /**
     * Get the boolean telling us whether or not auto-import is turned on.
     * @param defValue The default value to return if nothing is set.
     * @return Auto-import boolean.
     */
    public boolean getLibAutoImport(boolean defValue) {
        return mPrefs.getBoolean(LIB_AUTO_IMPORT + currLibNum, defValue);
    }

    /**
     * Put the library auto-import boolean.
     * @param libAutoImport Auto-import boolean.
     */
    public void putLibAutoImport(boolean libAutoImport) {
        mPrefs.edit().putBoolean(LIB_AUTO_IMPORT + currLibNum, libAutoImport).apply();
    }
}
