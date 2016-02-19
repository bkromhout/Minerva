package com.bkp.minerva.events;

/**
 * Fired when some sort of click event happens for a book list card.
 */
public class BookListCardClickEvent {
    /**
     * The various types of clicks that might have happened.
     */
    public enum Type {
        NORMAL, LONG, ACTIONS
    }

    /**
     * Which click type this event is reporting.
     */
    private final Type type;
    /**
     * Name of the list; can used to get a copy of the {@link com.bkp.minerva.realm.RBookList}.
     */
    private final String name;

    /**
     * Create a new {@link BookListCardClickEvent}.
     * @param type Type of click.
     * @param name Name of the list.
     */
    public BookListCardClickEvent(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Get the type of click this event represents.
     * @return Click type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the name of the list, which can be used to get the corresponding {@link com.bkp.minerva.realm.RBookList}.
     * @return List name.
     */
    public String getName() {
        return name;
    }
}
