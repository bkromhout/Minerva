package com.bkromhout.minerva.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.prefs.interfaces.BCTPref;

/**
 * Preferences class for list-related preferences.
 */
public class ListsPrefs implements BCTPref {
    // File name.
    private static final String FILE_NAME = "all_lists";
    // Key Strings.

    /**
     * Static instance.
     */
    private static ListsPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences prefs;

    // No public construction allowed.
    private ListsPrefs() {
        // Get shared preferences.
        this.prefs = Minerva.getAppCtx().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get an instance.
     * @return {@link ListsPrefs}.
     */
    public static ListsPrefs get() {
        if (INSTANCE == null) INSTANCE = new ListsPrefs();
        return INSTANCE;
    }

    @Override
    public BookCardType getCardType(BookCardType defValue) {
        BookCardType type = BookCardType.fromName(prefs.getString(CARD_TYPE, null));
        return type != null ? type : defValue;
    }

    @Override
    public void putCardType(BookCardType cardType) {
        prefs.edit().putString(CARD_TYPE, cardType.name()).apply();
    }
}
