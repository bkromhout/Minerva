package com.bkromhout.minerva.events;

import android.support.annotation.IdRes;

/**
 * Fired when we're missing a permission and need to notify the user of this.
 */
public class MissingPermEvent {
    /**
     * The permission that we are missing.
     */
    private final String permission;
    /**
     * The ID of the action to take if the permission gets granted.
     */
    @IdRes
    private final int actionId;

    /**
     * Create a new {@link MissingPermEvent}.
     * @param permission The permission that we are missing.
     * @param actionId   The action to take if the missing permission is granted.
     */
    public MissingPermEvent(String permission, @IdRes int actionId) {
        this.permission = permission;
        this.actionId = actionId;
    }

    /**
     * Get the missing permission string.
     * @return Missing permission string.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Get the ID of the action to take if the missing permission is granted.
     * @return ID of the action to take if the missing permission is granted.
     */
    @IdRes
    public int getActionId() {
        return actionId;
    }
}
