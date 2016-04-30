package com.bkromhout.minerva;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Constants.
 */
public final class C {
    /**
     * URL of Minerva's GitHub repository.
     */
    public static final String GITHUB_REPO = "https://github.com/bkromhout/Minerva";
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

    /* Keys */
    public static final String POS_TO_UPDATE = "position_to_update";
    public static final String NEEDS_POS_UPDATE = "needs_position_update";
    public static final String REL_PATH = "relative_path";
    public static final String IS_IN_ACTION_MODE = "is_in_action_mode";
    public static final String RUQ = "realm_user_query";

    /* Request codes. */
    public static final int RC_TAG_ACTIVITY = 1;
    public static final int RC_QUERY_BUILDER_ACTIVITY = 2;

    /*
     * Variables below this cannot be used until init() is called because they must be loaded from resources at
     * runtime. They are kept in memory for fast access.
     */
    /**
     * Whether {@link #init(Minerva)} has been called yet.
     */
    private static boolean initDone = false;
    /**
     * Default tag text color.
     */
    @ColorInt
    public static int DEFAULT_TAG_TEXT_COLOR;
    /**
     * Default tag background color.
     */
    @ColorInt
    public static int DEFAULT_TAG_BG_COLOR;
    /**
     * How much padding to use on the bottom of the tag.
     */
    public static float TAG_BOTTOM_PADDING;
    /**
     * Radius to use for rounded tag corners.
     */
    public static float TAG_CORNER_RADIUS;
    /**
     * Corner radii values for all corners.
     */
    public static float[] ALL_CORNERS;
    /**
     * Corner radii values for start corners only.
     */
    public static float[] START_CORNERS_ONLY;
    /**
     * Corner radii values for end corners only.
     */
    public static float[] END_CORNERS_ONLY;

    /**
     * Initialize static vars which we must load from resources.
     * @param appCtx Application context.
     */
    public static void init(Minerva appCtx) {
        if (initDone) return;
        Resources resources = appCtx.getResources();

        DEFAULT_TAG_TEXT_COLOR = ContextCompat.getColor(appCtx, R.color.grey200);
        DEFAULT_TAG_BG_COLOR = ContextCompat.getColor(appCtx, R.color.grey700);
        TAG_BOTTOM_PADDING = resources.getDimension(R.dimen.tag_bottom_padding);
        TAG_CORNER_RADIUS = resources.getDimension(R.dimen.tag_corner_radius);
        ALL_CORNERS = new float[] {TAG_CORNER_RADIUS, TAG_CORNER_RADIUS,  // Top left.
                                   TAG_CORNER_RADIUS, TAG_CORNER_RADIUS,  // Top right.
                                   TAG_CORNER_RADIUS, TAG_CORNER_RADIUS,  // Bottom right.
                                   TAG_CORNER_RADIUS, TAG_CORNER_RADIUS}; // Bottom left.
        float[] leftCornersOnly = new float[] {TAG_CORNER_RADIUS, TAG_CORNER_RADIUS,
                                               -1f, -1f,
                                               -1f, -1f,
                                               TAG_CORNER_RADIUS, TAG_CORNER_RADIUS};
        float[] rightCornersOnly = new float[] {0f, 0f,
                                                TAG_CORNER_RADIUS, TAG_CORNER_RADIUS,
                                                TAG_CORNER_RADIUS, TAG_CORNER_RADIUS,
                                                0f, 0f};

        boolean isLtr = resources.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
        START_CORNERS_ONLY = isLtr ? leftCornersOnly : rightCornersOnly;
        END_CORNERS_ONLY = isLtr ? rightCornersOnly : leftCornersOnly;

        initDone = true;
    }

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
