package com.bkromhout.minerva.prefs.interfaces;

import com.bkromhout.minerva.enums.BookCardType;

/**
 * Implementers have the ability to persist {@link com.bkromhout.minerva.enums.BookCardType} preferences.
 */
public interface BCTPref {
    // Key Strings.
    String CARD_TYPE = "CARD_TYPE";

    /**
     * Get card type.
     * @param defValue The default value to return if nothing is set.
     * @return Card type.
     */
    BookCardType getCardType(BookCardType defValue);

    /**
     * Put card type.
     * @param cardType Card type.
     */
    void putCardType(BookCardType cardType);
}
