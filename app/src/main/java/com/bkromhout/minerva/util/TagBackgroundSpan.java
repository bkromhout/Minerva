package com.bkromhout.minerva.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.text.style.LineBackgroundSpan;
import com.bkromhout.minerva.C;
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
        if (sidePadding == -1f) sidePadding = C.getDimen(R.dimen.tag_side_padding);
        if (bottomPadding == -1f) bottomPadding = C.getDimen(R.dimen.tag_bottom_padding);
        if (cornerRadius == -1f) cornerRadius = C.getDimen(R.dimen.tag_corner_radius);
        if (lineSpacingExtra == -1f) lineSpacingExtra = C.getDimen(R.dimen.tag_line_spacing_extra);
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
        // Draw the tag backgrounds for this line of text. Offset things a bit for the first line.
        drawTagBgs(c, p, lnum != 0 ? left : left + (int) cornerRadius, top, baseline, text.toString(), start, end);
        p.setColor(tempColor);
    }

    /**
     * Draw the tag backgrounds for the current line.
     * @param c        Canvas to draw on.
     * @param p        Paint to draw with.
     * @param x        X position to start drawing backgrounds at.
     * @param y        Y position to start drawing backgrounds at.
     * @param baseline Text baseline Y-position.
     * @param text     All of the text in the paragraph.
     * @param start    Inclusive position to start at in {@code text} for this line.
     * @param end      Exclusive position to end at in {@code text} for this line.
     */
    private void drawTagBgs(Canvas c, Paint p, int x, int y, int baseline, String text, int start, int end) {
        // We only care about the text for the current line, so get that first, then split it into parts using the
        // tag separator string, and measure those parts so that we can draw the individual backgrounds.
        String[] parts = TAG_SEP_PATTERN.split(text.substring(start, end));
        float[] partWidths = measureLineParts(p, parts);

        float xOffset = 0f;
        for (int i = 0; i < parts.length; i++) {
            p.setColor(getColorForPart(parts[i], text, start));
            float partWidth = partWidths[i];
            drawTagBg(c, p, x + xOffset, y, baseline, partWidth);
            xOffset += sepTextWidth + partWidth;
        }
    }

    /**
     * Draw the background for one of our tags' text.
     * @param c         Canvas to draw on.
     * @param p         Paint to draw with.
     * @param x         X position to draw at.
     * @param y         Y position to draw at.
     * @param baseline  Text baseline Y-position.
     * @param textWidth Width of the text which will be drawn.
     */
    private void drawTagBg(Canvas c, Paint p, float x, float y, int baseline, float textWidth) {
        rect.set(x - cornerRadius,
                y,
                x + textWidth + cornerRadius,
                baseline + p.descent() + bottomPadding);
        c.drawRoundRect(rect, cornerRadius, cornerRadius, p);
    }

    /**
     * Measure all of the parts of the line which we'll need to draw individual tag backgrounds for.
     * @param p         Paint to use to measure text.
     * @param lineParts Line part strings to measure the width of.
     * @return Array of line part string widths.
     */
    private float[] measureLineParts(Paint p, String[] lineParts) {
        float[] partWidths = new float[lineParts.length];
        for (int i = 0; i < lineParts.length; i++) partWidths[i] = p.measureText(lineParts[i]);
        return partWidths;
    }

    /**
     * Get the color to use for the background of the tag whose name is {@code part}. If {@code part} isn't a full tag
     * name, this will use {@code whole} and {@code start} to figure out what the full tag name is first.
     * @param part  String to try and use to get the color initially.
     * @param whole String to find {@code part} in if we need to.
     * @param start Position to start looking for {@code part} in {@code whole} at.
     * @return Background color for the tag.
     */
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
}
