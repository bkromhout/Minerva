package com.bkromhout.minerva.events;

import android.support.annotation.IdRes;

/**
 * Fired when some sort of click event happens for a book list card.
 */
public class BookListCardClickEvent {
    /**
     * The various types of clicks that might have happened.
     */
    public enum Type {
        LONG, ACTIONS
    }

    /**
     * Which click type this event is reporting.
     */
    private final Type type;
    /**
     * Name of the list; can used to get a copy of the {@link com.bkromhout.minerva.realm.RBookList}.
     */
    private final String name;
    /**
     * If the type is {@link Type#ACTIONS}, this is ID of the menu item that was clicked.
     */
    private final int actionId;
    /**
     * Position of the book list card in the adapter.
     */
    private final int position;

    /**
     * Create a new {@link BookListCardClickEvent}.
     * @param type Type of click.
     * @param name Name of the list.
     */
    public BookListCardClickEvent(Type type, String name, int position) {
        this(type, name, -1, position);
    }

    /**
     * Create a new {@link BookListCardClickEvent}
     * @param type     Type of click.
     * @param name     Name of the list.
     * @param actionId ID of the clicked menu item (if applicable).
     */
    public BookListCardClickEvent(Type type, String name, @IdRes int actionId, int position) {
        this.type = type;
        this.name = name;
        this.actionId = actionId;
        this.position = position;
    }

    /**
     * Get the type of click this event represents.
     * @return Click type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the name of the list, which can be used to get the corresponding
     * {@link com.bkromhout.minerva.realm.RBookList}.
     * @return List name.
     */
    public String getListName() {
        return name;
    }

    /**
     * If {@code type} is {@link Type#ACTIONS}, gets the ID of the clicked menu item. {@link Type#ACTIONS}.
     * @return Menu item ID, or -1.
     */
    public int getActionId() {
        return actionId;
    }

    /**
     * Get the position of the card in the list.
     * @return Position.
     */
    public int getPosition() {
        return position;
    }
}
