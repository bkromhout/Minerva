package com.bkromhout.minerva.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.ColorInt;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import com.binaryfork.spanny.Spanny;
import com.bkromhout.minerva.Minerva;
import com.bkromhout.minerva.realm.RBook;
import com.bkromhout.minerva.realm.RTag;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Draws backgrounds for our tags in a textview. We use this instead of a class which extends {@code ReplacementSpan}
 * because we want the multi-line behavior.
 */
public class TagBackgroundSpan implements LineBackgroundSpan {
    /**
     * String used to separate tag names in the tag string used as a spannable string.
     */
    private static final String TAG_SEP = "\u200B\u2002\u200B";
    /**
     * Length of the tag separator string.
     */
    private static final int TAG_SEP_LEN = TAG_SEP.length();
    /**
     * Pattern to use to split tag strings.
     */
    private static final Pattern TAG_SEP_PATTERN = Pattern.compile("\\Q" + TAG_SEP + "\\E");
    /**
     * Corner radii values for no corners.
     */
    private static final float[] NO_CORNERS = new float[] {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};

    /**
     * Colors to use for tag backgrounds.
     */
    private final HashMap<String, Integer> colorMap;
    /**
     * Number of lines to actually draw for. This was we won't waste time drawing on lines that we don't use.
     */
    private final int maxLines;
    /**
     * Path to reuse each time we draw a tag's background.
     */
    private final Path path;
    /**
     * Width of tag separator text.
     */
    private float sepTextWidth;

    /**
     * Get a SpannableString suitable for displaying in a TextView using the given {@code book}'s {@link RTag}s.
     * @param book     Book whose tags to use.
     * @param maxLines Maximum number of lines to draw backgrounds for.
     * @return A SpannedString.
     */
    public static SpannableString getSpannedTagString(RBook book, int maxLines) {
        Spanny spanny = new Spanny();
        HashMap<String, Integer> colorMap = new HashMap<>(book.tags.size());
        for (RTag tag : book.tags) {
            spanny.append(tag.name, new ForegroundColorSpan(tag.textColor)).append(TAG_SEP);
            colorMap.put(tag.name, tag.bgColor);
        }
        return Spanny.spanText(spanny,
                new LeadingMarginSpan.Standard((int) Minerva.d().TAG_CORNER_RADIUS),
                new TagBackgroundSpan(colorMap, maxLines));
    }

