package com.bkromhout.minerva.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.text.style.ReplacementSpan;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;

/**
 * Custom background span for book tags.
 */
public class TagSpan extends ReplacementSpan {
    /**
     * How much padding to use on either side of the tags.
     */
    private static float sidePadding = -1f;
    /**
     * How much padding to use on the bottom of the tag.
     */
    private static float bottomPadding = -1f;
    /**
     * Radius to use for rounded tag corners.
     */
    private static float cornerRadius = -1f;

    static {
        Resources resources = Minerva.getAppCtx().getResources();
        if (sidePadding == -1f) sidePadding = resources.getDimension(R.dimen.tag_side_padding);
        if (bottomPadding == -1f) bottomPadding = resources.getDimension(R.dimen.tag_bottom_padding);
        if (cornerRadius == -1f) cornerRadius = resources.getDimension(R.dimen.tag_corner_radius);
    }

    /**
     * Color to use for tag background.
     */
    @ColorInt
    private int bgColor;
    /**
     * Color to use for text.
     */
    @ColorInt
    private int textColor;
    /**
     * RectF to use as background.
     */
    private RectF rect;

    /**
     * Create a new {@link TagSpan}.
     * @param bgColor   Background color.
     * @param textColor Text color.
     */
    public TagSpan(@ColorRes int bgColor, @ColorRes int textColor) {
        Context context = Minerva.getAppCtx();
        this.bgColor = ContextCompat.getColor(context, bgColor);
        this.textColor = ContextCompat.getColor(context, textColor);
        this.rect = new RectF();
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + sidePadding * 2);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
                     Paint paint) {
        paint.setColor(bgColor);
        rect.set(x - cornerRadius + sidePadding, top,
                x + paint.measureText(text, start, end) + cornerRadius + sidePadding,
                y + paint.descent() + bottomPadding);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        paint.setColor(textColor);
        canvas.drawText(text, start, end, x + sidePadding, y - bottomPadding / 2, paint);
    }
}
