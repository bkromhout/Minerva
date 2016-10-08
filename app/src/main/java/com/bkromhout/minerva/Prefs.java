package com.bkromhout.minerva;

import android.content.SharedPreferences;
import com.bkromhout.minerva.activities.MainActivity;
import com.bkromhout.minerva.enums.BookCardType;
import com.bkromhout.minerva.enums.MainFrag;
import com.bkromhout.minerva.enums.SortDir;
import com.bkromhout.minerva.enums.SortType;

/**
 * Wrapper class for Shared Preferences.
 */
public class Prefs {
    /*
     * Key Strings.
     */
    // General
    private static final String FIRST_TIME_INIT = "first_time_init_done";
    private static final String INTRO_COMPLETED = "intro_completed";
    private static final String CURR_FRAG = "current_fragment";
    private static final String RATE_ME_COUNT_DOWN = "rate_me_count_down";
    // Settings
    public static final String LIB_DIR = Minerva.get().getString(R.string.key_lib_dir);
    private static final String DETECT_MOVED = Minerva.get().getString(R.string.key_detect_moved);
    public static final String DUPE_HANDLING = Minerva.get().getString(R.string.key_dupe_handling);
    private static final String LIB_AUTO_IMPORT = Minerva.get().getString(R.string.key_auto_import);
    public static final String NEW_BOOK_TAG = Minerva.get().getString(R.string.key_new_tag);
    public static final String UPDATED_BOOK_TAG = Minerva.get().getString(R.string.key_updated_tag);
    // Importing
    private static final String LAST_IMPORT_SUCCESS_TIME = "most_recent_import_success";
    private static final String FIRST_IMPORT_TRIGGERED = "first_import_triggered";
    // Recents
    public static final String RECENTS_CARD_TYPE = "recents_card_type";
    // Library
    private static final String LIBRARY_CARD_TYPE = "library_card_type";
    private static final String LIBRARY_SORT_DIR = "library_sort_dir";
    private static final String LIBRARY_SORT_TYPE = "library_sort_type";
    // All Lists/Lists
    public static final String LIST_CARD_TYPE = "list_card_type";
    // Power Search
    public static final String POWER_SEARCH_CARD_TYPE = "power_search_card_type";

    /*
     * Other Constants.
     */
    private static final int RATE_ME_INITIAL = 7;
    private static final int NEVER_RATE_ME = -1;

    /**
     * Shared Preferences.
     */
    private final SharedPreferences prefs;

