package com.bkromhout.minerva.events;

/**
 * Fired when one of the tag card action buttons are clicked.
 */
public class TagCardClickEvent {
    /**
     * The various types of clicks that might have happened.
     */
    public enum Type {
        RENAME, DELETE
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
     * Create a new {@link TagCardClickEvent}.
     * @param type Type of click.
     * @param name Name of clicked tag.
     */
    public TagCardClickEvent(Type type, String name) {
        this.type = type;
        this.name = name;
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
}
