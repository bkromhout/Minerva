package com.bkromhout.minerva.adapters;

/**
 * Delegates getting the text for a fast scroller's bubble to the implementer.
 */
public interface BubbleTextDelegate {
    String getBubbleText(int position);
}