    private TagBackgroundSpan(HashMap<String, Integer> colorMap, int maxLines) {
        this.colorMap = colorMap;
        this.maxLines = maxLines;
        this.path = new Path();
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom,
                               CharSequence text, int start, int end, int lnum) {
        // Don't bother drawing anything if this line is past our maximum number of lines.
        if (lnum >= maxLines) return;
        // Measure our tag separator text's width.
        sepTextWidth = p.measureText(TAG_SEP);
        int tempColor = p.getColor();
        // Draw the tag backgrounds for this line of text. Offset things a bit for the first line.
        drawTagBgs(c, p, left + (int) Minerva.d().TAG_CORNER_RADIUS, top, baseline, text.toString(), start, end);
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
            String part = parts[i];
            p.setColor(getColorForPart(part, text, start));
            float partWidth = partWidths[i];
            // Figure out if we should draw rounded corners on the start and end of this tag's background.
            boolean hasStartCorners = i > 0 || beginsOnLine(part, text, start);
            boolean hasEndCorners = i < parts.length - 1 || endsOnLine(part, text, end - 1);
            // Draw this tag's background.
            drawTagBg(c, p, x + xOffset, y, baseline, partWidth, hasStartCorners, hasEndCorners);
            // Increment the x-offset so that we don't draw on top of the background we just drew next time.
            xOffset += sepTextWidth + partWidth;
        }
    }

    /**
     * Draw the background for one of our tags' text.
     * @param c            Canvas to draw on.
     * @param p            Paint to draw with.
     * @param x            X position to draw at.
     * @param y            Y position to draw at.
     * @param baseline     Text baseline Y-position.
     * @param textWidth    Width of the text which will be drawn.
     * @param startCorners Whether to have rounded start corners. This is true if this tag starts on this line.
     * @param endCorners   Whether to have rounded end corners. This is true if this tag ends on this line.
     */
    private void drawTagBg(Canvas c, Paint p, float x, float y, int baseline, float textWidth, boolean startCorners,
                           boolean endCorners) {
        // Add a rounded rectangle to the path.
        path.addRoundRect(
                // Only leave space for round corners at the start if startCorners is true.
                startCorners ? x - Minerva.d().TAG_CORNER_RADIUS : x,
                y,
                // Only leave space for round corners at the end if endCorners is true.
                endCorners ? x + textWidth + Minerva.d().TAG_CORNER_RADIUS : x + textWidth,
                baseline + p.descent() + Minerva.d().TAG_BOTTOM_PADDING,
                // Use rounded corners based on the values of startCorners and endCorners.
                getCornerRadii(startCorners, endCorners),
                Path.Direction.CW);
        c.drawPath(path, p);
        path.reset();
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
     * Check whether {@code part} begins within the current line, whose start index in {@code whole} is {@code
     * lineStart} (inclusive). (It is assumed that some substring of {@code part} is on the line, so this method doesn't
     * check anything based on the line's end index.).
     * @param part      String part to check.
     * @param whole     Whole paragraph string.
     * @param lineStart Inclusive position where the current line starts at in {@code whole}.
     * @return True if {@code part} begins on the current line.
     */
    private boolean beginsOnLine(String part, String whole, int lineStart) {
        // Figure out where the part's start index is, starting from the line's start index.
        int partSIdx = whole.indexOf(part, lineStart);
        // If this part is the first part of the whole string, clearly it starts on this line.
        if (partSIdx == 0) return true;
        // If the part's start index follows the line's start index, then it starts on this line. (We assume we won't
        // be asked about a part whose start index >= the line's end index.)
        if (partSIdx > lineStart) return true;
        // Figure out the index of the closest preceding tag separator string relative to the part.
        int firstPrecedingSepIdx = whole.lastIndexOf(TAG_SEP, partSIdx);
        // If the start index for this part is the same as the line start index, and it is immediately preceded by a
        // tag separator, then the part starts on this line.
        if (partSIdx == lineStart && firstPrecedingSepIdx == lineStart - TAG_SEP_LEN) return true;
        // Otherwise, this part doesn't start on this line.
        return false;
    }

    /**
     * Check whether {@code part} ends within the current line, whose end index in {@code whole} is {@code lineEnd}
     * (inclusive). (It is assumed that some substring of {@code part} is on the line, so this method doesn't check
     * anything based on the line's start index.).
     * @param part    String part to check.
     * @param whole   Whole paragraph string.
     * @param lineEnd Inclusive position where the current line ends at in {@code whole}.
     * @return True if {@code part} ends on the current line.
     */
    private boolean endsOnLine(String part, String whole, int lineEnd) {
        // Figure out where the part's end index is, starting from the line's end index.
        int partEIdx = whole.lastIndexOf(part, lineEnd) + part.length() - 1;
        // If this part is the last part of the whole string, clearly it ends on this line. (We assume all whole
        // strings end with a tag separator string.)
        if (partEIdx == whole.length() - 1 - TAG_SEP_LEN) return true;
        // If the part's end index precedes the line's end index, then it ends on this line. (We assume we won't
        // be asked about a part whose end index <= the line's start index.)
        if (partEIdx < lineEnd) return true;
        // Figure out the index of the closest following tag separator string relative to the part.
        int firstFollowingSepIdx = whole.indexOf(TAG_SEP, partEIdx);
        // If the end index for this part is the same as the line end index, and it is immediately followed by a
        // tag separator, then the part ends on this line.
        if (partEIdx == lineEnd && firstFollowingSepIdx == lineEnd + 1) return true;
        // Otherwise, this part doesn't end on this line.
        return false;
    }

    /**
     * Return the correct corner radii array depending based on {@code startCorners} and {@code endCorners}.
     * @param startCorners Whether to have rounded start corners.
     * @param endCorners   Whether to have rounded end corners.
     * @return Corner radii array.
     */
    private float[] getCornerRadii(boolean startCorners, boolean endCorners) {
        if (startCorners && endCorners) return Minerva.d().ALL_CORNERS;
        else if (startCorners) return Minerva.d().START_CORNERS_ONLY;
        else if (endCorners) return Minerva.d().END_CORNERS_ONLY;
        else return NO_CORNERS;
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
        int precedingSepIdx = whole.lastIndexOf(TAG_SEP, partIdx);
        int followingSepIdx = whole.indexOf(TAG_SEP, partIdx);
        String fullPart = whole.substring(precedingSepIdx != -1 ? precedingSepIdx + TAG_SEP_LEN : 0, followingSepIdx);
        return colorMap.get(fullPart);
    }
}

