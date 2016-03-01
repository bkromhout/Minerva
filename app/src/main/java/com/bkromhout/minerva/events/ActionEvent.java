package com.bkromhout.minerva.events;

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
     * Create a new {@link ActionEvent}.
     * @param actionId Action to take.
     * @param data     Extra data.
     */
    public ActionEvent(int actionId, Object data) {
        this.actionId = actionId;
        this.data = data;
    }

    public int getActionId() {
        return actionId;
    }

    public Object getData() {
        return data;
    }
}
