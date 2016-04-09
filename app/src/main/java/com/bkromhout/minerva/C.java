package com.bkromhout.minerva;

import android.content.res.ColorStateList;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Constants.
 */
public final class C {
    /* Keys */
    public static final String IS_IN_ACTION_MODE = "is_in_action_mode";
    public static final String RUQ = "realm_user_query";

    /**
     * The initial number of position numbers between each item in a book list.
     */
    public static final long LIST_ITEM_GAP = 100L;
    /**
     * Valid file extensions.
     */
    public static final List<String> VALID_EXTS = ImmutableList.of("epub");
    /**
     * Color selector for card view backgrounds.
     */
    public static final ColorStateList CARD_BG_COLORS = ContextCompat.getColorStateList(Minerva.getAppCtx(),
            R.color.card_bg_color);

    /* Request codes. */
    public static final int RC_TAG_ACTIVITY = 1;
    public static final int RC_QUERY_BUILDER_ACTIVITY = 2;

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
}
