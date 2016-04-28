package com.bkromhout.minerva;

import android.content.res.ColorStateList;
import android.support.annotation.DimenRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Constants.
 */
public final class C {
    /* Keys */
    public static final String POS_TO_UPDATE = "position_to_update";
    public static final String NEEDS_POS_UPDATE = "needs_position_update";
    public static final String REL_PATH = "relative_path";
    public static final String IS_IN_ACTION_MODE = "is_in_action_mode";
    public static final String RUQ = "realm_user_query";

    /* Request codes. */
    public static final int RC_TAG_ACTIVITY = 1;
    public static final int RC_QUERY_BUILDER_ACTIVITY = 2;


    public static final String TAG_SEP = "\u200B\u2002\u200B";
    public static final int TAG_SEP_LEN = TAG_SEP.length();
    /**
     * This is the item type integer we supply to a recycler view for our empty footer item.
     */
    public static final int FOOTER_ITEM_TYPE = -1;
    /**
     * The initial number of position numbers between each item in a book list.
     */
    public static final long LIST_ITEM_GAP = 100L;
    /**
     * Valid file extensions.
     */
    public static final List<String> VALID_EXTENSIONS = ImmutableList.of("epub");
    /**
     * Color selector for card view backgrounds.
     */
    public static final ColorStateList CARD_BG_COLORS = ContextCompat.getColorStateList(Minerva.getAppCtx(),
            R.color.card_bg_color);

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
     * Get a dimension resource using the application context.
     * @param resId Dimension resource ID.
     * @return Dimension value from {@link android.content.res.Resources#getDimension(int)}.
     */
    public static float getDimen(@DimenRes int resId) {
        return Minerva.getAppCtx().getResources().getDimension(resId);
    }
}
