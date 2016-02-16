package com.bkp.minerva;

import android.support.annotation.StringRes;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Constants.
 */
public final class C {
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

    // Sort types.
    public static final String SORT_TITLE = "SORT_TITLE";
    public static final String SORT_AUTHOR = "SORT_AUTHOR";
    public static final String SORT_TIME_ADDED = "SORT_TIME_ADDED";
    public static final String SORT_RATING = "SORT_RATING";

    // Sort directions.
    public static final String SORT_ASC = "ASC";
    public static final String SORT_DESC = "DESC";

    // Book card types.
    public static final String CARD_NORMAL = "CARD_NORMAL";
    public static final String CARD_NO_COVER = "CARD_NO_COVER";
    public static final String CARD_COMPACT = "CARD_COMPACT";

    // Valid file extensions.
    public static final List<String> VALID_EXTS = ImmutableList.of("epub");

}
