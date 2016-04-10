package com.bkromhout.minerva.events;

/**
 * Fired to indicate that listeners should update the view at the position passed in the event.
 */
public class UpdatePosEvent {
    private final int position;

    /**
     * Create a new {@link UpdatePosEvent}.
     * @param position Position which should be updated.
     */
    public UpdatePosEvent(int position) {
        this.position = position;
    }

    /**
     * Get the position to update.
     * @return Position to update.
     */
    public int getPosition() {
        return position;
    }
}
