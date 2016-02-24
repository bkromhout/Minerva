package com.bkromhout.minerva.events;

/**
 * Fired from various places to make {@link com.bkromhout.minerva.fragments.LibraryFragment} execute some action.
 */
public class LibraryActionEvent {
    /**
     * The action to take.
     */
    private final int actionId;
    /**
     * Extra data to help take the action.
     */
    private final Object data;

    /**
     * Create a new {@link LibraryActionEvent}.
     * @param actionId Action to take.
     * @param data     Extra data.
     */
    public LibraryActionEvent(int actionId, Object data) {
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
