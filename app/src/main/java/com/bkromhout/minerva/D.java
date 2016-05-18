package com.bkromhout.minerva;

import android.app.Application;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.view.View;

/**
 * Similar to {@link C}, but for constants which we have to get from resources at runtime.
 */
public class D {
    /**
     * Default tag text color.
     */
    @ColorInt
    public int DEFAULT_TAG_TEXT_COLOR;
    /**
     * Default tag background color.
     */
    @ColorInt
    public int DEFAULT_TAG_BG_COLOR;
    /**
     * How much padding to use on the bottom of the tag.
     */
    public float TAG_BOTTOM_PADDING;
    /**
     * Radius to use for rounded tag corners.
     */
    public float TAG_CORNER_RADIUS;
    /**
     * Corner radii values for all corners.
     */
    public float[] ALL_CORNERS;
    /**
     * Corner radii values for start corners only.
     */
    public float[] START_CORNERS_ONLY;
    /**
     * Corner radii values for end corners only.
     */
    public float[] END_CORNERS_ONLY;

    /**
     * Create a new {@link D}.
     * @param application Application context to use.
     */
    D(Application application) {
        Resources resources = application.getResources();

        DEFAULT_TAG_TEXT_COLOR = ContextCompat.getColor(application, R.color.grey200);
        DEFAULT_TAG_BG_COLOR = ContextCompat.getColor(application, R.color.grey700);
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
    }
}