    // Only Minerva should create an instance of this.
    Prefs(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /*
     * General
     */

    /**
     * Check whether we've done first time init.
     * @return True if we've done first time init, otherwise false.
     */
    boolean doneFirstTimeInit() {
        return prefs.getBoolean(FIRST_TIME_INIT, false);
    }

    /**
     * Mark first time init as done.
     */
    void setFirstTimeInitDone() {
        prefs.edit().putBoolean(FIRST_TIME_INIT, true).apply();
    }

    /**
     * Check whether the user has completed the intro.
     * @return True if the user has completed the intro, otherwise false.
     */
    public boolean introCompleted() {
        return prefs.getBoolean(INTRO_COMPLETED, false);
    }

    /**
     * Mark intro as completed.
     */
    public void setIntroCompleted() {
        prefs.edit().putBoolean(INTRO_COMPLETED, true).apply();
    }

    /**
     * Get the {@link MainFrag} which specifies the fragment that is currently being shown in {@link MainActivity}.
     * @param defValue The default value to return if nothing is set.
     * @return Enum.
     */
    public MainFrag getCurrFrag(MainFrag defValue) {
        MainFrag mainFrag = MainFrag.fromIndex(prefs.getInt(CURR_FRAG, -1));
        return mainFrag != null ? mainFrag : defValue;
    }

    /**
     * Put the {@link MainFrag} which specifies the fragment that is currently being shown in {@link MainActivity}.
     * @param currFrag Enum.
     */
    public void putCurrFrag(MainFrag currFrag) {
        prefs.edit().putInt(CURR_FRAG, currFrag.getIndex()).apply();
    }

    /**
     * Check to see if we should show a "Rate Minerva" dialog. If this returns false, it'll also decrement the current
     * countdown, which starts at {@link #RATE_ME_INITIAL}.
     * @return False if {@link #hasFirstImportBeenTriggered()} returns {@code false} or if the current value stored for
     * {@link #RATE_ME_COUNT_DOWN} isn't 0 (in which case it will be decremented. True otherwise.
     */
    boolean shouldShowRateMeDialog() {
        if (!hasFirstImportBeenTriggered()) return false;
        int countDown = prefs.getInt(RATE_ME_COUNT_DOWN, RATE_ME_INITIAL);
        if (countDown == 0) return true;
        prefs.edit().putInt(RATE_ME_COUNT_DOWN, countDown - 1).apply();
        return false;
    }

    /**
     * Reset the value stored for {@link #RATE_ME_COUNT_DOWN} back to {@link #RATE_ME_INITIAL}.
     */
    public void resetRateMeCountDown() {
        prefs.edit().putInt(RATE_ME_COUNT_DOWN, RATE_ME_INITIAL).apply();
    }

    /**
     * Set the value stored for {@link #RATE_ME_COUNT_DOWN} to {@link #NEVER_RATE_ME}.
     */
    public void setNeverShowRateMe() {
        prefs.edit().putInt(RATE_ME_COUNT_DOWN, NEVER_RATE_ME).apply();
    }

    /*
     * Settings
     */

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
     * Get the boolean telling us whether or not to try and detect moved files during import.
     * @param defValue The default value to return if nothing is set.
     * @return Detect moved boolean.
     */
    public boolean shouldDetectMoved(boolean defValue) {
        return prefs.getBoolean(DETECT_MOVED, defValue);
    }

    /**
     * Put the detect moved boolean.
     * @param detectMoved Detect moved boolean.
     */
    public void putDetectMoved(boolean detectMoved) {
        prefs.edit().putBoolean(DETECT_MOVED, detectMoved).apply();
    }

    /**
     * Get the boolean telling us whether or not auto-import is turned on.
     * @param defValue The default value to return if nothing is set.
     * @return Auto-import boolean.
     */
    boolean isLibAutoImport(boolean defValue) {
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
     * Get the name of the tag to tag new books with.
     * @param defValue The default value to return if nothing is set.
     * @return New book tag name.
     */
    public String getNewBookTag(String defValue) {
        return prefs.getString(NEW_BOOK_TAG, defValue);
    }

    /**
     * Put the name of the tag to tag new books with.
     * @param newBookTag New book tag name.
     */
    public void putNewBookTag(String newBookTag) {
        prefs.edit().putString(NEW_BOOK_TAG, newBookTag).apply();
    }

    /**
     * Get the name of the tag to tag updated books with.
     * @param defValue The default value to return if nothing is set.
     * @return Updated book tag name.
     */
    public String getUpdatedBookTag(String defValue) {
        return prefs.getString(UPDATED_BOOK_TAG, defValue);
    }

    /**
     * Put the name of the tag to tag updated books with.
     * @param updatedBookTag Updated book tag name.
     */
    public void putUpdatedBookTag(String updatedBookTag) {
        prefs.edit().putString(UPDATED_BOOK_TAG, updatedBookTag).apply();
    }

    /*
     * Importing
     */

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

    /*
     * Dynamic
     */

    /**
     * Get the card type for some fragment.
     * @param defValue  The default value to return if nothing is set.
     * @param whichFrag The fragment to get the preference for.
     * @return Card type.
     */
    public BookCardType getBookCardType(BookCardType defValue, MainFrag whichFrag) {
        switch (whichFrag) {
            case RECENT:
                return getRecentsCardType(defValue);
            case LIBRARY:
                return getLibraryCardType(defValue);
            case ALL_LISTS:
                return getListCardType(defValue);
            case POWER_SEARCH:
                return getPowerSearchCardType(defValue);
            default:
                throw new IllegalArgumentException("Invalid frag.");
        }
    }

    /**
     * Put the card type for some fragment.
     * @param cardType  Card type.
     * @param whichFrag The fragment to get the preference for.
     * @return The key string for the preference which was updated.
     */
    public String putBookCardType(BookCardType cardType, MainFrag whichFrag) {
        switch (whichFrag) {
            case RECENT:
                putRecentsCardType(cardType);
                return RECENTS_CARD_TYPE;
            case LIBRARY:
                putLibraryCardType(cardType);
                return LIBRARY_CARD_TYPE;
            case ALL_LISTS:
                putListCardType(cardType);
                return LIST_CARD_TYPE;
            case POWER_SEARCH:
                putPowerSearchCardType(cardType);
                return POWER_SEARCH_CARD_TYPE;
            default:
                throw new IllegalArgumentException("Invalid frag.");
        }
    }

    /*
     * Recents
     */

    /**
     * Get recents card type.
     * @param defValue The default value to return if nothing is set.
     * @return Card type.
     */
    public BookCardType getRecentsCardType(BookCardType defValue) {
        BookCardType type = BookCardType.fromNumber(prefs.getInt(RECENTS_CARD_TYPE, -1));
        return type != null ? type : defValue;
    }

    /**
     * Put recents card type.
     * @param cardType Card type.
     */
    private void putRecentsCardType(BookCardType cardType) {
        prefs.edit().putInt(RECENTS_CARD_TYPE, cardType.getNum()).apply();
    }

    /*
     * Library
     */

    /**
     * Get library card type.
     * @param defValue The default value to return if nothing is set.
     * @return Card type.
     */
    public BookCardType getLibraryCardType(BookCardType defValue) {
        BookCardType type = BookCardType.fromNumber(prefs.getInt(LIBRARY_CARD_TYPE, -1));
        return type != null ? type : defValue;
    }

    /**
     * Put library card type.
     * @param cardType Card type.
     */
    private void putLibraryCardType(BookCardType cardType) {
        prefs.edit().putInt(LIBRARY_CARD_TYPE, cardType.getNum()).apply();
    }

    /**
     * Get library sort type.
     * @param defValue The default value to return if nothing is set.
     * @return Sort type.
     */
    public SortType getLibrarySortType(SortType defValue) {
        SortType sortType = SortType.fromNumber(prefs.getInt(LIBRARY_SORT_TYPE, -1));
        return sortType != null ? sortType : defValue;
    }

    /**
     * Put library sort type.
     * @param sortType Sort type.
     */
    public void putLibrarySortType(SortType sortType) {
        prefs.edit().putInt(LIBRARY_SORT_TYPE, sortType.getNum()).apply();
    }

    /**
     * Get library sort direction.
     * @param defValue The default value to return if nothing is set.
     * @return Sort direction.
     */
    public SortDir getLibrarySortDir(SortDir defValue) {
        SortDir sortDir = SortDir.fromNumber(prefs.getInt(LIBRARY_SORT_DIR, -1));
        return sortDir != null ? sortDir : defValue;
    }

    /**
     * Put library sort direction.
     * @param sortDir Sort direction.
     */
    public void putLibrarySortDir(SortDir sortDir) {
        prefs.edit().putInt(LIBRARY_SORT_DIR, sortDir.getNum()).apply();
    }

    /**
     * Put all library view options in one call.
     * @param sortType Sort type.
     * @param sortDir  Sort direction.
     * @param cardType Card type.
     */
    public void putLibraryViewOpts(SortType sortType, SortDir sortDir, BookCardType cardType) {
        prefs.edit()
             .putInt(LIBRARY_SORT_TYPE, sortType.getNum())
             .putInt(LIBRARY_SORT_DIR, sortDir.getNum())
             .putInt(LIBRARY_CARD_TYPE, cardType.getNum())
             .apply();
    }

    /*
     * All Lists/Lists
     */

    /**
     * Get list card type.
     * @param defValue The default value to return if nothing is set.
     * @return Card type.
     */
    public BookCardType getListCardType(BookCardType defValue) {
        BookCardType type = BookCardType.fromNumber(prefs.getInt(LIST_CARD_TYPE, -1));
        return type != null ? type : defValue;
    }

    /**
     * Put list card type.
     * @param cardType Card type.
     */
    private void putListCardType(BookCardType cardType) {
        prefs.edit().putInt(LIST_CARD_TYPE, cardType.getNum()).apply();
    }

    /*
     * Power Search
     */

    /**
     * Get power search card type.
     * @param defValue The default value to return if nothing is set.
     * @return Card type.
     */
    public BookCardType getPowerSearchCardType(BookCardType defValue) {
        BookCardType type = BookCardType.fromNumber(prefs.getInt(POWER_SEARCH_CARD_TYPE, -1));
        return type != null ? type : defValue;
    }

    /**
     * Put power search card type.
     * @param cardType Card type.
     */
    private void putPowerSearchCardType(BookCardType cardType) {
        prefs.edit().putInt(POWER_SEARCH_CARD_TYPE, cardType.getNum()).apply();
    }
}
