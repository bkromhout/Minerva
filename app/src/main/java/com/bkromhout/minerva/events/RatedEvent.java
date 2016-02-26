package com.bkromhout.minerva.events;

/**
 * Fired when user saves a rating from the rating dialog.
 */
public class RatedEvent {
    /**
     * What the rating value is.
     */
    private final int rating;

    /**
     * Create a new {@link RatedEvent}.
     * @param rating Rating value.
     */
    public RatedEvent(int rating) {
        this.rating = rating;
    }

    /**
     * Get the rating value.
     * @return Rating value.
     */
    public int getRating() {
        return rating;
    }
}
