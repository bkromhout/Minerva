package com.bkromhout.minerva.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.prefs.interfaces.BCTPref;

/**
 * Preferences class for {@link com.bkromhout.minerva.fragments.PowerSearchFragment}.
 */
public class PowerSearchPrefs implements BCTPref {
    // File name.
    private static final String FILE_NAME = "power_search";
    // Key Strings.

    /**
     * Static instance.
     */
    private static PowerSearchPrefs INSTANCE = null;
    /**
     * Shared Preferences.
     */
    private SharedPreferences prefs;

    // No public construction allowed.
    private PowerSearchPrefs() {
        // Get shared preferences.
        this.prefs = Minerva.getAppCtx().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get an instance.
     * @return {@link PowerSearchPrefs}.
     */
    public static PowerSearchPrefs get() {
        if (INSTANCE == null) INSTANCE = new PowerSearchPrefs();
        return INSTANCE;
    }

    @Override
    public BookCardType getCardType(BookCardType defValue) {
        BookCardType type = BookCardType.fromName(prefs.getString(CARD_TYPE, null));
        return type != null ? type : defValue;
    }

    @Override
    public void putCardType(BookCardType cardType) {
        prefs.edit().putString(CARD_TYPE, cardType.getName()).apply();
    }
}