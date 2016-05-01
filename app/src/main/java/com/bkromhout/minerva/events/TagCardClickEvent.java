package com.bkromhout.minerva.events;

/**
 * Fired when one of the tag card action buttons are clicked.
 */
public class TagCardClickEvent {
    /**
     * The various types of clicks that might have happened.
     */
    public enum Type {
        RENAME, DELETE, TEXT_COLOR, BG_COLOR
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
     * Position of the tag in the adapter. Can be used instead of the {@link #name} to get the {@link
     * com.bkromhout.minerva.realm.RTag} in situations where we know that taking an action won't change the position.
     */
    private final int pos;

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
     * @param type Type of click.
     * @param name Name of clicked tag.
     * @param pos  Position of the clicked tag in the adapter.
     */
    public TagCardClickEvent(Type type, String name, int pos) {
        this.type = type;
        this.name = name;
        this.pos = pos;
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
     * Get the position of the tag in the list. A valid position should only be expected for operations which couldn't
     * possibly cause that position to change.
     * @return Valid position, or -1.
     */
    public int getPos() {
        return pos;
    }
}
