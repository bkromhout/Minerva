package com.bkp.minerva.events;

import android.view.View;

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
     * If the type is {@link Type#ACTIONS}, this is the view to attach the menu to.
     */
    private final View view;

    /**
     * Create a new {@link BookListCardClickEvent}
     * @param type Type of click.
     * @param name Name of the list.
     * @param view View to attach a popup menu to.
     */
    public BookListCardClickEvent(Type type, String name, View view) {
        this.type = type;
        this.name = name;
        this.view = view;
    }

    /**
     * Create a new {@link BookListCardClickEvent}.
     * @param type Type of click.
     * @param name Name of the list.
     */
    public BookListCardClickEvent(Type type, String name) {
        this(type, name, null);
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

    /**
     * Get the view which would be appropriate to attach a popup menu to. Will be {@code null} if the event type isn't
     * {@link Type#ACTIONS}.
     * @return View to attach a popup menu to.
     */
    public View getView() {
        return view;
    }
}
