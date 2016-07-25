package com.bkromhout.minerva.events;

import android.support.annotation.IdRes;

/**
 * Fired from various places to make an activity or fragment follow-up on some intent to execute an action.
 */
public class ActionEvent {
    /**
     * The action to take.
     */
    private final int actionId;
    /**
     * Extra data to help take the action.
     */
    private final Object data;
    /**
     * If not -1, a position to update after the action is taken.
     */
    private final int posToUpdate;

    /**
     * Create a new {@link ActionEvent}.
     * @param actionId    Action to take.
     * @param data        Extra data.
     * @param posToUpdate Position to update, or -1 if none.
     */
    public ActionEvent(@IdRes int actionId, Object data, int posToUpdate) {
        this.actionId = actionId;
        this.data = data;
        this.posToUpdate = posToUpdate;
    }

    /**
     * Create a new {@link ActionEvent}.
     * @param actionId Action to take.
     * @param data     Extra data.
     */
    public ActionEvent(@IdRes int actionId, Object data) {
        this(actionId, data, -1);
    }

    public int getActionId() {
        return actionId;
    }

    public Object getData() {
        return data;
    }

    public int getPosToUpdate() {
        return posToUpdate;
    }
}
