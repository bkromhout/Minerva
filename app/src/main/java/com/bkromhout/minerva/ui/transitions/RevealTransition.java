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
 * TODO Comment this
 */
public class RevealTransition extends Visibility {
    private float centerX;
    private float centerY;

    public RevealTransition(float centerX, float centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public RevealTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.centerX = -1f;
        this.centerY = -1f;
    }

    public void setCenter(float centerX, float centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, final View view, TransitionValues startValues,
                             TransitionValues endValues) {
        float radius = calculateMaxRadius(view);
        return createAnimator(view, 0, radius);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                                TransitionValues endValues) {
        float radius = calculateMaxRadius(view);
        return createAnimator(view, radius, 0);
    }

    private Animator createAnimator(View view, float startRadius, float endRadius) {
        int centerX = this.centerX != -1f ? (int) this.centerX : view.getWidth() / 2;
        int centerY = this.centerY != -1f ? (int) this.centerY : view.getWidth() / 2;

        Animator reveal = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
        return new UiUtils.NoPauseAnimator(reveal);
    }

    static float calculateMaxRadius(View view) {
        // TODO make smarter based on centerX and centerY.
        return Math.max(view.getWidth(), view.getHeight());
//        float widthSquared = view.getWidth() * view.getWidth();
//        float heightSquared = view.getHeight() * view.getHeight();
//        return (float) (Math.sqrt(widthSquared + heightSquared) / 2);
    }
}
