package com.bkromhout.minerva.events;

import android.support.annotation.IdRes;

/**
 * Fired when one of the tag card action buttons are clicked.
 */
public class TagCardClickEvent {
    /**
     * The various types of clicks that might have happened.
     */
    public enum Type {
        TEXT_COLOR, BG_COLOR, ACTIONS
    }

    /**
     * Which click type this event is reporting.
     */
    private final Type type;
    /**
     * Name of the tag; can used to get a copy of the {@link com.bkromhout.minerva.realm.RTag}.
     */
    private final String name;
    /**
     * ID of the action to take.
     */
    private final int actionId;

    /**
     * Create a new {@link TagCardClickEvent}.
     * @param type Type of click.
     * @param name Name of clicked tag.
     */
    public TagCardClickEvent(Type type, String name) {
        this(type, name, -1);
    }

    /**
     * Create a new {@link TagCardClickEvent}.
     * @param type     Type of click.
     * @param name     Name of clicked tag.
     * @param actionId ID of the action clicked.
     */
    public TagCardClickEvent(Type type, String name, @IdRes int actionId) {
        this.type = type;
        this.name = name;
        this.actionId = actionId;
    }

    /**
     * Get the type of click event.
     * @return Click type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the name of the clicked tag card.
     * @return Tag name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the ID of the action clicked.
     * @return Action ID.
     */
    public int getActionId() {
        return actionId;
    }
}
