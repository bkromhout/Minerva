package com.bkp.minerva.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;

/**
 * Utility functions class.
 */
public class Util {

    /**
     * Mutates the given {@code drawable}, then tints it using {@link android.graphics.PorterDuff.Mode#SRC_IN} to the
     * color defined by the given {@code colorResId}.
     * @param drawable   Drawable to tint.
     * @param colorResId The resource ID for a color resource.
     */
    public static void tintDrawable(Context ctx, Drawable drawable, @ColorRes int colorResId) {
        int color = ContextCompat.getColor(ctx, colorResId);
        drawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    /**
     * Start the given activity from the given context, passing the given bundle of extras to it.
     * @param ctx      The context to start the activity from.
     * @param actClass The class of the activity to start.
     * @param params   The bundle of extras to pass to the activity.
     */
    public static void startActWithBundle(Context ctx, Class<? super Activity> actClass, Bundle params) {
        Intent intent = new Intent(ctx, actClass);
        intent.putExtras(params);
        ctx.startActivity(intent);
    }
}
