package com.bkp.minerva.prefs;

import net.orange_box.storebox.annotations.method.KeyByString;
import net.orange_box.storebox.annotations.type.DefaultSharedPreferences;

/**
 * Convenience accessor class for the application's default SharedPreferences. Uses .apply() to save preferences by
 * default.
 */
@DefaultSharedPreferences
public interface DefaultPrefs {
    // Key strings.
    String CURR_FRAG = "CURR_FRAG";
    String CURR_LIST_SEL = "CURR_LIST_UNIQUE_SEL";

    /**
     * Get the int which specifies the fragment that is currently being shown in {@link com.bkp.minerva.MainActivity}.
     * @param defValue The default value to return if nothing is set.
     * @return Frag int, or {@code defValue} if not set.
     */
    @KeyByString(CURR_FRAG)
    int getCurrFrag(int defValue);

    /**
     * Put the int which specifies the fragment that is currently being shown in {@link com.bkp.minerva.MainActivity}.
     * @param currFrag Frag int.
     */
    @KeyByString(CURR_FRAG)
    DefaultPrefs putCurrFrag(int currFrag);

    /**
     * Get the a string which uniquely identifies the list that was last shown in the list fragment.
     * @param defValue The default value to return if nothing is set.
     * @return The list selector string.
     */
    @KeyByString(CURR_LIST_SEL)
    String getCurrListSel(String defValue);

    /**
     * Put the a string which uniquely identifies the list that was last shown in the list fragment.
     * @param currList Unique list selector string.
     */
    @KeyByString(CURR_LIST_SEL)
    DefaultPrefs putCurrListSel(String currList);
}
