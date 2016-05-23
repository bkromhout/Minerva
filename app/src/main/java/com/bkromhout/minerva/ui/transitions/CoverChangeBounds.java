package com.bkromhout.minerva.ui.transitions;

import android.content.Context;
import android.graphics.Rect;
import android.transition.ChangeBounds;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import com.bkromhout.minerva.R;

/**
 * ChangeBounds fails to give our cover ImageView in BookInfoActivity the correct right and bottom values, so we use
 * this transition to directly copy them from its parent view since they should match that.
 */
public class CoverChangeBounds extends ChangeBounds {
    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";

    public CoverChangeBounds(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        if (transitionValues.view.getId() != R.id.cover_image) return;
        // Get right and bottom from parent.
        View parent = (View) transitionValues.values.get(PROPNAME_PARENT);
        if (parent.getId() != R.id.collapsing_toolbar) return;
        int width = parent.getWidth();
        int bottom = parent.getHeight();
        // Assign those values as the new end values.
        Rect bounds = (Rect) transitionValues.values.get(PROPNAME_BOUNDS);
        bounds.right = width;
        bounds.bottom = bottom;
        transitionValues.values.put(PROPNAME_BOUNDS, bounds);
    }
}
