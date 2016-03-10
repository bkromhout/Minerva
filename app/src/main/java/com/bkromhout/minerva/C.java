package com.bkromhout.minerva;

import android.support.annotation.StringRes;
import com.bkromhout.minerva.realm.RBook;
import com.google.common.collect.ImmutableList;
import io.realm.Sort;

import java.util.List;

/**
 * Constants.
 */
public final class C {
    /**
     * The initial number of position numbers between each item in a book list.
     */
    public static final long LIST_ITEM_GAP = 100L;
    /**
     * Valid file extensions.
     */
    public static final List<String> VALID_EXTS = ImmutableList.of("epub");

    /* Sort types. */
    public static final String SORT_TITLE = "SORT_TITLE";
    public static final String SORT_AUTHOR = "SORT_AUTHOR";
    public static final String SORT_TIME_ADDED = "SORT_TIME_ADDED";
    public static final String SORT_RATING = "SORT_RATING";

    /* Sort directions. */
    public static final String SORT_ASC = "ASC";
    public static final String SORT_DESC = "DESC";

    /* Book card types. */
    // TODO make into an enum class!!
    public static final String BOOK_CARD_NORMAL = "BOOK_CARD_NORMAL";
    public static final String BOOK_CARD_NO_COVER = "BOOK_CARD_NO_COVER";
    public static final String BOOK_CARD_COMPACT = "BOOK_CARD_COMPACT";

    /* Request codes. */
    public static final int RC_TAG_ACTIVITY = 1;

    /**
     * Get a string resource using the application context.
     * @param resId String resource ID.
     * @return String.
     */
    public static String getStr(@StringRes int resId) {
        return Minerva.getAppCtx().getString(resId);
    }

    /**
     * Get a formatted string resource using the application context.
     * @param resId      String resource ID.
     * @param formatArgs Format arguments.
     * @return Formatted string.
     */
    public static String getStr(@StringRes int resId, Object... formatArgs) {
        return Minerva.getAppCtx().getString(resId, formatArgs);
    }

    /**
     * Get the name of the {@link RBook} field to use to sort the data based on the given string constant.
     * @param sortType String constant that represents a sort type.
     * @return Field name for the given sort type.
     */
    public static String getRealmSortField(String sortType) {
        switch (sortType) {
            case SORT_TITLE:
                return "title";
            case SORT_AUTHOR:
                return "author";
            case SORT_TIME_ADDED:
                return "lastImportDate";
            case SORT_RATING:
                return "rating";
            default:
                throw new IllegalArgumentException(String.format("Invalid sort type: %s", sortType));
        }
    }

    /**
     * Get the Realm {@link Sort} enum value based on the given sort direction string constant.
     * @param sortDir Sort direction string constant.
     * @return Sort direction enum.
     */
    public static Sort getRealmSortDir(String sortDir) {
        if (SORT_ASC.equals(sortDir)) return Sort.ASCENDING;
        else if (SORT_DESC.equals(sortDir)) return Sort.DESCENDING;
        else throw new IllegalArgumentException(String.format("Invalid sort direction: %s", sortDir));
    }
}
