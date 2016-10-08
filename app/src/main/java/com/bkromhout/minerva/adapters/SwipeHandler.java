package com.bkromhout.minerva.adapters;

/**
 * Implemented by classes which use adapters that support swiping away items.
 */
public interface SwipeHandler {
    /**
     * Called when an item has been swiped away.
     * @param uniqueId The unique ID of the item so that the Realm object can be retrieved.
     */
    void handleSwiped(long uniqueId);
}
