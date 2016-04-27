package com.bkromhout.minerva.util;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.text.style.LineBackgroundSpan;
import com.bkromhout.minerva.C;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.R;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Draws backgrounds for our tags in a textview. We use this instead of a class which extends {@code ReplacementSpan}
 * because we want the multi-line behavior.
 */
public class TagBackgroundSpan implements LineBackgroundSpan {
    /**
     * Pattern to use to split tag strings.
     */
    private static final Pattern TAG_SEP_PATTERN = Pattern.compile("\\Q" + C.TAG_SEP + "\\E");
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
    /**
     * Extra space between lines.
     */
    private static float lineSpacingExtra = -1f;

    static {
        Resources resources = Minerva.getAppCtx().getResources();
        if (sidePadding == -1f) sidePadding = resources.getDimension(R.dimen.tag_side_padding);
        if (bottomPadding == -1f) bottomPadding = resources.getDimension(R.dimen.tag_bottom_padding);
        if (cornerRadius == -1f) cornerRadius = resources.getDimension(R.dimen.tag_corner_radius);
        if (lineSpacingExtra == -1f) lineSpacingExtra = resources.getDimension(R.dimen.tag_line_spacing_extra);
    }

    /**
     * Colors to use for tag backgrounds.
     */
    private HashMap<String, Integer> colorMap;
    /**
     * RectF to use as background.
     */
    private RectF rect;
    /**
     * Width of tag separator text.
     */
    private float sepTextWidth;

    public TagBackgroundSpan(HashMap<String, Integer> colorMap) {
        this.colorMap = colorMap;
        this.rect = new RectF();
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom,
                               CharSequence text, int start, int end, int lnum) {
        // Measure our tag separator text's width.
        sepTextWidth = p.measureText(C.TAG_SEP);
        int tempColor = p.getColor();
        drawTagBackgrounds(c, p, left, top, baseline, text.toString(), start, end);
        p.setColor(tempColor);
    }

    private void drawTagBackgrounds(Canvas c, Paint p, int x, int y, int baseline, String text, int start, int end) {
        String line = text.substring(start, end);
        boolean endsWithSep = line.endsWith(C.TAG_SEP);
        String[] parts = TAG_SEP_PATTERN.split(line);
        float[] partWidths = measureParts(p, parts);

        float xOffset = 0f;
        for (int i = 0; i < parts.length; i++) {
            p.setColor(getColorForPart(parts[i], text, start));
            float partWidth = partWidths[i];
            drawTagBackground(c, p, x + xOffset, y, baseline, partWidth);
            xOffset += sepTextWidth + partWidth;
        }
    }

    @ColorInt
    private int getColorForPart(String part, String whole, int start) {
        // Try getting it from the hashmap first.
        Integer color = colorMap.get(part);
        if (color != null) return color;
        // If that fails, we clearly have a partial string, and we'll need to get the whole one.
        int partIdx = whole.indexOf(part, start);
        int precedingSepIdx = whole.lastIndexOf(C.TAG_SEP, partIdx);
        int followingSepIdx = whole.indexOf(C.TAG_SEP, partIdx);
        String fullPart = whole.substring(precedingSepIdx != -1 ? precedingSepIdx + 3 : 0, followingSepIdx);
        return colorMap.get(fullPart);
    }

    private void drawTagBackground(Canvas c, Paint p, float x, float y, int baseline, float textWidth) {
        rect.set(x - cornerRadius,
                y,
                x + textWidth + cornerRadius,
                baseline + p.descent() + bottomPadding);
        c.drawRoundRect(rect, cornerRadius, cornerRadius, p);
    }

    private float[] measureParts(Paint p, String[] lineParts) {
        float[] partWidths = new float[lineParts.length];
        for (int i = 0; i < lineParts.length; i++) partWidths[i] = p.measureText(lineParts[i]);
        return partWidths;
    }
}
