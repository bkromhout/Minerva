package com.bkromhout.minerva.events;

import android.support.annotation.IdRes;

/**
 * Event fired to indicate to subscribers that a certain permission has been granted.
 */
public class PermGrantedEvent {
    private final String permission;
    @IdRes
    private final int actionId;

    public PermGrantedEvent(String permission, @IdRes int actionId) {
        this.permission = permission;
        this.actionId = actionId;
    }

    /**
     * Get the permission which was granted.
     * @return Granted permission string.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Get the ID of the action which should now be performed.
     * @return Action ID.
     */
    public int getActionId() {
        return actionId;
    }
}
