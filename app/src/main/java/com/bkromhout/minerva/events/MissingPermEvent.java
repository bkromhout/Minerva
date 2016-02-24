package com.bkromhout.minerva.events;

/**
 * Fired when we're missing a permission and need to notify the user of this.
 */
public class MissingPermEvent {
    /**
     * The permission that we are missing.
     */
    public final String permission;

    /**
     * Create a new {@link MissingPermEvent}.
     * @param permission The permission that we are missing.
     */
    public MissingPermEvent(String permission) {
        this.permission = permission;
    }
}
