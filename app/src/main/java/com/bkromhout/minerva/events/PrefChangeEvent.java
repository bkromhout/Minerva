package com.bkromhout.minerva.events;

/**
 * Fired to let listener know that a preference has changed.
 */
public class PrefChangeEvent {
    /**
     * Name of the changed preference.
     */
    private final String prefName;

    /**
     * Create a new {@link PrefChangeEvent}.
     * @param prefName Name of the changed preference.
     */
    public PrefChangeEvent(String prefName) {
        this.prefName = prefName;
    }

    /**
     * Get the name of the changed preference.
     * @return Changed preference name.
     */
    public String getPrefName() {
        return prefName;
    }
}
