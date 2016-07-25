package com.bkromhout.minerva.ui.transitions;

import android.animation.Animator;
import android.content.Context;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import com.bkromhout.minerva.ui.UiUtils;

/**
 * Custom content transition which uses a circular reveal animation to display an activity's contents.
 * <p>
 * Adapted from https://halfthought.wordpress.com/2014/11/07/reveal-transition/.
 */
public class CircularReveal extends Visibility {
    private int centerX;
    private int centerY;

    public CircularReveal(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public CircularReveal(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.centerX = -1;
        this.centerY = -1;
    }

    /**
     * Set the coordinates of the center point from which (or to which) the circular reveal animation will animate from
     * (or to).
     * @param centerX X location to to as center for circular reveal.
     * @param centerY Y location to to as center for circular reveal.
     */
    public void setCenter(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, final View view, TransitionValues startValues,
                             TransitionValues endValues) {
        return createAnimator(view, 0f, Math.max(view.getWidth(), view.getHeight()));
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                                TransitionValues endValues) {
        return createAnimator(view, Math.max(view.getWidth(), view.getHeight()), 0f);
    }

    private Animator createAnimator(View view, float startRadius, float endRadius) {
        int centerX = this.centerX != -1 ? this.centerX : view.getWidth() / 2;
        int centerY = this.centerY != -1 ? this.centerY : view.getWidth() / 2;

        Animator reveal = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
        return new UiUtils.NoPauseAnimator(reveal);
    }
}
