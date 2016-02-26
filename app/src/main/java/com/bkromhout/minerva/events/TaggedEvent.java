package com.bkromhout.minerva.events;

import java.util.ArrayList;

/**
 * Fired when user saves tags from the tagging dialog.
 */
public class TaggedEvent {
    /**
     * Tags which were removed.
     */
    private final ArrayList<String> removedTags;
    /**
     * Tags which were added.
     */
    private final ArrayList<String> addedTags;

    /**
     * Create a new {@link TaggedEvent}.
     * @param removedTags Tags which were removed.
     * @param addedTags   Tags which were added.
     */
    public TaggedEvent(ArrayList<String> removedTags, ArrayList<String> addedTags) {
        this.removedTags = removedTags;
        this.addedTags = addedTags;
    }

    /**
     * Get the list of removed tags.
     * @return List of removed tags.
     */
    public ArrayList<String> getRemovedTags() {
        return removedTags;
    }

    /**
     * Get the list of added tags.
     * @return List of added tags.
     */
    public ArrayList<String> getAddedTags() {
        return addedTags;
    }
}
