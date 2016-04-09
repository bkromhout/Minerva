package com.bkromhout.minerva.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.enums.SortDir;
import com.bkromhout.minerva.enums.SortType;
import com.bkromhout.minerva.prefs.interfaces.BCTPref;

/**
 * Preferences class for {@link com.bkromhout.minerva.fragments.LibraryFragment}.
 */
public class LibraryPrefs implements BCTPref {
    // File name.
    private static final String FILE_NAME = "library";
    // Key strings.
    private static final String SORT_TYPE = "SORT_TYPE";
    private static final String SORT_DIR = "SORT_DIRECTION";

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
     * Get sort type.
     * @param defValue The default value to return if nothing is set.
     * @return Sort type.
     */
    public SortType getSortType(SortType defValue) {
        SortType sortType = SortType.fromName(prefs.getString(SORT_TYPE, null));
        return sortType != null ? sortType : defValue;
    }

    /**
     * Put sort type.
     * @param sortType Sort type.
     */
    public void putSortType(SortType sortType) {
        prefs.edit().putString(SORT_TYPE, sortType.getName()).apply();
    }

    /**
     * Get sort direction.
     * @param defValue The default value to return if nothing is set.
     * @return Sort direction.
     */
    public SortDir getSortDir(SortDir defValue) {
        SortDir sortDir = SortDir.fromName(prefs.getString(SORT_DIR, null));
        return sortDir != null ? sortDir : defValue;
    }

    /**
     * Put sort direction.
     * @param sortDir Sort direction.
     */
    public void putSortDir(SortDir sortDir) {
        prefs.edit().putString(SORT_DIR, sortDir.getName()).apply();
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

    /**
     * Put all library view options in one call.
     * @param sortType Sort type.
     * @param sortDir  Sort direction.
     * @param cardType Card type.
     */
    public void putLibraryViewOpts(SortType sortType, SortDir sortDir, BookCardType cardType) {
        prefs.edit()
             .putString(SORT_TYPE, sortType.getName())
             .putString(SORT_DIR, sortDir.getName())
             .putString(CARD_TYPE, cardType.getName())
             .apply();
    }
}
