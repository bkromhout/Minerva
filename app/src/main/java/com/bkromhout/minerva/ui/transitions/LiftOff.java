package com.bkromhout.minerva.ui.transitions;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.bkromhout.minerva.R;

/**
 * A transition that animates the elevation of a View from a given value down to zero.
 * <p>
 * Useful for creating parent↔child navigation transitions (https://www.google
 * .com/design/spec/patterns/navigational-transitions.html#navigational-transitions-parent-to-child)
 * when combined with a {@link android.transition.ChangeBounds} on a shared element.
 * <p>
 * Adapted from https://github.com/nickbutcher/plaid.
 */
public class LiftOff extends Transition {

    private static final String PROPNAME_ELEVATION = "minerva:liftoff:elevation";

    private static final String[] transitionProperties = {
            PROPNAME_ELEVATION
    };

    private final float lift;

    public LiftOff(float lift) {
        this.lift = lift;
    }

    public LiftOff(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LiftOff);
        lift = ta.getDimensionPixelSize(R.styleable.LiftOff_android_elevation, 0);
        ta.recycle();
    }

    @Override
    public String[] getTransitionProperties() {
        return transitionProperties;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        transitionValues.values.put(PROPNAME_ELEVATION, 0f);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        transitionValues.values.put(PROPNAME_ELEVATION, lift);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
                                   TransitionValues endValues) {
        return ObjectAnimator.ofFloat(endValues.view, View.TRANSLATION_Z, lift, 0f);
    }

}
