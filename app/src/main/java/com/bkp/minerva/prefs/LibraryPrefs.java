package com.bkp.minerva.prefs;

import net.orange_box.storebox.annotations.method.KeyByString;
import net.orange_box.storebox.annotations.type.FilePreferences;

/**
 * Library fragment's SharedPreferences.
 */
@FilePreferences("LibraryFragPrefs")
public interface LibraryPrefs {
    // Key strings.
    String SORT_TYPE = "SORT_TYPE";
    String SORT_DIR = "SORT_DIRECTION";
    String CARD_TYPE = "CARD_TYPE";

    /**
     * Get the sort type.
     * @param defValue The value to return if not set.
     * @return The sort type.
     */
    @KeyByString(SORT_TYPE)
    String getSortType(String defValue);

    /**
     * Put the sort type.
     * @param sortType Sort type.
     */
    @KeyByString(SORT_TYPE)
    LibraryPrefs putSortType(String sortType);

    /**
     * Get the sort direction.
     * @param defValue The value to return if not set.
     * @return The sort direction.
     */
    @KeyByString(SORT_DIR)
    String getSortDir(String defValue);

    /**
     * Put the sort direction.
     * @param sortDir Sort direction.
     */
    @KeyByString(SORT_DIR)
    LibraryPrefs putSortDir(String sortDir);

    /**
     * Get the card type.
     * @param defValue The value to return if not set.
     * @return The card type.
     */
    @KeyByString(CARD_TYPE)
    String getCardType(String defValue);

    /**
     * Put the card type.
     * @param cardType Card type.
     */
    @KeyByString(CARD_TYPE)
    LibraryPrefs putCardType(String cardType);
}
