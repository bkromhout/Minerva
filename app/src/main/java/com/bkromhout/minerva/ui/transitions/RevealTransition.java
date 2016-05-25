package com.bkromhout.minerva.ui.transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import com.bkromhout.minerva.ui.AnimUtils;

public class RevealTransition extends Visibility {
    private final float xOrigin;
    private final float yOrigin;

    public RevealTransition(float xOrigin, float yOrigin) {
        this.xOrigin = xOrigin;
        this.yOrigin = yOrigin;
    }

    public RevealTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.xOrigin = -1f;
        this.yOrigin = -1f;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, final View view, TransitionValues startValues,
                             TransitionValues endValues) {
        float radius = calculateMaxRadius(view);
        final float originalAlpha = view.getAlpha();
        //view.setAlpha(0f);

        Animator reveal = createAnimator(view, 0, radius);
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setAlpha(originalAlpha);
            }
        });
        return reveal;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                                TransitionValues endValues) {
        float radius = calculateMaxRadius(view);
        return createAnimator(view, radius, 0);
    }

    private Animator createAnimator(View view, float startRadius, float endRadius) {
        int centerX = view.getWidth() / 2;
        int centerY = view.getHeight() / 2;

        Animator reveal = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
        return new AnimUtils.NoPauseAnimator(reveal);
    }

    static float calculateMaxRadius(View view) {
        float widthSquared = view.getWidth() * view.getWidth();
        float heightSquared = view.getHeight() * view.getHeight();
        return (float) (Math.sqrt(widthSquared + heightSquared) / 2);
    }
}
